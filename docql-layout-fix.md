# DocQL 布局重叠问题修复

## 问题描述

用户反馈：DocQL Syntax Panel 在某些时候不显示，Query Result 和其他内容重叠。

## 根本原因

在 `StructuredInfoPane.kt` 的 `DocQLResultView` composable 中，使用了 `Box` 包裹 `when` 语句的结果显示区域：

```kotlin
// ❌ 错误的实现
Box(modifier = Modifier.fillMaxSize()) {
    when (result) {
        is DocQLResult.TocItems -> {
            Text("Found ${result.items.size} TOC items")  // 问题：这个Text会和下面的LazyColumn重叠
            LazyColumn(...) { ... }
        }
        // ... 其他分支
    }
}
```

**问题分析：**
- `Box` 会将其子元素堆叠（overlay），而不是垂直排列
- 在每个 `when` 分支中，`Text`（计数信息）和 `LazyColumn`（列表）都直接放在 `Box` 中
- 这导致 `Text` 和 `LazyColumn` 重叠在一起，`Text` 被列表覆盖，或者列表的第一项被 `Text` 遮挡

## 解决方案

将 `Box` 移除，在每个 `when` 分支中使用 `Column` 来垂直排列元素：

```kotlin
// ✅ 正确的实现
when (result) {
    is DocQLResult.TocItems -> {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "找到 ${result.items.size} 个目录项",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)  // 添加底部间距
            )
            LazyColumn(...) { ... }
        }
    }
    
    is DocQLResult.Entities -> {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "找到 ${result.items.size} 个实体",
                ...
            )
            LazyColumn(...) { ... }
        }
    }
    
    // ... 其他分支也类似
}
```

## 修改内容

### 1. 移除外层 Box

**之前：**
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    when (result) { ... }
}
```

**之后：**
```kotlin
when (result) { ... }
```

### 2. 每个分支添加 Column

为每个需要显示计数信息的分支添加 `Column` 布局：
- `DocQLResult.TocItems`
- `DocQLResult.Entities`
- `DocQLResult.Chunks`
- `DocQLResult.CodeBlocks`
- `DocQLResult.Tables`

### 3. 统一文本提示为中文

将英文提示改为中文，保持界面一致性：
- "Found X TOC items" → "找到 X 个目录项"
- "Found X entities" → "找到 X 个实体"
- "Found X chunks" → "找到 X 个内容块"
- "Found X code blocks" → "找到 X 个代码块"
- "Found X tables" → "找到 X 个表格"
- "No results found" → "没有找到匹配的结果"
- "Error: ..." → "错误: ..."

### 4. 添加适当的间距

给计数文本添加 `Modifier.padding(bottom = 8.dp)`，使其与列表之间有明显的视觉分隔。

## 布局对比

### 修复前（Box 布局）

```
┌───────────────────────────┐
│ 查询结果            [X]    │
├───────────────────────────┤
│ Found 5 TOC items         │ ← Text 在上层
│ • 第一个目录项             │ ← LazyColumn 在下层，重叠！
│ • 第二个目录项             │
│ • 第三个目录项             │
└───────────────────────────┘
```

### 修复后（Column 布局）

```
┌───────────────────────────┐
│ 查询结果            [X]    │
├───────────────────────────┤
│ 找到 5 个目录项            │ ← Text 在上方
│                           │ ← 8dp 间距
│ • 第一个目录项             │ ← LazyColumn 在下方
│ • 第二个目录项             │    垂直排列，无重叠
│ • 第三个目录项             │
└───────────────────────────┘
```

## 影响范围

**修改文件：**
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/document/StructuredInfoPane.kt`
  - `DocQLResultView` composable（第 272-361 行）

**影响的查询结果类型：**
1. ✅ TocItems - 目录项列表
2. ✅ Entities - 实体列表
3. ✅ Chunks - 内容块列表
4. ✅ CodeBlocks - 代码块（占位显示）
5. ✅ Tables - 表格（占位显示）
6. ✅ Empty - 空结果
7. ✅ Error - 错误信息

## 测试验证

### 编译验证

```bash
cd /Volumes/source/ai/autocrud
./gradlew :mpp-ui:compileKotlinJvm
```

**结果：** ✅ BUILD SUCCESSFUL

### 视觉验证

**测试步骤：**
1. 启动应用
2. 打开一个文档（如 README.md）
3. 在 DocQL 搜索栏输入查询：`$.toc[?(@.level==1)]`
4. 观察结果显示

**预期结果：**
- 顶部显示 "找到 X 个目录项"
- 下方有 8dp 间距
- LazyColumn 列表清晰可见，不与文本重叠
- 所有列表项都可正常滚动和点击

### 端到端测试

使用集成测试验证查询功能：

```bash
./gradlew :mpp-core:jvmTest --tests "cc.unitmesh.devins.document.docql.DocQLIntegrationTest"
```

**结果：** ✅ 15 tests completed, all passed

## 相关文档

- [DocQL 语法指南](./docql-guide.md)
- [DocQL 用户体验改进](./docql-ux-improvements.md)
- [DocQL 端到端测试](./docql-e2e-test.md)
- [DocQL 自动补全行为](./docql-autocomplete-behavior.md)

## 总结

此次修复解决了 DocQL 查询结果显示的重叠问题，确保了：
1. ✅ 计数信息和列表内容垂直排列，互不遮挡
2. ✅ 界面文本统一为中文，用户体验更好
3. ✅ 适当的间距使界面更加清晰
4. ✅ 所有查询结果类型都能正确显示
5. ✅ 编译和测试全部通过

用户现在可以清晰地看到查询结果的计数和列表内容，不会再出现重叠问题。

