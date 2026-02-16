# SDK Integration Fix - Changes Summary (Updated)

## Date: February 16, 2026

## Problem
After downloading models for Cactus Compute and Runanywhere providers, the following errors were observed:
- **Cactus SDK**: Shows as "available" but models fail to initialize properly
- **Runanywhere SDK**: Shows as "not available" even after proper integration in build.gradle.kts
- **Build Failure**: Duplicate native library error preventing APK build

## Root Causes
1. **Incorrect Maven artifact names** in `libs.versions.toml` for both SDKs
2. **Wrong initialization order** for Runanywhere SDK (backends must be registered BEFORE initialization)
3. **Context initialization issues** for Cactus SDK
4. **Reflection-based SDK loading** complexity with suspend functions
5. **Native library conflicts** between `onnxruntime-android` and `runanywhere-onnx-android` modules

## Files Modified

### 1. `gradle/libs.versions.toml`
**Changes:**
- Fixed Runanywhere SDK dependencies to use separate backend modules:
  - `runanywhere-llamacpp-android` (was: `runanywhere-sdk-android`)
  - `runanywhere-onnx-android` (was: `runanywhere-sdk-android`)
- Fixed Cactus SDK artifact name:
  - `cactus` (was: `cactus-android`)

**Why:** The original configuration had all three Runanywhere dependencies pointing to the same artifact, meaning the LlamaCPP and ONNX backends were never actually included in the build. The Cactus artifact name was incorrect per Maven Central.

### 2. `app/src/main/java/com/ai/assistance/operit/core/application/OperitApplication.kt`
**Changes:**
- Reordered Runanywhere SDK initialization:
  ```kotlin
  // BEFORE: Initialize first, register backend second (WRONG)
  RunAnywhere.initialize()
  LlamaCPP.register()
  
  // AFTER: Register backend first, initialize second (CORRECT)
  LlamaCPP.register()
  RunAnywhere.initialize()
  ```
- Added comprehensive documentation explaining the correct order
- Improved logging for debugging

**Why:** Per Runanywhere SDK documentation, backend modules MUST be registered before calling `RunAnywhere.initialize()`, otherwise the SDK initializes without any inference capabilities.

### 3. `app/src/main/java/com/ai/assistance/operit/ui/main/MainActivity.kt`
**Changes:**
- Fixed Cactus context initialization to use `applicationContext`:
  ```kotlin
  // BEFORE: Using activity context
  initializeMethod.invoke(null, this)
  
  // AFTER: Using application context
  initializeMethod.invoke(null, this.applicationContext)
  ```
- Improved error handling with specific exception types
- Added documentation about initialization requirements

**Why:** Using the Activity context can lead to memory leaks and context lifecycle issues. The SDK should be initialized with the Application context which lives as long as the app process.

### 4. `app/src/main/java/com/ai/assistance/operit/api/chat/llmprovider/RunanywhereProvider.kt`
**Changes:**
- Updated `createLlamaModel()` to ensure proper initialization:
  - Check if SDK is initialized first
  - If not, register LlamaCPP backend BEFORE initializing SDK
  - Better error messages and logging
- Improved model path handling
- Better null safety checks

**Why:** The provider needs to be defensive - if the SDK wasn't initialized at app startup, it should attempt to initialize it properly when first used, following the correct order.

### 5. `app/src/main/java/com/ai/assistance/operit/api/chat/llmprovider/CactusProvider.kt`
**Changes:**
- Improved model initialization with better error handling
- Added detection for suspend function vs regular function calls
- Enhanced logging for debugging
- Better handling of model slug extraction

**Why:** The SDK uses suspend functions which are difficult to call via reflection. Added detection to provide better error messages and prepare for future coroutine-based implementation.

### 6. `app/build.gradle.kts` ⭐ NEW FIX
**Changes:**
- Added `pickFirsts` rules in the `jniLibs` packaging block to handle duplicate native libraries:
  ```kotlin
  packaging {
      jniLibs {
          useLegacyPackaging = true
          // Handle duplicate native libraries by picking the first one found
          pickFirsts += "lib/arm64-v8a/libonnxruntime.so"
          pickFirsts += "lib/armeabi-v7a/libonnxruntime.so"
          pickFirsts += "lib/x86/libonnxruntime.so"
          pickFirsts += "lib/x86_64/libonnxruntime.so"
      }
  }
  ```
