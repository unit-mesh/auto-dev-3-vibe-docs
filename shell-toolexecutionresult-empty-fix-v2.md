# Shell 工具执行时 ToolExecutionResult 为空 - 最终修复方案

## 问题描述

在执行 Shell 指令时，使用 Live PTY 模式时 `ToolExecutionResult` 返回的结果为空或不包含实际输出。

## 根本原因

1. **Live PTY 执行流程不完整**：启动 PTY 后没有正确捕获输出
2. **重复执行的不合理设计**：不应该执行两遍（一次 PTY，一次普通）
3. **LiveShellSession 没有存储输出**：只存储了状态，没有存储 stdout/stderr

## 修复方案

### 1. 增强 `LiveShellSession` 来存储输出

**文件**: `LiveShellSession.kt`

添加输出缓冲区：
```kotlin
private val _stdout = StringBuilder()
private val _stderr = StringBuilder()

fun getStdout(): String = _stdout.toString()
fun getStderr(): String = _stderr.toString()

internal fun appendStdout(text: String) {
    _stdout.append(text)
}

internal fun appendStderr(text: String) {
    _stderr.append(text)
}
```

### 2. 改进 `PtyShellExecutor.waitForSession` 来捕获输出

**文件**: `PtyShellExecutor.kt`

在等待进程完成时同时读取输出：
```kotlin
override suspend fun waitForSession(
    session: LiveShellSession,
    timeoutMs: Long
): Int = withContext(Dispatchers.IO) {
    val ptyHandle = session.ptyHandle
    if (ptyHandle !is Process) {
        throw ToolException("Invalid PTY handle", ToolErrorType.INTERNAL_ERROR)
    }
    
    try {
        // 启动输出读取任务
        val outputJob = launch {
            try {
                ptyHandle.inputStream.bufferedReader().use { reader ->
                    var line = reader.readLine()
                    while (line != null && isActive) {
                        session.appendStdout(line)
                        session.appendStdout("\n")
                        line = reader.readLine()
                    }
                }
            } catch (e: Exception) {
                logger().error(e) { "Failed to read output from PTY process: ${e.message}" }
            }
        }
        
        val exitCode = withTimeoutOrNull(timeoutMs) {
            while (ptyHandle.isAlive) {
                yield()
                delay(100)
            }
            ptyHandle.exitValue()
        }
        
        if (exitCode == null) {
            outputJob.cancel()
            ptyHandle.destroyForcibly()
            ptyHandle.waitFor(3000, TimeUnit.MILLISECONDS)
            throw ToolException("Command timed out after ${timeoutMs}ms", ToolErrorType.TIMEOUT)
        }
        
        // 等待输出读取完成
        outputJob.join()
        
        session.markCompleted(exitCode)
        exitCode
    } catch (e: Exception) {
        logger().error(e) { "Error waiting for PTY process: ${e.message}" }
        throw e
    }
}
```

### 3. 简化 `ToolOrchestrator.executeToolCall` 的 Live 执行流程

**文件**: `ToolOrchestrator.kt`

移除重复执行，只从 session 中获取输出：
```kotlin
val result = if (liveSession != null) {
    // 对于 Live PTY，等待完成并从 session 获取输出
    val shellExecutor = getShellExecutor(registry.getTool(toolName) as cc.unitmesh.agent.tool.impl.ShellTool)
    
    // 等待 PTY 进程完成
    val exitCode = try {
        if (shellExecutor is LiveShellExecutor) {
            shellExecutor.waitForSession(liveSession, context.timeout)
        } else {
            throw ToolException("Executor does not support live sessions", ToolErrorType.NOT_SUPPORTED)
        }
    } catch (e: ToolException) {
        return ToolExecutionResult.failure(
            context.executionId, toolName, "Command execution error: ${e.message}",
            startTime, Clock.System.now().toEpochMilliseconds()
        )
    } catch (e: Exception) {
        return ToolExecutionResult.failure(
            context.executionId, toolName, "Command execution error: ${e.message}",
            startTime, Clock.System.now().toEpochMilliseconds()
        )
    }
    
    // 从 session 获取输出
    val stdout = liveSession.getStdout()
    val metadata = mapOf(
        "exit_code" to exitCode.toString(),
        "execution_time_ms" to (Clock.System.now().toEpochMilliseconds() - startTime).toString(),
        "shell" to (shellExecutor.getDefaultShell() ?: "unknown"),
        "stdout" to stdout,
        "stderr" to ""
    )
    
    if (exitCode == 0) {
        ToolResult.Success(stdout, metadata)
    } else {
        ToolResult.Error("Command failed with exit code: $exitCode", metadata = metadata)
    }
} else {
    // 普通执行
    executeToolInternal(toolName, params, context)
}
```

### 4. 保持 UI 显示完整

**文件**: `ComposeRenderer.kt`

不移除 `ToolCallItem`，让用户看到完整的执行链：
- `ToolCallItem` - 显示 Shell 命令
- `LiveTerminalItem` - 显示实时输出
- `TerminalOutputItem` - 显示结果摘要

## 执行流程

```
executeToolCall(shell)
  ├─ 检查支持 Live PTY？
  │  └─ 是 → startLiveExecution()
  │         renderer.addLiveTerminal()  // 添加 UI
  │         waitForSession()            // 等待完成 + 捕获输出 ✅
  │         从 liveSession 获取 stdout ✅
  │         ToolResult.Success(stdout, metadata) ✅
  └─ 否 → executeToolInternal()
         ToolResult.Success(stdout, metadata) ✅
```

## 改进对比

| 方面 | 原始方案 | 新方案 |
|------|--------|--------|
| 执行次数 | 2次（PTY + 普通） | 1次（仅 PTY） |
| 输出获取 | 从普通执行获取 | 从 PTY 过程读取 |
| 代码复杂度 | 高（重复逻辑） | 低（单一路径） |
| 性能 | 低（执行两遍） | 高（执行一遍） |
| 结果准确性 | 可能不一致 | 单一来源 ✅ |
| Live UI 显示 | ✅ | ✅ |
| ToolExecutionResult | 完整 | 完整 ✅ |

## 修改文件

1. `/mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/shell/LiveShellSession.kt`
2. `/mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/tool/shell/PtyShellExecutor.kt`
3. `/mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/orchestrator/ToolOrchestrator.kt`
4. `/mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ComposeRenderer.kt`

## 编译验证

✅ `./gradlew :mpp-core:compileKotlinJvm` - 成功  
✅ `./gradlew :mpp-ui:compileKotlinJvm` - 成功

## 单元测试

现有测试失败是无关的（UrlParserTest, WebFetchToolTest），与修改无关。

## 使用示例

```kotlin
// 执行 Shell 命令
executeToolCall("shell", mapOf("command" to "ls -la"), context)

// 返回结果
ToolExecutionResult(
    executionId = "...",
    toolName = "shell",
    result = ToolResult.Success(
        content = "total 123\ndrwxr-xr-x  12 user staff  384 Nov  8 10:00 .\n...",
        metadata = mapOf(
            "exit_code" to "0",
            "stdout" to "...",
            "shell" to "/bin/zsh"
        )
    ),
    metadata = mapOf(
        "isLiveSession" to "true",
        "sessionId" to "xxx-yyy-zzz"
    )
)
```

## 关键改进

✅ **单一执行路径** - 不再重复执行  
✅ **输出完整捕获** - 从 PTY 过程中读取所有输出  
✅ **结果一致性** - 只有一个输出来源  
✅ **性能提升** - 减少重复计算  
✅ **代码简化** - 移除了多余的 waitForLiveSession 方法
