# GitHub PR Inline Comments 实现方案

## 需求概述

根据 [Issue #501](https://github.com/phodal/auto-dev/issues/501)，需要在 mpp-ui 模块中实现 GitHub PR Review Comments 的内联显示功能，让开发者可以在编辑器中直接看到 PR 的评审意见，而不需要在浏览器和 IDE 之间切换。

## IDEA 实现分析

### 1. 核心架构

IDEA GitHub 插件使用以下架构来实现 PR 评论的内联显示:

#### 1.1 数据模型层

```kotlin
// GHPRReviewCommentLocation.kt - 评论位置定义
sealed interface GHPRReviewCommentLocation {
  val side: Side  // LEFT/RIGHT (对应 diff 的两侧)
  val lineIdx: Int  // 行号
  
  data class SingleLine(override val side: Side, override val lineIdx: Int)
  data class MultiLine(override val side: Side, val startLineIdx: Int, override val lineIdx: Int)
}

// GHPRReviewThreadViewModel.kt - 评论线程视图模型
interface GHPRReviewThreadViewModel {
  val id: String
  val avatarIconsProvider: GHAvatarIconsProvider
  val canCreateReplies: StateFlow<Boolean>
  val newReplyVm: GHPRNewThreadCommentViewModel
}
```

#### 1.2 Diff 扩展机制

```kotlin
// GHPRReviewDiffExtension.kt - 核心扩展点
class GHPRReviewDiffExtension : DiffExtension() {
  override fun onViewerCreated(viewer: FrameDiffTool.DiffViewer, context: DiffContext, request: DiffRequest) {
    // 1. 获取 DiffViewModel
    val diffVm = context.getUserData(GHPRDiffViewModel.KEY)
    
    // 2. 安装 Inlays (内联组件)
    installInlays(diffVm, change, viewer)
  }
}
```

#### 1.3 Editor Inlay 渲染

```kotlin
// 使用 IntelliJ Platform 的 Inlay API
fun installInlays(reviewVm: GHPRDiffViewModel, change: RefComparisonChange, viewer: DiffViewerBase) {
  // 1. 创建 EditorModel
  val modelFactory = createEditorModelFactory(reviewVm, changeVm, change)
  
  // 2. 显示代码评审组件
  viewer.showCodeReview(modelFactory) { createRenderer(it, userIcon) }
}

// DiffEditorModel - 管理评论数据
private class DiffEditorModel {
  // 评论线程
  private val threads = diffVm.threads.mapStatefulToStateful { MappedThread(cs, it) }
  
  // 新评论
  private val newComments = diffVm.newComments.mapStatefulToStateful { MappedNewComment(it) }
  
  // AI 评论
  private val aiComments = diffVm.aiComments.mapStatefulToStateful { MappedAIComment(it) }
  
  // 合并所有 inlays
  override val inlays: StateFlow<Collection<GHPREditorMappedComponentModel>> =
    combineStateIn(cs, threads, newComments, aiComments) { threads, new, ai -> threads + new + ai }
}
```

### 2. 关键技术点

1. **位置映射**: `locationToLine` 和 `lineToLocation` 函数用于在 diff 位置和编辑器行号之间转换
2. **Inlay 管理**: 使用 `EditorComponentInlaysManager` 管理内联组件的生命周期
3. **状态同步**: 使用 Kotlin Flow 实现评论数据的响应式更新
4. **Gutter Controls**: 在编辑器左侧显示可评论的行标记

## mpp-ui 实现方案

### 1. 现有基础设施

mpp-ui 已经具备以下相关组件:

#### 1.1 Diff 渲染组件

- `DiffSketchRenderer`: 渲染 AI 生成的代码差异
- `DiffCenterView`: Diff 文件列表和内容展示
- `DiffHunkView` / `DiffLineView`: Hunk 和行级别的 diff 渲染

#### 1.2 内联组件示例

- `InlineIssueChip`: 已实现的内联 Issue 显示组件
- `IssueInfoCard`: Issue 详情卡片

#### 1.3 Code Review 基础设施

- `CodeReviewViewModel`: 代码评审视图模型
- `CodeReviewSideBySideView`: 三栏布局 (Commits | Diff | AI Analysis)
- `CodeReviewAgentPanel`: AI 分析面板

### 2. 实现架构设计

#### 2.1 数据模型

```kotlin
// 新增文件: mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/pr/PRReviewModels.kt

/**
 * PR 评论位置
 */
data class PRCommentLocation(
    val filePath: String,
    val side: DiffSide,  // OLD/NEW
    val lineNumber: Int,
    val isMultiLine: Boolean = false,
    val startLineNumber: Int? = null
)

enum class DiffSide {
    OLD,  // 左侧 (删除的代码)
    NEW   // 右侧 (新增的代码)
}

/**
 * PR 评论数据
 */
data class PRComment(
    val id: String,
    val author: String,
    val avatarUrl: String? = null,
    val body: String,
    val createdAt: String,
    val location: PRCommentLocation,
    val isResolved: Boolean = false,
    val replies: List<PRComment> = emptyList()
)

/**
 * PR 评论线程
 */
data class PRCommentThread(
    val id: String,
    val location: PRCommentLocation,
    val comments: List<PRComment>,
    val isResolved: Boolean = false,
    val isCollapsed: Boolean = false
)
```

#### 2.2 后端 API 集成

```kotlin
// 新增文件: mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/github/GitHubPRService.kt

interface GitHubPRService {
    /**
     * 获取 PR 的所有评论
     */
    suspend fun getPRComments(
        repoOwner: String,
        repoName: String,
        prNumber: Int
    ): Result<List<PRCommentThread>>

    /**
     * 获取特定文件的评论
     */
    suspend fun getFileComments(
        repoOwner: String,
        repoName: String,
        prNumber: Int,
        filePath: String
    ): Result<List<PRCommentThread>>

    /**
     * 添加新评论
     */
    suspend fun addComment(
        repoOwner: String,
        repoName: String,
        prNumber: Int,
        comment: PRComment
    ): Result<PRComment>

    /**
     * 解决评论线程
     */
    suspend fun resolveThread(
        repoOwner: String,
        repoName: String,
        prNumber: Int,
        threadId: String
    ): Result<Unit>
}
```

#### 2.3 UI 组件设计

##### 2.3.1 内联评论组件

```kotlin
// 新增文件: mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/pr/InlinePRComment.kt

@Composable
fun InlinePRCommentThread(
    thread: PRCommentThread,
    onReply: (String) -> Unit,
    onResolve: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (thread.isResolved)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 评论头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 作者头像
                    AsyncImage(
                        url = thread.comments.first().avatarUrl,
                        modifier = Modifier.size(24.dp).clip(CircleShape)
                    )

                    Text(
                        text = thread.comments.first().author,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = thread.comments.first().createdAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (thread.isResolved) {
                        Icon(
                            imageVector = AutoDevComposeIcons.CheckCircle,
                            contentDescription = "Resolved",
                            tint = AutoDevColors.Green.c600,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = if (thread.isCollapsed)
                                AutoDevComposeIcons.ExpandMore
                            else
                                AutoDevComposeIcons.ExpandLess,
                            contentDescription = "Toggle"
                        )
                    }
                }
            }

            if (!thread.isCollapsed) {
                Spacer(modifier = Modifier.height(8.dp))

                // 评论内容
                thread.comments.forEach { comment ->
                    PRCommentItem(comment)
                    if (comment != thread.comments.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // 回复和解决按钮
                if (!thread.isResolved) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* 显示回复输入框 */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(AutoDevComposeIcons.Reply, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Reply")
                        }

                        Button(
                            onClick = onResolve,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(AutoDevComposeIcons.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Resolve")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PRCommentItem(comment: PRComment) {
    Column {
        // 使用 MarkdownSketchRenderer 渲染评论内容
        SketchRenderer.RenderResponse(
            content = comment.body,
            isComplete = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

##### 2.3.2 增强 DiffLineView 支持内联评论

```kotlin
// 修改文件: mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/diff/DiffCenterView.kt

@Composable
fun DiffLineView(
    line: DiffLine,
    commentThreads: List<PRCommentThread> = emptyList(),  // 新增参数
    onAddComment: ((Int) -> Unit)? = null,  // 新增参数
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 原有的行渲染逻辑
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            // ... 原有代码 ...

            // 新增: 评论指示器
            if (commentThreads.isNotEmpty()) {
                Icon(
                    imageVector = AutoDevComposeIcons.Comment,
                    contentDescription = "Has comments",
                    tint = AutoDevColors.Indigo.c600,
                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                )
            }
        }

        // 新增: 渲染该行的所有评论线程
        commentThreads.forEach { thread ->
            InlinePRCommentThread(
                thread = thread,
                onReply = { /* TODO */ },
                onResolve = { /* TODO */ },
                onCollapse = { /* TODO */ },
                modifier = Modifier.padding(start = 80.dp, end = 8.dp, top = 4.dp)
            )
        }
    }
}
```

#### 2.4 ViewModel 集成

```kotlin
// 修改文件: mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/CodeReviewViewModel.kt

