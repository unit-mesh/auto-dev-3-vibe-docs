# DocQL Multi-File Query Enhancement

## 概述

优化了 DocQL 查询系统，使其支持多文件查询并自动标注源文件信息，同时优化了大量文件时的提示词大小。

## 主要改进

### 1. 多文件查询自动化

**之前**：
- 每次查询只能查一个文件
- 需要循环遍历多个文件
- 结果不包含源文件信息

**现在**：
- **所有 DocQL 查询自动搜索所有可用文档**
- 结果按源文件分组，清晰标注来自哪个文件
- 一次查询即可获得跨文件的完整结果

### 2. 新的结果格式 - 带源文件信息

所有 `DocQLResult` 类型现在都包含源文件信息：

```kotlin
// TOC Items - 按文件分组
data class TocItems(val itemsByFile: Map<String, List<TOCItem>>) : DocQLResult() {
    val totalCount: Int get() = itemsByFile.values.sumOf { it.size }
}

// Entities - 按文件分组
data class Entities(val itemsByFile: Map<String, List<Entity>>) : DocQLResult()

// Chunks - 按文件分组
data class Chunks(val itemsByFile: Map<String, List<DocumentChunk>>) : DocQLResult()

// CodeBlocks - 按文件分组
data class CodeBlocks(val itemsByFile: Map<String, List<CodeBlock>>) : DocQLResult()

// Tables - 按文件分组
data class Tables(val itemsByFile: Map<String, List<TableBlock>>) : DocQLResult()
```

### 3. 格式化输出示例

```
Found 15 chunks across 3 files:

## 📄 docs/architecture.md

(chunk content from architecture.md...)

---

## 📄 docs/design.md

(chunk content from design.md...)

---

## 📄 README.md

(chunk content from README.md...)
```

### 4. 路径压缩优化

当文件数量 > 20 时，自动压缩显示：

```
Available documents (300 total - showing directory structure):

Use DocQL `$.files[*]` to list all files, or `$.files[?(@.path contains "pattern")]` to filter.

├── docs/ (125 files)
│   ├── architecture/ (15 files)
│   ├── api/ (30 files)
│   └── guides/ (80 files)
├── src/ (150 files)
└── tests/ (25 files)

💡 Tip: Query specific directories to reduce context size, e.g.:
   $.files[?(@.path contains "docs")]
```

### 5. $.files 查询支持

```kotlin
// 列出所有文件
$.files[*]

// 按路径过滤
$.files[?(@.path contains "docs")]

// 按扩展名过滤
$.files[?(@.extension == "md")]

// 按目录过滤
$.files[?(@.directory contains "architecture")]
```

## API 变更

### DocumentRegistry

新增方法：

```kotlin
/**
 * Query multiple documents using DocQL and merge results with source file information
 * This is the recommended method for querying across all documents
 */
suspend fun queryDocuments(
    docqlQuery: String, 
    documentPaths: List<String>? = null
): DocQLResult
```

### DocQLExecutor

单文件查询结果格式已更新，始终包含源文件信息（即使只有一个文件）。

### DocQLTool

- `queryAllDocuments()` 现在使用 `DocumentRegistry.queryDocuments()`
- `formatDocQLResult()` 更新以支持新的按文件分组的结果格式
- `isEmptyResult()` 使用 `totalCount` 属性检查空结果

## 使用示例

### 查询所有文档中的标题

```kotlin
val result = DocumentRegistry.queryDocuments("$.content.heading(\"Introduction\")")

// 结果自动包含所有文件的匹配项
when (result) {
    is DocQLResult.Chunks -> {
        println("Found ${result.totalCount} chunks from ${result.itemsByFile.size} files")
        result.itemsByFile.forEach { (filePath, chunks) ->
            println("From: $filePath")
            chunks.forEach { chunk -> println(chunk.content) }
        }
    }
}
```

### 查询特定文件

```kotlin
val result = DocumentRegistry.queryDocument("docs/README.md", "$.toc[*]")
// 单文件查询也返回包含源文件信息的结果
```

### 列出文件

```kotlin
val result = DocumentRegistry.queryDocuments("$.files[?(@.path contains \"docs\")]")
// 返回过滤后的文件列表
```

## 性能优化

1. **提示词大小减少**：文件数 > 20 时，使用树形结构替代完整列表，节省 50-70% 空间
2. **按需加载**：文件内容不默认加载，只在需要时加载
3. **智能缓存**：已解析的文档保持在内存中，避免重复解析

## 向后兼容性

- 单文件查询 API (`queryDocument`) 仍然可用
- 结果格式变更可能需要更新使用 DocQLResult 的代码
- UI 组件已更新以支持新格式

## 测试

所有现有测试已通过，新增测试：
- `DocumentRegistryPathCompressionTest` - 路径压缩功能
- 多文件查询集成测试
- UI 组件更新测试

## 未来改进

1. 支持文件内容预览（在 $.files 结果中可选地包含内容摘要）
2. 支持更复杂的过滤条件（AND/OR 逻辑）
3. 支持排序（按文件名、大小、修改时间等）
4. 性能优化：并行查询多个文档

## 相关文件

- `mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/DocumentRegistry.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/docql/DocQLExecutor.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/DocQLTool.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/document/DocumentAgent.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/document/StructuredInfoPane.kt`

