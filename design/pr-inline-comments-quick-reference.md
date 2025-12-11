# PR Inline Comments 快速参考指南

## 📌 核心概念

### IDEA 实现方式
- **扩展点**: `DiffExtension` 在 Diff 视图创建时注入
- **渲染机制**: IntelliJ Platform `InlayModel` API
- **位置映射**: `locationToLine` / `lineToLocation` 函数
- **状态管理**: Kotlin `StateFlow` 响应式更新

### mpp-ui 实现方式
- **扩展点**: 修改现有 `DiffLineView` Composable
- **渲染机制**: Compose 组件嵌套 (`Column` 布局)
- **位置映射**: 直接使用 `DiffLine.newLineNumber` 匹配
- **状态管理**: `StateFlow` + Compose `collectAsState()`

## 🗂️ 文件结构

```
mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/github/
├── PRReviewModels.kt          # 数据模型
├── GitHubPRService.kt         # API 接口
└── GitHubPRServiceImpl.kt     # API 实现

mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/
├── CodeReviewViewModel.kt     # ViewModel (扩展)
├── diff/
│   └── DiffCenterView.kt      # Diff 视图 (修改)
└── pr/
    └── InlinePRComment.kt     # 新增评论组件
```

## 🔑 关键代码片段

### 1. 数据模型

```kotlin
data class PRCommentLocation(
    val filePath: String,
    val side: DiffSide,      // LEFT/RIGHT
    val lineNumber: Int
)

data class PRCommentThread(
    val id: String,
    val location: PRCommentLocation,
    val comments: List<PRComment>,
    val isResolved: Boolean = false
)
```

### 2. ViewModel 状态

```kotlin
class CodeReviewViewModel {
    private val _prComments = MutableStateFlow<Map<String, List<PRCommentThread>>>(emptyMap())
    val prComments: StateFlow<Map<String, List<PRCommentThread>>> = _prComments.asStateFlow()
    
    fun loadPRComments(owner: String, repo: String, prNumber: Int) { /* ... */ }
}
```

### 3. UI 集成

```kotlin
@Composable
fun DiffLineView(
    line: DiffLine,
    commentThreads: List<PRCommentThread> = emptyList()
) {
    Column {
        Row { /* 原有行渲染 */ }
        
        commentThreads.forEach { thread ->
            InlinePRCommentThread(thread, ...)
        }
    }
}
```

## 🎨 设计参考

### 复用现有组件

| 现有组件 | 复用方式 |
|---------|---------|
| `InlineIssueChip` | 卡片样式、折叠逻辑 |
| `SketchRenderer` | Markdown 渲染 |
| `DiffSketchRenderer` | Diff 颜色主题 |
| `AutoDevColors` | 统一配色方案 |

### 颜色方案

```kotlin
// 已解决评论
containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

// 评论指示器
tint = AutoDevColors.Indigo.c600

// 已解决标记
color = AutoDevColors.Green.c600
```

## 🔄 数据流

```
用户打开 PR Diff
    ↓
CodeReviewViewModel.loadPRComments()
    ↓
GitHubPRService.getPRComments()
    ↓
Backend API → GitHub API
    ↓
返回 PRCommentThread 列表
    ↓
按文件路径分组: Map<String, List<PRCommentThread>>
    ↓
StateFlow 更新
    ↓
DiffCenterView 重组
    ↓
DiffLineView 渲染评论
```

## 🛠️ 实现步骤

### Phase 1: 数据层
1. 创建 `PRReviewModels.kt`
2. 创建 `GitHubPRService.kt`
3. 实现 API 调用

### Phase 2: UI 组件
1. 创建 `InlinePRComment.kt`
2. 修改 `DiffLineView`
3. 添加评论指示器

### Phase 3: ViewModel
1. 扩展 `CodeReviewViewModel`
2. 实现评论管理逻辑
3. 添加错误处理

### Phase 4: 集成
1. 修改 `DiffCenterView`
2. 传递评论数据
3. 连接回调函数

## 📋 API 接口

### GitHub REST API

```
GET /repos/{owner}/{repo}/pulls/{pull_number}/comments
POST /repos/{owner}/{repo}/pulls/{pull_number}/comments
PATCH /repos/{owner}/{repo}/pulls/comments/{comment_id}
```

### Backend API (建议)

```
GET /api/v1/reviews?repo_id={id}&pr_number={num}
POST /api/v1/reviews/comments
PATCH /api/v1/reviews/threads/{threadId}
```

## 🎯 关键差异: IDEA vs mpp-ui

| 特性 | IDEA | mpp-ui |
|-----|------|--------|
| Inlay 渲染 | `InlayModel.addBlockElement()` | `Column { Row; InlinePRCommentThread }` |
| 位置映射 | `locationToLine(DiffLineLocation)` | `thread.location.lineNumber == line.newLineNumber` |
| 组件管理 | `EditorComponentInlaysManager` | Compose 自动管理 |
| 平台支持 | JVM Desktop | JVM, JS, WASM, Android, iOS |

## 🚀 快速开始

### 1. 创建数据模型

```bash
# 创建文件
touch mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/github/PRReviewModels.kt
```

### 2. 创建 UI 组件

```bash
# 创建目录和文件
mkdir -p mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/pr
touch mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/pr/InlinePRComment.kt
```

### 3. 修改现有文件

```bash
# 需要修改的文件
mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/CodeReviewViewModel.kt
mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/diff/DiffCenterView.kt
```

## 📚 相关文档

- [详细实现方案](./github-pr-inline-comments-implementation.md)
- [IDEA vs mpp-ui 对比](./idea-vs-mpp-ui-comparison.md)
- [代码示例](./code-examples-pr-comments.md)
- [总结文档](./pr-inline-comments-summary.md)

## 💡 最佳实践

1. **向后兼容**: 所有新增参数使用默认值
2. **性能优化**: 使用 `LazyColumn` 处理大量评论
3. **错误处理**: 优雅处理 API 失败
4. **用户体验**: 添加加载状态和空状态
5. **测试**: 编写单元测试和集成测试

