@echo off
REM SDK Integration Verification Script
REM This script helps verify that the SDK integration fixes are working

echo ==========================================
echo SDK Integration Verification
echo ==========================================
echo.

echo [1/5] Checking Gradle configuration...
echo.
echo Checking libs.versions.toml for correct artifact names...
findstr /C:"runanywhere-llamacpp-android" gradle\libs.versions.toml >nul
if %errorlevel%==0 (
    echo [OK] Runanywhere LlamaCPP artifact found
) else (
    echo [ERROR] Runanywhere LlamaCPP artifact NOT found
    echo Expected: runanywhere-llamacpp-android
)

findstr /C:"runanywhere-onnx-android" gradle\libs.versions.toml >nul
if %errorlevel%==0 (
    echo [OK] Runanywhere ONNX artifact found
) else (
    echo [ERROR] Runanywhere ONNX artifact NOT found
    echo Expected: runanywhere-onnx-android
)

findstr /C:"name = \"cactus\"" gradle\libs.versions.toml >nul
if %errorlevel%==0 (
    echo [OK] Cactus artifact name is correct
) else (
    echo [WARNING] Cactus artifact might be incorrect
    echo Expected: name = "cactus"
)

echo.
echo [2/5] Checking OperitApplication.kt for correct initialization order...
echo.
findstr /C:"LlamaCPP.register" app\src\main\java\com\ai\assistance\operit\core\application\OperitApplication.kt >nul
if %errorlevel%==0 (
    echo [OK] LlamaCPP.register call found
) else (
    echo [WARNING] LlamaCPP.register call not found
)

findstr /C:"RunAnywhere.initialize" app\src\main\java\com\ai\assistance\operit\core\application\OperitApplication.kt >nul
if %errorlevel%==0 (
    echo [OK] RunAnywhere.initialize call found
) else (
    echo [WARNING] RunAnywhere.initialize call not found
)

echo.
echo [3/5] Checking MainActivity.kt for Cactus initialization...
echo.
findstr /C:"CactusContextInitializer" app\src\main\java\com\ai\assistance\operit\ui\main\MainActivity.kt >nul
if %errorlevel%==0 (
    echo [OK] CactusContextInitializer usage found
) else (
    echo [WARNING] CactusContextInitializer not found
)

findstr /C:"applicationContext" app\src\main\java\com\ai\assistance\operit\ui\main\MainActivity.kt >nul
if %errorlevel%==0 (
    echo [OK] applicationContext usage found
) else (
    echo [WARNING] applicationContext not found in initialization
)

echo.
echo [4/5] Checking provider implementations...
echo.
findstr /C:"isInitialized" app\src\main\java\com\ai\assistance\operit\api\chat\llmprovider\RunanywhereProvider.kt >nul
if %errorlevel%==0 (
    echo [OK] Runanywhere provider checks SDK initialization
) else (
    echo [WARNING] Initialization check not found in RunanywhereProvider
)

findstr /C:"CactusLM" app\src\main\java\com\ai\assistance\operit\api\chat\llmprovider\CactusProvider.kt >nul
if %errorlevel%==0 (
    echo [OK] CactusLM usage found in provider
) else (
    echo [WARNING] CactusLM not found in CactusProvider
)

echo.
echo [5/5] Build and Installation Instructions
echo.
echo To apply these changes and test:
echo.
echo 1. Sync Gradle dependencies:
echo    gradlew --refresh-dependencies
echo    OR File ^> Sync Project with Gradle Files in Android Studio
echo.
echo 2. Clean build:
echo    gradlew clean
echo.
echo 3. Build and install:
echo    gradlew installDebug
echo.
echo 4. Check logcat for SDK initialization:
echo    adb logcat -s OperitApplication:D MainActivity:D CactusProvider:D RunanywhereProvider:D
echo.
echo 5. Test each SDK:
echo    - Download a model for Cactus provider
echo    - Download a model for Runanywhere provider
echo    - Check status messages in AI Chat page
echo    - Try inference with each model
echo.
echo ==========================================
echo Verification complete!
echo.
echo See SDK_INTEGRATION_FIX.md for detailed troubleshooting
echo ==========================================
echo.
pause
