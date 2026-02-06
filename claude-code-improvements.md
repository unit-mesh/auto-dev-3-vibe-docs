# Claude Code 集成改进总结

## 问题诊断与修复

### 问题 1: UI 没有显示内容
**原因**: 缺少 `--include-partial-messages` 参数，导致 Claude Code 只输出最终 `result` 消息，没有中间的 `stream_event` 消息。

**修复**: 
- 添加 `--include-partial-messages` 到命令行参数（第 58 行）
- 添加 fallback 逻辑：如果没有流式内容，则从 `result.result` 字段渲染最终响应文本（第 250-254 行）

### 问题 2: Tool 参数没有显示
**原因**: `content_block_start` 事件中的 `tool_use` 只有 tool name，没有 input 参数。参数通过 `input_json_delta` 流式传输，需要手动收集和解析。

**修复**:
1. **收集 `input_json_delta`** (第 126-127, 196-203 行):
   ```kotlin
   val pendingToolInputs = mutableMapOf<Int, StringBuilder>()
   // 在 input_json_delta 事件中累积 JSON
   pendingToolInputs.getOrPut(index) { StringBuilder() }.append(jsonChunk)
   ```

2. **从 `assistant` 消息解析完整参数** (第 221-244 行):
   ```kotlin
   val inputMap = parseJsonToMap(c.input?.toString())
   renderer.renderToolCallWithParams(toolName, inputMap)
   ```

3. **添加 JSON 解析辅助函数** (第 306-325 行):
   ```kotlin
   private fun parseJsonToMap(jsonStr: String): Map<String, Any>
   ```

### 问题 3: `AskUserQuestion` 工具导致阻塞
**原因**: `AskUserQuestion` 需要交互式终端输入，在 stream-json 模式下无法正常工作。

**修复**: 禁用该工具（第 67 行）:
```kotlin
cmd.addAll(listOf("--disallowed-tools", "AskUserQuestion"))
```

**参考**: zed-industries/claude-code-acp 的实现也明确禁用了此工具：
```typescript
const disallowedTools = ["AskUserQuestion"];
```

## 关键改进对比

### 与 claude-code-acp (TypeScript) 的功能对齐

| 功能 | claude-code-acp (Zed) | 我们的实现 (Kotlin) | 状态 |
|------|------------------------|---------------------|------|
| `--include-partial-messages` | ✅ `includePartialMessages: true` | ✅ 第 58 行 | ✅ |
| 禁用 `AskUserQuestion` | ✅ `disallowedTools` 数组 | ✅ 第 67 行 | ✅ |
| Tool 参数流式解析 | ✅ `input_json_delta` 处理 | ✅ 第 196-203 行 | ✅ |
| Tool info 提取 | ✅ `toolInfoFromToolUse()` | ✅ 第 221-244, 306-325 行 | ✅ |
| `assistant` 消息过滤 | ✅ 过滤 text/thinking | ✅ 第 220-245 行 | ✅ |

## 测试验证

测试脚本已更新：`docs/test-scripts/test-claude-code.sh`

运行测试：
```bash
./docs/test-scripts/test-claude-code.sh
cd mpp-ui && ../gradlew compileKotlin
```

验证检查点：
- [x] `--include-partial-messages` 参数添加
- [x] 流式输出工作正常（`stream_event` 消息）
- [x] Tool 参数正确显示（`renderToolCallWithParams`）
- [x] `AskUserQuestion` 被禁用
- [x] Fallback 渲染（无流式时显示最终 `result`）

## 架构改进

```
ClaudeCodeClient.promptAndRender()
  ├─ Send user message (JSON line)
  ├─ Receive SYSTEM init (session_id)
  ├─ Receive STREAM_EVENT messages:
  │    ├─ content_block_start (thinking/text/tool_use)
  │    ├─ content_block_delta (text_delta/thinking_delta/input_json_delta) ← 收集 tool params
  │    └─ content_block_stop
  ├─ Receive ASSISTANT message:
  │    ├─ tool_use: 完整的 tool + input JSON ← 解析并更新 UI
  │    └─ tool_result: tool 执行结果
  └─ Receive RESULT (success/error) ← Fallback 渲染文本
```

## 后续优化建议

1. **未来可支持 `AskUserQuestion`**:
   - 在 `CodingAgentRenderer` 添加 `renderMultiChoiceQuestion(questions, onResponse)` 方法
   - 在 Claude Code 客户端实现响应发送逻辑（通过 stdin 发送用户选择）
   - 目前暂时禁用，等有需求再实现

2. **Tool 参数格式化**:
   - 当前简单解析为 `Map<String, Any>`
   - 可以根据 tool 类型做更友好的格式化（例如 diff 显示、路径高亮）

3. **Permission 交互**:
   - Claude Code 的权限请求目前通过 `--permission-mode` 和 `--dangerously-skip-permissions` 处理
   - 未来可以实现 UI 层的权限确认对话框

## 文件变更

- `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/claude/ClaudeCodeClient.kt`:
  - 添加 `--include-partial-messages` 参数
  - 添加 `--disallowed-tools AskUserQuestion`
  - 实现 `input_json_delta` 收集逻辑
  - 实现 `assistant` 消息中 tool input 解析
  - 添加 `parseJsonToMap()` 辅助函数
  - 添加 fallback 渲染逻辑

## 参考资料

- [zed-industries/claude-code-acp](https://github.com/zed-industries/claude-code-acp) - TypeScript ACP 适配器
- [Issue #538](https://github.com/phodal/auto-dev/issues/538) - Claude Code 集成需求
- [Claude Code 文档](https://docs.anthropic.com/en/docs/claude-code)
