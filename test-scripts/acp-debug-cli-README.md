# ACP Debug CLI

用于调试 ACP (Agent Client Protocol) 会话和工具问题的命令行工具。

## 快速开始

```bash
# 构建项目
./gradlew :mpp-ui:compileKotlin

# 运行调试 CLI
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=wildcard"
```

## 用法

```bash
./gradlew :mpp-ui:run --args="acp-debug [OPTIONS]"
```

### 选项

- `--agent=<name>`: 指定要测试的 ACP Agent（例如：Gemini, Kimi, Claude）
- `--test=<type>`: 指定测试类型

### 测试类型

#### 1. wildcard - 通配符测试

测试 Bash tool 的通配符和 glob 模式支持：

```bash
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=wildcard"
```

测试的命令包括：
- `ls *.kt` - 列出所有 Kotlin 文件
- `find . -name '*.gradle.kts'` - 查找 Gradle 文件
- `echo src/**/*.kt` - Glob 模式
- `ls -la | wc -l` - 管道命令

#### 2. session - 会话生命周期测试

测试 ACP 会话的连接、断开和重连：

```bash
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=session"
```

模拟场景：
1. 创建第一个会话，发送简单问题
2. 断开会话
3. 创建新会话（模拟 newChat），发送新问题
4. 验证会话是否正确重置

#### 3. bash - Bash 命令测试

测试各种 bash 命令的执行：

```bash
./gradlew :mpp-ui:run --args="acp-debug --agent=Gemini --test=bash"
```

测试的命令包括：
- `pwd` - 打印工作目录
- `ls` - 列出文件
- `echo 'Hello World'` - 输出字符串
- `cat README.md | head -5` - 管道命令
- `find . -name '*.kt' | wc -l` - 查找并计数
- `ls *.kt` - 通配符
- `echo *.gradle.kts` - Shell 扩展

## 配置 Agent

在 `~/.autodev/config.yaml` 中配置 ACP Agent：

```yaml
acpAgents:
  Gemini:
    name: "Gemini"
    command: "/path/to/gemini"
    args: ["--acp"]
    env:
      API_KEY: "your-api-key"
  
  Kimi:
    name: "Kimi"
    command: "/path/to/kimi"
    args: []
    env:
      KIMI_API_KEY: "your-api-key"
```

## 查看日志

ACP 日志会自动保存到：

```bash
~/.autodev/acp-logs/<AgentName>_<timestamp>.jsonl
```

查看日志：

```bash
# 查看最新的 Gemini 日志
tail -f ~/.autodev/acp-logs/Gemini_*.jsonl | jq .

# 过滤特定事件类型
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.event_type == "PromptResponse")'

# 查看 Tool Call 事件
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.update_type == "ToolCallUpdate")'

# 查看终端命令
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.update.rawInput != null) | .update.rawInput'
```

## 输出示例

```
🔍 ACP Debug CLI
============================================================
🤖 Testing agent: Gemini
📝 Command: /usr/local/bin/gemini
🧪 Test type: wildcard

🧪 Testing wildcard/glob patterns
------------------------------------------------------------

Test 1: List all Kotlin files: ls *.kt
----------------------------------------
🔌 Connecting to Gemini...
✅ Connected (logging to ~/.autodev/acp-logs/)
👤 User: List all Kotlin files: ls *.kt
🤖 LLM Response Start
...
🔧 Tool: Bash(command=ls *.kt)
✅ Result: Main.kt Platform.kt ...
🤖 LLM Response End
✅ Final: ACP finished: END_TURN (iterations: 0)
Result: Main.kt Platform.kt ...

...
```

## 常见问题

### 1. 通配符不工作

**症状**：`ls *.kt` 返回 "No such file or directory"

**原因**：ProcessBuilder 不扩展通配符，需要通过 shell 执行

**解决**：已在 `AcpClientSessionOps.terminalCreate` 中修复，通过 `/bin/bash -c` 执行命令

### 2. 会话没有重置

**症状**：newChat 后 Agent 仍然记得之前的对话

**原因**：`newSession()` 没有断开 ACP 连接

**解决**：已在 `CodingAgentViewModel.newSession()` 中修复，添加了 `disconnectAcp()` 调用

### 3. PlantUML 图不显示

**症状**：Agent 说 "end" 就结束了，但没有生成图

**可能原因**：
- ContentBlock.Resource 处理不正确
- 停止原因判断有误
- Renderer 没有正确渲染资源内容

**调试**：
```bash
# 查看返回的 ContentBlock 类型
cat ~/.autodev/acp-logs/Gemini_*.jsonl | jq 'select(.update.content != null) | .update.content.blockType'
```

## 相关文件

- CLI 实现: `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/server/cli/AcpDebugCli.kt`
- ACP Client: `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/AcpClient.kt`
- Session Ops: `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/AcpClientSessionOps.kt`
- ViewModel: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/CodingAgentViewModel.kt`
- 调试指南: `docs/test-scripts/acp-debugging-guide.md`

## 贡献

如果你发现其他问题或需要添加新的测试类型，欢迎提交 PR。
