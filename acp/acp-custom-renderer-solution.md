# ACP Custom Renderer Solution

## 问题分析

批处理 (Batching) 方案虽然已实现并测试通过，但在实际使用 Kimi ACP agent 时可能仍存在 UI 冲突问题。根本原因是：
1. ACP agent 的输出直接通过 `ComposeRenderer` 渲染
2. `ComposeRenderer` 设计用于 AutoDev 内置 agent，可能不完全适配 ACP 的输出模式
3. Kimi ACP 可能有特殊的输出格式或事件序列

## 解决方案：自定义 ACP Renderer

创建专门的 `AcpRenderer` 来更好地处理 ACP agent 的输出。

### 实现步骤

#### 1. 捕获 Kimi 真实响应

首先需要捕获 Kimi 的原始输出作为测试用例：

```bash
# 编译并运行捕获工具（修复编译错误后）
./gradlew :mpp-ui:runAcpCapture -PacpPrompt="画一下项目架构图"

# 输出文件位置
docs/test-scripts/acp-captures/capture_YYYYMMDD_HHMMSS.log
```

捕获工具 (`AcpCaptureCli.kt`) 会记录所有事件：
- LLM chunks
- Tool calls (包括 tool 名称和参数)
- Tool results
- Token 信息
- 错误信息

#### 2. 分析捕获的数据

查看捕获文件，关注：
- Tool call 的频率和模式
- 是否有特殊的 tool 类型
- Output 的格式和长度
- 事件的顺序和时序

#### 3. 创建 AcpRenderer

基于分析结果，创建 `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/acp/AcpRenderer.kt`：

```kotlin
package cc.unitmesh.devins.ui.compose.agent.acp

import androidx.compose.runtime.*
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.render.TimelineItem
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.llm.compression.TokenInfo

/**
 * Specialized renderer for ACP agents.
 * Handles ACP-specific output patterns more gracefully than ComposeRenderer.
 */
class AcpRenderer : CodingAgentRenderer {
    // Timeline state
    private val _timeline = mutableStateListOf<TimelineItem>()
    val timeline: List<TimelineItem> = _timeline
    
    // ACP-specific batching strategy
    // Example: More aggressive batching for read-file (threshold: 3 instead of 5)
    private val READ_FILE_BATCH_THRESHOLD = 3
    
    // Track current batch
    private var currentBatch: ToolBatch? = null
    
    private data class ToolBatch(
        var toolName: String,
        var count: Int,
        var firstIndex: Int,
        val files: MutableList<String> = mutableListOf()
    )
    
    override fun renderToolCall(toolName: String, paramsStr: String) {
        // ACP-specific logic:
        // 1. More aggressive batching for read-file
        // 2. Special handling for ACP-specific tools
        // 3. Simplified output for non-critical tools
        
        when (toolName) {
            "read-file" -> handleReadFileBatching(paramsStr)
            "write-file" -> handleWriteFile(paramsStr)
            else -> handleGenericTool(toolName, paramsStr)
        }
    }
    
    private fun handleReadFileBatching(params: String) {
        // Extract filename from params
        val filePathMatch = Regex("""path="([^"]+)"""").find(params)
        val filePath = filePathMatch?.groups?.get(1)?.value ?: "unknown"
        
        val batch = currentBatch
        if (batch != null && batch.toolName == "read-file") {
            batch.count++
            batch.files.add(filePath)
            
            // More aggressive batching (threshold: 3)
            if (batch.count >= READ_FILE_BATCH_THRESHOLD) {
                updateBatchItem(batch)
                return
            }
        } else {
            // Start new batch
            currentBatch = ToolBatch(
                toolName = "read-file",
                count = 1,
                firstIndex = _timeline.size
            ).apply {
                files.add(filePath)
            }
        }
        
        // Add individual item (will be collapsed later)
        _timeline.add(createToolCallItem("read-file", filePath))
    }
    
    private fun updateBatchItem(batch: ToolBatch) {
        // Replace individual items with batch summary
        val itemsToRemove = _timeline.subList(batch.firstIndex, _timeline.size).toList()
        _timeline.removeAll(itemsToRemove)
        
        _timeline.add(createBatchItem(batch))
    }
    
    private fun createBatchItem(batch: ToolBatch): TimelineItem.ToolCallItem {
        return TimelineItem.ToolCallItem(
            toolName = "batch:${batch.toolName}",
            description = "📦 ${batch.count} files read",
            params = batch.files.take(3).joinToString(", ") + 
                     if (batch.files.size > 3) "..." else "",
            fullParams = batch.files.joinToString("\n"),
            filePath = null,
            toolType = ToolType.ReadFile,
            success = null,
            summary = null,
            output = null,
            fullOutput = null,
            executionTimeMs = null
        )
    }
    
    // ... implement other renderer methods
}
```

