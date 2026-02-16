# Quick Start After SDK Integration Fix

## ⚡ TL;DR - What To Do Now

The SDK integration has been fixed. Here's what you need to do:

### 1️⃣ Sync and Build (Required)

```bash
# In Android Studio:
File > Sync Project with Gradle Files

# Or in terminal:
cd /path/to/Operit
./gradlew --refresh-dependencies
./gradlew clean
./gradlew installDebug
```

**Expected**: Build should complete successfully without any "2 files found with path" errors.

### 2️⃣ Test Runanywhere SDK

1. Open Operit app
2. Go to **Settings > API Configuration**
3. Select **Runanywhere** as provider
4. Download a model (e.g., `smollm2-360m-instruct-q8_0`)
5. Go to **AI Chat**
6. Check status - should now say:
   ```
   ✅ Runanywhere SDK is available and initialized
   ```
   (Not "is not available" anymore!)
7. Send a test message - should get a response

### 3️⃣ Test Cactus SDK

1. Go to **Settings > API Configuration**
2. Select **Cactus** as provider
3. Download a model (e.g., `Qwen/Qwen2.5-0.5B-Instruct`)
4. Go to **AI Chat**
5. Check status - should say:
   ```
   ✅ Cactus SDK is available
   ```
6. Send a test message - should work or show specific error

### 4️⃣ Check Logs (Optional)

```bash
adb logcat -s OperitApplication:D MainActivity:D
```

Look for:
- ✅ "Runanywhere LlamaCPP backend registered"
- ✅ "Runanywhere SDK initialized successfully"
- ✅ "Cactus SDK context initialized successfully"

---

## 🔍 What Was Fixed?

### Problem 1: Build Failure ❌
**Error**: `2 files found with path 'lib/arm64-v8a/libonnxruntime.so'`

**Fixed**: Added `pickFirsts` rules in `app/build.gradle.kts` to handle duplicate native libraries.

### Problem 2: Runanywhere Not Available ❌
**Error**: "Runanywhere SDK is not available"

**Fixed**: 
- Corrected Maven artifact names in `libs.versions.toml`
- Fixed initialization order (register backends BEFORE initialize)

### Problem 3: Cactus Not Working ❌
**Error**: Silent failures or initialization errors

**Fixed**:
- Corrected Maven artifact name
- Fixed context initialization to use applicationContext

---

## 📋 Verification Checklist

Use this to confirm everything is working:

- [ ] **Build succeeds** without native library conflicts
- [ ] **App installs** on device without crashing
- [ ] **Runanywhere status** shows "is available and initialized"
- [ ] **Cactus status** shows "is available"
- [ ] **Runanywhere inference** works (can chat with downloaded model)
- [ ] **Cactus inference** works or shows specific error
- [ ] **OnnxEmbeddingService** still works (RAG features in app)
- [ ] **Logs show** successful SDK initialization

---

## 🚨 If Something Still Doesn't Work

### Build Still Fails?

```bash
# Try invalidating caches
./gradlew clean
rm -rf .gradle/
rm -rf build/

# Re-sync
./gradlew --refresh-dependencies
```

### SDK Still Shows "Not Available"?

1. Check you synced Gradle after editing `libs.versions.toml`
2. Verify in logcat that initialization is happening
3. Try a clean install:
   ```bash
   ./gradlew clean
   ./gradlew uninstallDebug
   ./gradlew installDebug
   ```

### Models Download But Don't Work?

1. Check the model file actually exists on device:
   ```bash
   adb shell ls /storage/emulated/0/Download/Operit/models/
   ```
2. Check logcat for specific errors during inference
3. Try the smallest model first (loads faster, easier to debug)

---

## 📚 Detailed Documentation

For more details, see:

- **`SDK_INTEGRATION_FIX.md`** - Complete troubleshooting guide
- **`BUILD_FIXES.md`** - Native library conflict explanation
- **`CHANGES_SUMMARY.md`** - All changes made
- **`verify_sdk_integration.bat`** - Automated checks (Windows)

---

## 🎯 Quick Commands Reference

### Build & Install
```bash
./gradlew clean installDebug
```

### Check Dependencies
```bash
./gradlew :app:dependencies | grep -E "cactus|runanywhere"
```

### View Logs
```bash
adb logcat | grep -E "Cactus|Runanywhere|SDK"
```

### Check APK Contents
```bash
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "\.so$"
```

### Run Verification Script (Windows)
```cmd
verify_sdk_integration.bat
```

---

## ✅ Success Indicators

You'll know it's working when you see:

### In Logcat:
```
D/OperitApplication: Runanywhere LlamaCPP backend registered
D/OperitApplication: Runanywhere SDK initialized successfully
D/MainActivity: Cactus SDK context initialized successfully
```

### In App UI:
```
[Runanywhere SDK Integration]
...
Runanywhere SDK is available and initialized ✅
```

```
[Cactus SDK Integration]
...
Cactus SDK is available ✅
```

### In Chat:
- Messages to Runanywhere models get responses
- Messages to Cactus models get responses (or specific errors, not silent failures)

---

## 🤝 Support

If issues persist after following this guide:

1. Read `SDK_INTEGRATION_FIX.md` for detailed troubleshooting
2. Check full logcat output: `adb logcat > logcat.txt`
3. Verify device architecture: `adb shell getprop ro.product.cpu.abi` (should be arm64-v8a)
4. Check Android version: `adb shell getprop ro.build.version.sdk` (should be ≥26)

---

**Last Updated**: February 16, 2026  
**Files Modified**: 6 files  
**New Docs**: 4 files  
**Status**: ✅ Ready to build and test
