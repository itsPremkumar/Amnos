# Amnos Project Commands

This document contains a list of common commands used to build, manage, and troubleshoot the Amnos project.

## 🚀 Build Commands
Generate installable APK files.

### Build Release APK
Produces a production-ready APK (signed with debug key by default if release keys are missing).
```powershell
./gradlew assembleRelease
```

### Build Debug APK
Produces a debuggable APK for testing.
```powershell
./gradlew assembleDebug
```

---

## 📱 Device Commands
Install and run the application on a connected Android device or emulator.

### Install Debug APK
Installs the debug build to your device.
```powershell
./gradlew installDebug
```

### Uninstall Application
Removes the application from the device.
```powershell
./gradlew uninstallAll
```

---

## 🧹 Maintenance Commands
Manage the build environment and clean up files.

### Clean Build Files
Deletes all build artifacts. Use this if you encounter strange build errors.
```powershell
./gradlew clean
```

### Check Build Info
Provides detailed output during the build process.
```powershell
./gradlew build --info
```

### Regenerate Gradle Wrapper
Restores the Windows `gradlew.bat` and Linux `gradlew` scripts if they are missing or corrupt.
```powershell
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain wrapper
```

---

## 🔍 Verification Commands
Verify the project state and portability.

### Check Gradle Version
```powershell
./gradlew --version
```

### Scan for Absolute Paths
Ensures the project remains portable by searching for hardcoded paths.
```powershell
Get-ChildItem -Recurse -File -Exclude .git | ForEach-Object { Select-String -Path $_.FullName -Pattern "C:/one/browser" }
```

---

> [!TIP]
> **Windows Compatibility**: If you are using a standard Command Prompt (CMD) instead of PowerShell, use `.\gradlew.bat` instead of `./gradlew`.
