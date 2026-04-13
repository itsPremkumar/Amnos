# Memory Pressure Bug Counterexamples

## Bug Condition Detected: Excessive Memory Pressure Wipes

**Test**: `MemoryPressureTest.testExcessiveMemoryPressureWipes`

**Date**: 2025-01-10

**Status**: ✗ FAILED (Bug Confirmed)

---

## Counterexample Details

### Input Conditions
- **Memory pressure events**: 4 within 3668ms (~3.7 seconds)
- **Event level**: TRIM_MEMORY_UI_HIDDEN (level 20)
- **Time between events**: ~1000ms (1 second)
- **Bug condition**: `timeSinceLastWipe < 5000ms`

### Observed Behavior (UNFIXED CODE)
- **Session wipes triggered**: 4 (one for each memory pressure event)
- **Wipe timestamps**: 
  - Event 1: T+0ms → Wipe occurred
  - Event 2: T+1000ms → Wipe occurred
  - Event 3: T+2500ms → Wipe occurred
  - Event 4: T+3500ms → Wipe occurred
- **Result**: User's browsing session destroyed repeatedly
- **Data loss**: All tabs, history, and session state wiped 4 times

### Current Implementation Issues
1. **No debouncing**: `MainActivity.onTrimMemory()` calls `SessionManager.killAll()` unconditionally
2. **No grace period**: Immediate wipe on TRIM_MEMORY_UI_HIDDEN without delay
3. **No timestamp tracking**: SessionManager doesn't track when last wipe occurred
4. **No evaluation logic**: No check if wipe is actually necessary

### Code Location
- **File**: `app/src/main/kotlin/com.amnos.browser/MainActivity.kt`
- **Method**: `onTrimMemory(level: Int)`
- **Current code**:
```kotlin
override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (::sessionManager.isInitialized && level >= TRIM_MEMORY_UI_HIDDEN && !isChangingConfigurations) {
        AmnosLog.d("MainActivity", "Memory pressure wipe triggered.")
        sessionManager.killAll(terminateProcess = false)
    }
}
```

---

## Expected Behavior (Requirements 2.8, 2.9, 2.10)

### Requirement 2.8: Evaluate Wipe Necessity
- Wipes should be evaluated based on session age and memory pressure severity
- Not all TRIM_MEMORY_UI_HIDDEN events require immediate wipe

### Requirement 2.9: Debounce Wipe Operations
- Maximum 1 wipe per 5 seconds
- Skip wipe if less than 5 seconds have elapsed since last wipe
- Prevent repeated session destruction from rapid memory pressure events

### Requirement 2.10: Grace Period for Brief Backgrounding
- Add 10-second grace period before wiping on TRIM_MEMORY_UI_HIDDEN
- Allow user to return to app without losing session
- Only wipe if app remains backgrounded for extended period

---

## Fix Requirements

### 1. Add Timestamp Tracking to SessionManager
```kotlin
// In SessionManager.kt
private var lastWipeTimestamp: Long = 0L

fun killAll(terminateProcess: Boolean = false) {
    // ... existing code ...
    lastWipeTimestamp = System.currentTimeMillis()
    // ... rest of existing code ...
}

fun shouldDebounceWipe(): Boolean {
    val timeSinceLastWipe = System.currentTimeMillis() - lastWipeTimestamp
    return timeSinceLastWipe < 5000 // 5 seconds debounce
}
```

