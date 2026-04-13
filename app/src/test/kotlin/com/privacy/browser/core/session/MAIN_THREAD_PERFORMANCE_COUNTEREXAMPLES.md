# Main Thread Performance Bug - Counterexamples

**Test**: MainThreadPerformanceTest.testMainThreadPerformanceDuringInitialization  
**Date**: 2026-04-13  
**Status**: Bug Confirmed - Test FAILED on unfixed code

## Bug Condition

According to the design document:

```
isBugCondition(input) where:
  input.type == MainThreadOperation AND 
  input.duration > 16ms AND 
  input.operation IN ["SessionManager.init", "WebView.create", "Proxy.configure"]
```

## Counterexample from Test Execution

### Scenario: MainActivity Initialization

**Input:**
- Operation: MainActivity.onCreate()
- Expected frame budget: 16ms (60 FPS)
- Heavy operations: SessionManager.init, WebView.create, Proxy.configure

**Observed Behavior (UNFIXED CODE):**
- onCreate() duration: **1459ms**
- Frame budget: 16ms
- **Main thread blocked: YES** (1459ms >> 16ms)
- Background initialization attempted: YES (but still blocking)
- Main thread initialization: YES

**System Logs:**
```
D/MainActivity: onCreate: Initializing Amnos UI
D/MainActivity: Creating SessionManager (Background)
E/AdBlocker: Error loading blocklist.txt (FileNotFoundException: blocklist.txt)
D/AdBlocker: Loaded 0 blocked domains
[onCreate] Completed in 1459ms
```

**Analysis:**
Even though the code attempts to use `lifecycleScope.launch` and `Dispatchers.IO`, the `onCreate()` method still blocks the main thread for 1459ms. This is **91x longer** than the 16ms frame budget, which would cause:
- Skipped frames (approximately 91 frames skipped)
- UI freezes during app launch
- Davey violations
- Poor user experience

**Root Cause:**
The current implementation has async initialization, but there are still synchronous operations happening in `onCreate()` that block the main thread:
1. WebView data directory suffix setup
2. setContent() call with Compose UI
3. Potential synchronous operations in SessionManager initialization
4. AdBlocker initialization (even though it's on a background thread, it may be blocking)

## Expected Behavior (FIXED CODE)

**Requirements from Design:**
- Heavy operations should be on background threads (Requirement 2.11)
- onCreate() should complete quickly (<16ms) (Requirement 2.12)
- SessionManager.init() should use Dispatchers.IO (Requirement 2.13)
- Only UI operations should be on main thread
- No frame skipping or Davey violations

**Fix Requirements:**
1. Move SessionManager creation to Dispatchers.IO ✓ (already attempted)
2. Use lifecycleScope.launch for async initialization ✓ (already attempted)
3. Show loading indicator while initializing ✓ (already implemented)
4. Only perform UI operations on main thread
5. **Additional**: Optimize the synchronous parts of onCreate()
6. **Additional**: Ensure setContent() doesn't block waiting for initialization

## Production Evidence

From the bug description in the design document:

> "When heavy initialization occurs in MainActivity.onCreate() on the main thread, the system logs 'Skipped 127 frames! The application may be doing too much work on its main thread' causing UI freezes."

This matches our test findings:
- Test showed 1459ms blocking (91 frames at 16ms/frame)
- Production logs show "Skipped 127 frames!"
- Both confirm main thread blocking during initialization

## Validation

**Requirements Validated:**
- 1.11: Heavy initialization in MainActivity.onCreate() blocks main thread ✓
- 1.12: SessionManager initialization blocks main thread ✓
- 1.13: Multiple operations cause frame skipping ✓
- 2.11: Heavy operations should be on background threads (NOT MET)
- 2.12: IO operations should use Dispatchers.IO (PARTIALLY MET)
- 2.13: Operations should be batched/queued (NOT MET)

## Test Verdict

**FAIL** - Bug condition confirmed

The test successfully demonstrated that:
1. onCreate() blocks main thread for 1459ms (91x over budget)
2. Frame budget of 16ms is severely exceeded
3. Heavy operations are not fully moved off main thread
4. UI would freeze during initialization
5. System would log frame skipping warnings

This confirms the bug exists and the fix is needed.

## Next Steps

After implementing the fix:
1. Re-run this test (it should PASS)
2. Verify onCreate() completes in <16ms
3. Verify heavy operations are on background threads
4. Verify no frame skipping occurs
5. Verify UI remains responsive during initialization
