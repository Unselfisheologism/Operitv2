# SDK Integration Fix

## Problem Summary

After downloading models for Cactus Compute and Runanywhere providers, the following errors were observed:

1. **Cactus SDK**: Shows as "available" but models fail to load for inference
2. **Runanywhere SDK**: Shows as "not available" even though it should be integrated

## Root Causes Identified

### 1. Incorrect Gradle Dependencies

**Issue**: The `libs.versions.toml` file had incorrect artifact names for the SDKs.

**Original (WRONG)**:
```toml
# Runanywhere - All three pointing to the same artifact
runanywhere-kotlin = { group = "io.github.sanchitmonga22", name = "runanywhere-sdk-android", version.ref = "runanywhereSdk" }
runanywhere-core-llamacpp = { group = "io.github.sanchitmonga22", name = "runanywhere-sdk-android", version.ref = "runanywhereSdk" }
runanywhere-core-onnx = { group = "io.github.sanchitmonga22", name = "runanywhere-sdk-android", version.ref = "runanywhereSdk" }

# Cactus - Wrong artifact name
cactus = { group = "com.cactuscompute", name = "cactus-android", version.ref = "cactusSdk" }
```

**Fixed (CORRECT)**:
```toml
# Runanywhere - Separate backend modules
runanywhere-kotlin = { group = "io.github.sanchitmonga22", name = "runanywhere-sdk-android", version.ref = "runanywhereSdk" }
runanywhere-core-llamacpp = { group = "io.github.sanchitmonga22", name = "runanywhere-llamacpp-android", version.ref = "runanywhereSdk" }
runanywhere-core-onnx = { group = "io.github.sanchitmonga22", name = "runanywhere-onnx-android", version.ref = "runanywhereSdk" }

# Cactus - Correct artifact name
cactus = { group = "com.cactuscompute", name = "cactus", version.ref = "cactusSdk" }
```

### 2. Incorrect Runanywhere SDK Initialization Order

**Issue**: According to the Runanywhere SDK documentation, backend modules (like LlamaCPP) MUST be registered BEFORE calling `RunAnywhere.initialize()`.

**Original (WRONG)**:
```kotlin
// Initialize first
RunAnywhere.initialize(environment = SDKEnvironment.DEVELOPMENT)
// Then register backend (TOO LATE!)
LlamaCPP.register()
```

**Fixed (CORRECT)**:
```kotlin
// Register backend modules FIRST
LlamaCPP.register()
// Then initialize SDK
RunAnywhere.initialize(environment = SDKEnvironment.DEVELOPMENT)
```

### 3. Cactus SDK Context Initialization

**Issue**: The Cactus SDK requires `CactusContextInitializer.initialize(context)` to be called before any SDK functionality can be used, but it was being called with the Activity context instead of the Application context.

**Fixed**: Now using `applicationContext` for proper context management.

### 4. CactusLM Suspend Functions

**Issue**: The `CactusLM.initializeModel()` method is a suspend function according to the SDK documentation, but the code was trying to call it synchronously using reflection.

**Status**: This is partially addressed with better error handling, but the full fix requires proper coroutine integration.

## Changes Made

### File: `gradle/libs.versions.toml`
- ✅ Fixed Runanywhere SDK dependency artifact names to use separate modules
- ✅ Fixed Cactus SDK artifact name from `cactus-android` to `cactus`

### File: `app/src/main/java/com/ai/assistance/operit/core/application/OperitApplication.kt`
- ✅ Reordered Runanywhere SDK initialization to register LlamaCPP backend BEFORE calling `RunAnywhere.initialize()`
- ✅ Added better logging to track initialization steps
- ✅ Added documentation explaining the correct order

### File: `app/src/main/java/com/ai/assistance/operit/ui/main/MainActivity.kt`
- ✅ Fixed Cactus SDK initialization to use `applicationContext` instead of activity context
- ✅ Improved error handling with more specific exception types
- ✅ Added documentation about the requirement to initialize in `onCreate()`

### File: `app/src/main/java/com/ai/assistance/operit/api/chat/llmprovider/RunanywhereProvider.kt`
- ✅ Updated `createLlamaModel()` to register LlamaCPP backend before SDK initialization
- ✅ Improved error messages when SDK is not initialized
- ✅ Better null checking and validation

### File: `app/src/main/java/com/ai/assistance/operit/api/chat/llmprovider/CactusProvider.kt`
- ✅ Improved model initialization with better error handling
- ✅ Added documentation about CactusLM being a suspend function
- ✅ Better logging for debugging

## Required Actions

### 1. Sync Gradle Dependencies
After making the changes to `libs.versions.toml`, you MUST sync your Gradle project:

```bash
./gradlew --refresh-dependencies
```

Or in Android Studio: **File > Sync Project with Gradle Files**

### 2. Clean Build
Perform a clean build to ensure all dependency changes are applied:

```bash
./gradlew clean
./gradlew build
```

### 3. Rebuild the App
After syncing and cleaning, rebuild and reinstall the app on your device:

```bash
./gradlew installDebug
```

## Testing the Fix

### Test Cactus SDK Integration

