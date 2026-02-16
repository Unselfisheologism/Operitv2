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
                // Try to use CactusLM class (Kotlin Multiplatform)
                val cactusClass = Class.forName("com.cactus.CactusLM")
                
                // Create a new instance
                val constructor = cactusClass.getDeclaredConstructor()
                val instance = constructor.newInstance()
                
                // Extract the model slug from modelId (e.g., "Qwen/Qwen2.5-0.5B-Instruct" -> "qwen2.5-0.5")
                val modelSlug = extractCactusModelSlug(modelId)
                
                AppLogger.d(TAG, "Initializing CactusLM model with slug: $modelSlug, context: $contextSize")
                
                // Get the CactusInitParams class and create params
                // Constructor signature: CactusInitParams(model: String?, contextSize: Int?)
                val initParamsClass = Class.forName("com.cactus.CactusInitParams")
                val paramsConstructor = initParamsClass.getDeclaredConstructor(
                    String::class.java,    // model
                    Int::class.javaObjectType  // contextSize (nullable Int)
                )
                val params = paramsConstructor.newInstance(modelSlug, contextSize)
                
                // Call initializeModel method (suspend function)
                // Need to check if it returns Boolean
                val initializeMethod = cactusClass.getDeclaredMethod("initializeModel", initParamsClass, kotlin.coroutines.Continuation::class.java)
                
                // For non-suspend version, try this
                try {
                    val initMethodSimple = cactusClass.getDeclaredMethod("initializeModel", initParamsClass)
                    val result = initMethodSimple.invoke(instance, params)
                    AppLogger.d(TAG, "CactusLM model initialized successfully. Result: $result")
                } catch (e: NoSuchMethodException) {
                    AppLogger.w(TAG, "initializeModel appears to be a suspend function. Using blocking call.")
                    // For suspend functions, we'd need kotlinx.coroutines integration
                    // For now, log the issue
                    throw IllegalStateException("CactusLM.initializeModel is a suspend function and requires coroutine context")
                }
                
                instance
            } catch (e: ClassNotFoundException) {
                // Try fallback to older Cactus class
                AppLogger.d(TAG, "CactusLM not found, trying fallback Cactus class")
                tryFallbackCactus(modelId, modelPath, threadCount, contextSize)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error creating Cactus model: ${e.message}", e)
                null
            }
        }
        
        /**
         * Extract the Cactus model slug from modelId.
         * e.g., "Qwen/Qwen2.5-0.5B-Instruct" -> "qwen2.5-0.5"
         *      "Qwen/Qwen2.5-1.5B-Instruct" -> "qwen2.5-1.5"
         */
        private fun extractCactusModelSlug(modelId: String): String {
            val lowerModelId = modelId.lowercase()
            return when {
                // Qwen2.5 models
                lowerModelId.contains("qwen2.5") && lowerModelId.contains("0.5b") -> "qwen2.5-0.5"
                lowerModelId.contains("qwen2.5") && lowerModelId.contains("1.5b") -> "qwen2.5-1.5"
                lowerModelId.contains("qwen2.5") && lowerModelId.contains("3b") -> "qwen2.5-3b"
                lowerModelId.contains("qwen2.5") && lowerModelId.contains("7b") -> "qwen2.5-7b"
                lowerModelId.contains("qwen2.5") && lowerModelId.contains("14b") -> "qwen2.5-14b"
                // Qwen3 models
                lowerModelId.contains("qwen3") && lowerModelId.contains("0.6b") -> "qwen3-0.6"
                lowerModelId.contains("qwen3") && lowerModelId.contains("1.7b") -> "qwen3-1.7"
                lowerModelId.contains("qwen3") && lowerModelId.contains("4b") -> "qwen3-4b"
                lowerModelId.contains("qwen3") && lowerModelId.contains("8b") -> "qwen3-8b"
                lowerModelId.contains("qwen3") && lowerModelId.contains("14b") -> "qwen3-14b"
                // Gemma3 models
                lowerModelId.contains("gemma3") && lowerModelId.contains("270m") -> "gemma3-270m"
                lowerModelId.contains("gemma3") && lowerModelId.contains("1b") -> "gemma3-1b"
                lowerModelId.contains("gemma3") && lowerModelId.contains("4b") -> "gemma3-4b"
                // Gemma2 models
                lowerModelId.contains("gemma2") && lowerModelId.contains("2b") -> "gemma2-2b"
                lowerModelId.contains("gemma2") && lowerModelId.contains("9b") -> "gemma2-9b"
                // Phi models
                lowerModelId.contains("phi") && lowerModelId.contains("3.5") -> "phi3.5-mini"
                lowerModelId.contains("phi4") -> "phi4-mini"
                // Llama models
                lowerModelId.contains("llama") && lowerModelId.contains("3.2") && lowerModelId.contains("1b") -> "llama3.2-1b"
                lowerModelId.contains("llama") && lowerModelId.contains("3.2") && lowerModelId.contains("3b") -> "llama3.2-3b"
                // LFM models
                lowerModelId.contains("lfm") && lowerModelId.contains("2.5") && lowerModelId.contains("1.2b") -> "lfm2.5-1.2b"
                lowerModelId.contains("lfm") && lowerModelId.contains("2") && lowerModelId.contains("350m") -> "lfm2-350m"
                lowerModelId.contains("lfm") && lowerModelId.contains("2") && lowerModelId.contains("700m") -> "lfm2-700m"
                // Default: try to extract from the model name
                else -> {
                    // Try to extract version number pattern
                    val regex = Regex("(\\d+\\.\\d+[bB]?)")
                    val match = regex.find(modelId)
                    if (match != null) {
                        val version = match.groupValues[1].lowercase().replace("b", "")
                        val baseName = modelId.substringBefore("/").lowercase()
                        "$baseName-$version"
                    } else {
                        // Default to a common model
                        "qwen2.5-0.5"
                    }
                }
            }
        }
        
        private fun tryFallbackCactus(modelId: String, modelPath: String, threadCount: Int, contextSize: Int): Any? {
            return try {
                // Fallback to older com.cactus.Cactus class
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
                AppLogger.e(TAG, "Error creating Cactus model (fallback): ${e.message}", e)
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
        
        /**
         * Check if Cactus SDK is available and can be used
         */
        fun isSdkAvailable(): Boolean {
            return try {
                Class.forName("com.cactus.CactusLM")
                true
            } catch (e: ClassNotFoundException) {
                try {
                    Class.forName("com.cactus.Cactus")
                    true
                } catch (e2: Exception) {
                    false
                }
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
                val sdkStatus = getSdkStatus()
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
                    |$sdkStatus
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
            val modelClass = model.javaClass
            
            // Try CactusLM API first (newer Kotlin Multiplatform SDK)
            if (modelClass.name.contains("CactusLM")) {
                return streamWithCactusLM(model, prompt, onTokensUpdated, onNonFatalError)
            }
            
            // Fall back to older Cactus API
            // Try to use streaming completion via reflection
            val streamMethod = modelClass.getDeclaredMethod(
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
    
    private suspend fun streamWithCactusLM(
        model: Any,
        prompt: String,
        onTokensUpdated: suspend (input: Int, cachedInput: Int, output: Int) -> Unit,
        onNonFatalError: suspend (error: String) -> Unit
    ): String {
        try {
            val cactusLmClass = model.javaClass
            
            // Try to get the ChatMessage class and create messages
            val chatMessageClass = Class.forName("com.cactus.ChatMessage")
            val chatMessageConstructor = chatMessageClass.getDeclaredConstructor(
                String::class.java, // content
                String::class.java  // role
            )
            
            // Create user message
            val userMessage = chatMessageConstructor.newInstance(prompt, "user")
            
            // Create messages list
            val messagesList = java.util.ArrayList<Any>()
            messagesList.add(userMessage)
            
            // Get CactusCompletionParams class
            val completionParamsClass = Class.forName("com.cactus.CactusCompletionParams")
            val completionConstructor = completionParamsClass.getDeclaredConstructor()
            val params = completionConstructor.newInstance()
            
            // Try streaming completion with callback
            try {
                val streamMethod = cactusLmClass.getDeclaredMethod(
                    "generateCompletion",
                    List::class.java,
                    completionParamsClass,
                    kotlin.jvm.functions.Function1::class.java
                )
                
                val outputTokens = StringBuilder()
                val callback = { result: Any ->
                    // Extract response from CactusCompletionResult
                    try {
                        val resultClass = result.javaClass
                        val responseField = resultClass.getDeclaredField("response")
                        val response = responseField.get(result) as? String
                        if (response != null) {
                            outputTokens.append(response)
                            _outputTokenCount = outputTokens.length / 4
                        }
                    } catch (e: Exception) {
                        // Try to convert result to string directly
                        outputTokens.append(result.toString())
                        _outputTokenCount = outputTokens.length / 4
                    }
                }
                
                streamMethod.invoke(model, messagesList, params, callback)
                
                onTokensUpdated(_inputTokenCount, _cachedInputTokenCount, _outputTokenCount)
                return outputTokens.toString()
            } catch (e: NoSuchMethodException) {
                // Try non-streaming version
                val completeMethod = cactusLmClass.getDeclaredMethod(
                    "generateCompletion",
                    List::class.java,
                    completionParamsClass
                )
                
                val result = completeMethod.invoke(model, messagesList, params)
                
                // Extract response from result
                val resultClass = result.javaClass
                val responseField = resultClass.getDeclaredField("response")
                val response = responseField.get(result) as? String ?: ""
                
                _outputTokenCount = response.length / 4
                onTokensUpdated(_inputTokenCount, _cachedInputTokenCount, _outputTokenCount)
                return response
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error in CactusLM streaming: ${e.message}", e)
            onNonFatalError("CactusLM streaming error: ${e.message}")
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
