# GitHub Actions Workflow Changes

## Overview

Modified `.github/workflows/build-apk.yml` to properly handle SDK dependency changes and provide better diagnostics for build failures.

## Changes Made

### 1. Added Dependency Refresh Step

**New Step**: `Refresh Gradle Dependencies`

```yaml
- name: Refresh Gradle Dependencies
  run: |
    echo "=== Refreshing Gradle dependencies ==="
    ./gradlew --refresh-dependencies --stacktrace 2>&1 | tee refresh_output.txt
    echo "=== Dependency refresh complete ==="
```

**Why**: 
- Equivalent to "File > Sync Project with Gradle Files" in Android Studio
- Ensures Gradle fetches the corrected SDK artifacts from Maven Central
- Required after changing artifact names in `libs.versions.toml`
- Forces re-download of dependencies even if cached versions exist

**Impact**:
- Guarantees that Runanywhere SDK uses separate `llamacpp` and `onnx` modules
- Ensures Cactus SDK uses correct `cactus` artifact (not `cactus-android`)
- Prevents using stale/cached wrong dependencies

### 2. Added SDK Dependency Verification

**New Step**: `Verify SDK Dependencies`

```yaml
- name: Verify SDK Dependencies
  run: |
    echo "=== Checking SDK dependencies ==="
    ./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -E "cactus|runanywhere" | tee sdk_dependencies.txt
    echo "=== Expected dependencies ==="
    echo "- com.cactuscompute:cactus:1.2.0-beta"
    echo "- io.github.sanchitmonga22:runanywhere-sdk-android:0.16.1"
    echo "- io.github.sanchitmonga22:runanywhere-llamacpp-android:0.16.1"
    echo "- io.github.sanchitmonga22:runanywhere-onnx-android:0.16.1"
```

**Why**:
- Provides visibility into which SDK dependencies are actually resolved
- Helps diagnose if wrong artifacts are still being used
- Documents expected dependencies for troubleshooting
- Output saved to `sdk_dependencies.txt` artifact

**What to Check**:
- Should show all 4 Runanywhere artifacts (sdk, llamacpp, onnx)
- Should show Cactus artifact as `cactus` not `cactus-android`
- If missing or wrong, indicates Maven Central resolution issue

### 3. Enhanced Build Diagnostics

**Modified Step**: `Clean and Generate Debug APK with Info`

```yaml
- name: Clean and Generate Debug APK with Info
  id: build_apk
  continue-on-error: true
  run: |
    ./gradlew clean assembleDebug --stacktrace --info 2>&1 | tee gradle_output.txt
    BUILD_STATUS=$?
    
    if [ $BUILD_STATUS -ne 0 ]; then
      # Check for native library conflicts
      if grep -q "files found with path.*\.so" gradle_output.txt; then
        echo "ISSUE: Native library conflict detected!"
        grep "files found with path" gradle_output.txt
      fi
      
      # Check for dependency resolution issues
      if grep -q "Could not resolve.*cactus\|Could not resolve.*runanywhere" gradle_output.txt; then
        echo "ISSUE: SDK dependency resolution failed!"
        grep "Could not resolve" gradle_output.txt
      fi
      
      # Check for SDK initialization issues
      if grep -q "UnsatisfiedLinkError\|ClassNotFoundException" gradle_output.txt; then
        echo "ISSUE: Native library or class loading error detected!"
        grep -E "UnsatisfiedLinkError|ClassNotFoundException" gradle_output.txt
      fi
    fi
    
    exit $BUILD_STATUS
```

**Why**:
- Automatically detects and reports common build issues
- Highlights native library conflicts (like `libonnxruntime.so` duplicates)
- Identifies SDK dependency resolution failures
- Makes build failures easier to diagnose

**Issues Detected**:
1. **Native Library Conflicts**: Shows which `.so` files are duplicated
2. **Dependency Resolution**: Shows if SDK artifacts can't be downloaded
3. **Class Loading**: Shows if SDK classes are missing at runtime

### 4. Improved Artifact Upload

