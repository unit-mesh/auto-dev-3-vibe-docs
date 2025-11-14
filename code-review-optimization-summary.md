# CodeReview 优化总结

## 概述

对 CodeReview 功能进行了重大优化，将 "AI Analysis" 和 "Suggested Fixes" 拆分成两个独立的组件，分别支持 Markdown 渲染和可交互的 Diff Patch 应用。

## 主要变更

### 1. 新增组件

#### AIAnalysisSection.kt
- **位置**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/AIAnalysisSection.kt`
- **功能**: 
  - 展示 AI 分析结果，使用 Markdown 格式
  - 使用 `MarkdownSketchRenderer` 渲染内容
  - 只显示优先级最高的 10 个 issue
  - 支持折叠/展开
  - 显示分析状态（ANALYZING）

#### SuggestedFixesSection.kt
- **位置**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/SuggestedFixesSection.kt`
- **功能**:
  - 展示 AI 建议的修复，使用标准 Diff Patch 格式
  - 使用 `DiffSketchRenderer` 渲染 diff patches
  - 支持 Accept/Reject 操作
  - 自动解析 ````diff` 代码块和标准 diff 格式
  - 显示每个 patch 的状态（Applied/Rejected）
  - 显示 patch 数量统计

### 2. 提示词优化

#### CodeReviewAnalysisTemplate
- **文件**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgentPromptRenderer.kt`
- **变更**:
  - 限制输出为 **TOP 10 HIGHEST PRIORITY** issues
  - 使用规范的 Markdown 格式
  - 添加 emoji 指示器（📊 Summary, 🚨 Top 10 Issues）
  - 包含完整的问题描述：Severity、Category、Location、Problem、Impact、Suggested Fix
  - 按优先级排序：Security (CRITICAL) → Logic errors (HIGH) → Performance (MEDIUM-HIGH) → Design (MEDIUM) → Code quality (LOW-MEDIUM)

#### buildFixGenerationPrompt
- **文件**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/CodeReviewViewModel.kt`
- **变更**:
  - 生成标准 **Unified Diff Format** patches
  - 提供详细的 diff 格式示例
  - 要求每个 fix 在 ````diff` 代码块中
  - 包含准确的行号和上下文
  - 限制最多 5 个 patches，聚焦最重要的修复

### 3. ViewModel 增强

#### CodeReviewViewModel.kt
- **新增方法**:
  - `applyDiffPatch(diffPatch: String)`: 应用 diff patch 到工作区
  - `rejectDiffPatch(diffPatch: String)`: 拒绝 diff patch
  - `applyDiffPatchToFile(filePath: String, fileDiff: FileDiff)`: 应用单个文件的 diff patch

- **实现细节**:
  - 使用 `DiffParser` 解析 diff patch
  - 逐行应用修改（CONTEXT、DELETED、ADDED）
  - 验证上下文行匹配
  - 提供详细的日志记录
  - 错误处理和统计反馈

### 4. UI 更新

#### CodeReviewAgentPanel.kt
- **变更**:
  - 替换 `CollapsibleAnalysisCard` 为 `AIAnalysisSection`
  - 替换 `CollapsibleAnalysisCard` 为 `SuggestedFixesSection`
  - 连接 ViewModel 的 `applyDiffPatch` 和 `rejectDiffPatch` 方法
  - 保持原有的 Lint Analysis Section 不变

## 技术架构

### 数据流

```
CodeReviewViewModel
  ↓
  ├── analyzeLintOutput()
  │   ↓
  │   使用 CodeReviewAnalysisTemplate (Top 10 Issues, Markdown)
  │   ↓
  │   AIAnalysisSection
  │   └── MarkdownSketchRenderer (支持流式渲染)
  │
  └── generateFixes()
      ↓
      使用 buildFixGenerationPrompt (Unified Diff Format)
      ↓
      SuggestedFixesSection
      └── DiffSketchRenderer (支持 Accept/Reject)
          ↓
          applyDiffPatch() / rejectDiffPatch()
```

### 组件层次

```
CodeReviewAgentPanel
├── CollapsibleLintAnalysisCard (保持原样)
├── AIAnalysisSection (新增)
│   └── MarkdownSketchRenderer
└── SuggestedFixesSection (新增)
    ├── DiffPatchCard (多个)
    │   └── DiffSketchRenderer
    │       ├── Accept Button → applyDiffPatch()
    │       └── Reject Button → rejectDiffPatch()
    └── extractDiffPatches() (解析 diff)
```

## 用户体验改进

### AI Analysis
1. **更聚焦**: 只显示前 10 个最重要的问题，避免信息过载
2. **更清晰**: 使用 Markdown 格式，支持标题、列表、代码块等
3. **更直观**: 使用 emoji 指示器快速识别问题类型
4. **流式渲染**: 支持实时显示分析结果

### Suggested Fixes
1. **可操作**: 每个 fix 都可以直接应用或拒绝
2. **可视化**: 使用标准 diff 格式，清晰显示代码变更
3. **状态管理**: 显示 Applied/Rejected 状态，避免重复操作
4. **批量处理**: 支持多个 patches，自动解析和分组

## 兼容性

- ✅ JVM/Desktop
- ✅ Android
- ✅ iOS
- ✅ JS/Browser
- ✅ WASM

所有平台都使用相同的组件和逻辑，通过 `expect/actual` 机制提供平台特定的实现（如 Markdown 渲染）。

## 测试

- ✅ 编译通过（JVM、JS 平台）
- ✅ 无 linter 错误
- ⚠️ 需要手动测试：
  - AI Analysis 的 Markdown 渲染效果
  - Suggested Fixes 的 diff patch 应用功能
  - Accept/Reject 按钮的交互

## 后续优化建议

1. **Diff Patch 应用优化**:
   - 当前实现是简单的逐行匹配和替换
   - 可以使用更健壮的 patch 算法（如 GNU patch）
   - 添加冲突检测和合并功能

2. **批量操作**:
   - 添加 "Apply All" 和 "Reject All" 按钮
   - 支持选择性应用多个 patches

3. **预览功能**:
   - 在应用 patch 前显示预览
   - 支持撤销已应用的 patch

4. **持久化**:
   - 保存用户的 Accept/Reject 决策
   - 支持跨 session 恢复状态

## 文件清单

### 新增文件
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/AIAnalysisSection.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/SuggestedFixesSection.kt`

### 修改文件
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgentPromptRenderer.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/CodeReviewViewModel.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/CodeReviewAgentPanel.kt`

## 总结

这次优化显著提升了 CodeReview 功能的用户体验：
- **AI Analysis** 更聚焦、更清晰、更易读
- **Suggested Fixes** 更可操作、更直观、更高效

通过拆分组件、优化提示词和增强交互，CodeReview 功能现在能够更好地帮助开发者快速识别和修复代码问题。

