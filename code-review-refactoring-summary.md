# Code Review Agent 重构总结

## 重构目标

将 CodeReviewAgent 的 JVM 和 JS 版本统一为两步方法（two-step approach），使用统一的提示词模板，简化代码结构。

## 重构内容

### 1. 统一两步方法

参考 `CodeReviewViewModel.kt` 的实现，将代码审查流程统一为两个步骤：

1. **analyzeLintOutput()** - 分析代码和 lint 结果
2. **generateFixes()** - 生成修复建议

### 2. 简化提示词模板

在 `CodeReviewAgentPromptRenderer.kt` 中只保留两套提示词模板：

1. **CodeReviewAnalysisTemplate** - 用于 analyzeLintOutput 步骤
2. **FixGenerationTemplate** - 用于 generateFixes 步骤

删除了以下旧模板：
- `CodeReviewAgentTemplate`
- `IntentAnalysisTemplate`

### 3. 核心方法移至 CodeReviewAgent.kt

将以下方法从 ViewModel 移至 `CodeReviewAgent.kt`，使其在所有平台（JVM、JS、Android）上可用：

```kotlin
suspend fun analyzeLintOutput(
    reviewType: String = "COMPREHENSIVE",
    filePaths: List<String>,
    codeContent: Map<String, String>,
    lintResults: Map<String, String>,
    diffContext: String = "",
    language: String = "EN",
    onProgress: (String) -> Unit = {}
): String

suspend fun generateFixes(
    codeContent: Map<String, String>,
    lintResults: List<LintFileResult>,
    analysisOutput: String,
    language: String = "EN",
    onProgress: (String) -> Unit = {}
): String
```

### 4. 添加数据类型定义

在 `CodeReviewAgent.kt` 中添加了以下数据类型：

```kotlin
data class LintFileResult(
    val filePath: String,
    val linterName: String,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val issues: List<LintIssueUI>
)

data class LintIssueUI(
    val line: Int,
    val column: Int,
    val severity: LintSeverityUI,
    val message: String,
    val rule: String? = null,
    val suggestion: String? = null
)

enum class LintSeverityUI {
    ERROR,
    WARNING,
    INFO
}
```

### 5. JS 导出更新

在 `CodeReviewAgentExports.kt` 中添加了新的 JS 导出方法：

```kotlin
@JsName("analyzeLintOutput")
fun analyzeLintOutput(...)

@JsName("generateFixes")
fun generateFixes(...)
```

并添加了相应的 JS 友好类型：
- `JsLintFileResult`
- `JsLintIssueUI`

### 6. CLI 更新

更新了 `ReviewMode.ts` 以使用新的两步方法：

1. 调用 `analyzeLintOutput()` 进行分析
2. 如果有 lint 问题，调用 `generateFixes()` 生成修复建议

## 文件变更列表

### 修改的文件

1. `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgent.kt`
   - 添加 `analyzeLintOutput()` 方法
   - 添加 `generateFixes()` 方法
   - 添加数据类型定义（LintFileResult, LintIssueUI, LintSeverityUI）
   - 简化旧的 `analyze()` 方法

2. `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgentPromptRenderer.kt`
   - 删除 `render()` 和 `renderIntentAnalysisPrompt()` 方法
   - 保留 `renderAnalysisPrompt()` 方法
   - 添加 `renderFixGenerationPrompt()` 方法
   - 删除 `CodeReviewAgentTemplate` 和 `IntentAnalysisTemplate`
   - 添加 `FixGenerationTemplate`

3. `mpp-core/src/jsMain/kotlin/cc/unitmesh/agent/CodeReviewAgentExports.kt`
   - 添加 `analyzeLintOutput()` JS 导出
   - 添加 `generateFixes()` JS 导出
   - 添加 `JsLintFileResult` 和 `JsLintIssueUI` 类型

4. `mpp-ui/src/jsMain/typescript/modes/ReviewMode.ts`
   - 更新为使用两步方法
   - 先调用 `analyzeLintOutput()`
   - 再调用 `generateFixes()`（如果有 lint 问题）

## 测试

使用以下命令测试：

```bash
cd mpp-ui && node dist/jsMain/typescript/index.js review -p ..
```

## 优势

1. **统一架构** - JVM 和 JS 版本使用相同的两步方法
2. **简化提示词** - 只有两套提示词模板，易于维护
3. **更好的分离** - 分析和修复分为两个独立步骤
4. **跨平台一致性** - 所有平台使用相同的核心逻辑

