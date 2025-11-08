# Shell 工具执行时 ToolExecutionResult 为空 - 问题分析与修复

## 问题描述

在执行 Shell 指令时，`ToolExecutionResult` 返回的结果为空，特别是在使用 Live PTY 模式执行时。这导致：
1. 命令输出没有被正确返回
2. UI 中看不到完整的执行结果
3. TerminalWidget 虽然显示了终端输出，但系统没有获得结构化的执行结果

---

## 根本原因分析

### 问题 1：Live PTY 执行时，执行流程不完整

**位置**: `ToolOrchestrator.kt` 第 143-152 行

**原始代码**:
```kotlin
val result = if (liveSession != null) {
    // 等待 PTY 进程完成
    val shellExecutor = getShellExecutor(registry.getTool(toolName) as cc.unitmesh.agent.tool.impl.ShellTool)
    waitForLiveSession(liveSession, shellExecutor, context)  // ❌ 只等待完成，不获取输出
} else {
    // 普通执行
    executeToolInternal(toolName, params, context)
}
```

**问题**:
- 当启动 Live PTY 后，代码调用 `waitForLiveSession()` 等待进程完成
- 但 `executeToolInternal()` 中的 `executeShellTool()` **根本没有被调用**
- 这意味着无法通过正常的 Shell 工具执行流程获取完整的 stdout/stderr

### 问题 2：waitForLiveSession 返回结果不包含实际输出

**位置**: `ToolOrchestrator.kt` 第 205-220 行

**原始代码**:
```kotlin
private suspend fun waitForLiveSession(
    session: LiveShellSession,
    executor: ShellExecutor,
    context: ToolExecutionContext
): ToolResult {
    return try {
        val exitCode = if (executor is LiveShellExecutor) {
            executor.waitForSession(session, context.timeout)
        } else {
            throw ToolException("Executor does not support live sessions", ToolErrorType.NOT_SUPPORTED)
        }
        
        if (exitCode == 0) {
            ToolResult.Success("Command executed successfully (live terminal)")  // ❌ 只有一条消息
        } else {
            ToolResult.Error("Command failed with exit code: $exitCode")  // ❌ 没有实际输出
        }
    } catch (e: ToolException) {
        ToolResult.Error("Command execution error: ${e.message}")
    } catch (e: Exception) {
        ToolResult.Error("Command execution error: ${e.message}")
    }
}
```

**问题**:
- 返回的 `ToolResult.Success` 只包含一条简短消息
- 没有实际的命令输出（stdout/stderr）
- 这导致 `ToolExecutionResult` 中的 `content` 字段为空或仅有简短消息

### 问题 3：PtyShellExecutor 的 startLiveExecution 没有捕获输出

**位置**: `PtyShellExecutor.kt` 第 220-260 行

**问题**:
- `startLiveExecution()` 只启动 PTY 进程
- 没有捕获输出流到任何可返回的变量
- 输出只显示在 JediTerm 组件中，不会存储在 `LiveShellSession` 中

### 问题 4：ComposeRenderer 会移除 ToolCallItem

**位置**: `ComposeRenderer.kt` 第 421 行

**原始代码**:
```kotlin
val lastItem = _timeline.lastOrNull()
if (lastItem is TimelineItem.ToolCallItem && lastItem.toolType == ToolType.Shell) {
    _timeline.removeAt(_timeline.size - 1)  // ❌ 移除了 ToolCallItem
}
```

**问题**:
- `addLiveTerminal()` 被调用时，会移除前一个 `ToolCallItem`
- 导致 UI 中看不到 Shell 工具的调用记录
- 只看到 LiveTerminalItem，缺少完整的上下文

### 问题 5：renderToolResult 在 Live session 时直接返回

**位置**: `ComposeRenderer.kt` 第 246 行

**原始代码**:
```kotlin
val isLiveSession = metadata["isLiveSession"] == "true"
if (isLiveSession) {
    // Live terminal already rendered - just mark tool call as complete
    _currentToolCall = null
    return  // ❌ 直接返回，不处理结果
}
```

**问题**:
- 当是 Live session 时，直接返回，不处理任何结果
- 导致终端输出结果没有被添加到 UI 中
- 用户看不到执行结果摘要

---

## 修复方案

