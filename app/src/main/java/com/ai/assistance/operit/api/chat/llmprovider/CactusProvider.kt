package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.data.model.ModelOption
import com.ai.assistance.operit.data.model.ModelParameter
import com.ai.assistance.operit.data.model.ToolPrompt
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.stream.Stream
import com.ai.assistance.operit.util.stream.stream
import com.cactus.CactusLM
import com.cactus.CactusInitParams
import com.cactus.CactusCompletionParams
import com.cactus.ChatMessage
import com.cactus.InferenceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Cactus Compute SDK provider for on-device LLM inference.
 * 
 * This provider supports local LLM inference using the Cactus Compute SDK.
 * Models are downloaded to the device's storage and loaded for inference.
 * 
 * Supported models (from Cactus SDK):
 * - Qwen3 (0.6B, 1.7B, 4B, 8B, 14B)
 * - Gemma3 (270M, 1B, 4B)
 * - LFM2 (350M, 700M, 2.6B)
 * - LFM2.5 (1.2B)
 * 
 * Features:
 * - Local inference (no internet required after model download)
 * - Model download with progress tracking
 * - Streaming text generation
 * - Configurable parameters (temperature, maxTokens, etc.)
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
        
        // Map of model names to Cactus model slugs
        private val MODEL_SLUG_MAP = mapOf(
            // Qwen2.5 models
            "Qwen/Qwen2.5-0.5B-Instruct" to "qwen2.5-0.5",
            "Qwen/Qwen2.5-1.5B-Instruct" to "qwen2.5-1.5",
            "Qwen/Qwen2.5-3B-Instruct" to "qwen2.5-3b",
            "Qwen/Qwen2.5-7B-Instruct" to "qwen2.5-7b",
            "Qwen/Qwen2.5-14B-Instruct" to "qwen2.5-14b",
            // Qwen3 models
            "Qwen/Qwen3-0.6B" to "qwen3-0.6",
            "Qwen/Qwen3-1.7B" to "qwen3-1.7",
            "Qwen/Qwen3-4B" to "qwen3-4b",
            "Qwen/Qwen3-8B" to "qwen3-8b",
            "Qwen/Qwen3-14B" to "qwen3-14b",
            // Gemma3 models
            "Google/gemma-3-270M" to "gemma3-270m",
            "Google/gemma-3-1B" to "gemma3-1b",
            "Google/gemma-3-4B" to "gemma3-4b",
            // LFM models
            "Liquid/lfm-2-350m" to "lfm2-350m",
            "Liquid/lfm-2-700m" to "lfm2-700m",
            "Liquid/lfm-2-2.6b" to "lfm2-2.6b",
            "Liquid/lfm-2.5-1.2b" to "lfm2.5-1.2b",
        )
        
        // Singleton CactusLM instance for model reuse
        @Volatile
        private var cactusLM: CactusLM? = null
        
        @Volatile
        private var loadedModelSlug: String? = null
        
        /**
         * Get the Cactus model slug from the model name/id
         */
        fun getModelSlug(modelName: String): String {
            return MODEL_SLUG_MAP[modelName] ?: run {
                // Try to extract slug from model name
                val lower = modelName.lowercase()
                when {
                    lower.contains("qwen2.5") && lower.contains("0.5") -> "qwen2.5-0.5"
                    lower.contains("qwen2.5") && lower.contains("1.5") -> "qwen2.5-1.5"
                    lower.contains("qwen2.5") && lower.contains("3b") -> "qwen2.5-3b"
                    lower.contains("qwen2.5") && lower.contains("7b") -> "qwen2.5-7b"
                    lower.contains("qwen3") && lower.contains("0.6") -> "qwen3-0.6"
                    lower.contains("qwen3") && lower.contains("1.7") -> "qwen3-1.7"
                    lower.contains("gemma3") && lower.contains("270m") -> "gemma3-270m"
                    lower.contains("gemma3") && lower.contains("1b") -> "gemma3-1b"
                    lower.contains("lfm2.5") && lower.contains("1.2b") -> "lfm2.5-1.2b"
                    lower.contains("lfm2") && lower.contains("350m") -> "lfm2-350m"
                    else -> "qwen3-0.6" // Default model
                }
            }
        }
        
        /**
         * Get or create the CactusLM instance
         */
        private fun getOrCreateCactusLM(): CactusLM {
            return cactusLM ?: synchronized(this) {
                cactusLM ?: CactusLM().also { cactusLM = it }
            }
        }
        
        /**
         * Initialize the model if not already loaded
         */
        suspend fun initializeModel(modelSlug: String, contextSize: Int): Result<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    val lm = getOrCreateCactusLM()
                    
                    // Check if already loaded
                    if (loadedModelSlug == modelSlug) {
                        return@withContext Result.success(Unit)
                    }
                    
                    // Initialize with the model
                    lm.initializeModel(
                        CactusInitParams(
                            model = modelSlug,
                            contextSize = contextSize
                        )
                    )
                    
                    loadedModelSlug = modelSlug
                    AppLogger.d(TAG, "Cactus model initialized: $modelSlug")
                    Result.success(Unit)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to initialize Cactus model: ${e.message}", e)
                    Result.failure(e)
                }
            }
        }
        
        /**
         * Download a model
         */
        suspend fun downloadModel(modelSlug: String): Result<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    val lm = getOrCreateCactusLM()
                    lm.downloadModel(modelSlug)
                    AppLogger.d(TAG, "Cactus model downloaded: $modelSlug")
                    Result.success(Unit)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to download Cactus model: ${e.message}", e)
                    Result.failure(e)
                }
            }
        }
        
        /**
         * Release the model resources
         */
        fun releaseModel() {
            try {
                cactusLM?.unload()
                cactusLM = null
                loadedModelSlug = null
                AppLogger.d(TAG, "Cactus model unloaded")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error releasing model: ${e.message}", e)
            }
        }
        
        fun getModelsDir(): File {
            return SdkModelManager.getModelsDir("cactus")
        }

        /**
         * Get all available models for Cactus SDK
         */
        fun getDefaultModels(): List<ModelOption> = 
            SdkModelManager.getCactusModels().map { model ->
                ModelOption(
                    id = model.id,
                    name = model.name
                )
            }

        /**
         * Get model options with download status
         */
        fun getModelOptionsWithStatus(context: Context): List<ModelOption> =
            SdkModelManager.getModelOptions(context, "cactus")

        /**
         * Check if a model is downloaded
         */
        fun isModelDownloaded(context: Context, modelId: String): Boolean =
            SdkModelManager.isModelDownloaded(context, "cactus", modelId)

        /**
         * Download a model with progress tracking
         */
        fun downloadModel(
            context: Context,
            modelId: String
        ): Flow<SdkModelManager.DownloadProgress> =
            SdkModelManager.downloadModel(context, "cactus", modelId)

        /**
         * Get the local path for a downloaded model
         */
        fun getModelPath(modelId: String): String? {
            val path = SdkModelManager.getModelLocalPath("cactus", modelId)
            return if (File(path).exists()) path else null
        }

        /**
         * Delete a downloaded model
         */
        fun deleteModel(modelId: String): Boolean {
            // Release the model if it's currently loaded
            if (loadedModelSlug == modelId) {
                releaseModel()
            }
            return SdkModelManager.deleteModel("cactus", modelId)
        }
        
        /**
         * Check if Cactus SDK is available and can be used
         */
        fun isSdkAvailable(): Boolean {
            return try {
                // Try to create a CactusLM instance to check if SDK is available
                val lm = CactusLM()
                // Check if a model is loaded
                lm.isLoaded()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Cactus SDK not available: ${e.message}")
                false
            }
        }
        
        /**
         * Check if the model is currently loaded
         */
        fun isModelLoaded(): Boolean {
            return try {
                cactusLM?.isLoaded() == true
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Get SDK status message for display
         */
        fun getSdkStatus(): String {
            return if (isSdkAvailable()) {
                "Cactus SDK is available"
            } else {
                "Cactus SDK is not available. Ensure it is properly integrated in the build configuration."
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

    override fun resetTokenCounts() {
        _inputTokenCount = 0
        _outputTokenCount = 0
        _cachedInputTokenCount = 0
    }

    override fun cancelStreaming() {
        isCancelled = true
    }

    override fun release() {
        // Model is managed at class level, don't release here
    }

    override suspend fun getModelsList(context: Context): Result<List<ModelOption>> {
        return try {
            val models = getModelOptionsWithStatus(context)
            Result.success(models)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to get models list: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun testConnection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        // Check if the SDK is available
        if (!isSdkAvailable()) {
            return@withContext Result.failure(Exception(
                "Cactus SDK is not available. Please ensure the app is properly built with the Cactus SDK."
            ))
        }
        
        // Get the model slug
        val modelSlug = getModelSlug(modelName)
        
        // Try to initialize the model
        val initResult = initializeModel(modelSlug, contextSize)
        
        if (initResult.isSuccess) {
            Result.success("Model '$modelName' (slug: $modelSlug) is ready for inference")
        } else {
            Result.failure(Exception(
                "Failed to initialize model '$modelName': ${initResult.exceptionOrNull()?.message}"
            ))
        }
    }

    override suspend fun calculateInputTokens(
        message: String,
        chatHistory: List<Pair<String, String>>,
        availableTools: List<ToolPrompt>?
    ): Int {
        return withContext(Dispatchers.IO) {
            // Approximate token count (actual tokenization would require the model)
            val totalChars = chatHistory.sumOf { it.second.length } + message.length
            totalChars / 4 // Rough approximation: ~4 chars per token
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
        
        // Check if SDK is available
        if (!isSdkAvailable()) {
            emit("""
                |
                |[Cactus SDK Not Available]
                |
                |The Cactus SDK is not available. Please ensure the app is properly 
                |built with the Cactus SDK integration.
                |
                |${getSdkStatus()}
                |
            """.trimMargin())
            return@stream
        }
        
        // Get model slug
        val modelSlug = getModelSlug(modelName)
        
        try {
            // Try to download and initialize the model
            val lm = getOrCreateCactusLM()
            
            // Download if needed (this is handled internally by the SDK)
            try {
                lm.downloadModel(modelSlug)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Model download may have failed or already exists: ${e.message}")
            }
            
            // Initialize the model
            initializeModel(modelSlug, contextSize).getOrThrow()
            
            // Build chat messages from history
            val messages = buildChatMessages(message, chatHistory)
            
            // Get completion parameters from modelParameters or use defaults
            val completionParams = buildCompletionParams(modelParameters)
            
            if (stream) {
                // Streaming mode
                val output = StringBuilder()
                
                lm.generateCompletion(
                    messages = messages,
                    params = completionParams,
                    onToken = { token, _ ->
                        if (!isCancelled) {
                            output.append(token)
                            _outputTokenCount = output.length / 4
                            onTokensUpdated(_inputTokenCount, _cachedInputTokenCount, _outputTokenCount)
                        }
                    }
                )?.let { result ->
                    if (result.success && result.response != null) {
                        emit(result.response)
                    } else if (!result.success) {
                        emit("\n[Error] ${result.response ?: "Unknown error"}\n")
                    }
                }
            } else {
                // Non-streaming mode
                val result = lm.generateCompletion(
                    messages = messages,
                    params = completionParams
                )
                
                if (result != null && result.success && result.response != null) {
                    _inputTokenCount = (result.prefillTokens ?: 0)
                    _outputTokenCount = (result.decodeTokens ?: 0)
                    onTokensUpdated(_inputTokenCount, _cachedInputTokenCount, _outputTokenCount)
                    emit(result.response)
                } else {
                    emit("\n[Error] ${result?.response ?: "Failed to generate completion"}\n")
                }
            }
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during inference: ${e.message}", e)
            onNonFatalError("Inference error: ${e.message}")
            emit("\n[Error] ${e.message}\n")
        }
    }
    
    /**
     * Build chat messages from history
     */
    private fun buildChatMessages(
        message: String,
        chatHistory: List<Pair<String, String>>
    ): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        
        // Add system prompt
        messages.add(
            ChatMessage(
                content = "You are a helpful AI assistant.",
                role = "system"
            )
        )
        
        // Add chat history
        for ((role, content) in chatHistory) {
            messages.add(
                ChatMessage(
                    content = content,
                    role = role
                )
            )
        }
        
        // Add current message
        messages.add(
            ChatMessage(
                content = message,
                role = "user"
            )
        )
        
        return messages
    }
    
    /**
     * Build completion parameters from model parameters or defaults
     */
    private fun buildCompletionParams(
        modelParameters: List<ModelParameter<*>>
    ): CactusCompletionParams {
        var temperature: Double? = null
        var maxTokens: Int = 512
        var topP: Double? = null
        var topK: Int? = null
        
        // Extract parameters
        for (param in modelParameters) {
            when (param.name.lowercase()) {
                "temperature" -> temperature = (param.value as? Number)?.toDouble()
                "max_tokens", "maxtokens" -> maxTokens = (param.value as? Number)?.toInt() ?: 512
                "top_p", "topp" -> topP = (param.value as? Number)?.toDouble()
                "top_k", "topk" -> topK = (param.value as? Number)?.toInt()
            }
        }
        
        // Parse inference mode
        val mode = when (inferenceMode.uppercase()) {
            "LOCAL" -> InferenceMode.LOCAL
            "REMOTE" -> InferenceMode.REMOTE
            "LOCAL_FIRST" -> InferenceMode.LOCAL_FIRST
            "REMOTE_FIRST" -> InferenceMode.REMOTE_FIRST
            else -> InferenceMode.LOCAL_FIRST
        }
        
        return CactusCompletionParams(
            temperature = temperature,
            maxTokens = maxTokens,
            topP = topP,
            topK = topK,
            mode = mode,
            cactusToken = cactusToken.ifEmpty { null }
        )
    }

    /**
     * Format bytes to human-readable string
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