1. Go to **Settings > API Configuration**
2. Select **Cactus** as the provider
3. Download a model (e.g., `Qwen/Qwen2.5-0.5B-Instruct`)
4. Open the **AI Chat** page
5. You should see:
   ```
   [Cactus SDK Integration]
   
   Model: Qwen/Qwen2.5-0.5B-Instruct
   Path: /storage/emulated/0/Download/Operit/models/cactus/Qwen_Qwen2.5-0.5B-Instruct.gguf
   Thread Count: 4
   Context Size: 2048
   Inference Mode: LOCAL_FIRST
   
   The model is downloaded and ready for inference.
   Cactus SDK is available
   ```

6. Try sending a message - it should either:
   - Work and return a response from the model
   - Show a specific error about the suspend function issue (see Known Issues below)

### Test Runanywhere SDK Integration

1. Go to **Settings > API Configuration**
2. Select **Runanywhere** as the provider
3. Download a model (e.g., `smollm2-360m-instruct-q8_0`)
4. Open the **AI Chat** page
5. You should now see:
   ```
   [Runanywhere SDK Integration]
   
   Model: smollm2-360m-instruct-q8_0
   Path: /storage/emulated/0/Download/Operit/models/runanywhere/smollm2-360m-instruct-q8_0.gguf
   Thread Count: 4
   Context Size: 4096
   
   The model is downloaded and ready for inference.
   Runanywhere SDK is available and initialized
   ```
   (Note: "is available and initialized" instead of "is not available")

6. Try sending a message - it should work and return a response

## Known Issues and Future Work

### Issue 1: CactusLM Suspend Function
**Problem**: The `CactusLM.initializeModel()` method is a suspend function, but we're using reflection which makes it difficult to call suspend functions properly.

**Workaround**: The code currently tries to call a non-suspend version first, then falls back with an error message if only the suspend version exists.

**Proper Fix**: Need to either:
- Use kotlinx.coroutines integration with reflection to properly call suspend functions
- OR wrap the entire model creation in a coroutine scope
- OR use a different approach that doesn't require reflection

**Code Example for Future Implementation**:
```kotlin
suspend fun createCactusModelSuspend(modelId: String, contextSize: Int): CactusLM {
    val model = CactusLM()
    model.initializeModel(CactusInitParams(model = modelSlug, contextSize = contextSize))
    return model
}
```

### Issue 2: Native Library Loading
**Problem**: Both SDKs rely on native libraries (.so files). If these aren't properly bundled with the Maven artifacts, the SDKs won't work.

**Symptoms**:
- `UnsatisfiedLinkError` exceptions in logcat
- SDK appears "available" (classes found) but fails during actual model loading

**Check**: After building, verify that native libraries exist in the APK:
```bash
# Extract APK and check for .so files
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "\.so$"

# Should see something like:
# lib/arm64-v8a/libcactus.so
# lib/arm64-v8a/librunanywhere.so
# lib/arm64-v8a/libllama.so
# etc.
```

**Solution**: If native libraries are missing:
1. Check that the SDK versions in `libs.versions.toml` are correct and published on Maven Central
2. Verify that the artifact names include the native libraries (some Maven artifacts exclude them)
3. May need to manually add native libraries to `app/src/main/jniLibs/arm64-v8a/`

### Issue 3: Model Format Compatibility
**Problem**: Each SDK expects models in specific formats:
- Cactus: Uses model "slugs" (e.g., "qwen2.5-0.5") that map to their catalog
- Runanywhere: Uses direct GGUF file paths

**Current Solution**: The `extractCactusModelSlug()` function tries to map model IDs to Cactus slugs, but may not cover all models.

**If models still don't work**: Check the SDK documentation for the exact model name/slug format expected.

## Verification Checklist

- [ ] Gradle sync completed without errors
- [ ] Clean build completed successfully
- [ ] App installs on device
- [ ] Cactus SDK shows "is available" in the chat page
- [ ] Runanywhere SDK shows "is available and initialized" in the chat page
- [ ] Check logcat for "Cactus SDK context initialized successfully"
- [ ] Check logcat for "Runanywhere SDK initialized successfully"
- [ ] Check logcat for "Runanywhere LlamaCPP backend registered"
- [ ] Try inference with each SDK and verify response or specific error message

## Logcat Commands for Debugging

```bash
# Watch for SDK initialization
adb logcat -s OperitApplication:D MainActivity:D

# Watch for model loading
adb logcat -s CactusProvider:D RunanywhereProvider:D

# Watch for native library issues
adb logcat | grep -i "unsatisfiedlinkerror"

# Full verbose logging
adb logcat *:V | grep -i "cactus\|runanywhere"
```

## Additional Resources

- **Cactus SDK Documentation**: `cactuscompute-docs.md` in the project root
- **Runanywhere SDK**: [Maven Central](https://central.sonatype.com/artifact/io.github.sanchitmonga22/runanywhere-sdk-android)
- **Cactus SDK**: [Maven Central](https://central.sonatype.com/artifact/com.cactuscompute/cactus)

## Support

If issues persist after applying these fixes:

1. Check Android Studio's **Build > Build Bundle(s) / APK(s) > Build APK** output for any native library warnings
2. Review the full logcat output during model loading
3. Verify the downloaded model files exist at the specified paths
4. Try with the smallest available models first (they load faster and help isolate issues)
5. Check if the device architecture matches (arm64-v8a is standard for modern Android devices)