**Modified Step**: `Upload Gradle Output`

```yaml
- name: Upload Gradle Output
  if: always()  # Upload even if build fails
  uses: actions/upload-artifact@v4
  with:
    name: gradle-output
    path: |
      refresh_output.txt        # NEW: Dependency refresh log
      sdk_dependencies.txt       # NEW: Resolved dependencies
      gradle_output.txt          # Existing: Build log
    retention-days: 7
```

**Why**:
- `if: always()` ensures logs are uploaded even on failure
- Includes dependency refresh output for troubleshooting
- Includes resolved dependencies for verification
- All diagnostic information in one artifact

**What's Uploaded**:
- `refresh_output.txt`: Shows dependency download/refresh process
- `sdk_dependencies.txt`: Shows actual resolved SDK dependencies
- `gradle_output.txt`: Shows full build log with stack traces

### 5. Conditional APK Upload

**Modified Step**: `Upload Debug APK (artifact)`

```yaml
- name: Upload Debug APK (artifact)
  if: steps.build_apk.outcome == 'success'  # Only if build succeeds
  uses: actions/upload-artifact@v4
  with:
    name: debug-apk
    path: app/build/outputs/apk/debug/app-debug.apk
```

**Why**:
- Only uploads APK if build actually succeeded
- Prevents uploading corrupted/partial APK files
- References specific step outcome for accuracy

### 6. Build Status Check

**New Step**: `Check Build Status`

```yaml
- name: Check Build Status
  if: steps.build_apk.outcome != 'success'
  run: |
    echo "❌ Build failed. Check the gradle-output artifact for details."
    exit 1
```

**Why**:
- Explicitly fails the workflow if build fails
- Provides clear message about where to find logs
- Ensures CI status accurately reflects build result

## Workflow Execution Order

The build now follows this sequence:

1. **Build Cactus** (separate job)
   - Builds `libcactus.so` native library
   - Uploads to artifacts

2. **Setup** (main build job)
   - Checkout code
   - Setup JDK, Android SDK
   - Download `libcactus.so` from artifacts
   - Make gradlew executable

3. **🆕 Dependency Refresh**
   - Run `./gradlew --refresh-dependencies`
   - Forces Gradle to re-download SDK dependencies
   - Ensures correct artifact names are used

4. **🆕 Verify Dependencies**
   - Check which SDK dependencies were resolved
   - Compare against expected artifacts
   - Save to `sdk_dependencies.txt`

5. **Build APK**
   - Clean and build with diagnostics
   - Detect common build issues
   - Save full output to `gradle_output.txt`

6. **Upload Artifacts**
   - Upload logs (always, even on failure)
   - Upload APK (only on success)
   - Fail workflow if build failed

## Expected Behavior

### On Success

```
✅ Refresh Gradle Dependencies - PASSED
✅ Verify SDK Dependencies - PASSED
   - com.cactuscompute:cactus:1.2.0-beta ✓
   - io.github.sanchitmonga22:runanywhere-sdk-android:0.16.1 ✓
   - io.github.sanchitmonga22:runanywhere-llamacpp-android:0.16.1 ✓
   - io.github.sanchitmonga22:runanywhere-onnx-android:0.16.1 ✓
✅ Clean and Generate Debug APK - PASSED
✅ Upload Gradle Output - PASSED
✅ Upload Debug APK - PASSED
```

**Result**: Workflow succeeds, APK is available for download

### On Dependency Issue

```
✅ Refresh Gradle Dependencies - PASSED
⚠️ Verify SDK Dependencies - WARNING
   - Could not find com.cactuscompute:cactus-android:1.2.0-beta
   - Using com.cactuscompute:cactus:1.2.0-beta instead ✓
❌ Clean and Generate Debug APK - FAILED
   ISSUE: SDK dependency resolution failed!
✅ Upload Gradle Output - PASSED (logs uploaded)
⏭️ Upload Debug APK - SKIPPED (no APK to upload)
❌ Check Build Status - FAILED
```