### 2. Add Grace Period to MainActivity
```kotlin
// In MainActivity.kt
private var memoryPressureWipeRunnable: Runnable? = null
private val handler = Handler(Looper.getMainLooper())

override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (::sessionManager.isInitialized && level >= TRIM_MEMORY_UI_HIDDEN && !isChangingConfigurations) {
        // Check debouncing
        if (sessionManager.shouldDebounceWipe()) {
            AmnosLog.d("MainActivity", "Memory pressure wipe skipped (debounced)")
            return
        }
        
        // Add grace period for TRIM_MEMORY_UI_HIDDEN
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            // Cancel any pending wipe
            memoryPressureWipeRunnable?.let { handler.removeCallbacks(it) }
            
            // Schedule wipe after 10-second grace period
            memoryPressureWipeRunnable = Runnable {
                AmnosLog.d("MainActivity", "Memory pressure wipe triggered after grace period")
                sessionManager.killAll(terminateProcess = false)
            }
            handler.postDelayed(memoryPressureWipeRunnable!!, 10000) // 10 seconds
            AmnosLog.d("MainActivity", "Memory pressure wipe scheduled (10s grace period)")
        } else {
            // For more severe memory pressure, wipe immediately
            AmnosLog.d("MainActivity", "Severe memory pressure wipe triggered")
            sessionManager.killAll(terminateProcess = false)
        }
    }
}

override fun onResume() {
    super.onResume()
    // Cancel pending wipe if user returns to app
    memoryPressureWipeRunnable?.let { 
        handler.removeCallbacks(it)
        AmnosLog.d("MainActivity", "Memory pressure wipe cancelled (app resumed)")
    }
}
```

### 3. Evaluate Memory Pressure Severity
```kotlin
// In SessionManager.kt
fun evaluateWipeNecessity(level: Int): Boolean {
    // Check if session is very new (< 30 seconds)
    val sessionAge = System.currentTimeMillis() - sessionCreationTimestamp
    if (sessionAge < 30000) {
        return false // Don't wipe very new sessions
    }
    
    // Check memory pressure severity
    return when (level) {
        ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> false // Use grace period instead
        ComponentCallbacks2.TRIM_MEMORY_MODERATE -> false // Not severe enough
        ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> true // Severe, wipe immediately
        else -> false
    }
}
```

---

## Validation

After implementing the fix, re-run `MemoryPressureTest.testExcessiveMemoryPressureWipes`:

**Expected outcome**: Test PASSES
- Only 1 wipe should occur (or 0 wipes with grace period)
- Subsequent memory pressure events within 5 seconds should be debounced
- User's session should be preserved during brief app switching

---

## Related Requirements

- **Requirement 1.8**: WHEN the system calls onTrimMemory() with level >= TRIM_MEMORY_UI_HIDDEN THEN SessionManager.killAll() is triggered unconditionally
- **Requirement 1.9**: WHEN multiple memory pressure events occur in quick succession THEN the user's browsing session is wiped repeatedly causing data loss
- **Requirement 1.10**: WHEN the app is briefly backgrounded (e.g., switching to another app momentarily) THEN the entire session is destroyed unnecessarily
- **Requirement 2.8**: WHEN onTrimMemory() is called with level >= TRIM_MEMORY_UI_HIDDEN THEN the system SHALL evaluate if the wipe is necessary based on session age and memory pressure severity
- **Requirement 2.9**: WHEN multiple memory pressure events occur within a short time window THEN the system SHALL debounce wipe operations to prevent repeated session destruction
- **Requirement 2.10**: WHEN the app is briefly backgrounded THEN the system SHALL delay session wipe with a grace period to allow quick app switching

---

## Additional Test Case: Brief Backgrounding

**Test**: `MemoryPressureTest.testBriefBackgroundingDoesNotTriggerWipe`

This test verifies that brief backgrounding (user switches to another app momentarily) does not trigger immediate session wipe. A grace period should allow the user to return without losing their session.

**Expected behavior**: 
- TRIM_MEMORY_UI_HIDDEN triggered
- Grace period starts (10 seconds)
- If user returns before grace period expires, wipe is cancelled
- Session is preserved

---

## Notes

This bug is particularly severe because:
1. **Data loss**: Users lose their entire browsing session (all tabs, history, state)
2. **Poor UX**: Brief app switching destroys session unnecessarily
3. **Frequency**: Memory pressure events are common on Android devices
4. **No recovery**: Once session is wiped, data cannot be recovered

The fix must balance privacy (wiping sessions when truly backgrounded) with usability (preserving sessions during brief app switching).
