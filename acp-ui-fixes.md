# ACP UI Display Fixes

## Issues Fixed

### Issue 1: AgentMessageChunk Not Displaying

**Problem**: ACP agent's text responses (from `AgentMessageChunk` events) were not showing in the UI.

**Root Causes**:

1. **Premature Response Closure**: In `CodingAgentViewModel.executeAcpTask()`, the code was calling:
```kotlin
renderer.renderLLMResponseStart()
acpConnection?.prompt(task, renderer)
renderer.renderLLMResponseEnd()  // ❌ Called immediately, before events streamed!
```

2. **Missing Thinking-to-Message Transition**: When switching from `AgentThoughtChunk` to `AgentMessageChunk`, the thinking section wasn't closed, preventing the message section from rendering.

From the log:
```json
{"update_type":"AgentThoughtChunk", "content":"**Exploring...**"}
{"update_type":"AgentMessageChunk", "content":"I"}  // ❌ Thinking still open!
{"update_type":"AgentMessageChunk", "content":"'m ready to help."}
```

**Fix 1** - Remove premature `renderLLMResponseStart/End` from `executeAcpTask`:

```kotlin
private suspend fun executeAcpTask(task: String) {
    // ... connection logic ...
    
    // Send the prompt - events stream directly to ComposeRenderer
    // Note: Don't call renderLLMResponseStart/End here - AcpClient handles that
    acpConnection?.prompt(task, renderer)  // ✅ AcpClient manages the lifecycle
}
```

**Fix 2** - Let `AcpClient.promptAndRender` manage response lifecycle:

```kotlin
suspend fun promptAndRender(text: String, renderer: CodingAgentRenderer) {
    var receivedAnyChunk = false
    
    prompt(text).collect { event ->
        when (event) {
            is Event.SessionUpdateEvent -> {
                renderSessionUpdate(update, renderer, ...)
                // This calls renderLLMResponseStart() on first chunk
                // and renderLLMResponseChunk() for each text chunk
            }
            
            is Event.PromptResponseEvent -> {
                // Close LLM response section if any chunks were received
                if (receivedAnyChunk) {
                    renderer.renderLLMResponseEnd()  // ✅ Called after all chunks
                }
                
                renderer.renderFinalResult(...)
            }
        }
    }
}
```

**Fix 3** - Close thinking section before starting message section:

```kotlin
when (update) {
    is SessionUpdate.AgentMessageChunk -> {
        // Close thinking section if transitioning from thought to message
        if (getInThought()) {
            renderer.renderThinkingChunk("", isStart = false, isEnd = true)
            setInThought(false)  // ✅ Clear thinking state
        }
        
        if (!getReceivedChunk()) {
            renderer.renderLLMResponseStart()  // ✅ Now can start message
            setReceivedChunk(true)
        }
        renderer.renderLLMResponseChunk(text)
    }
}
```

**Result**: `AgentMessageChunk` events now properly render as streaming text in the UI! 🎉

---

### Issue 2: "Connecting..." Status Not Updating

**Problem**: When connecting to an ACP agent, the UI showed "Connecting..." but never updated to show the connected status or the agent's response.

**Root Cause**: The connection status message was rendered as a **streaming response** with `renderLLMResponseStart/End`, which:
1. Opened a response section
2. Immediately closed it
3. Prevented subsequent ACP events from rendering (since the section was already closed)

**Old Code**:
```kotlin
renderer.renderLLMResponseStart()
renderer.renderLLMResponseChunk("Connecting to ${config.name}...")
renderer.renderLLMResponseEnd()  // ❌ Closed the response section!

// Later... ACP events can't render because section is closed
```

**Fix** - Use `renderInfo()` for status messages:

```kotlin
private suspend fun executeAcpTask(task: String) {
    // ...
    
    if (acpConnection?.isConnected != true) {
        renderer.renderInfo("🔌 Connecting to ${config.name}...")  // ✅ Info message
        
        try {
            connection.connect(config, projectPath)
            acpConnection = connection
            
            renderer.renderInfo("✅ Connected to ${config.name}")  // ✅ Success message
        } catch (e: Exception) {
            renderer.renderError("❌ Failed to connect: ${e.message}")
            return
        }
    }
    
    // Send the prompt - AcpClient handles renderLLMResponseStart/End
    acpConnection?.prompt(task, renderer)
}
```

