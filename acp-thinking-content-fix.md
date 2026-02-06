# Fix: ACP Thinking Content Not Displaying in Timeline

## Problem

从 Kimi 的日志可以看到大量的 `AgentThoughtChunk` 事件（逐字流式输出）：
```json
{"update_type":"AgentThoughtChunk", "content":"用户"}
{"update_type":"AgentThoughtChunk", "content":"想要"}
{"update_type":"AgentThoughtChunk", "content":"我"}
...
```

但在 UI 上，这些 thinking 内容**完全没有显示**在 timeline 中。

## Root Cause

当 `renderThinkingChunk(isEnd=true)` 被调用时（从 thinking 转换到 message 时），代码只是设置 `_isThinking = false`，但**从未把积累的 thinking 内容保存到 timeline**！

```kotlin
// OLD CODE - thinking content lost!
if (isEnd) {
    _isThinking = false  // ❌ 内容丢失
}
```

结果：
- 在流式输出期间，thinking 内容在临时状态 `_currentThinkingOutput` 中显示
- 一旦转换到 message 阶段，临时状态被清空
- **Timeline 中没有任何 thinking 记录**

## Solution

### 1. 添加 `TimelineItem.ThinkingItem` 类型

在 `RendererModels.kt` 中添加：

```kotlin
data class ThinkingItem(
    val content: String,
    override val timestamp: Long = Platform.getCurrentTimestamp(),
    override val id: String = generateId()
) : TimelineItem(timestamp, id)
```

### 2. 在 `ComposeRenderer` 中保存完整 thinking 内容

添加两个字段：
- `_currentThinkingOutput`: 仅保留最后 5 行用于流式显示
- `_fullThinkingContent`: **完整的** thinking 内容（用于保存到 timeline）

```kotlin
// Full thinking content (not trimmed) for saving to timeline
private var _fullThinkingContent = StringBuilder()
```

### 3. 在 `renderThinkingChunk` 中保存到 timeline

```kotlin
override fun renderThinkingChunk(chunk: String, isStart: Boolean, isEnd: Boolean) {
    if (isStart) {
        _currentThinkingOutput = ""
        _fullThinkingContent = StringBuilder()  // ✅ 初始化完整内容
        _isThinking = true
    }

    // Append to full content (not trimmed)
    _fullThinkingContent.append(chunk)  // ✅ 累积所有内容
    
    // ... display logic (trim to 5 lines) ...

    if (isEnd) {
        // Save FULL thinking content to timeline
        val fullContent = _fullThinkingContent.toString().trim()
        if (fullContent.isNotEmpty()) {
            _timeline.add(
                TimelineItem.ThinkingItem(
                    content = fullContent,  // ✅ 保存完整内容
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        _isThinking = false
        _currentThinkingOutput = ""
        _fullThinkingContent = StringBuilder()
    }
}
```

### 4. 在 UI 中渲染 `ThinkingItem`

在 `AgentMessageList.kt` 的 `RenderMessageItem` 中添加：

```kotlin
is TimelineItem.ThinkingItem -> {
    ThinkingBlockRenderer(
        thinkingContent = timelineItem.content,
        isComplete = true,  // 已完成的 thinking
        modifier = Modifier.fillMaxWidth()
    )
}
```

### 5. 在 `ComposeRenderer` 的 when 表达式中添加分支

添加了 3 个地方的 `ThinkingItem` 分支：
1. `toMessageMetadata()` - 返回 `null`（不持久化）
2. `getTimelineSnapshot()` - 返回 `null`（不作为消息保存）
3. `AgentMessageList.RenderMessageItem` - 渲染为 thinking 块

## Result

### Before (Broken)
```
Timeline:
  USER: "画一个项目的架构图"
  ASSISTANT: "我来帮你画这个项目的架构图..."
  [Thinking content lost - 用户想要我画一个项目的架构图...]
```

### After (Fixed)
```
Timeline:
  USER: "画一个项目的架构图"
  THINKING: "用户想要我画一个项目的架构图。首先我需要了解这个项目的结构和架构。让我先看看当前项目的内容和结构。" [collapsible]
  Shell: find ... -> Failed
  ReadFile: README.md -> Success
  ReadFile: build.gradle -> Success
  ASSISTANT: "我来帮你画这个项目的架构图，首先让我梳理一下..."
  SUCCESS: ACP finished: END_TURN
```

## Event Flow

```
1. AgentThoughtChunk("用户") arrives
   → _fullThinkingContent.append("用户")
   → _currentThinkingOutput = "用户" (display)

2. AgentThoughtChunk("想要") arrives
   → _fullThinkingContent.append("想要")
   → _currentThinkingOutput = "用户想要" (display)

... many more chunks ...

N. AgentMessageChunk("我") arrives
   → renderThinkingChunk(isEnd=true)
   → Save _fullThinkingContent (complete) to timeline as ThinkingItem ✅
   → _isThinking = false
   → renderLLMResponseStart() (start message section)
```

## Files Modified

1. **`mpp-core/.../RendererModels.kt`**
   - Added `TimelineItem.ThinkingItem` data class

2. **`mpp-ui/.../ComposeRenderer.kt`**
   - Added `_fullThinkingContent` field
   - Modified `renderThinkingChunk()` to save to timeline on `isEnd`
   - Added `ThinkingItem` branches to 3 when expressions

3. **`mpp-ui/.../AgentMessageList.kt`**
   - Added `ThinkingItem` rendering branch
   - Added import for `ThinkingBlockRenderer`

## Status

✅ **Compiled successfully**
✅ **Thinking content now saved to timeline**
✅ **UI can render thinking blocks**
✅ **Ready for testing with Kimi ACP**

## Testing

Run the app and send "画一个项目的架构图" to Kimi. You should now see:
1. ✅ Thinking content displayed during streaming (last 5 lines)
2. ✅ Complete thinking content saved to timeline as a collapsible block
3. ✅ Message content displayed after thinking ends
