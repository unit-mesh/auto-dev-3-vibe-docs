# GitHub PR Inline Comments 功能实现总结

## 📋 需求概述

根据 [Issue #501](https://github.com/phodal/auto-dev/issues/501)，需要在 mpp-ui 模块中实现 GitHub PR Review Comments 的内联显示功能。

**核心目标**: 让开发者可以在编辑器的 Diff 视图中直接看到 PR 的评审意见，减少在浏览器和 IDE 之间的上下文切换。

## 🔍 IDEA 实现分析

### 核心架构

IntelliJ IDEA GitHub 插件使用以下技术实现 PR 内联评论:

1. **DiffExtension 扩展点**: 通过 `GHPRReviewDiffExtension` 在 Diff 视图创建时注入评论功能
2. **Inlay API**: 使用 IntelliJ Platform 的 `InlayModel` 在编辑器中插入自定义组件
3. **位置映射**: 通过 `locationToLine` 和 `lineToLocation` 函数在 Diff 位置和编辑器行号之间转换
4. **状态管理**: 使用 Kotlin Flow 的 `StateFlow` 实现响应式数据更新

### 关键组件

- `GHPRReviewCommentLocation`: 评论位置数据模型 (SingleLine/MultiLine)
- `GHPRReviewThreadViewModel`: 评论线程视图模型
- `EditorComponentInlaysManager`: Inlay 组件管理器
- `DiffEditorModel`: 管理评论数据和 Inlay 渲染

### 参考文件

```
/Users/phodal/ide-devel/intellij-community/plugins/github/github-core/src/org/jetbrains/plugins/github/pullrequest/
├── GHPRListViewModel.kt
├── ui/comment/GHPRReviewCommentLocation.kt
├── ui/comment/GHPRReviewThreadViewModel.kt
└── ui/diff/GHPRReviewDiffExtension.kt
```

## 🎯 mpp-ui 实现方案

### 技术选型

由于 mpp-ui 是 Kotlin Multiplatform 项目，无法直接使用 IntelliJ Platform 的 Inlay API。我们采用 **Compose Multiplatform** 的组件嵌套方式实现。

### 架构设计

```
Backend API (GitHub/GitLab)
    ↓
GitHubPRService (mpp-core)
    ↓
CodeReviewViewModel (mpp-ui)
    ↓
DiffCenterView → DiffLineView → InlinePRCommentThread
```

### 核心组件

#### 1. 数据模型 (mpp-core)

```kotlin
// PRReviewModels.kt
data class PRCommentLocation(
    val filePath: String,
    val side: DiffSide,  // LEFT/RIGHT
    val lineNumber: Int,
    val isMultiLine: Boolean = false,
    val startLineNumber: Int? = null
)

data class PRComment(
    val id: String,
    val author: String,
    val body: String,
    val location: PRCommentLocation,
    val isResolved: Boolean = false,
    val replies: List<PRComment> = emptyList()
)

data class PRCommentThread(
    val id: String,
    val location: PRCommentLocation,
    val comments: List<PRComment>,
    val isResolved: Boolean = false
)
```

#### 2. API 服务 (mpp-core)

```kotlin
// GitHubPRService.kt
interface GitHubPRService {
    suspend fun getPRComments(owner: String, repo: String, prNumber: Int): Result<List<PRCommentThread>>
    suspend fun addComment(owner: String, repo: String, prNumber: Int, comment: PRComment): Result<PRComment>
    suspend fun resolveThread(owner: String, repo: String, prNumber: Int, threadId: String): Result<Unit>
}
```

#### 3. ViewModel 扩展 (mpp-ui)

```kotlin
// CodeReviewViewModel.kt
class CodeReviewViewModel {
    private val _prComments = MutableStateFlow<Map<String, List<PRCommentThread>>>(emptyMap())
    val prComments: StateFlow<Map<String, List<PRCommentThread>>> = _prComments.asStateFlow()
    
    fun loadPRComments(owner: String, repo: String, prNumber: Int) { /* ... */ }
    fun addComment(owner: String, repo: String, prNumber: Int, location: PRCommentLocation, body: String) { /* ... */ }
    fun resolveThread(owner: String, repo: String, prNumber: Int, threadId: String) { /* ... */ }
}
```

#### 4. UI 组件 (mpp-ui)

**修改现有的 DiffLineView**:

```kotlin
@Composable
fun DiffLineView(
    line: DiffLine,
    commentThreads: List<PRCommentThread> = emptyList(),  // 新增
    onAddComment: ((Int) -> Unit)? = null  // 新增
) {
    Column {
        // 原有的行渲染
        Row { /* ... */ }
        
        // 新增: 渲染评论
        commentThreads.forEach { thread ->
            InlinePRCommentThread(thread, ...)
        }
    }
}
```

**新增 InlinePRCommentThread 组件**:

```kotlin
@Composable
fun InlinePRCommentThread(
    thread: PRCommentThread,
    onReply: (String) -> Unit,
    onResolve: () -> Unit
) {
    Card {
        Column {
            // 评论头部 (作者、时间、状态)
            // 评论内容 (使用 SketchRenderer 渲染 Markdown)
            // 回复列表
            // 操作按钮 (Reply, Resolve)
        }
    }
}
```

### 与现有功能的集成

1. **复用 InlineIssueChip 的设计模式**: 卡片样式、展开/折叠逻辑
2. **复用 SketchRenderer**: 渲染评论的 Markdown 内容
3. **复用 DiffSketchRenderer 的颜色主题**: 统一的 Diff 样式
4. **集成到 CodeReviewSideBySideView**: 在三栏布局中显示评论

## 📊 对比分析

| 方面 | IDEA 实现 | mpp-ui 实现 |
|------|----------|------------|
| **UI 框架** | Swing + IntelliJ Platform | Compose Multiplatform |
| **Inlay 机制** | IntelliJ Inlay API | Compose 组件嵌套 |
| **平台支持** | JVM Desktop | JVM, JS, WASM, Android, iOS |
| **集成难度** | 高 (需深入了解 Platform API) | 中 (熟悉 Compose 即可) |
| **性能** | 优秀 (原生支持) | 良好 (需优化大量评论场景) |
| **可维护性** | 中 (依赖 Platform API) | 高 (声明式 UI) |

## 🚀 实施计划

### Phase 1: 数据层 (1-2 天)
- [ ] 创建 `PRReviewModels.kt`
- [ ] 创建 `GitHubPRService.kt` 接口
- [ ] 实现 `GitHubPRServiceImpl.kt`
- [ ] 添加单元测试

### Phase 2: UI 组件 (2-3 天)
- [ ] 创建 `InlinePRComment.kt`
- [ ] 修改 `DiffLineView` 支持评论
- [ ] 创建评论输入对话框
- [ ] 添加评论指示器

### Phase 3: ViewModel 集成 (1-2 天)
- [ ] 扩展 `CodeReviewViewModel`
- [ ] 实现评论加载逻辑
- [ ] 实现评论添加/回复/解决
- [ ] 添加错误处理

### Phase 4: UI 集成 (1-2 天)
- [ ] 修改 `DiffCenterView`
- [ ] 修改 `CompactFileDiffItem`
- [ ] 修改 `DiffHunkView`
- [ ] 添加过滤和排序

### Phase 5: 测试和优化 (2-3 天)
- [ ] 端到端测试
- [ ] 性能优化
- [ ] UI/UX 优化
- [ ] 文档编写

**总计**: 约 8-12 天

## ⚠️ 技术挑战

### 1. 行号映射
**问题**: Diff 中的行号与原始文件行号不一致

**解决方案**: 使用 `DiffLine` 的 `oldLineNumber` 和 `newLineNumber`，维护映射表

### 2. 评论位置同步
**问题**: 代码变更后评论位置可能失效

**解决方案**: 使用 GitHub API 的 `position` 字段 (基于 diff 的相对位置)，显示"过时评论"标记

### 3. 性能优化
**问题**: 大量评论时渲染性能问题

**解决方案**: 
- 使用 LazyColumn 虚拟化
- 评论折叠/展开
- 按需加载
- 缓存渲染结果

### 4. 跨平台兼容性
**问题**: 不同平台的 UI 适配

**解决方案**: 使用 Compose Multiplatform 通用组件，响应式布局

## 📚 相关文档

- [详细实现方案](./github-pr-inline-comments-implementation.md)
- [IDEA vs mpp-ui 对比](./idea-vs-mpp-ui-comparison.md)
- [代码示例](./code-examples-pr-comments.md)

## 🔗 参考资料

- [GitHub Issue #501](https://github.com/phodal/auto-dev/issues/501)
- [GitHub REST API - Pull Request Reviews](https://docs.github.com/en/rest/pulls/reviews)
- [IntelliJ Platform SDK - Inlay Hints](https://plugins.jetbrains.com/docs/intellij/inlay-hints.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

