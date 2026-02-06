# ACP Custom Renderer - Implementation Progress

## 🎯 Problem Identified

Kimi ACP agent generating **2385 tool calls** for "画一下项目架构图":
- **2279x** `WriteFile: docs/architecture.md` (逐字写入！)
- **18x** `Glob: *.md`
- **15x** `ReadFile`
- **其他** 73个调用

这就是 UI 炸掉的根本原因：每个字符写入都触发一次 `renderToolCall`。

## ✅ What We Built

### 1. 捕获工具 (AcpCaptureCli)
```bash
./gradlew :mpp-ui:runAcpCapture -PacpPrompt="画一下项目架构图"
```
- 保存所有事件到 `.log` (人类可读)
- 保存结构化事件到 `.jsonl` (机器可回放)
- **成功捕获了 2402 events** from Kimi

### 2. 回放工具 (AcpReplayCli)
```bash
./gradlew :mpp-ui:runAcpReplay -PacpCapture="capture_20260206_152815.jsonl"
```
- 从 `.jsonl` 回放事件
- 不需要连接 Kimi，可以稳定复现
- 测试不同 renderer 的效果

### 3. ACP专用 Renderer (AcpRenderer)
- **聚合 WriteFile**：相同文件的连续写入 → 单个更新项
- **激进批处理 ReadFile**：阈值 3 (vs ComposeRenderer 的 5)
- **过滤噪音事件**：`status="IN_PROGRESS" input=""` 

## ⚠️ Current Limitation

**ACP SDK 的 paramsStr 不包含实际路径！**

捕获的数据：
```json
{"tool_name":"WriteFile","params":"kind=\"UNKNOWN\" status=\"IN_PROGRESS\" input=\"\""}
```

没有 `path=` 或 `output=` 字段，导致：
- ✅ 过滤掉了噪音事件
- ❌ 但也丢失了路径信息，无法做聚合

**压缩效果：2385 → 2326 (1.0x，几乎没压缩)**

## 🔍 Root Cause

ACP SDK (`AcpClient.promptAndRender`) 调用 `renderToolCall(toolName, paramsStr)` 时：
- `paramsStr` 是 ACP 协议返回的占位符字符串
- **真正的参数（文件路径）可能在 ACP 事件的其他字段**

需要修改 `mpp-core` 中的 `AcpClient`，传递完整参数。

## 📋 Next Steps

### Option A: 修改 AcpClient (推荐)
修改 `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/AcpClient.kt`:
```kotlin
// 当前代码（推测）
fun handleToolCall(event: AcpEvent) {
    val params = event.params.toString()  // 只取占位符
    renderer.renderToolCall(event.toolName, params)
}

// 改进方案
fun handleToolCall(event: AcpEvent) {
    // 提取实际参数（path, content, etc.）
    val actualParams = buildParamsString(event)
    renderer.renderToolCall(event.toolName, actualParams)
}
```

### Option B: 在 Renderer 层做智能推断
如果无法修改 AcpClient，在 AcpRenderer 中：
1. 追踪 LLM 输出中的文件名提及
2. 根据调用序列推断路径
3. 使用启发式规则（如"连续100个 WriteFile → 可能是同一文件"）

### Option C: 改用 MCP 风格的批量协议
如果 ACP 本身不支持批量，考虑：
1. 在客户端缓冲写操作
2. 定期flush（如每100ms）
3. 一次性发送batch

## 🧪 Testing

当前可以测试的：
```bash
# 1. 回放真实捕获（验证 renderer 不崩溃）
./gradlew :mpp-ui:runAcpReplay -PacpCapture="capture_20260206_152815.jsonl"

# 2. 捕获新场景
./gradlew :mpp-ui:runAcpCapture -PacpPrompt="实现一个简单的功能"

# 3. 批处理测试（继续有效）
./gradlew :mpp-ui:runBatchTest
```

## 📊 Captured Data

文件位置：
- `mpp-ui/docs/test-scripts/acp-captures/capture_20260206_152815.log`
- `mpp-ui/docs/test-scripts/acp-captures/capture_20260206_152815.jsonl`

统计：
- 总事件：2402
- LLM chunks：15
- Tool calls：2385
- Tool results：0

工具分布：
- WriteFile: 2279 (95.5%)
- Glob: 18
- ReadFile: 15
- 其他：73

## 🎯 Recommended Action

**最高优先级：修改 AcpClient 传递完整参数**

1. 定位 `mpp-core` 中 ACP 事件处理代码
2. 找到 `renderToolCall` 调用点
3. 提取完整参数（特别是 `path`/`output` 字段）
4. 重新捕获测试用例
5. 验证 AcpRenderer 聚合效果

预期效果：
- 2279个 WriteFile → **1个** "Writing docs/architecture.md (streaming...)"
- 压缩比：2385 → ~50 items (**47.7x 压缩**)

## 📄 Implementation Files

- ✅ `mpp-ui/.../acp/AcpRenderer.kt` - 自定义renderer
- ✅ `mpp-ui/.../cli/AcpCaptureCli.kt` - 捕获工具
- ✅ `mpp-ui/.../cli/AcpReplayCli.kt` - 回放工具
- ⏳ `mpp-core/.../acp/AcpClient.kt` - 需要修改传参逻辑

## ✅ What Works

- ✅ 捕获系统完整可用
- ✅ 回放系统稳定复现
- ✅ AcpRenderer 框架正确
- ✅ 噪音过滤生效
- ✅ ReadFile 批处理生效（3个阈值）

## ❌ What Doesn't Work Yet

- ❌ WriteFile 聚合（因缺少路径信息）
- ❌ 压缩比不理想（1.0x vs 期望 47x）
- ❌ Timeline 仍会有 2300+ 项

## 🚀 Quick Win

如果短期内无法修改 AcpClient，可以：
1. **在 AcpRenderer 中硬编码聚合所有 WriteFile**
2. 假设连续100+个 WriteFile = 同一文件
3. 显示"Writing file (streaming...)"而不管具体路径

代码示例：
```kotlin
private var consecutiveWrites = 0
private var writeItemIndex: Int? = null

fun handleWriteFile(params: String) {
    if (consecutiveWrites < 100 && writeItemIndex == null) {
        // 前几个显示
        consecutiveWrites++
        addNormalItem()
    } else if (writeItemIndex == null) {
        // 第100个：创建聚合项
        writeItemIndex = _timeline.size
        addAggregatedItem()
    } else {
        // 后续：更新聚合项
        updateAggregatedItem(consecutiveWrites++)
    }
}
```

这样即使没有路径，也能达到类似效果。
