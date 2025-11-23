# DocQL 端到端测试指南

## 概述

本文档展示了 DocQL（Document Query Language）从文档解析到查询执行的完整测试流程，确保查询语言能够正确工作。

## 完整测试用例

### 测试文件
`mpp-core/src/commonTest/kotlin/cc/unitmesh/devins/document/docql/DocQLIntegrationTest.kt`

### 1. 文档结构

测试使用一个模拟的 `README.md` 文档，包含以下结构：

```markdown
# AutoCRUD Project (Level 1)
  ## Overview (Level 2)
  ## Features (Level 2)
    ### Core Features (Level 3)

# Installation (Level 1)
  ## Requirements (Level 2)
  ## Quick Start (Level 2)

# Architecture (Level 1)
  ## System Design (Level 2)
  ## Database Schema (Level 2)

# API Reference (Level 1)

# License (Level 1)
```

**实体列表：**
- Term: `CRUD`
- API: `createUser`, `getUser`
- Class: `UserService`
- Function: `validateUser`

### 2. 查询示例

#### 查询所有一级标题

```docql
$.toc[?(@.level==1)]
```

**预期结果：** 5 个一级标题
- AutoCRUD Project
- Installation
- Architecture
- API Reference
- License

**测试代码：**
```kotlin
val query = parseDocQL("$.toc[?(@.level==1)]")
val result = executor.execute(query)

assertIs<DocQLResult.TocItems>(result)
assertEquals(5, result.items.size)
assertTrue(result.items.all { it.level == 1 })
```

#### 查询所有二级标题

```docql
$.toc[?(@.level==2)]
```

**预期结果：** 6 个二级标题
- Overview
- Features
- Requirements
- Quick Start
- System Design
- Database Schema

#### 查询包含特定关键词的标题

```docql
$.toc[?(@.title~="Architecture")]
```

**预期结果：** 包含 "Architecture" 的所有标题（至少 1 个）

#### 查询所有 TOC 项（扁平化）

```docql
$.toc[*]
```

**预期结果：** 12 个项（5 个一级 + 6 个二级 + 1 个三级）

#### 查询第一个 TOC 项

```docql
$.toc[0]
```

**预期结果：** "AutoCRUD Project" (level=1)

#### 查询所有实体

```docql
$.entities[*]
```

**预期结果：** 5 个实体

#### 按类型查询实体

```docql
$.entities[?(@.type=="API")]
```

**预期结果：** 2 个 API 实体（createUser, getUser）

```docql
$.entities[?(@.type=="Term")]
```

**预期结果：** 1 个术语实体（CRUD）

#### 按名称模糊匹配查询实体

```docql
$.entities[?(@.name~="User")]
```

**预期结果：** 4 个包含 "User" 的实体
- createUser
- getUser
- UserService
- validateUser

#### 范围查询

```docql
$.toc[?(@.level>1)]
```

**预期结果：** 7 个项（6 个二级 + 1 个三级）

```docql
$.toc[?(@.level<3)]
```

**预期结果：** 11 个项（5 个一级 + 6 个二级）

#### 精确匹配查询

```docql
$.toc[?(@.title=="Architecture")]
```

**预期结果：** 1 个标题（Architecture）

### 3. 完整的工作流测试

以下测试展示了从文档创建到查询结果的完整流程：

```kotlin
@Test
fun `test complete workflow from parse to query to result`() = runTest {
    // Step 1: 创建文档（模拟从 README.md 解析）
    println("Step 1: 创建 README.md 文档")
    val readme = createReadmeDocument()
    println("  - 文档名称: ${readme.name}")
    println("  - TOC 项数: ${readme.toc.size} 个一级标题")
    println("  - 实体数量: ${readme.entities.size}")
    
    // Step 2: 解析 DocQL 查询
    println("\nStep 2: 解析 DocQL 查询")
    val queryString = "$.toc[?(@.level==1)]"
    println("  - 查询字符串: $queryString")
    
    val query = parseDocQL(queryString)
    println("  - 解析成功，节点数: ${query.nodes.size}")
    
    // Step 3: 执行查询
    println("\nStep 3: 执行查询")
    val executor = DocQLExecutor(readme, null)
    val result = executor.execute(query)
    
    // Step 4: 验证结果
    println("\nStep 4: 验证结果")
    assertIs<DocQLResult.TocItems>(result)
    println("  - 结果类型: TocItems")
    println("  - 找到 ${result.items.size} 个一级标题:")
    result.items.forEach { item ->
        println("    * ${item.title} (level=${item.level}, anchor=${item.anchor})")
    }
    
    assertEquals(5, result.items.size)
    println("\n✅ 端到端测试通过！")
}
```

