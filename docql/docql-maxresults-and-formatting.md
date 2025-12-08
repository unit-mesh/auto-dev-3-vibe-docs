# DocQL多文件查询优化：MaxResults与格式化改进

## 问题背景

用户报告在使用多文件查询时遇到以下问题：

1. **结果数量失控**：查询返回13438个chunks across 731 files，但maxResults=20参数没有生效
2. **格式化问题**：输出包含大量空分隔符（`---\n\n`），造成视觉混乱
3. **上下文溢出风险**：大量结果可能直接超出LLM上下文限制

## 解决方案

### 1. 添加maxResults参数 (#1)

**位置**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/DocQLTool.kt`

#### 参数定义

```kotlin
data class DocQLParams(
    val query: String,
    val documentPath: String? = null,
    val maxResults: Int? = 20  // 新增：默认限制20条结果
)
```

#### Schema更新

```kotlin
"maxResults" to integer(
    description = """
        Maximum number of results to return. Default is 20.
        Use lower values for quick overview, higher values for comprehensive search.
        Note: Very high values may exceed context limits for large result sets.
    """.trimIndent(),
    required = false
)
```

### 2. 格式化逻辑优化 (#2)

**改进内容**：

#### 2.1 结果数量截断

```kotlin
private fun formatDocQLResult(
    result: DocQLResult, 
    documentPath: String?,
    maxResults: Int = 20
): String {
    // 为每种result类型添加计数器
    var count = 0
    for ((filePath, items) in result.itemsByFile) {
        if (count >= maxResults) break
        // ...
    }
}
```

#### 2.2 过滤空内容

针对Chunks类型的结果：

```kotlin
// 过滤掉空的或只包含空白字符的chunks
val nonEmptyItems = items.filter { it.content.trim().isNotEmpty() }
if (nonEmptyItems.isEmpty()) continue

for (chunk in nonEmptyItems) {
    val content = chunk.content.trim()
    if (content.isNotEmpty()) {
        appendLine(content)
        appendLine()
        appendLine("---")
        appendLine()
        count++
    }
}
```

#### 2.3 截断提示信息

当结果被截断时，提供友好的提示：

```kotlin
if (truncated) {
    appendLine("⚠️ Showing first $maxResults results (${totalItems - maxResults} more available)")
    appendLine("💡 Tip: Narrow down your search to specific files or directories")
    appendLine("   Example: Query documents in a specific directory only")
}
```

### 3. 效果对比

#### Before（问题场景）

```
Found 13438 content chunks across 731 file(s):

## 📄 .augment/CHECKBOX_DELAY_FIX.md

---

---

---

---
[... 数千行类似输出 ...]
```

#### After（改进后）

```
Found 13438 content chunks across 731 file(s):
⚠️ Showing first 20 results (13418 more available)
💡 Tip: Narrow down your search to specific files or directories
   Example: Query documents in a specific directory only

## 📄 .augment/CHECKBOX_DELAY_FIX.md

private var categoryPanel: JPanel? = null

---

private fun createCategoryPanel(): JPanel {
    val categoryFormBuilder = FormBuilder.createFormBuilder()
    ...
}

---

[... 仅显示前20条有内容的结果 ...]
```

## 使用建议

### 1. 小范围查询（推荐）

```json
{
  "query": "$.content.chunks(\"devin\")",
  "documentPath": "docs/specific-file.md",
  "maxResults": 10
}
```

### 2. 全局查询with限制

```json
{
  "query": "$.toc[*]",
  "maxResults": 50
}
```

### 3. 利用过滤减少结果

```json
{
  "query": "$.files[?(@.path contains \"docs/agent\")]"
}
```

## 测试验证

### 单元测试

已添加`DocumentRegistryMultiFileQueryTest.kt`，包含以下测试场景：

1. ✅ 多文件TOC查询并按文件分组
2. ✅ 多文件heading搜索
3. ✅ $.files[*]列举所有文件
4. ✅ $.files过滤（按目录、扩展名）
5. ✅ 查询特定文件子集
6. ✅ totalCount计算验证
7. ✅ 压缩路径摘要触发阈值测试

### 集成测试

使用`DocumentCli`在实际项目上测试：

```bash
./gradlew :mpp-ui:compileKotlinJvm && \
./gradlew :mpp-ui:runDocumentCli \
  -PdocProjectPath="/Volumes/source/ai/autocrud/docs" \
  -PdocQuery="What is DocQL?"
```

**结果**：✅ 成功返回14个相关chunks，格式清晰，无空分隔符堆积

## 待办事项

### 高优先级

- [x] 添加maxResults参数到DocQLParams
- [x] 在formatDocQLResult中实现结果截断
- [x] 过滤空chunks
- [x] 添加截断提示信息
- [x] 更新schema文档

### 中优先级

- [ ] 更新所有旧测试文件以适应新的DocQLResult结构
  - DocQLIntegrationTest.kt
  - DocQLPositionQueryTest.kt
  - DocQLMultiFormatTest.kt
- [ ] 添加性能测试（大量文件场景）
- [ ] 优化分页策略（支持offset参数）

### 低优先级

- [ ] 添加结果排序选项（按相关性、文件名等）
- [ ] 支持结果导出（JSON格式）
- [ ] 添加查询缓存机制

## 相关文档

- [DocQL多文件查询设计](./docql-multi-file-query-enhancement.md)
- [文档索引流程](./document-indexing-flow.md)
- [DocQL指南](./docql-guide.md)

## 注意事项

1. **默认值**：maxResults默认为20，平衡了可读性和信息量
2. **空内容过滤**：只应用于Chunks类型，其他类型（TOC、Entities等）不需要
3. **截断提示**：帮助Agent理解有更多结果可用，引导更精确的查询
4. **向后兼容**：旧代码调用不传maxResults参数仍可工作（使用默认值20）

## 更新时间

2025-11-25