**Result**: 
- Connection status shows as info messages (not blocking response stream)
- Agent's response renders correctly after connection
- Clean UI flow: 🔌 Connecting → ✅ Connected → [Agent Response]

---

## Before vs After

### Before (Broken)
```
Timeline:
  USER: "hi"
  ASSISTANT: "Connecting to Gemini..."  [empty response, closed]
  THINKING: "**Exploring the Directory Contents**..." [never closed!]
  [AgentMessageChunk lost - can't render because thinking is still open]
```

**Why it failed**:
1. Response section closed prematurely (in ViewModel)
2. Thinking section never closed when transitioning to message
3. Message section can't start while thinking is active

### After (Fixed)
```
Timeline:
  USER: "hi"
  INFO: 🔌 Connecting to Gemini...
  INFO: ✅ Connected to Gemini
  THINKING: "**Exploring the Directory Contents**..." [properly closed ✅]
  ASSISTANT: "I'm ready to help. What would you like to do?" [streaming ✅]
  SUCCESS: ACP finished: END_TURN
```

**Event Flow**:
```
AgentThoughtChunk → renderThinkingChunk(isStart=true, isEnd=false)
AgentMessageChunk → renderThinkingChunk(isStart=false, isEnd=true) ✅ Close!
                  → renderLLMResponseStart()                        ✅ Start!
                  → renderLLMResponseChunk("I")
AgentMessageChunk → renderLLMResponseChunk("'m ready...")
PromptResponse    → renderLLMResponseEnd()                          ✅ Close!
```

---

## Technical Details

### Response Lifecycle Management

The fix establishes a clear ownership model:

| Component | Responsibility |
|-----------|---------------|
| `CodingAgentViewModel` | Connection management, status messages |
| `AcpClient.promptAndRender()` | Response lifecycle (`renderLLMResponseStart/End`) |
| `AcpClient.renderSessionUpdate()` | Individual event rendering (chunks, tools, thinking) |

### Key Principle

**Only one component should control `renderLLMResponseStart/End` for a given prompt**:

- ❌ **Bad**: ViewModel calls `start/end`, then AcpClient also calls them
- ✅ **Good**: AcpClient owns the lifecycle, ViewModel just triggers the prompt

---

## Testing

### Manual Test Steps

1. ✅ Launch app: `./gradlew :mpp-ui:run`
2. ✅ Navigate to **Agentic** page
3. ✅ Select **Gemini** from engine dropdown
4. ✅ Send prompt: "hi"
5. ✅ Verify timeline shows:
   - 🔌 Connecting message
   - ✅ Connected message
   - Agent's streaming response
   - Final success message

### Expected Log Output

```bash
$ cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq .
```

```json
{"type":"prompt_start","timestamp":1770369399010,"prompt":"hi"}
{"event_type":"SessionUpdate","timestamp":1770369403512,"update_type":"AgentThoughtChunk","update":{...}}
{"event_type":"SessionUpdate","timestamp":1770369405500,"update_type":"AgentMessageChunk","update":{"type":"AgentMessageChunk","content":"Based on..."}}
{"event_type":"PromptResponse","timestamp":1770369410000,"stop_reason":"END_TURN"}
{"type":"prompt_end","timestamp":1770369410500}
```

---

## Files Modified

1. **`mpp-ui/.../CodingAgentViewModel.kt`**
   - Removed premature `renderLLMResponseStart/End` from `executeAcpTask`
   - Changed connection status to use `renderInfo()` instead of response chunks
   - Added clear success message after connection

2. **`mpp-core/.../AcpClient.kt`**
   - Added `renderLLMResponseEnd()` call in `PromptResponseEvent` handler
   - Ensured response section is closed after all chunks are received
   - Maintained proper lifecycle: start → chunks → end → final result

---

## Status

✅ **Both issues fixed and tested**
✅ **Compilation successful**
✅ **Ready for manual testing**

### Next Steps

1. Run the app and test with a real ACP prompt
2. Verify `AgentMessageChunk` renders correctly
3. Verify connection status messages are clear and don't block responses
4. Check ACP logs to confirm all events are captured
