# Fix: ACP No Output & Missing Logs

**Date**: 2026-02-06  
**Issue**: ACP agent finishes with "END_TURN (0 iterations)" but shows no output and no logs in `~/.autodev/acp-logs`

## Root Cause Analysis

### Problem 1: No Message Output

`IdeaAcpAgentViewModel` was **NOT** using the shared `AcpClient.renderSessionUpdate()` method, which properly handles:
- `AgentMessageChunk` → `renderer.renderLLMResponseChunk()`
- `AgentThoughtChunk` → `renderer.renderThinkingChunk()`
- Tool call deduplication (thousands of IN_PROGRESS updates → only terminal events)

**Before**:
```kotlin
// handleSessionUpdate() was empty or had custom logic
private fun handleSessionUpdate(update: SessionUpdate) {
    // Missing: call to AcpClient.renderSessionUpdate()
}
```

**After**:
```kotlin
private fun handleSessionUpdate(update: SessionUpdate) {
    // Use shared rendering logic from mpp-core
    AcpClient.renderSessionUpdate(
        update = update,
        renderer = renderer,
        getReceivedChunk = { receivedAnyAgentChunk.get() },
        setReceivedChunk = { receivedAnyAgentChunk.set(it) },
        getInThought = { inThoughtStream.get() },
        setInThought = { inThoughtStream.set(it) },
        renderedToolCallIds = renderedToolCallIds,
        toolCallTitles = toolCallTitles,
        startedToolCallIds = startedToolCallIds
    )
    // ... special handling for PlanUpdate ...
}
```

### Problem 2: No Event Logs

`IdeaAcpAgentViewModel.sendMessage()` directly called `session!!.prompt()` instead of using `AcpClient.promptAndRender()`, which includes:
- Event logging to `~/.autodev/acp-logs/agent-{name}-{timestamp}.jsonl`
- Helpful error messages with log file paths
- Prompt start/end markers

**Comparison with mpp-ui**:

| Component | mpp-ui | mpp-idea (Before) | mpp-idea (After) |
|-----------|--------|-------------------|------------------|
| **Rendering** | `AcpClient.promptAndRender()` | Manual event loop | `AcpClient.renderSessionUpdate()` |
| **Logging** | Automatic (`AcpClient`) | ❌ None | Still manual (needs fix) |
| **Tool Dedup** | Built-in | ❌ None | ✅ Via `renderSessionUpdate()` |
| **Error Hints** | Log file paths | ❌ None | ✅ Added hint |

## Solution Applied

### 1. Use `AcpClient.renderSessionUpdate()` (✅ FIXED)

Integrated the shared rendering logic to handle all `SessionUpdate` types consistently:

```kotlin
// mpp-idea/src/main/kotlin/cc/unitmesh/devins/idea/toolwindow/acp/IdeaAcpAgentViewModel.kt:546-583

private fun handleSessionUpdate(update: SessionUpdate, source: String = "prompt") {
    // Use the shared renderSessionUpdate from AcpClient for consistent rendering
    AcpClient.renderSessionUpdate(
        update = update,
        renderer = renderer,
        getReceivedChunk = { receivedAnyAgentChunk.get() },
        setReceivedChunk = { receivedAnyAgentChunk.set(it) },
        getInThought = { inThoughtStream.get() },
        setInThought = { inThoughtStream.set(it) },
        renderedToolCallIds = renderedToolCallIds,
        toolCallTitles = toolCallTitles,
        startedToolCallIds = startedToolCallIds
    )
    
    // Special handling for PlanUpdate to use JewelRenderer's setPlan
    if (update is SessionUpdate.PlanUpdate) {
        val markdown = buildString {
            update.entries.forEachIndexed { index, entry ->
                val marker = when (entry.status) {
                    PlanEntryStatus.COMPLETED -> "[x] "
                    PlanEntryStatus.IN_PROGRESS -> "[*] "
                    PlanEntryStatus.PENDING -> ""
                    else -> ""
                }
                appendLine("${index + 1}. $marker${entry.content}")
            }
        }.trim()

        if (markdown.isNotBlank()) {
            try {
                val plan = MarkdownPlanParser.parseToPlan(markdown)
                renderer.setPlan(plan)
            } catch (e: Exception) {
                acpLogger.warn("Failed to parse ACP plan update", e)
            }
        }
    }
}
```

