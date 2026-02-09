package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import android.os.Environment
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.data.model.ModelOption
import com.ai.assistance.operit.data.model.ModelParameter
import com.ai.assistance.operit.data.model.ToolPrompt
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.stream.Stream
import com.ai.assistance.operit.util.stream.stream
import com.runanywhere.sdk.runanywherekotlin.Runanywhere
import com.runanywhere.sdk.runanywherekotlin.LLMConfig
import com.runanywhere.sdk.runanywherekotlin.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Runanywhere SDK provider for on-device AI (LLM, STT, TTS).
 * 
 * Supported models:
 * - LLM: Phi-4-mini (3.8B), Phi-3.5-mini, Gemma-2-2B, Qwen2.5-0.5B
 * - STT: Whisper, Moonshine, Paraformer-zh
 * - TTS: ChatTTS, CosyVoice
 * 
 * Features:
 * - Local/Remote inference modes
 * - Speech-to-Text
 * - Text-to-Speech
 * - Function calling
 */
class RunanywhereProvider(
    private val context: Context,
    private val modelName: String,
    private val threadCount: Int = 4,
    private val contextSize: Int = 4096,
    private val inferenceMode: String = "LOCAL_FIRST",
    private val runanywhereToken: String = "",
    private val providerType: ApiProviderType = ApiProviderType.RUNANYWHERE
) : AIService {

    companion object {
        private const val TAG = "RunanywhereProvider"

        fun getModelsDir(): File {
            return File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Operit/models/runanywhere"
            )
        }

        fun getDefaultModels(): List<ModelOption> = listOf(
            ModelOption("phi-4-mini", "Phi-4-mini 3.8B (Default)", false),
            ModelOption("phi-3.5-mini", "Phi-3.5-mini", false),
            ModelOption("gemma-2-2b", "Gemma-2 2B", false),
            ModelOption("qwen2.5-0.5b", "Qwen2.5 0.5B", false),
            ModelOption("whisper", "Whisper STT", false),
            ModelOption("moonshine", "Moonshine STT", false),
            ModelOption("paraformer-zh", "Paraformer-zh STT", false),
            ModelOption("chattts", "ChatTTS TTS", false),
            ModelOption("cosyvoice", "CosyVoice TTS", false)
        )

        private var runanywhereInstance: Runanywhere? = null
        private var isInitialized = false

        private fun getLLMBackend(modelName: String): String {
            return when {
                modelName.contains("whisper", ignoreCase = true) -> "WHISPER"
                modelName.contains("moonshine", ignoreCase = true) -> "MOONSHINE"
                modelName.contains("paraformer", ignoreCase = true) -> "PARAFORMER"
                modelName.contains("chattts", ignoreCase = true) -> "CHATTTS"
                modelName.contains("cosyvoice", ignoreCase = true) -> "COSYVOICE"
                else -> "LLAMACPP" // Default LLM backend
            }
        }
    }

    private var _inputTokenCount: Int = 0
    private var _outputTokenCount: Int = 0
    private var _cachedInputTokenCount: Int = 0

    @Volatile
    private var isCancelled = false

    override val inputTokenCount: Int
        get() = _inputTokenCount

    override val cachedInputTokenCount: Int
        get() = _cachedInputTokenCount

    override val outputTokenCount: Int
        get() = _outputTokenCount

    override fun resetTokenCounts() {
        _inputTokenCount = 0
        _outputTokenCount = 0
        _cachedInputTokenCount = 0
    }

    override fun cancelStreaming() {
        isCancelled = true
        runanywhereInstance?.cancelGeneration()
    }

    override fun release() {
        runanywhereInstance?.release()
        runanywhereInstance = null
        isInitialized = false
    }

    override val providerModel: String
        get() = "${providerType.name}:$modelName"

    private fun initializeRunanywhere() {
        if (!isInitialized) {
            val backend = getLLMBackend(modelName)
            val config = LLMConfig(
                modelPath = File(getModelsDir(), modelName).absolutePath,
                contextSize = contextSize,
                threadCount = threadCount,
                useCloud = inferenceMode.uppercase() in listOf("REMOTE", "REMOTE_FIRST"),
                cloudToken = if (runanywhereToken.isNotEmpty()) runanywhereToken else null
            )
            runanywhereInstance = Runanywhere.initialize(config)
            isInitialized = true
        }
    }

    override suspend fun getModelsList(context: Context): Result<List<ModelOption>> {
        return Result.success(getDefaultModels())
    }

    override suspend fun testConnection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            initializeRunanywhere()
            if (runanywhereInstance != null) {
                Result.success("Runanywhere SDK connected successfully!\nModel: $modelName")
            } else {
                Result.failure(Exception("Failed to initialize Runanywhere SDK"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculateInputTokens(
        message: String,
        chatHistory: List<Pair<String, String>>,
        availableTools: List<ToolPrompt>?
    ): Int {
        return withContext(Dispatchers.IO) {
            val totalChars = chatHistory.sumOf { it.second.length } + message.length
            totalChars / 4
        }
    }

    override suspend fun sendMessage(
        context: Context,
        message: String,
        chatHistory: List<Pair<String, String>>,
        modelParameters: List<ModelParameter<*>>,
        enableThinking: Boolean,
        stream: Boolean,
        availableTools: List<ToolPrompt>?,
        preserveThinkInHistory: Boolean,
        onTokensUpdated: suspend (input: Int, cachedInput: Int, output: Int) -> Unit,
        onNonFatalError: suspend (error: String) -> Unit
    ): Stream<String> = stream {
        initializeRunanywhere()

        val messages = chatHistory.map { (role, content) ->
            ChatMessage(role = role, content = content)
        } + ChatMessage(role = "user", content = message)

        val config = LLMConfig(
            modelPath = File(getModelsDir(), modelName).absolutePath,
            contextSize = contextSize,
            threadCount = threadCount,
            maxTokens = modelParameters.find { it.name == "maxTokens" }?.value as? Int ?: 4096,
            temperature = modelParameters.find { it.name == "temperature" }?.value as? Float ?: 0.7f,
            stream = stream
        )

        try {
            runanywhereInstance?.let { runanywhere ->
                val response = runanywhere.complete(config, messages) { chunk ->
                    if (!isCancelled) {
                        _outputTokenCount++
                        onTokensUpdated(_inputTokenCount, _cachedInputTokenCount, _outputTokenCount)
                        emit(chunk)
                    }
                }
                // For non-streaming, emit the full response
                if (!stream && response != null) {
                    emit(response)
                }
            } ?: run {
                emit("Error: Runanywhere SDK not initialized")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Runanywhere inference error", e)
            emit("Error: ${e.message}")
        }
    }
}
