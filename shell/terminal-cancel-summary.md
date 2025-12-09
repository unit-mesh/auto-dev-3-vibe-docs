# Terminal Cancel 功能改进总结

## 问题描述

用户在取消正在运行的 Terminal 命令时遇到以下问题：

1. **Exit Code 137**：用户点击 Cancel 按钮后，进程被 `destroyForcibly()` 强制终止，返回 exit code 137（128 + 9 SIGKILL）
2. **AI 不知道是用户取消的**：AI 只看到 "Command failed with exit code: 137"，无法区分是用户主动取消还是其他错误
3. **AI 收不到运行日志**：取消前的输出日志没有正确发送给 AI

## 解决方案

### 1. 添加用户取消标记

在 `ManagedSession` 类中添加 `cancelledByUser` 标志：

```kotlin
private var _cancelledByUser: Boolean = false
val cancelledByUser: Boolean get() = _cancelledByUser

fun markCancelledByUser() {
    _cancelledByUser = true
}
```

### 2. 在取消时标记会话

在 `IdeaAgentViewModel.handleProcessCancel()` 中，在终止进程**之前**标记会话：

```kotlin
// Mark the session as cancelled by user BEFORE terminating the process
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
```

### 3. 跳过重复的渲染更新

在 `ToolOrchestrator.startSessionMonitoring()` 中检测用户取消：

```kotlin
// Check if this was a user cancellation
if (managedSession?.cancelledByUser == true) {
    // User cancelled - don't update renderer as handleProcessCancel already did
    logger.debug { "Session was cancelled by user, skipping renderer update" }
    return@launch
}
```

### 4. 改进 Exit Code 137 的错误消息

在 `updateLiveTerminalStatus()` 中提供更友好的错误消息：

```kotlin
val errorMessage = if (exitCode == 137) {
    "Command was terminated (exit code 137 - SIGKILL). This usually means the process was killed by the user or system.\n\nOutput before termination:\n${output ?: "(no output)"}"
} else {
    "Command failed with exit code: $exitCode\n\nOutput:\n${output ?: "(no output)"}"
}
```

## 执行流程

```
用户点击 Cancel
    ↓
handleProcessCancel()
    ↓
标记 session.cancelledByUser = true
    ↓
process.destroyForcibly() → exit code 137
    ↓
渲染取消消息给 AI（包含输出日志）
    ↓
startSessionMonitoring() 检测到 cancelledByUser = true
    ↓
跳过渲染更新（避免重复消息）
```

## 改进效果

✅ AI 收到明确的用户取消消息  
✅ AI 收到取消前的完整输出日志  
✅ 没有重复的错误消息  
✅ Exit code 137 有清晰的解释  
✅ 更好的用户体验

## 修改的文件

1. `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/shell/ShellSessionManager.kt`
   - 添加 `cancelledByUser` 标志和 `markCancelledByUser()` 方法

2. `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/orchestrator/ToolOrchestrator.kt`
   - 在 `startSessionMonitoring()` 中检测用户取消并跳过渲染更新

3. `mpp-idea/src/main/kotlin/cc/unitmesh/devins/idea/toolwindow/IdeaAgentViewModel.kt`
   - 在 `handleProcessCancel()` 中标记会话为用户取消

4. `mpp-idea/src/main/kotlin/cc/unitmesh/devins/idea/renderer/JewelRenderer.kt`
   - 改进 exit code 137 的错误消息

5. `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ComposeRenderer.kt`
   - 改进 exit code 137 的错误消息

## 测试

编译测试通过：
- ✅ `./gradlew :mpp-core:compileKotlinJvm`
- ✅ `cd mpp-idea && ../gradlew compileKotlin`

## 后续建议

1. 添加单元测试验证取消逻辑
2. 考虑为其他 exit code（如 130 = Ctrl+C）添加类似的友好消息
3. 在 CLI 版本中也应用相同的改进

