# Terminal Cancel Handling

## Problem

When a user cancels a running terminal command, the system had the following issues:

1. **Exit Code 137**: When user clicks Cancel, `process.destroyForcibly()` is called, which sends SIGKILL (signal 9) to the process. This results in exit code 137 (128 + 9).

2. **AI doesn't know it was user cancellation**: The background monitoring coroutine in `ToolOrchestrator.startSessionMonitoring()` receives exit code 137 and treats it as a regular error, not user cancellation.

3. **AI doesn't receive the output log**: The output collected before cancellation is not properly sent to the AI.

## Solution

### 1. Add cancellation tracking to ManagedSession

Added a `cancelledByUser` flag to `ManagedSession` class:

```kotlin
class ManagedSession(...) {
    private var _cancelledByUser: Boolean = false
    val cancelledByUser: Boolean get() = _cancelledByUser
    
    fun markCancelledByUser() {
        _cancelledByUser = true
    }
}
```

### 2. Mark session as cancelled in handleProcessCancel

When user clicks Cancel button, mark the session BEFORE terminating the process:

```kotlin
fun handleProcessCancel(cancelEvent: CancelEvent) {
    // Mark the session as cancelled by user BEFORE terminating
    kotlinx.coroutines.GlobalScope.launch {
        val session = ShellSessionManager.getSession(cancelEvent.sessionId)
        session?.markCancelledByUser()
    }
    
    // Terminate the process
    cancelEvent.process.destroyForcibly()
    
    // Render cancellation message with output to AI
    renderer.renderToolResult(
        toolName = "shell",
        success = false,
        output = cancelMessage,
        fullOutput = cancelEvent.output,
        metadata = mapOf(
            "exit_code" to "137",
            "cancelled" to "true"
        )
    )
}
```

### 3. Skip renderer update for user-cancelled sessions

In `ToolOrchestrator.startSessionMonitoring()`, check if session was cancelled by user:

```kotlin
private fun startSessionMonitoring(...) {
    backgroundScope.launch {
        try {
            val exitCode = shellExecutor.waitForSession(session, timeoutMs)
            val managedSession = ShellSessionManager.getSession(session.sessionId)
            
            // Check if this was a user cancellation
            if (managedSession?.cancelledByUser == true) {
                // User cancelled - don't update renderer as handleProcessCancel already did
                return@launch
            }
            
            // Normal completion - update renderer
            renderer.updateLiveTerminalStatus(...)
        } catch (e: Exception) {
            // Also check for user cancellation in error case
            if (managedSession?.cancelledByUser == true) {
                return@launch
            }
            // Report error
        }
    }
}
```

### 4. Improve exit code 137 error message

In `updateLiveTerminalStatus()`, provide better error message for exit code 137:

```kotlin
val errorMessage = if (exitCode == 137) {
    "Command was terminated (exit code 137 - SIGKILL). This usually means the process was killed by the user or system.\n\nOutput before termination:\n${output ?: "(no output)"}"
} else {
    "Command failed with exit code: $exitCode\n\nOutput:\n${output ?: "(no output)"}"
}
```

## Flow Diagram

```
User clicks Cancel
    ↓
handleProcessCancel()
    ↓
Mark session.cancelledByUser = true
    ↓
process.destroyForcibly() → exit code 137
    ↓
Render cancellation message to AI (with output log)
    ↓
startSessionMonitoring() detects cancelledByUser = true
    ↓
Skip renderer update (avoid duplicate message)
```

## Benefits

1. ✅ AI receives clear message that user cancelled the command
2. ✅ AI receives the output log before cancellation
3. ✅ No duplicate error messages
4. ✅ Exit code 137 is properly explained when it's not user cancellation
5. ✅ Better user experience with clear feedback

## Files Modified

- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/shell/ShellSessionManager.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/orchestrator/ToolOrchestrator.kt`
- `mpp-idea/src/main/kotlin/cc/unitmesh/devins/idea/toolwindow/IdeaAgentViewModel.kt`
- `mpp-idea/src/main/kotlin/cc/unitmesh/devins/idea/renderer/JewelRenderer.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ComposeRenderer.kt`