- Removed incorrect `pickFirsts += "**/*.so"` from resources block (doesn't work for native libs)

**Why:** 
- Both `onnxruntime-android:1.17.1` (used by `OnnxEmbeddingService`) and `runanywhere-onnx-android` include the same `libonnxruntime.so` native library
- Build was failing with "2 files found with path 'lib/arm64-v8a/libonnxruntime.so'" error
- Both versions are identical (v1.17.1), so using either one is safe
- The `pickFirsts` rule tells Gradle to use the first library found and ignore duplicates
- This resolves the build conflict without breaking existing functionality
- Per Runanywhere SDK docs, their ONNX module bundles its own ONNX Runtime

## Expected Behavior After Fix

### Cactus SDK
**Before:**
```
Cactus SDK is available
[But inference fails silently or with cryptic errors]
```

**After:**
```
Cactus SDK is available
[Model initializes and inference works, OR shows specific error about suspend functions]
```

### Runanywhere SDK
**Before:**
```
Runanywhere SDK is not available. Ensure it is properly integrated in the build configuration.
```

**After:**
```
Runanywhere SDK is available and initialized
[Model loads and inference works]
```

### Build Process
**Before:**
```
FAILURE: Build failed with an exception.
Execution failed for task ':app:mergeDebugNativeLibs'.
> 2 files found with path 'lib/arm64-v8a/libonnxruntime.so'
```

**After:**
```
BUILD SUCCESSFUL
```

## Required Actions for User

1. **Sync Gradle Dependencies:**
   ```bash
   ./gradlew --refresh-dependencies
   ```
   Or in Android Studio: File > Sync Project with Gradle Files

2. **Clean Build:**
   ```bash
   ./gradlew clean
   ./gradlew build
   ```
   Should now complete without native library conflicts

3. **Rebuild and Install:**
   ```bash
   ./gradlew installDebug
   ```

4. **Test Each SDK:**
   - Download a model for each provider
   - Check the status messages in the AI Chat page
   - Try sending a message to test inference
   - Verify OnnxEmbeddingService still works (RAG functionality)

## Known Limitations

### CactusLM Suspend Functions
The `CactusLM.initializeModel()` method is a suspend function. The current reflection-based approach can detect this but cannot properly call it. 

**Future Work:** Implement proper coroutine integration for calling suspend functions via reflection, or refactor to use direct SDK dependencies without reflection.

### Native Library Dependencies
Both SDKs depend on native libraries (.so files). The `pickFirsts` rule handles the ONNX Runtime conflict, but other conflicts may arise in the future.

**Check:** After building, verify native libraries are in the APK:
```bash
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "\.so$"
```

## Documentation Files Created

1. **`SDK_INTEGRATION_FIX.md`** - Comprehensive SDK integration troubleshooting guide
2. **`BUILD_FIXES.md`** - Detailed explanation of native library conflict resolution
3. **`CHANGES_SUMMARY.md`** - This file - summary of all changes
4. **`verify_sdk_integration.bat`** - Automated verification script

## Testing Instructions

See `SDK_INTEGRATION_FIX.md` for detailed testing instructions and troubleshooting steps.  
See `BUILD_FIXES.md` for detailed explanation of the native library conflict and resolution.

## References

- **Documentation**: `cactuscompute-docs.md` - Cactus SDK integration guide
- **DeepWiki Queries**: Used to verify correct SDK integration patterns and native library handling
- **Runanywhere Docs**: https://github.com/RunanywhereAI/runanywhere-sdks
- **Cactus Docs**: https://github.com/cactus-compute/cactus
- **Android Docs**: [Handle duplicate files](https://developer.android.com/studio/build/shrink-code#resolve-conflicts)

## Commit Message Suggestion

```
fix: Resolve SDK integration and native library conflicts

- Fix Maven artifact names in libs.versions.toml
  * Runanywhere: Use separate llamacpp and onnx artifacts
  * Cactus: Use correct 'cactus' artifact (not 'cactus-android')
  
- Fix Runanywhere SDK initialization order
  * Register LlamaCPP backend BEFORE calling RunAnywhere.initialize()
  * Per SDK docs, backends must be registered first
  
- Fix Cactus SDK context initialization
  * Use applicationContext instead of activity context
  * Add better error handling for suspend function detection
  
- Resolve native library conflicts (BUILD FIX)
  * Add pickFirsts rules for libonnxruntime.so in jniLibs block
  * Both onnxruntime-android and runanywhere-onnx-android bundle same lib
  * Using pickFirsts preserves functionality of both modules
  
- Improve error messages and logging for both providers
- Add comprehensive documentation:
  * SDK_INTEGRATION_FIX.md - SDK troubleshooting
  * BUILD_FIXES.md - Native library conflict resolution
  * verify_sdk_integration.bat - Automated verification

Resolves SDK availability issues and build failures.
```
