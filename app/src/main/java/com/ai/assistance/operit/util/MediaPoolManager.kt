package com.ai.assistance.operit.util

import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.util.UUID

object MediaPoolManager {
    private const val TAG = "MediaPoolManager"

    private const val MAX_INPUT_BYTES = 20 * 1024 * 1024

    var maxPoolSize = 12
        set(value) {
            if (value > 0) {
                field = value
                AppLogger.d(TAG, "池子大小限制已更新为: $value")
            }
        }

    private var cacheDir: File? = null

    data class MediaData(
        val base64: String,
        val mimeType: String
    )

    fun initialize(cacheDirPath: File) {
        cacheDir = File(cacheDirPath, "media_pool")
        if (!cacheDir!!.exists()) {
            cacheDir!!.mkdirs()
            AppLogger.d(TAG, "创建媒体缓存目录: ${cacheDir!!.absolutePath}")
        }
        loadFromDisk()
    }

    private val mediaPool = object : LinkedHashMap<String, MediaData>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MediaData>?): Boolean {
            val shouldRemove = size > maxPoolSize
            if (shouldRemove && eldest != null) {
                AppLogger.d(TAG, "池子已满，移除最旧的媒体: ${eldest.key}")
                deleteFromDisk(eldest.key)
            }
            return shouldRemove
        }
    }

    @Synchronized
    fun addMedia(filePath: String, mimeType: String): String {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) {
                AppLogger.e(TAG, "文件不存在或不是文件: $filePath")
                return "error"
            }

            val bytes = try {
                FileInputStream(file).use { it.readBytes() }
            } catch (e: Exception) {
                AppLogger.e(TAG, "读取文件失败", e)
                return "error"
            }

            if (bytes.size > MAX_INPUT_BYTES) {
                AppLogger.e(TAG, "媒体文件过大，拒绝加入池子: $filePath, bytes=${bytes.size}")
                return "error"
            }

            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val id = UUID.randomUUID().toString()
            val mediaData = MediaData(base64 = base64, mimeType = mimeType)
            mediaPool[id] = mediaData
            saveToDisk(id, mediaData)
            AppLogger.d(TAG, "成功添加媒体到池子: $id, mimeType=$mimeType, sizeBytes=${bytes.size}")
            id
        } catch (e: Exception) {
            AppLogger.e(TAG, "添加媒体时发生异常: $filePath", e)
            "error"
        }
    }

    @Synchronized
    fun addMediaFromBase64(base64: String, mimeType: String): String {
        return try {
            val id = UUID.randomUUID().toString()
            val mediaData = MediaData(base64 = base64, mimeType = mimeType)
            mediaPool[id] = mediaData
            saveToDisk(id, mediaData)
            AppLogger.d(TAG, "成功从base64添加媒体到池子: $id, mimeType=$mimeType, sizeChars=${base64.length}")
            id
        } catch (e: Exception) {
            AppLogger.e(TAG, "从base64添加媒体时发生异常", e)
            "error"
        }
    }

    @Synchronized
    fun getMedia(id: String): MediaData? {
        return mediaPool[id]
    }

    @Synchronized
    fun removeMedia(id: String) {
        mediaPool.remove(id)
        deleteFromDisk(id)
    }

    private fun saveToDisk(id: String, data: MediaData) {
        val dir = cacheDir ?: return
        try {
            val metaFile = File(dir, "$id.meta")
            val b64File = File(dir, "$id.b64")
            metaFile.writeText(data.mimeType)
            b64File.writeText(data.base64)
        } catch (e: Exception) {
            AppLogger.e(TAG, "保存媒体到磁盘失败: $id", e)
        }
    }

    private fun deleteFromDisk(id: String) {
        val dir = cacheDir ?: return
        try {
            File(dir, "$id.meta").delete()
            File(dir, "$id.b64").delete()
        } catch (_: Exception) {
        }
    }

    private fun loadFromDisk() {
        val dir = cacheDir ?: return
        try {
            val metaFiles = dir.listFiles { file -> file.isFile && file.name.endsWith(".meta") } ?: return
            metaFiles.forEach { metaFile ->
                val id = metaFile.name.removeSuffix(".meta")
                val b64File = File(dir, "$id.b64")
                if (!b64File.exists()) {
                    return@forEach
                }
                val mimeType = runCatching { metaFile.readText().trim() }.getOrNull() ?: return@forEach
                val base64 = runCatching { b64File.readText().trim() }.getOrNull() ?: return@forEach
                if (mimeType.isBlank() || base64.isBlank()) {
                    return@forEach
                }
                mediaPool[id] = MediaData(base64 = base64, mimeType = mimeType)
            }
            AppLogger.d(TAG, "从磁盘加载媒体缓存完成: size=${mediaPool.size}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "从磁盘加载媒体缓存失败", e)
        }
    }
}
