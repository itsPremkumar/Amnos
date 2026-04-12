# verify_build.ps1
# Amnos Production Readiness Verification Script

$ErrorActionPreference = "Stop"
$Gradle = ".\gradlew.bat"
$GradleArgs = @("--no-build-cache")

Write-Host "--- Amnos Production Build Verification ---" -ForegroundColor Cyan

# 1. Clean stale build state to avoid Kotlin incremental cache corruption.
Write-Host "[1/5] Cleaning build state..." -ForegroundColor Yellow
& $Gradle @GradleArgs clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "Clean FAILED!" -ForegroundColor Red
    exit 1
}

# 2. Run Unit Tests
Write-Host "[2/5] Running Unit Tests..." -ForegroundColor Yellow
& $Gradle @GradleArgs testDebugUnitTest
if ($LASTEXITCODE -ne 0) {
    Write-Host "Unit Tests FAILED!" -ForegroundColor Red
    exit 1
}

# 3. Run Lint (Static Analysis)
Write-Host "[3/5] Running Lint Analysis..." -ForegroundColor Yellow
& $Gradle @GradleArgs lintDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "Lint Analysis FAILED!" -ForegroundColor Red
    exit 1
}

# 4. Build Debug APK for smoke-install testing
Write-Host "[4/5] Building Debug APK..." -ForegroundColor Yellow
& $Gradle @GradleArgs assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "Debug Build FAILED!" -ForegroundColor Red
    exit 1
}

# 5. Build Release APK
Write-Host "[5/5] Building Release APK..." -ForegroundColor Yellow
& $Gradle @GradleArgs assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "Release Build FAILED!" -ForegroundColor Red
    exit 1
}

Write-Host "--- SUCCESS: Amnos is production ready! ---" -ForegroundColor Green
Write-Host "Debug APK:   app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Gray
Write-Host "Target APK: app\build\outputs\apk\release\app-release-unsigned.apk" -ForegroundColor Gray
