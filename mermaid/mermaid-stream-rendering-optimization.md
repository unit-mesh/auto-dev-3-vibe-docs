# Mermaid Stream Rendering Optimization

## 问题背景

当 AI 以流式（stream）方式返回内容时，Mermaid 代码块会在内容不完整的情况下被渲染，导致：

1. **渲染错误**：不完整的 Mermaid 语法会导致渲染失败
2. **性能问题**：每次更新都尝试渲染不完整的图表
3. **用户体验差**：用户会看到频繁的错误信息

## 解决方案

### 1. 添加 `isComplete` 参数

在 `MarkdownSketchRenderer.RenderMarkdown` 中添加 `isComplete` 参数，用于标识内容是否完整：

```kotlin
@Composable
fun RenderMarkdown(
    markdown: String,
    isComplete: Boolean = true,  // 新增参数
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
)
```

### 2. 条件渲染 Mermaid 图表

只有在内容完整时才渲染 Mermaid 图表，否则显示代码块：

```kotlin
codeFence = {
    val style = LocalMarkdownTypography.current.code
    MarkdownCodeFence(it.content, it.node, style) { code, language, style ->
        // Only render mermaid diagrams when content is complete
        if (language?.lowercase() == "mermaid" && isComplete && Platform.isJvm) {
            MermaidRenderer(
                mermaidCode = code,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Show code block during streaming or for other languages
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

### 3. WalkthroughBlockRenderer 传递 isComplete

```kotlin
@Composable
fun WalkthroughBlockRenderer(
    walkthroughContent: String,
    modifier: Modifier = Modifier,
    isComplete: Boolean = true
) {
    // ...
    MarkdownSketchRenderer.RenderMarkdown(
        markdown = walkthroughContent,
        isComplete = isComplete,  // 传递参数
        isDarkTheme = isDarkTheme,
        modifier = modifier
    )
}
```

## 渲染流程

### 流式传输中 (isComplete = false)

```
AI Response (Streaming) → WalkthroughBlockRenderer
                          ↓ isComplete=false
                     RenderMarkdown
                          ↓
                   检测到 mermaid 代码块
                          ↓
                   显示带语法高亮的代码块
                   (不尝试渲染图表)
```

### 内容完整后 (isComplete = true)

```
AI Response (Complete) → WalkthroughBlockRenderer
                         ↓ isComplete=true
                    RenderMarkdown
                         ↓
                  检测到 mermaid 代码块
                         ↓
                    MermaidRenderer
                         ↓
                   渲染完整的图表
```

## 效果对比

### 优化前

| 阶段 | 行为 | 结果 |
|------|------|------|
| 流式传输中 | 尝试渲染不完整的 Mermaid 代码 | ❌ 渲染错误 |
| 每次更新 | 重新渲染 | ❌ 性能浪费 |
| 用户看到 | 频繁的错误提示 | ❌ 体验差 |

### 优化后

| 阶段 | 行为 | 结果 |
|------|------|------|
| 流式传输中 | 显示代码块 | ✅ 显示正常 |
| 每次更新 | 只更新代码块文本 | ✅ 性能好 |
| 内容完整 | 渲染图表 | ✅ 一次性渲染 |
| 用户看到 | 平滑过渡 | ✅ 体验好 |

## 代码变更

### 1. 接口定义

**文件**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/sketch/MarkdownSketchRenderer.kt`

```kotlin
expect object MarkdownSketchRenderer {
    @Composable
    fun RenderMarkdown(
        markdown: String,
        isComplete: Boolean = true,  // 新增
        isDarkTheme: Boolean = false,
        modifier: Modifier = Modifier
    )
}
```

### 2. JVM 实现

**文件**: `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/sketch/MarkdownSketchRenderer.jvm.kt`

关键逻辑：
```kotlin
if (language?.lowercase() == "mermaid" && isComplete && Platform.isJvm) {
    // 只在完整时渲染
    MermaidRenderer(...)
} else {
    // 流式传输时显示代码
    MarkdownHighlightedCode(...)
}
```

### 3. 其他平台实现

所有平台（JS、Android、iOS、WasmJS）的实现都添加了 `isComplete` 参数以保持接口一致性。

## 测试

### 测试场景

1. **流式传输中**:
   ```kotlin
   WalkthroughBlockRenderer(
       walkthroughContent = partialMermaidCode,
       isComplete = false
   )
   ```
   **预期**: 显示代码块，不渲染图表

2. **内容完整**:
   ```kotlin
   WalkthroughBlockRenderer(
       walkthroughContent = completeMermaidCode,
       isComplete = true
   )
   ```
   **预期**: 渲染 Mermaid 图表

3. **非 Mermaid 代码**:
   ```kotlin
   // 无论 isComplete 为何值，都显示语法高亮代码
   ```
   **预期**: 始终显示语法高亮的代码块

### 编译测试

```bash
# 编译所有平台
./gradlew :mpp-ui:compileKotlinJvm
./gradlew :mpp-viewer-web:build

# 运行测试
./gradlew :mpp-ui:jvmTest
```

✅ **所有测试通过**

## 最佳实践

### 1. 在流式 AI 响应中使用

```kotlin
@Composable
fun AIResponseRenderer(response: AIResponse) {
    WalkthroughBlockRenderer(
        walkthroughContent = response.content,
        isComplete = response.isComplete,  // 从 AI 响应获取状态
        modifier = Modifier.fillMaxWidth()
    )
}
```

### 2. 默认值

如果内容总是完整的（非流式），可以省略 `isComplete` 参数（默认为 `true`）：

```kotlin
MarkdownSketchRenderer.RenderMarkdown(
    markdown = completeMarkdown,
    // isComplete 默认为 true
    isDarkTheme = isDarkTheme
)
```

### 3. 性能考虑

- **流式传输时**: 只更新文本内容，不渲染图表（轻量级操作）
- **内容完整后**: 一次性渲染图表（重量级操作，但只执行一次）

## 兼容性

- ✅ **JVM Desktop**: 完全支持，Mermaid 图表正常渲染
- ✅ **Android**: 接口兼容，显示代码块
- ✅ **iOS**: 接口兼容，显示代码块
- ✅ **JS/Web**: 接口兼容，显示代码块
- ✅ **WASM**: 接口兼容，显示代码块

**注意**: Mermaid 图表渲染目前仅在 JVM 平台支持（使用 KCEF/WebView）。

## 未来增强

- [ ] 在流式传输过程中显示"正在接收..."提示
- [ ] 添加渲染进度指示器
- [ ] 支持其他平台的 Mermaid 渲染（如 Android 使用 WebView）
- [ ] 缓存已渲染的图表以避免重复渲染

