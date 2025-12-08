# DocQL - Document Query Language

DocQL 是一个类似 JSONPath 的文档查询语言，用于在文档中查询目录、实体和内容。它既适合用户在 UI 中使用，也适合 AI Agent 使用。

## 语法概述

DocQL 查询以 `$` 开头，后跟属性访问、数组访问或函数调用。

### 基本结构

```
$ . property [ array_access ] . function("argument")
```

## 查询类型

### 1. TOC (目录) 查询

查询文档的目录结构：

```docql
# 获取所有目录项（扁平化）
$.toc[*]

# 获取第一个目录项
$.toc[0]

# 获取一级标题
$.toc[?(@.level==1)]

# 查找标题包含"架构"的目录项
$.toc[?(@.title~="架构")]

# 获取 level > 1 的目录项
$.toc[?(@.level>1)]

# 获取 level < 3 的目录项
$.toc[?(@.level<3)]
```

### 2. Entities (实体) 查询

查询文档中的实体（术语、API、类、函数等）：

```docql
# 获取所有实体
$.entities[*]

# 获取第一个实体
$.entities[0]

# 获取所有 API 类型的实体
$.entities[?(@.type=="API")]

# 获取所有术语类型的实体
$.entities[?(@.type=="Term")]

# 获取所有类实体
$.entities[?(@.type=="ClassEntity")]

# 获取所有函数实体
$.entities[?(@.type=="FunctionEntity")]

# 查找名称包含"User"的实体
$.entities[?(@.name~="User")]

# 精确匹配名称
$.entities[?(@.name=="UserService")]
```

### 3. Content (内容) 查询

查询文档的具体内容：

```docql
# 查询标题包含"架构"的章节内容
$.content.heading("架构")

# 查询特定章节
$.content.chapter("1.2")

# 查询 H1 标题
$.content.h1("Introduction")

# 查询 H2 标题
$.content.h2("Design")

# 查询 H3-H6 标题
$.content.h3("概述")

# 全文搜索（grep）
$.content.grep("关键词")

# 获取所有代码块（未完全实现）
$.content.code[*]

# 获取所有表格（未完全实现）
$.content.table[*]
```

## 运算符

DocQL 支持以下过滤运算符：

- `==` - 等于（适用于字符串和数字）
- `~=` - 包含（适用于字符串）
- `>`  - 大于（适用于数字）
- `<`  - 小于（适用于数字）

## 实体类型

查询实体时可用的类型：

- `Term` - 术语定义
- `API` - API 接口
- `ClassEntity` - 类定义
- `FunctionEntity` - 函数/方法定义

## 使用示例

### 在 UI 中使用

```kotlin
// 在 StructuredInfoPane 中使用 DocQL
val result = executeDocQL(
    queryString = """$.toc[?(@.level==1)]""",
    documentFile = myDocument,
    parserService = parserService
)

when (result) {
    is DocQLResult.TocItems -> {
        println("Found ${result.items.size} TOC items")
        result.items.forEach { println(it.title) }
    }
    is DocQLResult.Error -> {
        println("Error: ${result.message}")
    }
    // ... handle other result types
}
```

### 在 AI Agent 中使用

DocumentAgent 已经集成了 DocQL 支持，AI 可以直接使用 DocQL 查询文档：

```
User: "显示所有一级标题"
Assistant: 我会查询一级标题。
$.toc[?(@.level==1)]

[Agent 执行查询并返回结果]
```

## 架构

DocQL 由以下组件构成：

1. **DocQLSyntax** - 定义语法和 AST 数据结构
2. **DocQLLexer** - 词法分析器，将查询字符串转换为 token 流
3. **DocQLParser** - 语法分析器，将 token 流解析为 AST
4. **DocQLExecutor** - 执行器，根据 AST 执行查询并返回结果

## 错误处理

DocQL 提供强壮的错误处理：

- 语法错误会在解析时捕获并返回详细错误信息
- 执行错误会返回 `DocQLResult.Error`
- 支持实时语法验证（在 UI 输入时）

## 示例查询场景

### 场景 1：查找架构相关内容

```docql
# 查找标题包含"架构"的所有章节
$.content.heading("架构")
```

### 场景 2：获取 API 文档

```docql
# 获取所有 API 实体
$.entities[?(@.type=="API")]
```

### 场景 3：导航大纲

```docql
# 获取所有一级标题
$.toc[?(@.level==1)]

# 然后查看某个章节的内容
$.content.chapter("1")
```

### 场景 4：全文搜索

```docql
# 搜索包含"性能优化"的内容
$.content.grep("性能优化")
```

## 限制和未来改进

当前版本的限制：

1. 代码块和表格的提取尚未完全实现
2. 不支持复杂的嵌套过滤条件
3. 不支持多条件组合（AND/OR）

未来可能的改进：

1. 支持更复杂的过滤表达式
2. 支持聚合函数（count, sum 等）
3. 支持排序和限制结果数量
4. 支持关系查询（如"查找引用了某个实体的所有章节"）