class CodeReviewViewModel {
    // 新增: PR 评论相关状态
    private val _prComments = MutableStateFlow<Map<String, List<PRCommentThread>>>(emptyMap())
    val prComments: StateFlow<Map<String, List<PRCommentThread>>> = _prComments.asStateFlow()

    private val _isLoadingComments = MutableStateFlow(false)
    val isLoadingComments: StateFlow<Boolean> = _isLoadingComments.asStateFlow()

    // 新增: GitHub PR 服务
    private val githubPRService: GitHubPRService? = null  // TODO: 注入实现

    /**
     * 加载 PR 评论
     */
    fun loadPRComments(repoOwner: String, repoName: String, prNumber: Int) {
        viewModelScope.launch {
            _isLoadingComments.value = true
            try {
                githubPRService?.getPRComments(repoOwner, repoName, prNumber)
                    ?.onSuccess { threads ->
                        // 按文件路径分组
                        val grouped = threads.groupBy { it.location.filePath }
                        _prComments.value = grouped
                    }
                    ?.onFailure { error ->
                        // 处理错误
                        logger.error("Failed to load PR comments", error)
                    }
            } finally {
                _isLoadingComments.value = false
            }
        }
    }

    /**
     * 添加新评论
     */
    fun addComment(
        repoOwner: String,
        repoName: String,
        prNumber: Int,
        location: PRCommentLocation,
        body: String
    ) {
        viewModelScope.launch {
            val comment = PRComment(
                id = "",  // 服务器生成
                author = currentUser,
                body = body,
                createdAt = Clock.System.now().toString(),
                location = location
            )

            githubPRService?.addComment(repoOwner, repoName, prNumber, comment)
                ?.onSuccess { newComment ->
                    // 更新本地状态
                    val filePath = location.filePath
                    val currentThreads = _prComments.value[filePath] ?: emptyList()
                    val newThread = PRCommentThread(
                        id = newComment.id,
                        location = location,
                        comments = listOf(newComment)
                    )
                    _prComments.value = _prComments.value + (filePath to (currentThreads + newThread))
                }
        }
    }

