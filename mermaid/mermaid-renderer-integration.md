# Mermaid Renderer Integration

## 概述

成功将 Mermaid 图表渲染器集成到 AutoDev 项目中，使其能够在 Desktop Compose 应用中渲染 Mermaid 图表。

## 主要改动

### 1. 初始化 KCEF (Kotlin CEF Framework)

**文件**: `mpp-viewer-web/src/jvmMain/kotlin/cc/unitmesh/viewer/web/MermaidPreview.kt`

- 添加了 KCEF 初始化逻辑（Desktop WebView 所需）
- 实现了下载进度显示和错误处理
- 配置了 CEF bundle 和缓存目录

**文件**: `mpp-viewer-web/build.gradle.kts`

- 添加了 JVM 参数以支持 KCEF 运行
- 配置了 macOS 特定的参数
- 添加了 Jogamp Maven 仓库

### 2. 简化 HTML 和清理 Debug 信息

**文件**: `mpp-viewer-web/src/commonMain/resources/mermaid.html`

- 移除了所有 alert 和 debug 日志
- 简化了 UI，只保留核心渲染功能
- 实现了自动高度调整
- 使用 dark theme 配置，适配 Compose UI
- 参考 IntelliJ IDEA Mermaid 插件的简洁样式

**样式特点**:
- 透明背景，便于集成到任何 UI 中
- 暗色主题，配色与 IDE 一致
- 自动调整 SVG 大小以适应容器
- 错误信息友好展示

### 3. 清理 Kotlin 代码中的 Debug 信息

**文件**: `mpp-viewer-web/src/commonMain/kotlin/cc/unitmesh/viewer/web/MermaidRenderer.kt`

- 移除了所有 println 日志
- 移除了 alert 调试代码
- 简化了 JSBridge 处理器（只保留必要的回调）
- 优化了代码可读性

**文件**: `mpp-viewer-web/src/jvmMain/kotlin/cc/unitmesh/viewer/web/MermaidHtml.jvm.kt`

- 移除了加载日志
- 简化了 fallback HTML

**文件**: `mpp-viewer-web/src/jvmMain/kotlin/cc/unitmesh/viewer/web/MermaidPreview.kt`

- 移除了所有调试日志
- 简化了 KCEF 初始化流程

### 4. 集成到 Markdown 渲染器

**文件**: `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/sketch/MarkdownSketchRenderer.jvm.kt`

- 在 `codeFence` 处理中添加了 Mermaid 检测
- 当代码块语言为 "mermaid" 时，使用 `MermaidRenderer` 组件
- 其他语言继续使用 `MarkdownHighlightedCode`

**关键代码**:
```kotlin
codeFence = {
    val style = LocalMarkdownTypography.current.code
    MarkdownCodeFence(it.content, it.node, style) { code, language, style ->
        // Render mermaid diagrams with MermaidRenderer
        if (language?.lowercase() == "mermaid") {
            cc.unitmesh.viewer.web.MermaidRenderer(
                mermaidCode = code,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MarkdownHighlightedCode(
                code = code,
                language = language,
                style = style,
                highlightsBuilder = highlightsBuilder,
                showHeader = true,
            )
        }
    }
}
```

## 使用方法

### 在 Markdown 中使用

```markdown
\`\`\`mermaid
graph TD
    A[Start] --> B{Is it working?}
    B -->|Yes| C[Great!]
    B -->|No| D[Debug]
    C --> E[End]
    D --> B
\`\`\`
```

### 测试 Mermaid 渲染器

运行 MermaidPreview 应用：

```bash
./gradlew :mpp-viewer-web:run
```

## 测试结果

✅ 所有模块编译通过：
- `mpp-viewer-web:build` - SUCCESS
- `mpp-ui:compileKotlinJvm` - SUCCESS
- `mpp-ui:jvmTest` - SUCCESS (所有测试通过)

✅ MermaidPreview 应用正常运行：
- KCEF 成功初始化
- WebView 正常加载
- Mermaid 图表成功渲染

## 技术栈

- **KCEF**: v2024.04.20.4 - Kotlin CEF (Chromium Embedded Framework)
- **Mermaid.js**: v11.4.0 - 从 CDN 加载
- **Compose Multiplatform**: Desktop WebView
- **compose-webview-multiplatform**: v2.0.3

## 依赖关系

```
mpp-ui (JVM)
  ├── mpp-viewer-web
  │   ├── mpp-viewer
  │   └── compose-webview-multiplatform
  │       └── KCEF
  └── multiplatform-markdown-renderer
```

## 注意事项

1. **首次运行**: KCEF 会下载 Chromium 二进制文件（约 200MB），需要等待一段时间
2. **网络依赖**: Mermaid.js 从 CDN 加载，需要网络连接
3. **平台限制**: 当前仅支持 JVM Desktop 平台

## 未来优化建议

1. 将 Mermaid.js 打包到本地资源，避免网络依赖
2. 添加更多主题选项（light/dark 切换）
3. 支持导出为 PNG/SVG
4. 添加缩放和拖拽功能
5. 考虑添加编辑器模式（实时预览）

