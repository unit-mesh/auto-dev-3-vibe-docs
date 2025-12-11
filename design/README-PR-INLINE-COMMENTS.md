# GitHub PR Inline Comments 功能设计文档

> 本文档分析了如何在 mpp-ui 模块中实现 GitHub PR Review Comments 的内联显示功能 ([Issue #501](https://github.com/phodal/auto-dev/issues/501))

## 📖 文档导航

### 核心文档

1. **[快速参考指南](./pr-inline-comments-quick-reference.md)** ⭐ 推荐首先阅读
   - 核心概念速览
   - 关键代码片段
   - 实现步骤清单

2. **[功能总结](./pr-inline-comments-summary.md)**
   - 需求概述
   - IDEA 实现分析
   - mpp-ui 实现方案
   - 实施计划

3. **[详细实现方案](./github-pr-inline-comments-implementation.md)**
   - IDEA 实现深度分析
   - mpp-ui 架构设计
   - 数据模型定义
   - UI 组件设计
   - ViewModel 集成
   - 技术挑战和解决方案

4. **[IDEA vs mpp-ui 对比](./idea-vs-mpp-ui-comparison.md)**
   - 核心技术对比
   - Inlay 渲染机制对比
   - 位置映射对比
   - 状态管理对比
   - 实现建议

5. **[代码示例](./code-examples-pr-comments.md)**
   - 数据模型代码
   - DiffLineView 修改示例
   - InlinePRComment 组件完整实现
   - 使用示例

### 架构图

本文档包含以下可视化图表:

- **架构图**: 展示从 Backend API 到 UI 组件的完整架构
- **数据流图**: 展示评论加载、显示、添加、解决的完整流程

## 🎯 核心目标

让开发者可以在 IDE 的 Diff 视图中直接看到 GitHub PR 的评审意见，减少在浏览器和 IDE 之间的上下文切换。

## 🔍 关键发现

### IDEA 的实现方式

IntelliJ IDEA GitHub 插件使用以下技术栈:

```
DiffExtension (扩展点)
    ↓
InlayModel API (渲染机制)
    ↓
EditorComponentInlaysManager (组件管理)
    ↓
StateFlow (状态管理)
```

**核心文件**:
- `GHPRReviewDiffExtension.kt` - Diff 扩展实现
- `GHPRReviewCommentLocation.kt` - 评论位置模型
- `GHPRReviewThreadViewModel.kt` - 评论线程视图模型
- `EditorComponentInlaysManager.kt` - Inlay 管理器

### mpp-ui 的实现方式

由于 mpp-ui 是 Kotlin Multiplatform 项目，无法使用 IntelliJ Platform 的 Inlay API，我们采用 Compose Multiplatform 的组件嵌套方式:

```
Compose Column 布局
    ↓
DiffLineView (修改现有组件)
    ↓
InlinePRCommentThread (新增组件)
    ↓
StateFlow + collectAsState() (状态管理)
```

**关键优势**:
- ✅ 跨平台支持 (Desktop, Web, Mobile)
- ✅ 声明式 UI，代码简洁
- ✅ 与现有 Compose 组件无缝集成
- ✅ 复用现有设计系统 (AutoDevColors, InlineIssueChip)

## 📊 技术对比

| 方面 | IDEA 实现 | mpp-ui 实现 |
|------|----------|------------|
| **UI 框架** | Swing + IntelliJ Platform | Compose Multiplatform |
| **Inlay 机制** | IntelliJ Inlay API | Compose 组件嵌套 |
| **平台支持** | JVM Desktop | JVM, JS, WASM, Android, iOS |
| **集成难度** | 高 | 中 |
| **可维护性** | 中 | 高 |

## 🏗️ 实现架构

### 数据层 (mpp-core)

```kotlin
// 数据模型
PRCommentLocation → PRComment → PRCommentThread

// API 服务
GitHubPRService (接口) → GitHubPRServiceImpl (实现)
```

### UI 层 (mpp-ui)

```kotlin
// ViewModel
CodeReviewViewModel
  ├── prComments: StateFlow<Map<String, List<PRCommentThread>>>
  ├── loadPRComments()
  ├── addComment()
  └── resolveThread()

// UI 组件
DiffCenterView
  └── CompactFileDiffItem
      └── DiffHunkView
          └── DiffLineView (修改)
              └── InlinePRCommentThread (新增)
                  └── PRCommentItem
```

## 🚀 实施计划

### 时间估算: 8-12 天

| Phase | 任务 | 时间 |
|-------|-----|------|
| **Phase 1** | 数据层 (模型、API、测试) | 1-2 天 |
| **Phase 2** | UI 组件 (InlinePRComment, DiffLineView) | 2-3 天 |
| **Phase 3** | ViewModel 集成 (状态管理、逻辑) | 1-2 天 |
| **Phase 4** | UI 集成 (DiffCenterView, 数据传递) | 1-2 天 |
| **Phase 5** | 测试和优化 (E2E、性能、UX) | 2-3 天 |

### 检查清单

#### Phase 1: 数据层
- [ ] 创建 `PRReviewModels.kt` (PRCommentLocation, PRComment, PRCommentThread)
- [ ] 创建 `GitHubPRService.kt` 接口
- [ ] 实现 `GitHubPRServiceImpl.kt` (API 调用、缓存)
- [ ] 添加单元测试

#### Phase 2: UI 组件
- [ ] 创建 `InlinePRComment.kt` (InlinePRCommentThread, PRCommentItem)
- [ ] 修改 `DiffLineView` 支持 commentThreads 参数
- [ ] 创建评论输入对话框
- [ ] 添加评论指示器和交互

#### Phase 3: ViewModel 集成
- [ ] 扩展 `CodeReviewViewModel` 添加 PR 评论状态
- [ ] 实现 `loadPRComments()` 方法
- [ ] 实现 `addComment()` 方法
- [ ] 实现 `resolveThread()` 方法
- [ ] 添加错误处理和加载状态

#### Phase 4: UI 集成
- [ ] 修改 `DiffCenterView` 传递 prComments
- [ ] 修改 `CompactFileDiffItem` 传递评论数据
- [ ] 修改 `DiffHunkView` 支持行级评论
- [ ] 添加评论过滤和排序功能

#### Phase 5: 测试和优化
- [ ] 端到端测试 (加载、显示、添加、解决)
- [ ] 性能优化 (LazyColumn、折叠、缓存)
- [ ] UI/UX 优化 (动画、交互、响应式)
- [ ] 文档编写 (用户文档、开发文档)

## 🎨 设计原则

### 1. 复用现有组件

- **InlineIssueChip**: 卡片样式、折叠逻辑
- **SketchRenderer**: Markdown 渲染
- **DiffSketchRenderer**: Diff 颜色主题
- **AutoDevColors**: 统一配色方案

### 2. 向后兼容

所有修改都使用默认参数，确保现有代码无需修改:

```kotlin
@Composable
fun DiffLineView(
    line: DiffLine,
    commentThreads: List<PRCommentThread> = emptyList(),  // 默认值
    onAddComment: ((Int) -> Unit)? = null  // 可选参数
)
```

### 3. 性能优化

- 使用 `LazyColumn` 虚拟化渲染
- 评论折叠/展开功能
- 按需加载评论内容
- 缓存渲染结果

### 4. 跨平台兼容

- 使用 Compose Multiplatform 通用组件
- 避免平台特定的实现
- 响应式布局适配不同屏幕

## ⚠️ 技术挑战

### 1. 行号映射
**问题**: Diff 中的行号与原始文件行号不一致  
**解决**: 使用 `DiffLine.newLineNumber` 直接匹配

### 2. 评论位置同步
**问题**: 代码变更后评论位置可能失效  
**解决**: 使用 GitHub API 的 `position` 字段，显示"过时评论"标记

### 3. 性能优化
**问题**: 大量评论时渲染性能问题  
**解决**: LazyColumn、折叠、按需加载、缓存

### 4. 跨平台兼容性
**问题**: 不同平台的 UI 适配  
**解决**: Compose Multiplatform 通用组件、响应式布局

## 📚 参考资料

- [GitHub Issue #501](https://github.com/phodal/auto-dev/issues/501)
- [GitHub REST API - Pull Request Reviews](https://docs.github.com/en/rest/pulls/reviews)
- [IntelliJ Platform SDK - Inlay Hints](https://plugins.jetbrains.com/docs/intellij/inlay-hints.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

## 🤝 贡献

如有问题或建议，请在 [Issue #501](https://github.com/phodal/auto-dev/issues/501) 中讨论。

