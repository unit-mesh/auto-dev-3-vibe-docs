# ACP 问题修复总结

## 问题分析

根据您提供的信息和图片，发现了以下问题：

### 1. ✅ ACP 会话在 newChat 时没有重置（已修复）

**问题现象：**
- 用户点击 "New Chat" 后，虽然 UI 清空了，但 ACP 连接没有断开
- Agent 侧仍然保留着之前的会话上下文
- 例如：让 Gemini 画架构图，然后 newChat，Agent 仍然记得之前的对话

**根本原因：**
`CodingAgentViewModel.newSession()` 只清理了本地状态，没有重置 ACP 连接

**修复内容：**
文件：`mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/CodingAgentViewModel.kt`

```kotlin
fun newSession() {
    // ... 原有的清理逻辑 ...
    
    // CRITICAL FIX: Disconnect and reconnect ACP session for new chat
    // ACP agents maintain conversation context on their side, so we must
    // explicitly disconnect and reconnect to get a fresh session
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

### 2. ✅ Gemini CLI 的 Bash Tool 通配符问题（已修复）

**问题现象：**
- Gemini 执行包含通配符的 bash 命令时失败
- 例如：`ls *.kt`, `find . -name '*.gradle.kts'`, `echo src/**/*.kt`

**根本原因：**
`AcpClientSessionOps.terminalCreate()` 使用 `ProcessBuilder` 直接执行命令，但 `ProcessBuilder` 不会扩展 shell 通配符。通配符需要通过 shell（如 bash/zsh）来扩展。

**修复内容：**
文件：`mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/AcpClientSessionOps.kt`

```kotlin
override suspend fun terminalCreate(...): CreateTerminalResponse {
    // CRITICAL FIX: Execute through shell to support wildcards and shell features
    // Direct ProcessBuilder doesn't expand wildcards - the shell needs to do it
    val fullCommand = if (args.isEmpty()) {
        command
    } else {
        "$command ${args.joinToString(" ")}"
    }

    // Detect OS and use appropriate shell
    val osName = System.getProperty("os.name").lowercase()
    val cmdList = when {
        osName.contains("win") -> listOf("cmd", "/c", fullCommand)
        else -> {
            // Use bash if available, fallback to sh
            val shell = File("/bin/bash").takeIf { it.exists() }?.absolutePath 
                ?: File("/bin/sh").absolutePath
            listOf(shell, "-c", fullCommand)
        }
    }
    
    // ... rest of the code ...
}
```

**为什么修复有效：**
- 原来：`ProcessBuilder(["ls", "*.kt"])` → 直接执行，shell 不参与，通配符当作普通字符串
- 修复后：`ProcessBuilder(["/bin/bash", "-c", "ls *.kt"])` → 通过 bash 执行，bash 扩展通配符

### 3. ⚠️  PlantUML 架构图生成问题（待验证）

**问题现象：**
- 用户让 Gemini 画 DDD 架构图
- Agent 告诉用户 "end" 就结束了，但没有生成图

**可能原因：**
1. **ContentBlock.Resource 处理不正确**：
   - Gemini 可能返回 `ContentBlock.Resource` 类型
   - 当前的 `handleResourceContent` 只是 toString，可能没有正确处理

2. **停止原因判断有误**：
   - `stopReason` 可能是 `END_TURN` 但应该算作成功
   - 当前代码可能错误地提前结束

**建议验证方法：**
1. 查看 ACP 日志查看实际返回的 ContentBlock 类型
2. 使用调试 CLI 测试画图场景
3. 改进 Resource ContentBlock 的处理逻辑

## 已创建的工具和文档

### 1. ACP Debug CLI

**位置：** `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/server/cli/AcpDebugCli.kt`

**用法：**
```bash
# 测试通配符
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=wildcard"

# 测试会话生命周期
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=session"

