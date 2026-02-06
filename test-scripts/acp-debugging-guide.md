# ACP Debugging Guide

## 已发现的问题

### 1. ACP 会话在 newChat 时没有重置

**问题描述：**
当用户点击 "New Chat" 时，虽然 UI 清空了，但 ACP 连接没有断开重连，导致 Agent 侧仍然保留着之前的会话上下文。

例如：
1. Session 1: 用户让 Gemini 画架构图
2. 点击 "New Chat"
3. Session 2: 用户发送新的请求
4. Gemini 仍然认为是在同一个会话中，可能会引用之前的上下文

**根本原因：**
`CodingAgentViewModel.newSession()` 只清理了本地状态，没有重置 ACP 连接：

```kotlin
fun newSession() {
    // ✅ 清理了本地状态
    renderer.clearMessages()
    chatHistoryManager?.createSession()
    _codingAgent?.clearConversation()
    FileChangeTracker.clearChanges()
    
    // ❌ 但没有重置 ACP 连接
    // acpConnection 仍然连接着，Agent 侧的会话没有重置
}
```

**解决方案：**
已修复 - 在 `newSession()` 中添加了 ACP 断开重连逻辑：

```kotlin
fun newSession() {
    // ... 原有的清理逻辑 ...
    
    // CRITICAL FIX: Disconnect and reconnect ACP session for new chat
    if (currentEngine == GuiAgentEngine.ACP && currentAcpAgentConfig != null) {
        scope.launch {
            try {
                println("[ACP] Resetting session for new chat...")
                disconnectAcp()
                // The next prompt will trigger a fresh connection
            } catch (e: Exception) {
                println("[ACP] Failed to reset session: ${e.message}")
            }
        }
    }
}
```

**验证方法：**
```bash
# 使用调试 CLI 测试会话生命周期
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=session"
```

### 2. Gemini CLI 的 Bash Tool 通配符问题

**问题描述：**
Gemini CLI 执行包含通配符的 bash 命令时失败，例如：
- `ls *.kt`
- `find . -name '*.gradle.kts'`
- `echo src/**/*.kt`

**可能的原因：**

1. **Shell 扩展问题**：
   - Gemini 可能直接执行命令而不通过 shell
   - 通配符需要 shell 来扩展（如 bash/zsh）

2. **工作目录问题**：
   - 命令的 cwd 可能不正确
   - Gemini 使用的 terminal/create 可能没有正确设置工作目录

3. **Terminal 能力问题**：
   - ACP ClientSessionOps 的 terminal 实现可能有问题
   - Java ProcessBuilder 的 shell 调用方式不对

**调试步骤：**

1. **检查 ACP 日志**：
```bash
# 日志位置
~/.autodev/acp-logs/

# 查看最近的 Gemini 日志
tail -f ~/.autodev/acp-logs/Gemini_*.jsonl | jq .
```

2. **使用调试 CLI 测试**：
```bash
# 测试通配符
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=wildcard"

# 测试 bash 命令
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=bash"
```

3. **检查 AcpClientSessionOps 的 terminal 实现**：
```kotlin
// mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/AcpClientSessionOps.kt

override suspend fun terminalCreate(
    command: String,
    args: List<String>,
    cwd: String?,
    env: List<EnvVariable>,
    outputByteLimit: ULong?,
    _meta: JsonElement?,
): CreateTerminalResponse {
    // 问题可能在这里：
    // 1. ProcessBuilder 是否正确处理通配符？
    // 2. 是否需要通过 shell 执行？
    
    val cmdList = mutableListOf(command).apply { addAll(args) }
    val pb = ProcessBuilder(cmdList)  // ❌ 这不会扩展通配符！
    
    // 解决方案：通过 shell 执行
    // val pb = ProcessBuilder("bash", "-c", "$command ${args.joinToString(" ")}")
}
```

**可能的修复方案：**

1. **修改 terminalCreate 使用 shell**：
```kotlin
override suspend fun terminalCreate(
    command: String,
    args: List<String>,
    cwd: String?,
    env: List<EnvVariable>,
    outputByteLimit: ULong?,
    _meta: JsonElement?,
): CreateTerminalResponse {
    val terminalId = "term-${terminalIdCounter.incrementAndGet()}"
    val effectiveCwd = cwd ?: this@AcpClientSessionOps.cwd
    
    // FIX: 通过 shell 执行以支持通配符
    val fullCommand = if (args.isEmpty()) {
        command
    } else {
        "$command ${args.joinToString(" ")}"
    }
    
    // 检测操作系统并使用合适的 shell
    val shell = when {
        System.getProperty("os.name").lowercase().contains("win") -> 
            listOf("cmd", "/c", fullCommand)
        else -> 
            listOf("/bin/bash", "-c", fullCommand)
    }
    
    val pb = ProcessBuilder(shell)
    pb.directory(File(effectiveCwd))
    pb.redirectErrorStream(true)
    env.forEach { envVar -> pb.environment()[envVar.name] = envVar.value }
    
    // ... 其余代码 ...
}
```

2. **对比本地 ACP 实现**：
查看 `/Users/phodal/ai/agent-client-protocol` 中的参考实现，看是否有类似的处理。

### 3. PlantUML 架构图生成问题

**问题描述：**
用户让 Gemini 画 DDD 架构图时，Agent 告诉用户 "end" 就结束了，但实际上没有生成图。

**可能的原因：**

1. **Resource ContentBlock 处理不正确**：
   - Gemini 可能返回 ContentBlock.Resource 类型
   - 当前的 `handleResourceContent` 只是 toString，没有正确处理

```kotlin
// AcpClient.kt
private fun handleResourceContent(block: ContentBlock.Resource, renderer: CodingAgentRenderer) {
    // ❌ 当前实现太简单
    val text = extractText(block)
    renderer.renderLLMResponseChunk(text)
    
    logger.info { "Received ContentBlock.Resource: ${block.resource}" }
}
```

2. **停止原因处理不对**：
```kotlin
// AcpClient.kt
is Event.PromptResponseEvent -> {
    // ...
    val success = event.response.stopReason != StopReason.REFUSAL &&
        event.response.stopReason != StopReason.CANCELLED
    renderer.renderFinalResult(
        success = success,
        message = "ACP finished: ${event.response.stopReason}",
        iterations = 0
    )
}
```

**调试方法：**

1. 查看 ACP 日志中的 ContentBlock 类型：
```bash
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.update_type == "AgentMessageChunk") | .update.content'
```

2. 检查 stopReason：
```bash
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.event_type == "PromptResponse") | .stop_reason'
```

## 使用调试 CLI

已创建 `AcpDebugCli` 用于调试 ACP 相关问题：

```bash
# 构建
./gradlew :mpp-ui:compileKotlin

# 测试通配符
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=wildcard"

# 测试会话生命周期
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=session"

# 测试 bash 命令
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=bash"
```

## 下一步

1. **运行调试 CLI** 确认通配符问题的根本原因
2. **查看 ACP 日志** 分析 Gemini 的实际行为
3. **修复 terminalCreate** 支持通配符
4. **改进 Resource ContentBlock 处理** 正确渲染 PlantUML 等资源
5. **验证修复** 确保所有场景正常工作

## 参考

- ACP 规范: `/Users/phodal/ai/agent-client-protocol`
- ACP Kotlin SDK: `com.agentclientprotocol:acp:0.15.3`
- 日志位置: `~/.autodev/acp-logs/`
- 相关代码:
  - `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/AcpClient.kt`
  - `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/AcpClientSessionOps.kt`
  - `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/CodingAgentViewModel.kt`