    /**
     * 解决评论线程
     */
    fun resolveThread(
        repoOwner: String,
        repoName: String,
        prNumber: Int,
        threadId: String
    ) {
        viewModelScope.launch {
            githubPRService?.resolveThread(repoOwner, repoName, prNumber, threadId)
                ?.onSuccess {
                    // 更新本地状态
                    _prComments.value = _prComments.value.mapValues { (_, threads) ->
                        threads.map { thread ->
                            if (thread.id == threadId) {
                                thread.copy(isResolved = true)
                            } else {
                                thread
                            }
                        }
                    }
                }
        }
    }
}
```

#### 2.5 集成到现有 UI

```kotlin
// 修改文件: mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/diff/DiffCenterView.kt

@Composable
fun DiffCenterView(
    diffFiles: List<DiffFileInfo>,
    selectedCommits: List<CommitInfo>,
    prComments: Map<String, List<PRCommentThread>> = emptyMap(),  // 新增参数
    onAddComment: ((String, PRCommentLocation, String) -> Unit)? = null,  // 新增参数
    modifier: Modifier = Modifier,
    // ... 其他参数
) {
    // ... 现有代码 ...

    // 在渲染文件 diff 时传递评论数据
    diffFiles.forEach { file ->
        val fileComments = prComments[file.path] ?: emptyList()

        CompactFileDiffItem(
            file = file,
            comments = fileComments,  // 新增
            onViewFile = onViewFile,
            onAddComment = { location, body ->
                onAddComment?.invoke(file.path, location, body)
            }
        )
    }
}

@Composable
fun CompactFileDiffItem(
    file: DiffFileInfo,
    comments: List<PRCommentThread> = emptyList(),  // 新增参数
    onViewFile: ((String) -> Unit)? = null,
    onAddComment: ((PRCommentLocation, String) -> Unit)? = null  // 新增参数
) {
    var expanded by remember { mutableStateOf(false) }

    Column(/* ... */) {
        // ... 文件头部 ...

        if (expanded && file.hunks.isNotEmpty()) {
            Column(/* ... */) {
                file.hunks.forEach { hunk ->
                    DiffHunkView(
                        hunk = hunk,
                        comments = comments,  // 新增
                        onAddComment = onAddComment  // 新增
                    )
                }
            }
        }
    }
}