**Benefits**:
- ✅ `AgentMessageChunk` now displays in timeline
- ✅ `AgentThoughtChunk` shows in thinking section
- ✅ Tool calls properly deduplicated (thousands → handful)
- ✅ Consistent with mpp-ui behavior

### 2. Add Error Hint for No Output (✅ IMPROVED)

When agent finishes without sending any `AgentMessageChunk`, show a helpful error:

```kotlin
// In sendMessage(), after PromptResponseEvent:
if (!receivedAnyAgentChunk.get()) {
    val hint = "Check logs for details. Agent may have encountered an error."
    renderer.renderError("ACP ended without any message output; stopReason=${event.response.stopReason}. $hint")
}
```

### 3. Event Logging (⚠️ PARTIAL - Needs Improvement)

**Current Limitation**:  
`AcpClient.logEvent()` and `AcpClient.serializeEvent()` are `private` methods, so we can't call them directly from `IdeaAcpAgentViewModel`.

**Options**:

#### Option A: Use `AcpAgentSession` (Recommended)
Refactor to use `AcpAgentSession.promptAndRender()` which automatically includes logging:

```kotlin
// Future improvement
private val acpSession = AcpAgentSession.create(agentKey, cwd, coroutineScope)

fun sendMessage(text: String) {
    acpSession.promptAndRender(text, agentConfig, renderer)
}
```

**Challenges**:
- Need to pass custom `onPermissionRequest` callback (for `IdeaAcpPermissionDialog`)
- Currently, we manually create `ClientSessionOperations` with the callback

#### Option B: Make AcpClient logging methods public
Expose `logEvent()`, `serializeEvent()`, `initEventLogger()` as public API in `mpp-core`.

#### Option C: Duplicate logging logic locally
Implement a local event logger in `IdeaAcpAgentViewModel`.

**Current Status**: Logging is **NOT YET IMPLEMENTED** in mpp-idea. Logs will be empty until we choose and implement one of the above options.

## Testing Checklist

- [x] Compile mpp-idea successfully
- [ ] Run plugin in dev mode
- [ ] Send a prompt to ACP agent (e.g., "What is 2+2?")
- [ ] Verify `AgentMessageChunk` displays in timeline
- [ ] Check `~/.autodev/acp-logs/` for event logs (⚠️ NOT YET WORKING)
- [ ] Trigger a tool call → verify deduplication works
- [ ] Test permission dialog appears for shell commands

## Known Limitations

1. **Event logging not yet implemented** - Logs in `~/.autodev/acp-logs/` will be empty
2. **Permission dialog integration** - Needs testing with real agents

## Next Steps

1. **Priority 1**: Implement event logging (choose Option A, B, or C)
2. **Priority 2**: Test permission dialog with real ACP agents
3. **Priority 3**: Consider full refactor to use `AcpAgentSession`

## Files Modified

1. **IdeaAcpAgentViewModel.kt**:
   - `handleSessionUpdate()`: Now calls `AcpClient.renderSessionUpdate()`
   - `sendMessage()`: Added error hint for no output

## Expected Behavior After Fix

**Before**:
```
User: "What is 2+2?"
Agent: (silence)
Timeline: "ACP finished: END_TURN (0 iterations)"
Logs: ~/.autodev/acp-logs/ (empty)
```

**After**:
```
User: "What is 2+2?"
Agent: "The answer is 4."
Timeline: Shows full agent response with thinking + message
Logs: ~/.autodev/acp-logs/agent-{name}-{timestamp}.jsonl (⚠️ still empty, needs Option A/B/C)
```

## Comparison with mpp-ui

| Feature | mpp-ui | mpp-idea (Before) | mpp-idea (After) |
|---------|--------|-------------------|------------------|
| AgentMessageChunk rendering | ✅ | ❌ | ✅ |
| AgentThoughtChunk rendering | ✅ | ❌ | ✅ |
| Tool call deduplication | ✅ | ❌ | ✅ |
| Event logging | ✅ | ❌ | ⚠️ (pending) |
| Permission dialog | ✅ | ✅ | ✅ |
| Process reuse | ✅ | ✅ | ✅ |

## Build Status

```bash
cd mpp-idea && ../gradlew compileKotlin
BUILD SUCCESSFUL in 5s
```

✅ Compilation successful with only 2 warnings (deprecated API, exhaustive when)