# 测试 bash 命令
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=bash"
```

**功能：**
- wildcard: 测试各种通配符和 glob 模式
- session: 测试 ACP 会话的连接、断开和重连
- bash: 测试各种 bash 命令的执行

### 2. 文档

**ACP 调试指南：** `docs/test-scripts/acp-debugging-guide.md`
- 详细描述了所有问题和修复方案
- 包含调试步骤和验证方法
- 提供了日志查看命令

**CLI 使用文档：** `docs/test-scripts/acp-debug-cli-README.md`
- 快速开始指南
- 详细的用法说明
- 常见问题解答
- 日志查看方法

## 验证步骤

### 1. 验证通配符修复

```bash
# 使用调试 CLI
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=wildcard"

# 或在 UI 中测试
# 打开 xiuper -> 选择 Gemini -> 发送命令：请列出所有 Kotlin 文件
```

### 2. 验证会话重置

```bash
# 使用调试 CLI
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=session"

# 或在 UI 中测试
# 1. 发送第一个问题："你好"
# 2. 点击 "New Chat"
# 3. 发送第二个问题："2+2等于多少？"
# 4. 验证 Gemini 不应该记得之前的 "你好"
```

### 3. 查看 ACP 日志

```bash
# 查看最新的 ACP 日志
tail -f ~/.autodev/acp-logs/Gemini_*.jsonl | jq .

# 查看 Tool Call 事件
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.update_type == "ToolCallUpdate")'

# 查看终端命令
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.update.rawInput != null) | .update.rawInput'

# 查看 ContentBlock 类型
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.update.content != null) | .update.content.blockType'
```

## 测试 & 构建

### 编译项目

```bash
cd /Users/phodal/ai/xiuper

# 编译 JVM 代码
./gradlew :mpp-core:compileKotlinJvm :mpp-ui:compileKotlinJvm

# 或编译所有模块
./gradlew compileKotlin
```

### 运行测试

```bash
# 运行核心测试
./gradlew :mpp-core:test

# 运行 UI 测试
./gradlew :mpp-ui:test
```

## 技术细节

### ACP SDK 版本

当前使用：`com.agentclientprotocol:acp:0.15.3`（Kotlin SDK）

### 参考实现

本地 ACP 参考实现：`/Users/phodal/ai/agent-client-protocol`

### 修改的文件

1. ✅ `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/CodingAgentViewModel.kt`
   - 修复：newSession 时重置 ACP 连接

2. ✅ `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/AcpClientSessionOps.kt`
   - 修复：terminalCreate 支持通配符

3. ✅ `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/server/cli/AcpDebugCli.kt`
   - 新增：调试 CLI 工具

4. ✅ `docs/test-scripts/acp-debugging-guide.md`
   - 新增：详细的调试指南

5. ✅ `docs/test-scripts/acp-debug-cli-README.md`
   - 新增：CLI 使用文档

## 下一步建议

1. **运行调试 CLI** 验证通配符修复
   ```bash
   ./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=wildcard"
   ```

2. **测试会话重置** 确认 newSession 正确工作
   ```bash
   ./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=session"
   ```

3. **测试 PlantUML 场景** 让 Gemini 画架构图，查看日志
   - 检查 ContentBlock 类型
   - 验证图是否正确生成

4. **查看 ACP 日志** 分析实际的 Agent 行为
   ```bash
   tail -f ~/.autodev/acp-logs/Gemini_*.jsonl | jq .
   ```

5. **如果仍有问题**：
   - 查看日志中的 stopReason
   - 检查 ContentBlock.Resource 的内容
   - 改进 handleResourceContent 的处理逻辑

## 总结

✅ **已修复：**
- ACP 会话在 newChat 时正确重置
- Bash Tool 支持通配符和 shell 扩展
- 创建了调试 CLI 工具和详细文档

⚠️  **待验证：**
- PlantUML 架构图生成问题（需要运行实际测试和查看日志）

📚 **已添加：**
- 完整的调试工具
- 详细的使用文档
- 调试和验证指南
