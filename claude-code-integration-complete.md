# Claude Code 集成 - 完整修复报告

## Issue #538 - Claude Code 集成完成

参考：
- [zed-industries/claude-code-acp](https://github.com/zed-industries/claude-code-acp) (TypeScript)
- [IDEA ml-llm](https://github.com/phodal) implementation
- [Issue #538](https://github.com/phodal/auto-dev/issues/538)

---

## 核心问题与修复

### 1. ✅ UI 无内容显示
**原因**: 缺少 `--include-partial-messages` 参数

**修复**:
```kotlin
// mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/claude/ClaudeCodeClient.kt:58
cmd.add("--include-partial-messages")
```

**验证**: 现在 Claude Code 会输出 `stream_event` 消息，实现流式渲染。

---

### 2. ✅ Tool 参数不显示（Bash 等）
**原因**: `stream_event` 中的 `tool_use` 只有 `input: {}`，完整参数在后续的 `assistant` 消息中。

**日志示例**:
```json
// stream_event - input 为空
{"type":"stream_event","event":{"content_block":{"type":"tool_use","name":"Bash","input":{}}}}

// assistant - 完整参数
{"type":"assistant","content":[{"type":"tool_use","name":"Bash","input":{"command":"ls -la","description":"List project files"}}]}
```

**修复策略**: **延迟渲染** - 不在 `stream_event` 时渲染，等 `assistant` 消息到达时一次性渲染完整的 tool call。

```kotlin
// 在 content_block_start (tool_use) 时：只记录，不渲染
toolUseNames[toolId] = toolName
// DO NOT render yet

// 在 assistant 消息中：渲染完整参数
if (!renderedToolIds.contains(toolId)) {
    val inputMap = parseJsonToMap(c.input.toString())
    renderer.renderToolCallWithParams(toolName, inputMap)
    renderedToolIds.add(toolId)
}
```

---

### 3. ✅ Gradle 无法启动 / 文件无法写入
**原因**: `--permission-mode` 默认为 `default`，需要用户批准每个工具调用。在 `-p` 模式下，Claude Code 无法发送交互式权限请求，导致直接拒绝。

**日志证据**:
```json
{
  "permission_denials": [{
    "tool_name": "Bash",
    "tool_use_id": "call_function_yuqfb93kzdhx_1",
    "tool_input": {"command": "./gradlew bootRun"}
  }]
}
```

**修复**: 默认使用 `--permission-mode acceptEdits`
```kotlin
// mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/claude/ClaudeCodeClient.kt:60-63
val effectivePermissionMode = permissionMode ?: "acceptEdits"
cmd.addAll(listOf("--permission-mode", effectivePermissionMode))
```

**Permission模式说明**:
- `default` - 提示危险操作 ⚠️ 在 `-p` 模式下会阻塞
- `acceptEdits` - 自动批准 Edit/Write ✅ 推荐
- `bypassPermissions` - 绕过所有检查（危险）
- `dontAsk` - 拒绝未预批准的操作
- `plan` - 计划模式，不执行

---

### 4. ✅ AskUserQuestion 导致阻塞
**原因**: 该工具需要交互式终端，`stream-json` 模式不支持。

**修复**: 禁用该工具
```kotlin
cmd.addAll(listOf("--disallowed-tools", "AskUserQuestion"))
```

---

## 完整代码修复

### ClaudeCodeClient.kt 关键修复点

```kotlin
// 1. 启用流式消息
cmd.add("--include-partial-messages")  // 第 58 行

// 2. 设置权限模式避免阻塞
val effectivePermissionMode = permissionMode ?: "acceptEdits"
cmd.addAll(listOf("--permission-mode", effectivePermissionMode))  // 第 60-63 行

// 3. 禁用不兼容的工具
cmd.addAll(listOf("--disallowed-tools", "AskUserQuestion"))  // 第 69 行

// 4. stream_event 中不渲染 tool_use，避免参数缺失
// content_block_start (tool_use) - 只记录，不渲染 (第 173-184 行)

// 5. assistant 消息中渲染完整 tool call
if (!renderedToolIds.contains(toolId)) {
    val inputMap = parseJsonToMap(c.input.toString())
    renderer.renderToolCallWithParams(toolName, inputMap)  // 第 234-239 行
    renderedToolIds.add(toolId)
}

// 6. Fallback: 无流式时从 result 提取文本
if (resultText.isNotEmpty() && !hasRenderedStreamContent) {
    renderer.renderLLMResponseChunk(resultText)  // 第 266-268 行
}
```

---

## 测试验证

### 预期行为修复

| 问题 | 修复前 | 修复后 |
|------|--------|--------|
| 流式输出 | ❌ 无输出 | ✅ 逐字显示 |
| Tool 参数 | ❌ `Bash` (空参数) | ✅ `Bash command="ls -la" description="..."` |
| Gradle 启动 | ❌ 权限拒绝 | ✅ 自动批准（`acceptEdits` 模式） |
| 文件写入 | ❌ 权限拒绝 | ✅ 自动批准 |
| AskUserQuestion | ❌ 阻塞 | ✅ 禁用，不会调用 |

### 手动测试步骤

1. **启动桌面应用**:
   ```bash
   cd mpp-ui && ../gradlew :mpp-compose-app:run
   ```

2. **选择 Claude Code 引擎**

3. **测试命令**:
   - "帮我启动这个项目" - 验证 Bash 工具参数显示和执行
   - "创建一个 README.md 文件" - 验证 Write 工具工作
   - "分析项目结构" - 验证 Read/Glob 等工具

---

## 架构设计对比

### claude-code-acp (TypeScript + Claude Agent SDK)

```typescript
// 使用 Claude Agent SDK (Node.js 包)
import { query } from "@anthropic-ai/claude-agent-sdk";

const q = query({
  prompt: input,
  options: {
    includePartialMessages: true,  // ← 流式消息
    canUseTool: async (toolName, toolInput) => {
      // ← 权限拦截回调
      const response = await client.requestPermission({...});
      return { behavior: "allow" };
    }
  }
});

// SDK 处理所有协议细节
for await (const message of q) {
  // 处理消息
}
```

### 我们的实现 (Kotlin + Claude CLI)

```kotlin
// 直接调用 claude 二进制
val cmd = listOf(
    "claude", "-p",
    "--output-format", "stream-json",
    "--input-format", "stream-json",
    "--include-partial-messages",  // ← 流式消息
    "--permission-mode", "acceptEdits"  // ← 通过 CLI 参数控制权限
)

val process = ProcessBuilder(cmd).start()

// 手动解析 JSON 行
val line = reader.readLine()
val msg = parseClaudeOutputLine(line)
```

**关键差异**:
- SDK 版本：可以通过 `canUseTool` 回调实现 UI 权限请求
- CLI 版本：必须通过 `--permission-mode` 预设权限策略

---

## 配置建议

### ~/.autodev/config.yaml

```yaml
acpAgents:
  claude:
    name: Claude Code
    command: /opt/homebrew/bin/claude
    args: --permission-mode bypassPermissions  # 可选：完全自动化
    env: ''
```

### Permission 模式选择

| 模式 | 适用场景 | 安全性 |
|------|----------|--------|
| `acceptEdits` (默认) | 日常开发，自动批准文件操作 | 中 ⭐⭐⭐ |
| `bypassPermissions` | 沙箱环境，完全自动化 | 低 ⭐ |
| `default` | 需要手动审查每个操作 | 高 ⭐⭐⭐⭐⭐ (但在 -p 模式不可用) |

---

## 已知限制

### 1. 无法实现 UI 层权限对话框
- **原因**: 使用 `claude` CLI 二进制（`-p` 模式），无法拦截权限请求
- **workaround**: 通过 `--permission-mode` 预设策略
- **未来改进**: 如果 Anthropic 发布 JVM/Kotlin SDK，可以实现 `canUseTool` 回调

### 2. AskUserQuestion 不支持
- **原因**: 该工具需要交互式终端，与 stream-json 不兼容
- **workaround**: 禁用该工具（`--disallowed-tools AskUserQuestion`）
- **未来改进**: 在协议层实现 `control_request`/`control_response` 消息处理

### 3. input_json_delta 未使用
- **原因**: 参数流式传输需要复杂的 JSON 流解析和 UI 更新逻辑
- **当前方案**: 等待 `assistant` 消息中的完整参数，一次性渲染
- **未来改进**: 实现增量 JSON 解析和 UI 实时更新

---

## 文件清单

### 新增
- `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/claude/ClaudeCodeProtocol.kt` - 协议定义
- `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/claude/ClaudeCodeClient.kt` - 核心客户端
- `docs/claude-code-improvements.md` - 改进文档
- `docs/test-scripts/test-claude-code.sh` - 测试脚本

### 修改
- `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/agent/acp/AcpConnectionProvider.jvm.kt` - 添加 `JvmClaudeCodeConnection`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/acp/AcpConnectionProvider.kt` - 添加 `createConnectionForAgent()`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/config/AcpAgentPresets.kt` - 更新 Claude 预设
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/CodingAgentViewModel.kt` - 使用新工厂函数
- `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/server/cli/AcpDebugCli.kt` - 添加 `--test=claude` 模式
- 各平台 `AcpConnectionProvider.*.kt` - 添加 `actual createConnectionForAgent()`

---

## 测试命令

```bash
# 1. 编译
cd mpp-ui && ../gradlew compileKotlin

# 2. 运行桌面应用
cd mpp-ui && ../gradlew :mpp-compose-app:run

# 3. CLI 测试
./gradlew :mpp-ui:runAcpDebug --args="--agent=claude --test=claude"

# 4. 验证脚本
./docs/test-scripts/test-claude-code.sh
```

---

## 成功标志

- ✅ JVM/Desktop 编译通过
- ✅ 流式文本输出工作正常
- ✅ Tool 参数完整显示（`command="..."`）
- ✅ Bash/Edit/Write 工具可以执行
- ✅ Permission 不再阻塞
- ✅ 与 claude-code-acp 功能对齐