**输出示例：**
```
Step 1: 创建 README.md 文档
  - 文档名称: README.md
  - TOC 项数: 5 个一级标题
  - 实体数量: 5

Step 2: 解析 DocQL 查询
  - 查询字符串: $.toc[?(@.level==1)]
  - 解析成功，节点数: 4

Step 3: 执行查询

Step 4: 验证结果
  - 结果类型: TocItems
  - 找到 5 个一级标题:
    * AutoCRUD Project (level=1, anchor=#autocrud-project)
    * Installation (level=1, anchor=#installation)
    * Architecture (level=1, anchor=#architecture)
    * API Reference (level=1, anchor=#api-reference)
    * License (level=1, anchor=#license)

✅ 端到端测试通过！
```

### 4. 错误处理测试

#### 无效查询

```docql
$.invalid[*]
```

**预期结果：** `DocQLResult.Error` with message "Unknown context"

#### 空结果

```docql
$.toc[?(@.title~="NonExistent")]
```

**预期结果：** `DocQLResult.TocItems` with empty list

## 测试执行

### 运行所有集成测试

```bash
cd /Volumes/source/ai/autocrud
./gradlew :mpp-core:jvmTest --tests "cc.unitmesh.devins.document.docql.DocQLIntegrationTest"
```

### 运行特定测试

```bash
./gradlew :mpp-core:jvmTest --tests "cc.unitmesh.devins.document.docql.DocQLIntegrationTest.test complete workflow from parse to query to result"
```

## 测试结果

✅ **所有 15 个测试用例均通过**

```
BUILD SUCCESSFUL in 11s
5 actionable tasks: 5 executed
```

## 测试覆盖范围

1. ✅ 基本查询（所有项、按索引）
2. ✅ 过滤查询（等于、大于、小于）
3. ✅ 模糊匹配（contains）
4. ✅ 精确匹配（equals）
5. ✅ TOC 查询（各级标题）
6. ✅ 实体查询（按类型、按名称）
7. ✅ 错误处理（无效查询、空结果）
8. ✅ 端到端工作流

## 实际应用场景

### 在 UI 中使用

用户在 `DocQLSearchBar` 中输入查询：

```docql
$.toc[?(@.level==1)]
```

系统实时执行查询并在 `DocQLResultView` 中显示结果。

### 在 AI Agent 中使用

AI 可以在 `DocumentAgent` 中生成并执行查询：

```kotlin
val query = "$.toc[?(@.title~=\"API\")]"
val result = executeDocQL(query, document, chunks)
```

AI 使用返回的结果来回答用户关于文档的问题。

## 常见问题

### Q: 为什么 `$.toc[?(@.level==1)]` 没有返回结果？

**A:** 可能的原因：
1. 文档的 TOC 尚未正确解析
2. level 字段的值不是预期的类型
3. 查询语法错误

**解决方法：**
1. 检查 `DocumentFile.toc` 是否包含数据
2. 使用集成测试验证解析和查询流程
3. 查看 `DocQLResult.Error` 中的错误信息

### Q: 如何调试查询失败？

**A:** 使用完整的工作流测试：
```kotlin
val query = parseDocQL(queryString)  // 检查解析是否成功
val executor = DocQLExecutor(document, null)
val result = executor.execute(query)  // 检查执行结果
when (result) {
    is DocQLResult.Error -> println("Error: ${result.message}")
    else -> println("Success: ${result}")
}
```

## 未来改进

1. 支持更复杂的查询组合（AND、OR）
2. 支持排序和限制结果数量
3. 支持聚合查询（count、group by）
4. 性能优化（索引、缓存）

## 参考文档

- [DocQL 语法指南](./docql-guide.md)
- [DocQL 用户体验改进](./docql-ux-improvements.md)
- [DocQL 自动补全行为](./docql-autocomplete-behavior.md)

