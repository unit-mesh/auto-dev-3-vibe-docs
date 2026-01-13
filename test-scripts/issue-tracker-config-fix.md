# Issue Tracker Configuration 修复总结

## 问题描述

Issue Tracker Configuration 保存之后不生效，Dialog 关闭后重新打开取不到已保存的配置。

## 根本原因

### 1. Compose `remember` 缓存问题
Dialog 的 state 使用 `remember` 但没有正确的 key，导致 Dialog 关闭后重新打开时使用了缓存的旧值。

### 2. Auto-detection 总是覆盖手动配置
`IssueService.autoDetectRepo()` 总是会覆盖用户手动配置的 `repoOwner` 和 `repoName`。

## 修复内容

### 修复 1: Dialog 调用方的 state 管理

**文件**: `CodeReviewSideBySideView.kt`, `CodeReviewPage.kt`

```kotlin
// 修复前
var currentConfig by remember {
    mutableStateOf(IssueTrackerConfig())
}
LaunchedEffect(Unit) {
    currentConfig = ConfigManager.getIssueTracker()
}

// 修复后
var currentConfig by remember(showConfigDialog) {
    mutableStateOf(IssueTrackerConfig())
}
LaunchedEffect(showConfigDialog) {
    currentConfig = ConfigManager.getIssueTracker()
}
```

### 修复 2: Dialog 内部的 state 管理

**文件**: `IssueTrackerConfigDialog.kt`

```kotlin
// 修复前
var type by remember { mutableStateOf(initialConfig.type) }
var token by remember { mutableStateOf(initialConfig.token) }

// 修复后
var type by remember(initialConfig) { mutableStateOf(initialConfig.type) }
var token by remember(initialConfig) { mutableStateOf(initialConfig.token) }
```

### 修复 3: Auto-detection 逻辑

**文件**: `IssueService.kt`

```kotlin
// 修复后 - 只在 repo 信息为空时才自动检测
private suspend fun autoDetectRepo(...): IssueTrackerConfig {
    if (config.repoOwner.isNotBlank() && config.repoName.isNotBlank()) {
        return config  // 保留手动配置
    }
    // ... 自动检测逻辑
}
```

## 新增功能：可点击的 Token 创建链接

### 功能描述
在 Dialog 中，"Create token at: xxx" 的文本现在是可点击的链接，点击后会在系统默认浏览器中打开对应的 token 创建页面。

### 支持的平台
- **GitHub**: `https://github.com/settings/tokens`
- **GitLab**: `{serverUrl}/-/profile/personal_access_tokens`
- **Jira**: `https://id.atlassian.com/manage-profile/security/api-tokens`

### 跨平台实现
创建了新的 `UrlOpener` 工具类，支持所有平台：
- **JVM**: `java.awt.Desktop.browse()`
- **JS/Node**: `window.open()`
- **WASM**: JS interop
- **Android**: `Intent.ACTION_VIEW`
- **iOS**: `UIApplication.openURL()`

## 测试步骤

### 1. 测试手动配置保存和加载
1. 打开 Code Review 页面
2. 点击 Issue Tracker 配置按钮
3. 填写配置并保存
4. 关闭 Dialog 后重新打开
5. **验证**: 所有字段应该显示刚才保存的值

### 2. 测试可点击链接
1. 打开 Dialog
2. 选择 GitHub 类型
3. 点击 "Create token at: github.com/settings/tokens"
4. **验证**: 浏览器应该打开 GitHub token 创建页面

### 3. 测试 Auto-detection 不覆盖手动配置
1. 手动配置 repo 为 `phodal/auto-dev`
2. 保存并重启应用
3. **验证**: 日志显示 "Using manually configured repo: phodal/auto-dev"

## 相关文件

### 修改的文件
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/IssueTrackerConfigDialog.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/CodeReviewSideBySideView.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/CodeReviewPage.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/IssueService.kt`

### 新增的文件
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/platform/UrlOpener.kt`
- `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/platform/UrlOpener.jvm.kt`
- `mpp-ui/src/jsMain/kotlin/cc/unitmesh/devins/ui/platform/UrlOpener.js.kt`
- `mpp-ui/src/androidMain/kotlin/cc/unitmesh/devins/ui/platform/UrlOpener.android.kt`
- `mpp-ui/src/iosMain/kotlin/cc/unitmesh/devins/ui/platform/UrlOpener.ios.kt`
- `mpp-ui/src/wasmJsMain/kotlin/cc/unitmesh/devins/ui/platform/UrlOpener.wasmJs.kt`

