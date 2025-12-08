# 文档阅读器交互优化

## 概述

本次优化主要针对 DocumentReaderPage 的交互体验，解决了两个关键问题：

1. **状态管理优化**：引入明确的状态机来处理各种加载状态
2. **导航面板增强**：添加索引状态显示和全文搜索功能

## 问题分析

### 问题 1：状态管理不明确

**原问题**：
- 一进来时，如果正在 loading，不应该显示"暂无文档"
- 没有区分"加载中"、"空列表"、"加载失败"等不同状态

**解决方案**：
- 引入 `DocumentLoadState` 状态机，包含以下状态：
  - `Initial`: 初始状态
  - `Loading`: 加载中
  - `Success`: 加载成功（包含文档列表）
  - `Empty`: 无文档
  - `Error`: 加载失败

### 问题 2：文档太多，逐个点击不便

**原问题**：
- 文档数量多时，逐个点击查找效率低
- 没有搜索和过滤功能
- 索引功能虽然存在，但没有在 UI 上显示状态

**解决方案**：
- 在导航面板顶部添加工具栏，包含：
  - 索引状态指示器（显示索引进度）
  - 搜索框（支持文件名和内容搜索）
  - 刷新按钮
- 搜索功能：
  - 按文件名搜索
  - 按索引内容搜索（全文搜索）
  - 实时过滤显示结果

## 主要改动

### 1. DocumentReaderViewModel.kt

#### 新增状态管理

```kotlin
sealed class DocumentLoadState {
    object Initial : DocumentLoadState()
    object Loading : DocumentLoadState()
    data class Success(val documents: List<DocumentFile>) : DocumentLoadState()
    object Empty : DocumentLoadState()
    data class Error(val message: String) : DocumentLoadState()
}
```

#### 新增搜索功能

```kotlin
// 搜索状态
var searchQuery by mutableStateOf("")
    private set

var filteredDocuments by mutableStateOf<List<DocumentFile>>(emptyList())
    private set

// 更新搜索查询
fun updateSearchQuery(query: String) {
    searchQuery = query
    filterDocuments()
}

// 过滤文档（支持文件名和内容搜索）
private fun filterDocuments() {
    filteredDocuments = if (searchQuery.isBlank()) {
        documents
    } else {
        val query = searchQuery.lowercase()
        documents.filter { doc ->
            // 按文件名搜索
            val nameMatch = doc.name.lowercase().contains(query) ||
                    doc.path.lowercase().contains(query)

            // 按索引内容搜索
            val contentMatch = indexService.getIndexStatus(doc.path)?.let { record ->
                record.status == "INDEXED" && 
                record.content?.lowercase()?.contains(query) == true
            } ?: false

            nameMatch || contentMatch
        }
    }
}
```

#### 新增刷新功能

```kotlin
fun refreshDocuments() {
    loadDocuments()
    indexService.indexWorkspace()
}
```

### 2. DocumentNavigationPane.kt

#### 更新接口

```kotlin
@Composable
fun DocumentNavigationPane(
    documentLoadState: DocumentLoadState = DocumentLoadState.Initial,
    documents: List<DocumentFile> = emptyList(),
    indexingStatus: IndexingStatus = IndexingStatus.Idle,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onDocumentSelected: (DocumentFile) -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
)
```

#### 新增顶部工具栏

- **索引状态指示器**：显示索引进度（如"索引中: 10/50"）
- **搜索框**：
  - 支持实时搜索
  - 索引完成后才能使用
  - 可清空搜索内容
- **刷新按钮**：重新加载文档列表和重建索引

#### 新增多个状态视图

- `LoadingDocumentsState`: 加载中状态
- `EmptyDocumentState`: 空文档状态
- `ErrorDocumentState`: 错误状态
- `NoSearchResultsState`: 搜索无结果状态

### 3. DocumentReaderPage.kt

更新 DocumentNavigationPane 的调用，传入新的状态和回调：

```kotlin
DocumentNavigationPane(
    documentLoadState = viewModel.documentLoadState,
    documents = viewModel.filteredDocuments,  // 使用过滤后的文档列表
    indexingStatus = viewModel.indexingStatus.collectAsState().value,
    searchQuery = viewModel.searchQuery,
    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
    onDocumentSelected = { viewModel.selectDocument(it) },
    onRefresh = { viewModel.refreshDocuments() }
)
```

## 用户体验改进

### 改进前

1. 页面加载时直接显示"暂无文档"，用户不知道是在加载还是真的没有文档
2. 没有搜索功能，文档多时需要逐个查找
3. 索引功能在后台运行，用户不知道进度

### 改进后

1. **明确的加载状态**：
   - 加载时显示加载动画和"加载文档中..."提示
   - 加载完成后，根据结果显示不同状态
   - 出错时显示错误信息

2. **强大的搜索功能**：
   - 支持按文件名搜索
   - 支持按内容全文搜索（基于索引）
   - 实时过滤显示结果
   - 搜索无结果时有明确提示

3. **可见的索引状态**：
   - 顶部显示索引进度
   - 索引完成后才启用搜索功能
   - 可以手动刷新重建索引

## 技术亮点

1. **状态机设计**：使用 sealed class 实现明确的状态管理，避免状态混乱
2. **响应式搜索**：使用 `mutableStateOf` 实现实时搜索过滤
3. **全文搜索**：利用现有的 DocumentIndexService 实现内容搜索
4. **渐进式体验**：索引完成前显示文件名搜索，索引完成后支持全文搜索

## 测试建议

1. **状态测试**：
   - 测试初始加载状态
   - 测试空文档状态
   - 测试加载错误状态

2. **搜索测试**：
   - 测试文件名搜索
   - 测试内容搜索（索引完成后）
   - 测试搜索无结果情况

3. **索引测试**：
   - 测试索引进度显示
   - 测试索引完成后搜索可用
   - 测试手动刷新功能

## 未来改进方向

1. **搜索增强**：
   - 支持正则表达式搜索
   - 支持高级过滤（按文件类型、日期等）
   - 搜索结果高亮显示

2. **性能优化**：
   - 搜索防抖（debounce）
   - 虚拟滚动支持大量文档

3. **用户体验**：
   - 搜索历史记录
   - 最近访问的文档
   - 书签和收藏功能

