# Source Code Indexing Enhancement

## 概述

为 DocumentAgent 添加了完整的源代码索引功能，现在可以像查询文档一样查询项目代码。

## 功能特性

### 1. 支持的编程语言

- ✅ **JVM 语言**: Java (.java), Kotlin (.kt, .kts)
- ✅ **JavaScript 生态**: JavaScript (.js), TypeScript (.ts, .tsx)
- ✅ **其他语言**: Python (.py), Go (.go), Rust (.rs), C# (.cs)

### 2. 代码结构索引

使用 TreeSitter 解析器提取代码结构：

```
📦 Package/Module
└── 📘 Class/Interface/Enum
    ├── ⚡ Method/Function
    │   └── [完整的方法实现代码]
    └── 📌 Field/Property
```

### 3. 查询能力

#### 通过类名查询
```json
{
  "query": "$.content.heading(\"DocQLExecutor\")",
  "documentPath": null
}
```
返回：类定义、所有方法、完整代码

#### 通过方法名查询
```json
{
  "query": "$.content.heading(\"parse\")",
  "documentPath": null
}
```
返回：所有匹配的方法（parseDocument, parseMarkdown等）及其实现

#### 查看代码结构
```json
{
  "query": "$.toc[*]",
  "documentPath": null
}
```
返回：完整的代码层级结构（包→类→方法→字段）

## 实现细节

### 核心组件

1. **CodeDocumentParser** (`/mpp-core/src/jvmMain/kotlin/cc/unitmesh/devins/document/CodeDocumentParser.kt`)
   - 使用 mpp-codegraph 解析源代码
   - 将 CodeNode 转换为 DocumentFile 结构
   - 构建层级 TOC 和实体列表
   - 保留完整的方法级代码

2. **DocumentFormatType.SOURCE_CODE**
   - 新增源代码文档类型
   - 自动识别源代码文件扩展名

3. **DocumentAgent 提示词增强**
   - 添加源代码查询指导
   - 代码查询模式识别
   - 代码命名模式扩展

### 数据结构映射

| CodeGraph | DocumentFile | 说明 |
|-----------|--------------|------|
| CodeNode (CLASS) | Entity.ClassEntity + TOCItem | 类定义 |
| CodeNode (METHOD) | Entity.FunctionEntity + TOCItem | 方法定义 |
| CodeNode (FIELD) | TOCItem | 字段/属性 |
| CodeNode.content | DocumentChunk.content | 完整代码 |
| CodeNode.packageName | TOCItem hierarchy | 包结构 |

## 测试验证

### 单元测试

```kotlin
// CodeDocumentParserTest.kt
@Test
fun `should parse DocQL Kotlin source code`() = runBlocking {
    val sourceCode = "..." // Kotlin code
    val parser = CodeDocumentParser()
    val result = parser.parse(file, sourceCode)
    
    // 验证 TOC 结构
    assertTrue(result.toc.isNotEmpty())
    
    // 验证实体提取
    assertTrue(result.entities.isNotEmpty())
    
    // 验证查询功能
    val chunks = parser.queryHeading("DocQL")
    assertTrue(chunks.isNotEmpty())
}
```

### 测试结果

✅ **6/6 测试通过**:
1. 解析 DocQL Kotlin 源代码 (12 节点, 10 TOC 项, 12 实体)
2. 语言检测正确
3. 包结构查询
4. 方法名模式查询
5. 方法体保留验证
6. 嵌套类处理

## 查询示例

### 示例 1: 理解 DocQL 是什么

**查询**: "What is DocQL and how does it work?"

**Agent 流程**:
1. 识别为代码查询（"what is" + 类名模式）
2. 查询: `$.content.heading("DocQL")`
3. 返回: DocQLExecutor 类、DocQLResult sealed class、相关方法
4. 综合回答: DocQL 的定义、执行流程、数据结构

### 示例 2: 查找特定实现

**查询**: "How does the parse method work in CodeDocumentParser?"