#### 4. 在 AcpConnectionProvider 中使用

修改 `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/agent/acp/AcpConnectionProvider.jvm.kt`：

```kotlin
class JvmAcpConnection : AcpConnection {
    // ...
    
    override suspend fun prompt(text: String, renderer: CodingAgentRenderer): String {
        // Check if we should use ACP-specific renderer
        val effectiveRenderer = if (renderer is ComposeRenderer) {
            // Wrap or replace with AcpRenderer
            AcpRenderer().also {
                // Copy state if needed
            }
        } else {
            renderer
        }
        
        return withContext(Dispatchers.IO) {
            acpClient.promptAndRender(text, effectiveRenderer)
        }
    }
}
```

#### 5. 在 CodingAgentViewModel 中配置

修改 `executeAcpTask()` 方法，使用 `AcpRenderer` 而不是 `ComposeRenderer`：

```kotlin
private suspend fun executeAcpTask(task: String) {
    val config = currentAcpAgentConfig ?: run {
        _isExecuting = false
        renderer.renderError("No ACP agent configured")
        return
    }
    
    try {
        // Use ACP-specific renderer
        val acpRenderer = AcpRenderer()
        
        val result = connection.prompt(task, acpRenderer)
        
        // Merge ACP timeline into main timeline
        renderer.mergeTimeline(acpRenderer.timeline)
        
        _isExecuting = false
    } catch (e: Exception) {
        // ...
    }
}
```

### 优化策略

基于捕获的数据，可以实施以下优化：

1. **更激进的批处理**
   - ReadFile: threshold = 3 (vs 5)
   - 自动折叠非关键工具

2. **简化输出**
   - 文件读取只显示文件名，不显示完整路径
   - 合并连续的相似操作

3. **进度指示**
   - 显示 "Processing files... (15/50)"
   - 实时更新批处理计数

4. **智能过滤**
   - 过滤掉某些 verbose 的 ACP 事件
   - 只显示用户关心的关键步骤

### 测试

```bash
# 1. 测试捕获工具
./gradlew :mpp-ui:runAcpCapture -PacpPrompt="简单测试"

# 2. 实现 AcpRenderer 后，运行应用
./gradlew :mpp-ui:run

# 3. 切换到 Kimi ACP agent
# 4. 测试多种场景：
#    - 画项目架构图
#    - Review 整个项目
#    - 实现新功能
#    - 分析代码
```

## 下一步

1. ✅ 修复 `AcpCaptureCli` 的编译错误
2. ⏳ 运行捕获工具获取真实数据
3. ⏳ 分析捕获数据
4. ⏳ 实现 `AcpRenderer`
5. ⏳ 测试多种场景
6. ⏳ 根据反馈迭代优化

## 当前状态

- ✅ 捕获工具已创建 (`AcpCaptureCli.kt`)
- ✅ Gradle 任务已添加 (`runAcpCapture`)
- ⚠️  编译错误需要修复（已添加 `AcpAgentConfig` 导入）
- ⏳ 等待第一次成功捕获以获取真实数据

## 附录：为什么需要自定义 Renderer

1. **ACP 协议特性**: ACP agents 可能有不同的事件序列和频率
2. **工具使用模式**: Kimi 可能更频繁地使用某些工具
3. **输出格式**: ACP 可能有特殊的输出格式需要特殊处理
4. **性能优化**: 可以针对 ACP 的使用模式进行优化
5. **用户体验**: 为 ACP agents 提供更流畅的 UI 体验
