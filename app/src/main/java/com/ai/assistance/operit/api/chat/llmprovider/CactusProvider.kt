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
import com.cactuscompute.cactus.Cactus
import com.cactuscompute.cactus.CactusCompletionParams
import com.cactuscompute.cactus.CactusInitParams
import com.cactuscompute.cactus.ChatMessage
import com.cactuscompute.cactus.InferenceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cactus Compute SDK provider for on-device LLM inference.
 * 
 * Supported models:
 * - Qwen3 0.6B (default), Qwen2.5 0.5B, Gemma3 270M/1B
 * - SmolLM2 360M, LFM2 1B/3B
 * 
 * Features:
 * - Local/Remote inference modes (LOCAL, REMOTE, LOCAL_FIRST, REMOTE_FIRST)
 * - Function calling
 * - Vision/Multimodal support
 * - Text embeddings
 */
class CactusProvider(
    private val context: Context,
    private val modelName: String,
    private val threadCount: Int = 4,
    private val contextSize: Int = 2048,
    private val inferenceMode: String = "LOCAL_FIRST",
    private val cactusToken: String = "",
    private val providerType: ApiProviderType = ApiProviderType.CACTUS
) : AIService {

    companion object {
        private const val TAG = "CactusProvider"

        fun getModelsDir(): File {
            return File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Operit/models/cactus"
            )
        }

        fun getDefaultModels(): List<ModelOption> = listOf(
            ModelOption("qwen3-0.6", "Qwen3 0.6B (Default)", false),
            ModelOption("qwen2.5-0.5b", "Qwen2.5 0.5B", false),
            ModelOption("gemma3-270m", "Gemma3 270M", false),
            ModelOption("gemma3-1b", "Gemma3 1B", false),
            ModelOption("smollm2-360m", "SmolLM2 360M", false),
            ModelOption("lfm2-1b", "LFM2 1B", false),
            ModelOption("lfm2-3b", "LFM2 3B", false)
        )

        private var cactusInstance: Cactus? = null
        private var isInitialized = false

        private fun getInferenceMode(mode: String): InferenceMode {
            return when (mode.uppercase()) {
                "LOCAL" -> InferenceMode.LOCAL
                "REMOTE" -> InferenceMode.REMOTE
                "LOCAL_FIRST" -> InferenceMode.LOCAL_FIRST
                "REMOTE_FIRST" -> InferenceMode.REMOTE_FIRST
                else -> InferenceMode.LOCAL_FIRST
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

    override val providerModel: String
        get() = "${providerType.name}:$modelName"

    private fun initializeCactus() {
        if (!isInitialized) {
            val initParams = CactusInitParams(
                modelsDir = getModelsDir().absolutePath,
                threadCount = threadCount,
                contextSize = contextSize,
                inferenceMode = getInferenceMode(inferenceMode),
                cloudToken = if (cactusToken.isNotEmpty()) cactusToken else null
            )
            cactusInstance = Cactus.initialize(initParams)
            isInitialized = true
        }
    }

    override fun resetTokenCounts() {
        _inputTokenCount = 0
        _outputTokenCount = 0
        _cachedInputTokenCount = 0
    }

    override fun cancelStreaming() {
        isCancelled = true
        cactusInstance?.cancelGeneration()
    }

    override fun release() {
        cactusInstance?.release()
        cactusInstance = null
        isInitialized = false
    }

    override suspend fun getModelsList(context: Context): Result<List<ModelOption>> {
        return Result.success(getDefaultModels())
    }

    override suspend fun testConnection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            initializeCactus()
            if (cactusInstance != null) {
                Result.success("Cactus SDK connected successfully!\nModel: $modelName")
            } else {
                Result.failure(Exception("Failed to initialize Cactus SDK"))
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
        initializeCactus()
        
        val messages = chatHistory.map { (role, content) ->
            ChatMessage(role = role, content = content)
        } + ChatMessage(role = "user", content = message)

        val params = CactusCompletionParams(
            model = modelName,
            messages = messages,
            maxTokens = modelParameters.find { it.name == "maxTokens" }?.value as? Int ?: 4096,
            temperature = modelParameters.find { it.name == "temperature" }?.value as? Float ?: 0.7f,
            stream = stream
        )

        try {
            cactusInstance?.let { cactus ->
                val response = cactus.complete(params) { chunk ->
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
                emit("Error: Cactus SDK not initialized")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Cactus inference error", e)
            emit("Error: ${e.message}")
        }
    }
}