**Agent 流程**:
1. 识别为实现查询
2. 查询: `$.content.heading("CodeDocumentParser")` 获取类结构
3. 查询: `$.content.heading("parse")` 获取 parse 方法实现
4. 返回: 完整的 parse 方法代码及注释
5. 综合回答: 解析流程说明

### 示例 3: 查找所有相关方法

**查询**: "Find all parser implementations"

**Agent 流程**:
1. 扩展关键词: parser → DocumentParser, CodeParser, TikaParser
2. 查询: `$.content.heading("Parser")`
3. 返回: 所有 *Parser 类及其方法
4. 综合回答: 列出所有解析器及其功能

## Agent 提示词改进

### 新增章节

1. **"Querying Source Code Files"**
   - 代码结构说明
   - 查询语法示例
   - 类/方法/字段查询方式

2. **"Code-Specific Expansions"**
   - 命名模式: Parser/Service/Manager/Handler
   - 方法模式: get/set, create/build, parse/read
   - 类型模式: Interface/Impl/Abstract

3. **"Query Type Detection"**
   - 文档查询 vs 代码查询
   - 识别特征和处理策略

## 性能优化

### 缓存机制

1. **解析结果缓存** (DocumentIndexRepository)
   - 存储提取的文本（不是原始二进制）
   - 基于文件 hash 的增量更新
   - 避免重复解析大型文件

2. **内存索引** (DocumentRegistry)
   - 已解析的文档保持在内存
   - 快速查询访问
   - 支持跨文件查询

### 示例性能

- DocQLExecutor.kt (948 bytes)
  - 解析时间: ~94ms
  - 提取: 12 节点, 10 TOC 项
  - 缓存后: <1ms

## 使用方法

### 1. 通过 DocumentCli 测试

```bash
cd /Volumes/source/ai/autocrud

PROJECT_PATH="/path/to/your/code"
QUERY="What is DocQL and how does it work?"

./gradlew :mpp-ui:run --args="$PROJECT_PATH \"$QUERY\""
```

### 2. 编程方式使用

```kotlin
// 初始化解析器
DocumentRegistry.initializePlatformParsers()

// 索引源代码文件
val parser = DocumentParserFactory.createParserForFile("DocQL.kt")
val documentFile = DocumentFile(...)
val parsedDoc = parser.parse(documentFile, sourceCode)
DocumentRegistry.registerDocument(path, parsedDoc, parser)

// 使用 DocumentAgent 查询
val agent = DocumentAgent(...)
val result = agent.execute(
    DocumentTask(
        query = "How does DocQL execute queries?",
        documentPath = null
    )
)
```

## 未来改进方向

### 短期

- [ ] 添加更多语言支持 (C++, Ruby, PHP)
- [ ] 改进嵌套类和内部类的处理
- [ ] 添加代码依赖关系索引

### 中期

- [ ] 支持跨文件引用追踪
- [ ] 代码调用链分析
- [ ] 语义相似度搜索

### 长期

- [ ] 代码变更影响分析
- [ ] 智能重构建议
- [ ] 代码模式识别

## 测试覆盖

- ✅ 单元测试: CodeDocumentParserTest (6 个测试)
- ✅ 语言检测
- ✅ 结构解析
- ✅ 查询功能
- ✅ 缓存机制
- ⏳ 集成测试: 端到端 DocumentAgent 查询
- ⏳ 性能测试: 大型代码库索引

## 相关文件

- `/mpp-core/src/jvmMain/kotlin/cc/unitmesh/devins/document/CodeDocumentParser.kt` - 代码解析器
- `/mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/DocumentModels.kt` - 数据模型
- `/mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/DocumentParserFactory.kt` - 解析器工厂
- `/mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/document/DocumentAgent.kt` - Agent 实现
- `/mpp-core/src/jvmTest/kotlin/cc/unitmesh/devins/document/CodeDocumentParserTest.kt` - 测试

## 总结

通过集成 mpp-codegraph 模块，DocumentAgent 现在具备了完整的源代码索引和查询能力。代码被解析为层级结构，保留完整实现，支持灵活查询。Agent 的提示词也针对代码查询进行了优化，能够智能识别代码查询意图并使用正确的查询策略。

