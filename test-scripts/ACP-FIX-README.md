# ACP 问题修复与调试工具

## 快速总结

本次修复了两个关键的 ACP (Agent Client Protocol) 问题：

1. ✅ **ACP 会话重置问题** - newChat 时没有断开 ACP 连接
2. ✅ **Bash Tool 通配符问题** - 通配符命令执行失败

并创建了完整的调试工具来验证和测试这些修复。

## 🔧 已修复的问题

### 1. ACP 会话重置问题

**文件：** `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/CodingAgentViewModel.kt`

**问题：** 用户点击 "New Chat" 后，虽然 UI 清空了，但 ACP Agent 仍然保留着之前的会话上下文。

**修复：** 在 `newSession()` 中添加了 ACP 断开重连逻辑：

```kotlin
if (currentEngine == GuiAgentEngine.ACP && currentAcpAgentConfig != null) {
    scope.launch {
        println("[ACP] Resetting session for new chat...")
        disconnectAcp()
        // The next prompt will trigger a fresh connection
    }
}
```

### 2. Bash Tool 通配符问题

**文件：** `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/AcpClientSessionOps.kt`

**问题：** Gemini 执行包含通配符的命令（如 `ls *.kt`）时失败。

**根本原因：** `ProcessBuilder` 不会扩展 shell 通配符，需要通过 shell（bash/zsh）来扩展。

**修复：** 修改 `terminalCreate()` 通过 shell 执行命令：

```kotlin
// 原来：ProcessBuilder(["ls", "*.kt"])
// 修复：ProcessBuilder(["/bin/bash", "-c", "ls *.kt"])

val fullCommand = "$command ${args.joinToString(" ")}"
val cmdList = listOf("/bin/bash", "-c", fullCommand)
```

## 🛠️ 调试工具

### ACP Debug CLI

**位置：** `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/server/cli/AcpDebugCli.kt`

用于测试和验证 ACP 相关问题的命令行工具。

**用法：**

```bash
# 测试通配符（验证 Bash Tool 修复）
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=wildcard"

# 测试会话生命周期（验证 newSession 修复）
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=session"

# 测试各种 bash 命令
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=bash"
```

**测试场景：**

- **wildcard**: 测试 `ls *.kt`、`find . -name '*.gradle.kts'` 等通配符命令
- **session**: 测试连接→提问→断开→重连→新提问的完整生命周期
- **bash**: 测试 `pwd`、`echo`、管道命令等各种 bash 功能

## 📚 文档

创建了三个文档来帮助理解和使用：

1. **调试指南** - `docs/test-scripts/acp-debugging-guide.md`
   - 详细的问题分析
   - 修复方案说明
   - 调试步骤和方法

2. **CLI 使用文档** - `docs/test-scripts/acp-debug-cli-README.md`
   - 快速开始指南
   - 详细用法说明
   - 日志查看方法
   - 常见问题解答

3. **修复总结** - `docs/test-scripts/acp-fix-summary.md`
   - 完整的修复总结
   - 验证步骤
   - 技术细节

## 🔍 查看 ACP 日志

ACP 日志自动保存到 `~/.autodev/acp-logs/`：

```bash
# 实时查看最新日志
tail -f ~/.autodev/acp-logs/Gemini_*.jsonl | jq .

# 查看 Tool Call 事件
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.update_type == "ToolCallUpdate")'

# 查看终端命令
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.update.rawInput != null) | .update.rawInput'

# 查看停止原因
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.event_type == "PromptResponse") | .stop_reason'
```

## ✅ 验证步骤

### 1. 验证通配符修复

```bash
# 方法 1: 使用调试 CLI
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=wildcard"

# 方法 2: 在 UI 中测试
# - 打开 xiuper
# - 选择 Gemini 
# - 发送命令："请列出所有 Kotlin 文件（使用 ls *.kt）"
# - 验证命令成功执行
```

### 2. 验证会话重置

```bash
# 方法 1: 使用调试 CLI
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=session"

# 方法 2: 在 UI 中测试
# 1. 发送第一个问题："你好，我是 Alice"
# 2. 点击 "New Chat" 按钮
# 3. 发送第二个问题："我的名字是什么？"
# 4. 验证 Gemini 应该回答不知道（不应该记得 Alice）
```

## 🔨 构建与测试

```bash
cd /Users/phodal/ai/xiuper

# 编译项目
./gradlew :mpp-core:compileKotlinJvm :mpp-ui:compileKotlinJvm

# 运行测试
./gradlew :mpp-core:jvmTest

# 清理构建
./gradlew clean
```

## 🐛 待验证问题

### PlantUML 架构图生成

**问题：** Gemini 说 "end" 就结束了，但没有生成图

**可能原因：**
- ContentBlock.Resource 处理不正确
- stopReason 判断有误

**调试方法：**
1. 让 Gemini 画一个 DDD 架构图
2. 查看日志中的 ContentBlock 类型：
   ```bash
   cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.update.content != null) | .update.content.blockType'
   ```
3. 检查 stopReason：
   ```bash
   cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.event_type == "PromptResponse") | .stop_reason'
   ```

## 📝 修改的文件

1. `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/CodingAgentViewModel.kt`
2. `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/AcpClientSessionOps.kt`
3. `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/server/cli/AcpDebugCli.kt` (新增)
4. `docs/test-scripts/acp-debugging-guide.md` (新增)
5. `docs/test-scripts/acp-debug-cli-README.md` (新增)
6. `docs/test-scripts/acp-fix-summary.md` (新增)

## 🚀 下一步

1. **运行调试 CLI** 验证修复
2. **测试实际场景** 在 UI 中使用 Gemini
3. **查看日志** 分析 Agent 行为
4. **如果有 PlantUML 问题** 使用日志进行深入调试

## 📖 参考

- ACP 规范: `/Users/phodal/ai/agent-client-protocol`
- ACP Kotlin SDK: `com.agentclientprotocol:acp:0.15.3`
- 日志目录: `~/.autodev/acp-logs/`
- AGENTS.md: 项目的开发规范

---

**编译测试通过** ✅  
**日期：** 2026-02-06  
**修复者：** Claude (Sonnet 4.5)
