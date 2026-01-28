package com.ai.assistance.operit.data.backup

import android.content.Context
import android.net.Uri
import com.ai.assistance.operit.data.db.AppDatabase
import com.ai.assistance.operit.data.db.ObjectBoxManager
import com.ai.assistance.operit.util.AppLogger
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RawSnapshotBackupManager {

    private const val TAG = "RawSnapshotBackup"
    private const val FORMAT_VERSION = 1

    private const val ZIP_PREFIX = "operit_raw_snapshot_"

    private const val ENTRY_MANIFEST = "manifest.json"
    private const val ENTRY_PAYLOAD_PREFIX = "payload/"

    private const val ENTRY_FILES = "payload/files/"
    private const val ENTRY_SHARED_PREFS = "payload/shared_prefs/"
    private const val ENTRY_DATASTORE = "payload/datastore/"
    private const val ENTRY_DATABASES = "payload/databases/"

    private val terminalTopLevelDirNames = setOf("usr", "tmp", "bin")

    private val mutex = Mutex()

    @Serializable
    data class Manifest(
        val formatVersion: Int,
        val packageName: String,
        val createdAt: Long,
        val includes: List<String>,
        val includeTerminalData: Boolean = true
    )

    data class SnapshotOptions(
        val includeTerminalData: Boolean = false
    )

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun exportToBackupDir(
        context: Context,
        options: SnapshotOptions = SnapshotOptions()
    ): File = withContext(Dispatchers.IO) {
        mutex.withLock {
            AppLogger.i(TAG, "export start (includeTerminalData=${options.includeTerminalData})")
            val exportDir = OperitBackupDirs.rawSnapshotDir()
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val outFile = File(exportDir, "$ZIP_PREFIX$timestamp.zip")
            val tmpFile = File(exportDir, "${outFile.name}.tmp")

            if (tmpFile.exists()) {
                tmpFile.delete()
            }

            val dataDir = context.dataDir
            val sharedPrefsDir = File(dataDir, "shared_prefs")
            val datastoreDir = File(dataDir, "datastore")
            val databasesDir = File(dataDir, "databases")

            try {
                val sqliteDb = AppDatabase.getDatabase(context).openHelper.writableDatabase
                sqliteDb.query("PRAGMA wal_checkpoint(FULL)").close()
            } catch (e: Exception) {
                AppLogger.w(TAG, "wal_checkpoint failed", e)
            }

            val includes = listOf(ENTRY_FILES, ENTRY_SHARED_PREFS, ENTRY_DATASTORE, ENTRY_DATABASES)
            val manifest = Manifest(
                formatVersion = FORMAT_VERSION,
                packageName = context.packageName,
                createdAt = System.currentTimeMillis(),
                includes = includes,
                includeTerminalData = options.includeTerminalData
            )

            ZipOutputStream(BufferedOutputStream(FileOutputStream(tmpFile))).use { zos ->
                zos.putNextEntry(ZipEntry(ENTRY_MANIFEST))
                zos.write(json.encodeToString(manifest).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                val excludedNames = if (options.includeTerminalData) emptySet() else terminalTopLevelDirNames
                val filesMs = measureTimeMillis {
                    addDirToZip(
                        zos = zos,
                        dir = context.filesDir,
                        entryPrefix = ENTRY_FILES,
                        excludedTopLevelDirNames = excludedNames
                    )
                }
                AppLogger.i(TAG, "export add files done in ${filesMs}ms (excludedTopLevel=${excludedNames.size})")

                val sharedPrefsMs = measureTimeMillis { addDirToZip(zos, sharedPrefsDir, ENTRY_SHARED_PREFS) }
                AppLogger.i(TAG, "export add shared_prefs done in ${sharedPrefsMs}ms")

                val datastoreMs = measureTimeMillis { addDirToZip(zos, datastoreDir, ENTRY_DATASTORE) }
                AppLogger.i(TAG, "export add datastore done in ${datastoreMs}ms")

                val databasesMs = measureTimeMillis { addDirToZip(zos, databasesDir, ENTRY_DATABASES) }
                AppLogger.i(TAG, "export add databases done in ${databasesMs}ms")
            }

            if (outFile.exists()) {
                outFile.delete()
            }

            if (!tmpFile.renameTo(outFile)) {
                tmpFile.copyTo(outFile, overwrite = true)
                tmpFile.delete()
            }

            AppLogger.i(TAG, "export done: ${outFile.absolutePath} (${outFile.length()} bytes)")
            outFile
        }
    }

    suspend fun restoreFromBackupUri(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cacheZip = File.createTempFile("raw_snapshot_restore_", ".zip", context.cacheDir)
            val workDir = File(context.cacheDir, "raw_snapshot_restore_work").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            try {
                AppLogger.i(TAG, "restore start uri=$uri")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheZip).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Failed to open uri")

                AppLogger.i(TAG, "restore cached zip: ${cacheZip.absolutePath} (${cacheZip.length()} bytes)")

                AppDatabase.closeDatabase()
                ObjectBoxManager.closeAll()

                AppLogger.i(TAG, "restore closed databases (room + objectbox)")

                val manifest = extractZipToWorkDir(cacheZip, workDir, expectedPackageName = context.packageName)

                val payloadDir = File(workDir, "payload")

                val preserveTerminal = !manifest.includeTerminalData
                val preservedNames = if (preserveTerminal) terminalTopLevelDirNames else emptySet()

                AppLogger.i(
                    TAG,
                    "restore manifest ok (formatVersion=${manifest.formatVersion}, includeTerminalData=${manifest.includeTerminalData})"
                )

                AppLogger.i(TAG, "restore replace dirs (preserveTerminalTopLevel=${preservedNames.isNotEmpty()})")

                replaceDirContents(File(payloadDir, "files"), context.filesDir, preservedTopLevelDirNames = preservedNames)
                replaceDirContents(File(payloadDir, "shared_prefs"), File(context.dataDir, "shared_prefs"))
                replaceDirContents(File(payloadDir, "datastore"), File(context.dataDir, "datastore"))
                replaceDirContents(File(payloadDir, "databases"), File(context.dataDir, "databases"))

                AppLogger.i(TAG, "restore done: ${manifest.packageName}")
            } catch (e: Exception) {
                AppLogger.e(TAG, "restore failed", e)
                throw e
            } finally {
                try {
                    cacheZip.delete()
                } catch (_: Exception) {
                }
                try {
                    workDir.deleteRecursively()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun extractZipToWorkDir(zipFile: File, workDir: File, expectedPackageName: String): Manifest {
        val payloadRoot = File(workDir, "payload")
        payloadRoot.mkdirs()

        var manifestText: String? = null
        var extractedPayloadFiles = 0

        val buffer = ByteArray(64 * 1024)
        val extractMs = measureTimeMillis {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    val name = entry.name

                    if (entry.isDirectory) {
                        zis.closeEntry()
                        continue
                    }

                    if (name == ENTRY_MANIFEST) {
                        val bytes = zis.readBytesSafely(maxBytes = 512 * 1024)
                        manifestText = bytes.toString(Charsets.UTF_8)
                        zis.closeEntry()
                        continue
                    }

                    if (!name.startsWith(ENTRY_PAYLOAD_PREFIX)) {
                        zis.closeEntry()
                        continue
                    }

                    val target = File(workDir, name)
                    val workCanonical = workDir.canonicalFile
                    val targetCanonical = target.canonicalFile
                    if (!targetCanonical.path.startsWith(workCanonical.path + File.separator)) {
                        zis.closeEntry()
                        throw IllegalArgumentException("Invalid zip entry path: $name")
                    }

                    target.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(target)).use { output ->
                        while (true) {
                            val read = zis.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                        }
                    }

                    extractedPayloadFiles++

                    zis.closeEntry()
                }
            }
        }

        AppLogger.i(TAG, "restore extract done in ${extractMs}ms (payloadFiles=$extractedPayloadFiles)")

        val manifest = manifestText?.let { json.decodeFromString(Manifest.serializer(), it) }
            ?: throw IllegalArgumentException("Invalid backup zip: missing $ENTRY_MANIFEST")

        if (manifest.formatVersion != FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: ${manifest.formatVersion}")
        }

        if (manifest.packageName != expectedPackageName) {
            throw IllegalArgumentException("Backup package mismatch: ${manifest.packageName}")
        }

        return manifest
    }

    private fun addDirToZip(
        zos: ZipOutputStream,
        dir: File,
        entryPrefix: String,
        excludedTopLevelDirNames: Set<String> = emptySet()
    ) {
        if (!dir.exists() || !dir.isDirectory) return

        val baseCanonical = dir.canonicalFile
        val buffer = ByteArray(64 * 1024)

        dir.walkTopDown().forEach { f ->
            if (!f.isFile) return@forEach

            val canonical = f.canonicalFile
            if (!canonical.path.startsWith(baseCanonical.path + File.separator)) return@forEach

            val rel = canonical.path.substring(baseCanonical.path.length + 1)
            if (excludedTopLevelDirNames.isNotEmpty()) {
                val relNormalized = rel.replace(File.separatorChar, '/')
                val top = relNormalized.substringBefore('/', missingDelimiterValue = relNormalized)
                if (excludedTopLevelDirNames.contains(top)) {
                    return@forEach
                }
            }
            val entryName = entryPrefix + rel.replace(File.separatorChar, '/')

            zos.putNextEntry(ZipEntry(entryName))
            BufferedInputStream(FileInputStream(canonical)).use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    zos.write(buffer, 0, read)
                }
            }
            zos.closeEntry()
        }
    }

    private fun replaceDirContents(
        fromDir: File,
        toDir: File,
        preservedTopLevelDirNames: Set<String> = emptySet()
    ) {
        if (toDir.exists()) {
            toDir.listFiles()?.forEach { child ->
                if (preservedTopLevelDirNames.contains(child.name)) return@forEach
                child.deleteRecursively()
            }
        } else {
            toDir.mkdirs()
        }

        if (!fromDir.exists() || !fromDir.isDirectory) return

        copyDir(fromDir, toDir, preservedTopLevelDirNames)
    }

    private fun copyDir(
        fromDir: File,
        toDir: File,
        preservedTopLevelDirNames: Set<String>
    ) {
        val baseCanonical = fromDir.canonicalFile
        fromDir.walkTopDown().forEach { f ->
            val canonical = f.canonicalFile
            if (!canonical.path.startsWith(baseCanonical.path + File.separator) && canonical != baseCanonical) {
                return@forEach
            }

            if (canonical == baseCanonical) return@forEach

            val rel = canonical.path.substring(baseCanonical.path.length + 1)
            if (preservedTopLevelDirNames.isNotEmpty()) {
                val relNormalized = rel.replace(File.separatorChar, '/')
                val top = relNormalized.substringBefore('/', missingDelimiterValue = relNormalized)
                if (preservedTopLevelDirNames.contains(top)) {
                    return@forEach
                }
            }
            val target = File(toDir, rel)

            if (canonical.isDirectory) {
                target.mkdirs()
            } else if (canonical.isFile) {
                target.parentFile?.mkdirs()
                canonical.inputStream().use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun ZipInputStream.readBytesSafely(maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            if (out.size() + read > maxBytes) {
                throw IllegalArgumentException("Zip entry too large")
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}
