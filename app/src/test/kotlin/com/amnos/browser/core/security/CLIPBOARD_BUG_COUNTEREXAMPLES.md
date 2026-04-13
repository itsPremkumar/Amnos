# Clipboard Access Bug - Counterexamples Documentation

## Bug Condition

**isBugCondition(input)** where:
- input.type == ClipboardAccess
- input.hasFocus == false
- input.androidVersion >= 29 (Android 10+)

## Counterexamples from Production

### Counterexample 1: App Backgrounding (onStop)
**Scenario**: User backgrounds the app (switches to another app or goes to home screen)

**Input**:
- Android Version: 29+ (Android 10+)
- App has focus: false (backgrounded)
- Operation: ClipboardSentinel.wipe() called from MainActivity.onStop()

**Actual Behavior**:
- System denies clipboard access
- System logs: "Denying clipboard access to com.amnos.browser"
- Exception is caught by try-catch in ClipboardSentinel
- App continues but system error is logged

**Expected Behavior**:
- Operation should skip clipboard access gracefully
- No system log errors should be generated
- App should detect lack of focus and return early

### Counterexample 2: Memory Pressure (onTrimMemory)
**Scenario**: System calls onTrimMemory() when app is in background

**Input**:
- Android Version: 29+ (Android 10+)
- App has focus: false (backgrounded)
- Operation: ClipboardSentinel.wipe() called from MainActivity.onTrimMemory()

**Actual Behavior**:
- System denies clipboard access
- System logs: "Denying clipboard access to com.amnos.browser"
- Exception is caught but system error is logged repeatedly

**Expected Behavior**:
- Operation should skip clipboard access gracefully
- No system log errors should be generated

## Root Cause Analysis

The current ClipboardSentinel.wipe() implementation:
1. Has a try-catch that handles the exception
2. Logs a debug message when the exception is caught
3. BUT: The system logs the denial error BEFORE throwing the exception

The issue is that the system-level denial happens before the app-level exception handling, so catching the exception doesn't prevent the system log noise.

## Required Fix

The fix should:
1. Check if the app has window focus BEFORE attempting clipboard access on Android 10+
2. Skip the clipboard access operation gracefully when out of focus
3. Avoid triggering the system-level denial that generates log errors

**Implementation approach**:
- Add a `hasFocus: Boolean` parameter to ClipboardSentinel.wipe()
- Check `hasFocus` before attempting clipboard access on API 29+
- Return early with a debug log message if focus is not available
- This prevents the system-level denial from occurring

## Test Limitation

The unit test in ClipboardSentinelTest.kt cannot reproduce this bug because:
- Robolectric's ClipboardManager mock doesn't enforce focus requirements
- The system-level denial only occurs on real Android devices/emulators
- The test documents the expected behavior but passes in the test environment

To verify the fix works in production:
1. Run the app on a real device or emulator with Android 10+
2. Background the app
3. Check logcat for "Denying clipboard access" errors
4. After fix: No such errors should appear