### 修复 1：改进 ToolOrchestrator 的执行流程

**文件**: `ToolOrchestrator.kt` 第 143-152 行

**修改后代码**:
```kotlin
val result = if (liveSession != null) {
    // 对于 Live PTY，我们需要等待完成并获取输出
    // 同时仍然通过普通执行流程获取完整结果（stdout/stderr）
    val shellExecutor = getShellExecutor(registry.getTool(toolName) as cc.unitmesh.agent.tool.impl.ShellTool)
    
    // 方案 1：等待 PTY 进程完成
    val exitCode = try {
        if (shellExecutor is LiveShellExecutor) {
            shellExecutor.waitForSession(liveSession, context.timeout)
        } else {
            -1
        }
    } catch (e: Exception) {
        logger.error(e) { "Error waiting for live session: ${e.message}" }
        -1
    }
    
    // 方案 2：通过普通执行流程获取完整的 stdout/stderr
    // 这样可以同时获得 Live 显示和完整的结果输出
    executeToolInternal(toolName, params, context).let { normalResult ->
        // 如果 Live 执行返回了不同的 exit code，更新结果中的元数据
        if (exitCode >= 0) {
            val metadata = normalResult.extractMetadata().toMutableMap()
            metadata["live_exit_code"] = exitCode.toString()
            
            when (normalResult) {
                is ToolResult.Success -> ToolResult.Success(normalResult.content, metadata)
                is ToolResult.Error -> ToolResult.Error(normalResult.message, normalResult.errorType, metadata)
                is ToolResult.AgentResult -> ToolResult.AgentResult(normalResult.success, normalResult.content, metadata)
            }
        } else {
            normalResult
        }
    }
} else {
    // 普通执行
    executeToolInternal(toolName, params, context)
}
```

**改进点**:
✅ 当使用 Live PTY 时，仍然调用 `executeToolInternal()` 获取完整的 stdout/stderr  
✅ PTY 的 exit code 被记录到元数据中作为参考  
✅ 用户能同时看到实时的终端输出和最终的结构化结果  

### 修复 2：不移除 ToolCallItem

**文件**: `ComposeRenderer.kt` 第 410-432 行

**修改后代码**:
```kotlin
/**
 * Adds a live terminal session to the timeline.
 * This is called when a Shell tool is executed with PTY support.
 *
 * Note: We keep the ToolCallItem so the user can see both the command call
 * and the live terminal output side by side.
 */
override fun addLiveTerminal(
    sessionId: String,
    command: String,
    workingDirectory: String?,
    ptyHandle: Any?
) {
    // Add the live terminal item to the timeline
    // We no longer remove the ToolCallItem - both should be shown for complete visibility
    _timeline.add(
        TimelineItem.LiveTerminalItem(
            sessionId = sessionId,
            command = command,
            workingDirectory = workingDirectory,
            ptyHandle = ptyHandle
        )
    )
}
```

**改进点**:
✅ ToolCallItem 被保留，用户能看到完整的工具调用链  
✅ LiveTerminalItem 显示实时终端输出  
✅ 两者一起显示，提供完整的执行上下文  

### 修复 3：改进 renderToolResult 处理 Live session

**文件**: `ComposeRenderer.kt` 第 237-280 行

**修改后代码**:
```kotlin
override fun renderToolResult(
    toolName: String,
    success: Boolean,
    output: String?,
    fullOutput: String?,
    metadata: Map<String, String>
) {
    val summary = formatToolResultSummary(toolName, success, output)

    // Check if this was a live terminal session
    val isLiveSession = metadata["isLiveSession"] == "true"
    val liveExitCode = metadata["live_exit_code"]?.toIntOrNull()

    // For shell commands, use special terminal output rendering
    val toolType = toolName.toToolType()
    if (toolType == ToolType.Shell && output != null) {
        // Try to extract shell result information
        val exitCode = liveExitCode ?: (if (success) 0 else 1)
        val executionTime = metadata["execution_time_ms"]?.toLongOrNull() ?: 0L

        // Extract command from the last tool call if available
        val command = _currentToolCall?.details?.removePrefix("Executing: ") ?: "unknown"

        // For Live sessions, we show both the terminal widget and the result summary
        // Don't remove anything, just add a result item after the live terminal
        if (isLiveSession) {
            // Add a summary result item after the live terminal
            _timeline.add(
                TimelineItem.TerminalOutputItem(
                    command = command,
                    output = fullOutput ?: output,
                    exitCode = exitCode,
                    executionTimeMs = executionTime
                )
            )
        } else {
            // For non-live sessions, replace the combined tool item with terminal output
            val lastItem = _timeline.lastOrNull()
            if (lastItem is TimelineItem.CombinedToolItem && lastItem.toolType == ToolType.Shell) {
                _timeline.removeAt(_timeline.size - 1)
            }

            _timeline.add(
                TimelineItem.TerminalOutputItem(
                    command = command,
                    output = fullOutput ?: output,
                    exitCode = exitCode,
                    executionTimeMs = executionTime
                )
            )
        }
    } else {
        // Update the last CombinedToolItem with result information
        // ... (existing code)
    }
}
```

