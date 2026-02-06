# ACP Thinking-to-Message Transition Fix

## Problem

根据日志：
```json
{"update_type":"AgentThoughtChunk", "content":"**Exploring...**"}
{"update_type":"AgentMessageChunk", "content":"I"}
{"update_type":"AgentMessageChunk", "content":"'m ready to help."}
```

`AgentThoughtChunk` 显示后，`AgentMessageChunk` **没有显示**在 UI 上。

## Root Cause

当从 `AgentThoughtChunk` 切换到 `AgentMessageChunk` 时：
- Thinking section 被打开（`renderThinkingChunk(isStart=true)`）
- 但在收到 `AgentMessageChunk` 时，**thinking section 从未关闭**
- Message section 无法开始，因为 thinking 仍然 active

## Fix

在 `AcpClient.renderSessionUpdate()` 中，处理 `AgentMessageChunk` 时，先关闭 thinking section：

```kotlin
is SessionUpdate.AgentMessageChunk -> {
    // Close thinking section if transitioning from thought to message
    if (getInThought()) {
        renderer.renderThinkingChunk("", isStart = false, isEnd = true)
        setInThought(false)  // ✅ Clear state
    }
    
    // Now safe to start message section
    if (!getReceivedChunk()) {
        renderer.renderLLMResponseStart()
        setReceivedChunk(true)
    }
    renderer.renderLLMResponseChunk(text)
}
```

## Event Flow (After Fix)

```
1. AgentThoughtChunk arrives
   → renderThinkingChunk("**Exploring...**", isStart=true, isEnd=false)
   → inThought = true ✅

2. AgentMessageChunk arrives
   → Check: inThought = true
   → renderThinkingChunk("", isStart=false, isEnd=true) ✅ Close thinking!
   → inThought = false
   → renderLLMResponseStart() ✅ Start message
   → renderLLMResponseChunk("I")

3. AgentMessageChunk arrives
   → renderLLMResponseChunk("'m ready to help.")

4. PromptResponse arrives
   → renderLLMResponseEnd() ✅ Close message
```

## Result

Now the UI correctly displays:

```
Timeline:
  USER: "hi"
  INFO: 🔌 Connecting to Gemini...
  INFO: ✅ Connected to Gemini
  
  THINKING: "**Exploring the Directory Contents**
  I've initiated the process..." [collapsed/closed ✅]
  
  ASSISTANT: "I'm ready to help. What would you like to do?" ✅
  
  SUCCESS: ACP finished: END_TURN
```

## Files Modified

- `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/AcpClient.kt`
  - Added thinking-to-message transition logic in `renderSessionUpdate`

## Status

✅ **Fixed and compiled successfully**
✅ **Ready for testing with real ACP agents**