@Composable
fun DiffHunkView(
    hunk: DiffHunk,
    comments: List<PRCommentThread> = emptyList(),  // 新增参数
    onAddComment: ((PRCommentLocation, String) -> Unit)? = null  // 新增参数
) {
    Column(/* ... */) {
        hunk.lines.forEach { line ->
            if (line.type != DiffLineType.HEADER) {
                // 查找该行的评论
                val lineComments = comments.filter { thread ->
                    thread.location.lineNumber == line.newLineNumber
                }

                DiffLineView(
                    line = line,
                    commentThreads = lineComments,
                    onAddComment = { lineNum ->
                        // 显示评论输入对话框
                        val location = PRCommentLocation(
                            filePath = "",  // 从上下文获取
                            side = DiffSide.NEW,
                            lineNumber = lineNum
                        )
                        // TODO: 显示输入对话框
                    }
                )
            }
        }
    }
}
```

### 3. 实现步骤

#### Phase 1: 数据层 (1-2 天)

1. ✅ 创建 `PRReviewModels.kt` - 定义数据模型
2. ✅ 创建 `GitHubPRService.kt` - 定义 API 接口
3. ⬜ 实现 `GitHubPRServiceImpl.kt` - 实现 GitHub API 调用
4. ⬜ 添加单元测试

#### Phase 2: UI 组件 (2-3 天)

1. ✅ 创建 `InlinePRComment.kt` - 内联评论组件
2. ⬜ 增强 `DiffLineView` - 支持显示评论
3. ⬜ 创建 `PRCommentInputDialog.kt` - 评论输入对话框
4. ⬜ 添加评论指示器和交互

#### Phase 3: ViewModel 集成 (1-2 天)

1. ✅ 扩展 `CodeReviewViewModel` - 添加 PR 评论管理
2. ⬜ 实现评论加载逻辑
3. ⬜ 实现评论添加/回复/解决逻辑
4. ⬜ 添加状态管理和错误处理

#### Phase 4: UI 集成 (1-2 天)

1. ✅ 修改 `DiffCenterView` - 传递评论数据
2. ⬜ 修改 `CompactFileDiffItem` - 渲染评论
3. ⬜ 修改 `DiffHunkView` - 支持行级评论
4. ⬜ 添加评论过滤和排序功能

#### Phase 5: 测试和优化 (2-3 天)

1. ⬜ 端到端测试
2. ⬜ 性能优化 (大量评论时的渲染性能)
3. ⬜ UI/UX 优化
4. ⬜ 文档编写

### 4. 技术挑战和解决方案

#### 4.1 行号映射

**挑战**: Diff 中的行号与原始文件行号不一致

**解决方案**:
- 使用 `DiffLine` 中的 `oldLineNumber` 和 `newLineNumber`
- 维护一个映射表: `Map<Int, PRCommentLocation>`
- 参考 IDEA 的 `locationToLine` 和 `lineToLocation` 实现

#### 4.2 评论位置同步

**挑战**: 代码变更后评论位置可能失效

**解决方案**:
- 使用 GitHub API 的 `position` 字段 (基于 diff 的相对位置)
- 实现智能位置更新算法
- 显示"过时评论"标记

#### 4.3 性能优化

**挑战**: 大量评论时渲染性能问题

**解决方案**:
- 使用 LazyColumn 虚拟化渲染
- 评论折叠/展开功能
- 按需加载评论内容
- 缓存渲染结果

#### 4.4 跨平台兼容性

**挑战**: mpp-ui 需要支持多平台 (Desktop, Web, Mobile)

**解决方案**:
- 使用 Compose Multiplatform 的通用组件
- 平台特定的 `expect`/`actual` 实现
- 响应式布局适配不同屏幕尺寸

### 5. 与现有功能的集成

#### 5.1 与 Issue Tracker 集成

- 复用 `InlineIssueChip` 的设计模式
- 统一的卡片样式和交互模式
- 共享 `IssueService` 的缓存机制

#### 5.2 与 AI Analysis 集成

- AI 可以分析 PR 评论并生成修复建议
- 在 `CodeReviewAgentPanel` 中显示评论摘要
- 支持 AI 自动回复评论

#### 5.3 与 Diff Viewer 集成

- 复用 `DiffSketchRenderer` 的渲染逻辑
- 统一的颜色主题和样式
- 支持 Side-by-Side 和 Unified 两种模式

### 6. 未来扩展

1. **多 Git 平台支持**: GitLab, Bitbucket
2. **实时协作**: WebSocket 推送新评论
3. **评论搜索**: 全文搜索评论内容
4. **评论统计**: 显示评论数量、解决率等
5. **评论模板**: 常用评论的快捷输入
6. **代码建议**: 支持 GitHub 的 Suggested Changes 功能

## 参考资料

- [GitHub REST API - Pull Request Reviews](https://docs.github.com/en/rest/pulls/reviews)
- [IntelliJ Platform SDK - Inlay Hints](https://plugins.jetbrains.com/docs/intellij/inlay-hints.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