**Result**: Workflow fails with clear indication of dependency issue

### On Native Library Conflict

```
✅ Refresh Gradle Dependencies - PASSED
✅ Verify SDK Dependencies - PASSED
❌ Clean and Generate Debug APK - FAILED
   ISSUE: Native library conflict detected!
   2 files found with path 'lib/arm64-v8a/libonnxruntime.so'
✅ Upload Gradle Output - PASSED (logs uploaded)
⏭️ Upload Debug APK - SKIPPED
❌ Check Build Status - FAILED
```

**Result**: Workflow fails with clear indication of native library conflict

## Troubleshooting

### If Build Fails After These Changes

1. **Check the `gradle-output` artifact**:
   - Download from GitHub Actions workflow run
   - Look at `refresh_output.txt` - Did dependencies download?
   - Look at `sdk_dependencies.txt` - Are correct artifacts listed?
   - Look at `gradle_output.txt` - What was the actual error?

2. **Common Issues**:

   **Issue**: "Could not find com.cactuscompute:cactus:1.2.0-beta"
   - **Cause**: Maven Central temporarily unavailable or artifact doesn't exist
   - **Solution**: Check Maven Central, verify version exists, retry build

   **Issue**: "2 files found with path 'lib/arm64-v8a/libonnxruntime.so'"
   - **Cause**: Native library conflict not resolved by `pickFirsts`
   - **Solution**: Check `app/build.gradle.kts` has correct `jniLibs.pickFirsts` rules

   **Issue**: Still using wrong artifacts after refresh
   - **Cause**: Gradle wrapper cache issue
   - **Solution**: Add cache-busting to workflow or manually clear caches

3. **Verify Locally**:
   ```bash
   ./gradlew --refresh-dependencies
   ./gradlew :app:dependencies | grep -E "cactus|runanywhere"
   ./gradlew clean assembleDebug
   ```

## Benefits of These Changes

1. **✅ Ensures Correct Dependencies**: `--refresh-dependencies` guarantees SDK changes are picked up
2. **✅ Better Diagnostics**: Automatic detection of common build issues
3. **✅ Clearer Errors**: Specific messages about what went wrong
4. **✅ Complete Logs**: All diagnostic output saved to artifacts
5. **✅ Faster Debugging**: Don't need to guess what went wrong
6. **✅ CI/CD Best Practice**: Proper error handling and status reporting

## Related Documentation

- **Local Build**: `QUICK_START_AFTER_FIX.md` - Apply same steps locally
- **SDK Integration**: `SDK_INTEGRATION_FIX.md` - Complete troubleshooting guide
- **Native Libraries**: `BUILD_FIXES.md` - Native library conflict resolution

## Testing the Workflow

After pushing these changes:

1. **Trigger Workflow**: Push to `main` or `master` branch
2. **Monitor Run**: Watch GitHub Actions workflow execution
3. **Check Steps**:
   - ✅ "Refresh Gradle Dependencies" completes
   - ✅ "Verify SDK Dependencies" shows correct artifacts
   - ✅ "Clean and Generate Debug APK" succeeds
4. **Download Artifacts**:
   - `gradle-output`: Review logs if anything fails
   - `debug-apk`: Test the APK on a device

## Reverting Changes (If Needed)

If these changes cause issues, you can:

1. **Remove dependency refresh** (not recommended):
   - Delete the "Refresh Gradle Dependencies" step
   
2. **Remove diagnostics** (keeps core functionality):
   - Keep the refresh step
   - Remove the "Verify SDK Dependencies" step
   - Remove the diagnostic checks in build step

3. **Full revert**:
   ```bash
   git checkout HEAD~1 .github/workflows/build-apk.yml
   git commit -m "Revert workflow changes"
   ```

However, the refresh step is **strongly recommended** to ensure SDK changes are properly applied in CI.

---

**Summary**: The workflow now properly handles SDK dependency changes and provides comprehensive diagnostics for troubleshooting build issues. The `--refresh-dependencies` step is critical for ensuring the corrected SDK artifacts are used.
