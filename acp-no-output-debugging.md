# ACP Debugging Guide: No Message Output

**Date**: 2026-02-06  
**Issue**: "ACP ended without any message output; stopReason=END_TURN"

## Symptoms

```
User sends: "hi"
Agent responds: (nothing)
Timeline shows:
  ❌ ACP ended without any message output; stopReason=END_TURN. Check logs for details.
  ✅ ACP finished: END_TURN (0 iterations)
```

## Possible Causes

### 1. Agent Refused Request
- Agent determines request is inappropriate/unsafe
- Agent lacks context to respond
- **Check**: stopReason should be `REFUSAL`

### 2. Permission Denied
- Agent requested permission (e.g., file access, terminal)
- User cancelled permission dialog OR dialog didn't show
- Agent cannot proceed without permission
- **Check**: Look for `ToolCall` or `ToolCallUpdate` in logs

### 3. Agent Process Crashed
- Agent binary crashed or exited
- Connection lost mid-turn
- **Check**: stderr output in connection error panel

### 4. Agent Sent No Message
- Agent only sent tool calls, no text response
- Agent only sent thoughts, no final message
- **Check**: Session update types in logs

### 5. Rendering Not Working
- `AgentMessageChunk` events not being processed
- `AcpClient.renderSessionUpdate()` not called
- **Check**: Debug logs for "AgentMessageChunk received"

## Debug Logging (Added in Latest Version)

### Event Flow Logging

```kotlin
// In sendMessage()
flow.collect { event ->
    when (event) {
        is Event.SessionUpdateEvent -> {
            acpLogger.info("ACP SessionUpdate: ${event.update::class.simpleName}")
            handleSessionUpdate(event.update)
        }
        is Event.PromptResponseEvent -> {
            acpLogger.info("ACP PromptResponse: stopReason=${event.response.stopReason}, receivedChunks=${receivedAnyAgentChunk.get()}")
            // ...
        }
    }
}
```

### Session Update Logging

```kotlin
// In handleSessionUpdate()
when (update) {
    is SessionUpdate.AgentMessageChunk -> {
        acpLogger.info("ACP AgentMessageChunk received, content type: ${update.content::class.simpleName}")
    }
    is SessionUpdate.AgentThoughtChunk -> {
        acpLogger.info("ACP AgentThoughtChunk received")
    }
    is SessionUpdate.ToolCall -> {
        acpLogger.info("ACP ToolCall: ${update.title}, status=${update.status}")
    }
    is SessionUpdate.ToolCallUpdate -> {
        acpLogger.info("ACP ToolCallUpdate: ${update.title}, status=${update.status}")
    }
    // ...
}
```

## How to Debug

### Step 1: Check IntelliJ IDEA Logs

**Location**: `Help > Show Log in Finder/Explorer`

Look for:
```
INFO  ACP SessionUpdate: AgentMessageChunk     ← Message received!
INFO  ACP AgentMessageChunk received, content type: Text
INFO  ACP SessionUpdate: AgentThoughtChunk     ← Thinking received
INFO  ACP SessionUpdate: ToolCall              ← Tool call
INFO  ACP SessionUpdate: ToolCallUpdate        ← Tool progress
INFO  ACP PromptResponse: stopReason=END_TURN, receivedChunks=true
```

### Step 2: Check for Permission Requests

If you see:
```
INFO  ACP requestPermissions: tool=Shell: which colima && colima status
INFO  ACP permission response: Selected(optionId=allow-once)
```

But no `AgentMessageChunk` after, the agent may have failed to execute the tool.

### Step 3: Check Agent stderr

The connection error panel shows stderr from the agent process. Look for:
- Error messages from the agent
- Python/Node.js stack traces
- "permission denied" errors
- Network connection issues (for remote agents)

### Step 4: Test with Simple Prompt

Try a trivial prompt that doesn't require tools:
```
User: "What is 2+2?"
```

If this works, the issue is likely permission-related.

### Step 5: Check Agent Configuration

Verify in `~/.autodev/config.yaml`:
```yaml
acpAgents:
  kimi:
    name: Kimi
    command: /path/to/kimi
    args: ["acp"]
    # Verify path is correct and executable
```

Run manually to test:
```bash
/path/to/kimi acp
# Should not exit immediately
# Should accept JSON-RPC input
```

## Expected Log Output (Normal Case)

```
INFO  ACP agent 'kimi' connected successfully (session=sess_xyz)
INFO  ACP SessionUpdate: AgentThoughtChunk
INFO  ACP AgentThoughtChunk received
INFO  ACP SessionUpdate: AgentMessageChunk
INFO  ACP AgentMessageChunk received, content type: Text
INFO  ACP SessionUpdate: AgentMessageChunk
INFO  ACP AgentMessageChunk received, content type: Text
INFO  ACP PromptResponse: stopReason=END_TURN, receivedChunks=true
```

## Expected Log Output (Permission Denial Case)

```
INFO  ACP SessionUpdate: ToolCall
INFO  ACP ToolCall: Shell: which colima, status=PENDING
INFO  ACP requestPermissions: tool=Shell: which colima
INFO  ACP permission cancelled by user (dialog closed)
INFO  ACP PromptResponse: stopReason=END_TURN, receivedChunks=false
WARN  ACP ended without output. This may indicate: 1) Agent refused/failed, 2) Permission denied, 3) Agent process crashed
```

## Common Issues & Solutions

### Issue 1: "END_TURN (0 iterations)" Immediately

**Cause**: Agent process not running or connection failed

**Solution**:
1. Check if agent process is alive
2. Verify command path in config
3. Check stderr for errors

### Issue 2: Permission Dialog Not Showing

**Cause**: UI thread deadlock (fixed in latest version)

**Solution**: Update to latest code with `invokeLater` + `CompletableFuture`

### Issue 3: Agent Receives Message But Doesn't Respond

**Cause**: Agent doesn't understand prompt or lacks context

**Solution**:
1. Try simpler prompt
2. Check agent supports the language/task
3. Verify agent is not in error state

### Issue 4: Only Tool Calls, No Message

**Cause**: Agent executed tools but didn't synthesize response

**Solution**: This is a bug in the agent itself, not the client

## Testing Commands

### Test Connection
```kotlin
// In plugin dev console
val vm = /* get IdeaAcpAgentViewModel */
vm.connectSelectedAgent()
// Check isConnected.value == true
```

### Test Simple Prompt
```kotlin
vm.sendMessage("What is 2+2?")
// Should see AgentMessageChunk in logs
```

### Test Tool Call (Requires Permission)
```kotlin
vm.sendMessage("List files in current directory")
// Should see:
// 1. ToolCall: Shell
// 2. Permission dialog
// 3. ToolCallUpdate: completed
// 4. AgentMessageChunk with results
```

## Next Steps

1. **Add debug logs** (✅ Done in latest version)
2. **Test with working agent** (e.g., mpp-ui's Claude Code)
3. **Compare event logs** between mpp-ui and mpp-idea
4. **Check for protocol version mismatch**
5. **Verify agent supports basic chat** (without tools)

## Related Files

- `IdeaAcpAgentViewModel.kt` - Event handling + logging
- `AcpClient.kt` (mpp-core) - Session update rendering
- `~/.autodev/config.yaml` - Agent configuration
- IntelliJ IDEA log - Full event trace

## Build Status

```bash
cd mpp-idea && ../gradlew compileKotlin
BUILD SUCCESSFUL in 6s
```

✅ Debug logging compiled successfully
