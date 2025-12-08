# KCEF WebView Integration

## 概述

本文档描述了 AutoDev Desktop 版本中 KCEF (Kotlin Chromium Embedded Framework) 的集成方案。

## 实现内容

### 1. KCEF 安装目录

KCEF 安装到 `~/.autodev/kcef-bundle` 目录，用户也可以手动下载并放置到该目录。

#### ConfigManager 扩展

在 `ConfigManager` 中添加了 `getKcefInstallDir()` 方法来获取 KCEF 安装路径：

```kotlin
// Common Main
expect fun getKcefInstallDir(): String

// JVM Implementation
actual fun getKcefInstallDir(): String {
    val kcefDir = File(configDir, "kcef-bundle")
    return kcefDir.absolutePath  // Returns ~/.autodev/kcef-bundle
}
```

### 2. KCEF 管理器

`KcefManager` 负责 KCEF 的初始化和生命周期管理：

- **位置**: `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/kcef/KcefManager.kt`
- **功能**:
  - 后台初始化 KCEF
  - 进度追踪（0-100%）
  - 状态管理（Idle, Initializing, Initialized, Error, RestartRequired）
  - 检查 KCEF 是否已安装

#### 状态流

```kotlin
val initState: StateFlow<KcefInitState>
val downloadProgress: StateFlow<Float>
```

### 3. 进度条 UI

`KcefProgressBar` 在 AutoDevApp 底部显示下载进度：

- **位置**: `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/kcef/KcefProgressBar.kt`
- **特性**:
  - 动画显示/隐藏
  - 实时进度更新
  - 用户可关闭通知（下载继续在后台进行）
  - 错误提示显示

#### UI 状态

- **初始化中**: 显示进度条和下载百分比
- **错误**: 显示错误信息
- **完成**: 自动隐藏

### 4. Desktop 集成

`DesktopAutoDevApp` 是 Desktop 特定的 AutoDevApp 包装器：

- **位置**: `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/DesktopAutoDevApp.kt`
- **功能**:
  - 在启动时自动初始化 KCEF（后台）
  - 在底部显示 KCEF 进度条（使用 Box overlay）
  - 不阻塞主 UI 的使用

#### 使用示例

在 `Main.kt` 中：

```kotlin
DesktopAutoDevApp(
    triggerFileChooser = triggerFileChooser,
    onFileChooserHandled = { triggerFileChooser = false },
    initialMode = mode,
    showTopBarInContent = false,
    // ... other parameters
    onNotification = { title, message ->
        trayState.sendNotification(Notification(title, message))
    }
)
```

### 5. ProGuard 规则

添加了 KCEF 相关的 ProGuard 规则：

```proguard
# KCEF (Kotlin Chromium Embedded Framework)
-keep class org.cef.** { *; }
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory
-dontwarn org.cef.**

# Compose WebView Multiplatform
-keep class com.multiplatform.webview.** { *; }
-dontwarn com.multiplatform.webview.**
```

## 用户体验

### 首次启动

1. 应用启动，显示主界面
2. 底部出现进度条："正在下载 WebView 组件 (X%)"
3. 提示："下载在后台进行，不影响使用"
4. 用户可以继续使用应用的其他功能
5. 下载完成后，进度条自动隐藏

### 手动安装

用户可以手动下载 KCEF 并放置到 `~/.autodev/kcef-bundle` 目录：

1. 从 [KCEF Releases](https://github.com/DatL4g/KCEF/releases) 下载对应平台的版本
2. 解压到 `~/.autodev/kcef-bundle`
3. 重启应用

## 技术细节

### 依赖

```kotlin
// build.gradle.kts
implementation("io.github.kevinnzou:compose-webview-multiplatform:2.0.3")
```

### 初始化流程

```
Main.kt
  └─> DesktopAutoDevApp
      ├─> LaunchedEffect: KcefManager.initialize()
      │   └─> 创建安装目录
      │   └─> 检查是否已安装
      │   └─> 初始化 KCEF (后台)
      │
      └─> KcefProgressBar (底部显示)
          └─> 监听 initState 和 downloadProgress
```

### 状态管理

```kotlin
sealed class KcefInitState {
    data object Idle : KcefInitState()
    data object Initializing : KcefInitState()
    data object Initialized : KcefInitState()
    data object RestartRequired : KcefInitState()
    data class Error(val exception: Throwable) : KcefInitState()
}
```

## 注意事项

### 平台限制

KCEF 仅在 JVM Desktop 平台可用：

- ✅ macOS (Intel, Apple Silicon)
- ✅ Windows (x64)
- ✅ Linux (x64)
- ❌ Android
- ❌ iOS
- ❌ WASM
- ❌ JS (Node.js)

### JVM 参数

Desktop 应用需要特定的 JVM 参数（已在 `build.gradle.kts` 中配置）：

```kotlin
jvmArgs += listOf(
    "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
    "--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED"
)

// macOS 额外参数
if (System.getProperty("os.name").contains("Mac")) {
    jvmArgs += listOf(
        "--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED",
        "--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED"
    )
}
```

## 实际下载流程

当用户首次启动 Desktop 应用时：

1. `DesktopAutoDevApp` 在 `LaunchedEffect` 中检查 KCEF 是否已安装
2. 如果未安装（`~/.autodev/kcef-bundle` 目录为空或不存在）
3. `KcefManager.initialize()` 调用 `KCEF.init()` API
4. KCEF 自动从 GitHub Releases 下载适合当前平台的二进制文件
5. 下载进度通过 `onDownloading` 回调实时更新
6. 下载完成后，KCEF 自动解压并初始化
7. 进度条显示 100% 并自动隐藏

### 下载内容

KCEF 会下载以下内容（根据平台不同）：
- **macOS**: JCEF binaries for macOS (Intel/ARM)
- **Windows**: JCEF binaries for Windows x64
- **Linux**: JCEF binaries for Linux x64

文件大小通常在 80-150MB，取决于平台。

## 未来改进

1. ~~**实际 KCEF 集成**: 当前实现是占位符，需要集成实际的 KCEF API~~ ✅ 已完成
2. **下载镜像**: 提供国内镜像加速下载
3. **离线安装包**: 提供包含 KCEF 的完整安装包
4. **更新机制**: 检测并更新 KCEF 版本
5. **断点续传**: 支持下载失败后的断点续传

## 参考资料

- [KCEF GitHub](https://github.com/DatL4g/KCEF)
- [Compose WebView Multiplatform](https://github.com/KevinnZou/compose-webview-multiplatform)
- [Desktop Setup Guide](https://github.com/KevinnZou/compose-webview-multiplatform/blob/main/README.desktop.md)