**改进点**:
✅ Live session 不再直接返回，而是添加结果摘要  
✅ PTY 的 exit code 被正确使用  
✅ 用户能看到 Live 输出和结构化的执行结果  

---

## 执行流程图

### 原始流程（有问题）

```
executeToolCall()
  ├─ 检查权限
  └─ 是 Shell 工具？
      ├─ 是 → 检查支持 Live PTY？
      │       ├─ 是 → startLiveExecution()
      │       │       renderer.addLiveTerminal()  // 移除 ToolCallItem ❌
      │       │       waitForLiveSession()        // 返回简短消息 ❌
      │       │       ToolExecutionResult (结果为空) ❌
      │       └─ 否 → executeToolInternal() ✅
      └─ 否 → executeToolInternal() ✅
```

### 修复后流程

```
executeToolCall()
  ├─ 检查权限
  └─ 是 Shell 工具？
      ├─ 是 → 检查支持 Live PTY？
      │       ├─ 是 → startLiveExecution()
      │       │       renderer.addLiveTerminal()  // 保留 ToolCallItem ✅
      │       │       waitForLiveSession()        // 获取 exit code
      │       │       executeToolInternal()       // 获取完整输出 ✅
      │       │       ToolExecutionResult (完整结果) ✅
      │       └─ 否 → executeToolInternal() ✅
      └─ 否 → executeToolInternal() ✅
```

---

## 修复后的行为

### 用户看到的 UI 效果

1. **ToolCallItem** - 显示 Shell 命令和参数
2. **LiveTerminalItem** - 显示实时终端输出（JediTerm widget）
3. **TerminalOutputItem** - 显示执行结果摘要（exit code、执行时间等）

### ToolExecutionResult 中的数据

```kotlin
ToolExecutionResult(
    executionId = "xxx",
    toolName = "shell",
    result = ToolResult.Success(
        content = "输出内容...",
        metadata = mapOf(
            "command" to "ls -la",
            "exit_code" to "0",
            "execution_time_ms" to "1234",
            "live_exit_code" to "0",  // 新增
            "isLiveSession" to "true"
        )
    ),
    startTime = ...,
    endTime = ...,
    metadata = mapOf(
        "isLiveSession" to "true",
        "sessionId" to "xxx-yyy-zzz"
    )
)
```

---

## 测试建议

1. **测试 Live PTY 执行**
   ```bash
   # 执行 Shell 命令，验证 ToolExecutionResult 不为空
   shell command="ls -la /tmp" workingDirectory="/tmp"
   ```

2. **验证 UI 显示**
   - ✅ ToolCallItem 显示
   - ✅ LiveTerminalItem 显示实时输出
   - ✅ TerminalOutputItem 显示结果摘要

3. **检查元数据**
   - ✅ `isLiveSession` = "true"
   - ✅ `live_exit_code` 存在
   - ✅ 完整的 stdout/stderr 在 result.content 中

---

## 相关文件

- `/mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/orchestrator/ToolOrchestrator.kt`
- `/mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ComposeRenderer.kt`
- `/mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/tool/shell/PtyShellExecutor.kt`
- `/mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/terminal/TerminalWidget.kt`

---

## 构建验证

✅ 编译成功：`./gradlew :mpp-core:compileKotlinJvm`  
✅ 编译成功：`./gradlew :mpp-ui:compileKotlinJvm`  
✅ 无语法错误
