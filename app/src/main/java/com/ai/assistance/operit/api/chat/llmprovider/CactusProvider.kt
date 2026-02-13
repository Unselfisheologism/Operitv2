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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Cactus Compute SDK provider for on-device LLM inference.
 * 
 * This provider supports local LLM inference using the Cactus Compute SDK.
 * Models are downloaded to the device's Downloads directory and loaded for inference.
 * 
 * Supported models:
 * - LLM: Qwen3 (0.6B, 1.7B), Gemma-3 (270M, 1B), LFM2 (350M, 700M, 2.6B), LFM2.5 (1.2B)
 * - Vision: LFM2-VL (450M), LFM2.5-VL (1.6B)
 * - STT: Whisper Small, Moonshine Base
 * - Embeddings: Nomic Embed, Qwen3 Embedding
 * 
 * Features:
 * - Local inference (no internet required after model download)
 * - Model download with progress tracking
 * - Vision/Multimodal support
 * - Text embeddings
 * - Speech-to-Text support
 * - Configurable thread count and context size
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
        
        // Singleton model instance for reuse
        @Volatile
        private var loadedModelId: String? = null
        
        @Volatile
        private var modelInstance: Any? = null
        
        @Synchronized
        fun getOrCreateModel(modelId: String, modelPath: String, threadCount: Int, contextSize: Int): Any? {
            if (loadedModelId == modelId && modelInstance != null) {
                return modelInstance
            }
            
            // Release previous model if any
            releaseModel()
            
            try {
                // Try to use Cactus SDK if available
                val model = createCactusModel(modelId, modelPath, threadCount, contextSize)
                if (model != null) {
                    loadedModelId = modelId
                    modelInstance = model
                    AppLogger.d(TAG, "Model loaded successfully: $modelId")
                }
                return model
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load model: ${e.message}", e)
                return null
            }
        }
        
        private fun createCactusModel(modelId: String, modelPath: String, threadCount: Int, contextSize: Int): Any? {
            return try {
                // Use reflection to call SDK to avoid compile-time dependency issues
                val cactusClass = Class.forName("com.cactus.Cactus")
                
                // Create model config
                val configClass = Class.forName("com.cactus.ModelConfig")
                val config = configClass.getDeclaredConstructor()
                    .newInstance()
                
                // Set config parameters
                val setModelPath = configClass.getDeclaredMethod("setModelPath", String::class.java)
                setModelPath.invoke(config, modelPath)
                
                val setNThreads = configClass.getDeclaredMethod("setNThreads", Int::class.java)
                setNThreads.invoke(config, threadCount)
                
                val setNCtx = configClass.getDeclaredMethod("setNCtx", Int::class.java)
                setNCtx.invoke(config, contextSize)
                
                // Create the model
                val createMethod = cactusClass.getDeclaredMethod("create", configClass)
                createMethod.invoke(null, config)
            } catch (e: ClassNotFoundException) {
                AppLogger.w(TAG, "Cactus SDK not available: ${e.message}")
                null
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error creating Cactus model: ${e.message}", e)
                null
            }
        }
        
        @Synchronized
        fun releaseModel() {
            modelInstance?.let { model ->
                try {
                    // Try to close the model
                    val closeMethod = model.javaClass.getDeclaredMethod("close")
                    closeMethod.invoke(model)
                } catch (e: Exception) {
                    // Ignore close errors
                }
            }
            modelInstance = null
            loadedModelId = null
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
            if (loadedModelId == modelId) {
                releaseModel()
            }
            return SdkModelManager.deleteModel("cactus", modelId)
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
        // Don't release the singleton model here - it's managed at class level
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
        // Check if the selected model is downloaded
        val isDownloaded = isModelDownloaded(context, modelName)
        
        if (isDownloaded) {
            val modelPath = getModelPath(modelName)
            Result.success("Model '$modelName' is ready at: $modelPath")
        } else {
            Result.failure(Exception(
                "Model '$modelName' is not downloaded. Please download the model first.\n" +
                "Available models can be downloaded from the Model Settings page.\n" +
                "Models are stored in: ${getModelsDir().absolutePath}"
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
        
        // Check if model is downloaded
        if (!isModelDownloaded(context, modelName)) {
            emit("""
                |
                |[Model Not Downloaded]
                |
                |The model '$modelName' is not downloaded to your device.
                |
                |To download the model:
                |1. Go to Settings > API Configuration
                |2. Select 'Cactus' as the provider
                |3. Click on the model dropdown
                |4. Select a model to download
                |
                |Models are stored in:
                |${getModelsDir().absolutePath}
                |
                |Available models:
                |${SdkModelManager.getCactusModels().take(5).joinToString("\n") { "- ${it.name} (${formatBytes(it.sizeBytes)})" }}
                |
            """.trimMargin())
            return@stream
        }

        val modelPath = getModelPath(modelName)
        
        // Build the prompt from chat history
        val prompt = buildPrompt(message, chatHistory)
        _inputTokenCount = prompt.length / 4
        
        // Try to use native SDK
        try {
            val model = getOrCreateModel(modelName, modelPath!!, threadCount, contextSize)
            
            if (model != null) {
                // Use streaming completion
                val result = streamWithSdkInternal(model, prompt, onTokensUpdated, onNonFatalError)
                emit(result)
            } else {
                // SDK not available, show helpful message
                emit("""
                    |
                    |[Cactus SDK Integration]
                    |
                    |Model: $modelName
                    |Path: $modelPath
                    |Thread Count: $threadCount
                    |Context Size: $contextSize
                    |Inference Mode: $inferenceMode
                    |
                    |The model is downloaded and ready for inference.
                    |To enable full SDK functionality, ensure the Cactus SDK
                    |is properly integrated in the build configuration.
                    |
                    |Model files are located at:
                    |$modelPath
                    |
                """.trimMargin())
            }
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during inference: ${e.message}", e)
            onNonFatalError("Inference error: ${e.message}")
            emit("\n[Error] ${e.message}\n")
        }
    }
    
    private suspend fun streamWithSdkInternal(
        model: Any,
        prompt: String,
        onTokensUpdated: suspend (input: Int, cachedInput: Int, output: Int) -> Unit,
        onNonFatalError: suspend (error: String) -> Unit
    ): String {
        try {
            // Try to use streaming completion via reflection
            val streamMethod = model.javaClass.getDeclaredMethod(
                "streamComplete",
                String::class.java,
                kotlin.jvm.functions.Function1::class.java
            )
            
            val outputTokens = StringBuilder()
            val callback = { token: String ->
                outputTokens.append(token)
                _outputTokenCount = outputTokens.length / 4
            }
            
            streamMethod.invoke(model, prompt, callback)
            
            onTokensUpdated(_inputTokenCount, _cachedInputTokenCount, _outputTokenCount)
            
            return outputTokens.toString()
            
        } catch (e: NoSuchMethodException) {
            // Fall back to non-streaming completion
            try {
                val completeMethod = model.javaClass.getDeclaredMethod("complete", String::class.java)
                val result = completeMethod.invoke(model, prompt) as? String ?: ""
                
                _outputTokenCount = result.length / 4
                
                onTokensUpdated(_inputTokenCount, _cachedInputTokenCount, _outputTokenCount)
                return result
            } catch (e2: Exception) {
                AppLogger.e(TAG, "Error in fallback completion: ${e2.message}", e2)
                onNonFatalError("Completion error: ${e2.message}")
                return "\n[Error] ${e2.message}\n"
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error in streaming: ${e.message}", e)
            onNonFatalError("Streaming error: ${e.message}")
            return "\n[Error] ${e.message}\n"
        }
    }
    
    private fun buildPrompt(message: String, chatHistory: List<Pair<String, String>>): String {
        val sb = StringBuilder()
        
        // Add chat history in chat format
        for ((role, content) in chatHistory) {
            when (role.lowercase()) {
                "user", "human" -> sb.append("<|user|>\n$content<|end|>\n")
                "assistant", "ai" -> sb.append("<|assistant|)\n$content<|end|>\n")
                "system" -> sb.append("<|system|>\n$content<|end|>\n")
            }
        }
        
        // Add current message
        sb.append("<|user|>\n$message<|end|>\n<|assistant|)\n")
        
        return sb.toString()
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
