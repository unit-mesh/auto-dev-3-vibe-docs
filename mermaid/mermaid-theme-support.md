# Mermaid Theme Support

## 概述

成功为 Mermaid 渲染器添加了主题支持，现在可以根据系统主题自动切换 Dark/Light 模式。

## 主题特性

### 1. 双主题配置

**Dark Theme (Darcula)**:
- 基于 IntelliJ IDEA Darcula 配色方案
- 主色调：`#8888C6` (紫蓝色)
- 边框颜色：`#6897BB` (蓝色)
- 线条颜色：`#0F9795` (青色)
- 注释背景：`#364135` (深绿色)
- 透明背景，完美集成到 Dark UI

**Light Theme (Default)**:
- 基于 IntelliJ IDEA 默认配色方案
- 主色调：`#0033B3` (深蓝色)
- 边框颜色：`#174AD4` (亮蓝色)
- 线条颜色：`#008080` (Teal)
- 注释背景：`#EDFCED` (浅绿色)
- 透明背景，完美集成到 Light UI

### 2. 颜色配置参考

主题配置参考了 IntelliJ IDEA Mermaid 插件的颜色方案：

```
Dark Theme:
- MERMAID_DIAGRAM_NAME: #8888C6 (Bold Italic)
- MERMAID_KEYWORD: #6897BB (Italic)
- MERMAID_IDENTIFIER: #94558D
- MERMAID_EDGE: #0F9795
- MERMAID_NOTE_BKG: #364135
- MERMAID_GENERIC: #20999D
- MERMAID_TITLE: #20999D

Light Theme:
- MERMAID_DIAGRAM_NAME: #0033B3 (Bold Italic)
- MERMAID_KEYWORD: #174AD4 (Italic)
- MERMAID_IDENTIFIER: #94558D
- MERMAID_EDGE: #008080
- MERMAID_NOTE_BKG: #EDFCED
- MERMAID_GENERIC: #007E8A
- MERMAID_TITLE: #007E8A
```

## 使用方法

### 在 Markdown 中使用

主题会自动根据系统设置切换：

```markdown
\`\`\`mermaid
graph TD
    A[Start] --> B{Decision}
    B -->|Yes| C[Success]
    B -->|No| D[Retry]
    C --> E[End]
    D --> A
\`\`\`
```

### 手动指定主题

在代码中可以手动指定主题：

```kotlin
MermaidRenderer(
    mermaidCode = code,
    isDarkTheme = true,  // 或 false
    modifier = Modifier.fillMaxSize()
)
```

### 测试主题切换

运行 MermaidPreview 应用可以测试主题切换功能：

```bash
./gradlew :mpp-viewer-web:run
```

应用中有一个 "Switch to Light/Dark" 按钮可以实时切换主题。

## API 变更

### MermaidRenderer

新增了 `isDarkTheme` 参数：

```kotlin
@Composable
fun MermaidRenderer(
    mermaidCode: String,
    isDarkTheme: Boolean = true,  // 新增参数
    modifier: Modifier = Modifier,
    onRenderComplete: ((success: Boolean, message: String) -> Unit)? = null
)
```

### MarkdownSketchRenderer 集成

自动从系统获取主题设置：

```kotlin
val isDarkTheme = isSystemInDarkTheme()

// Mermaid 图表会自动使用正确的主题
if (language?.lowercase() == "mermaid") {
    MermaidRenderer(
        mermaidCode = code,
        isDarkTheme = isDarkTheme,  // 自动传递主题
        modifier = Modifier.fillMaxSize()
    )
}
```

## 运行说明

### ✅ 推荐方式 - 通过 Gradle 运行

```bash
# 运行 Desktop 应用
./gradlew :mpp-ui:run

# 运行 Mermaid 预览
./gradlew :mpp-viewer-web:run
```

### ⚠️ IntelliJ IDEA 运行配置

如果在 IntelliJ IDEA 中遇到 `NoClassDefFoundError: cc/unitmesh/viewer/web/MermaidRendererKt`：

**原因**: IDE 的 Run Configuration 可能没有正确包含所有依赖。

**解决方案**:
1. **推荐**: 使用 Gradle 运行（见上方）
2. 或者在 IDE 中配置 Run Configuration:
   - Run → Edit Configurations
   - 选择你的运行配置
   - 在 "Use classpath of module" 中选择 `mpp-ui.jvmMain`
   - 点击 "Modify options" → 勾选 "Include dependencies with 'Provided' scope"
   - Apply 并重新运行

3. 或者重新导入 Gradle 项目:
   - File → Invalidate Caches / Restart
   - 重新 Sync Gradle

## 技术实现

### HTML 端

`mermaid.html` 中实现了双主题配置：

```javascript
const themes = {
    dark: { /* Darcula 配色 */ },
    default: { /* IntelliJ Light 配色 */ }
};

function initMermaid(themeName) {
    const themeConfig = themes[themeName];
    mermaid.initialize({ 
        theme: themeConfig.theme,
        themeVariables: themeConfig.themeVariables
    });
}

window.renderMermaid = async function(code, themeName) {
    // 如果主题改变，重新初始化
    if (themeName && themeName !== currentTheme) {
        initMermaid(themeName);
    }
    // 渲染图表
};
```

### Kotlin 端

通过参数传递主题选择：

```kotlin
LaunchedEffect(webViewState.isLoading, mermaidCode, isDarkTheme) {
    val theme = if (isDarkTheme) "dark" else "default"
    val jsCode = """
        if (typeof renderMermaid === 'function') {
            renderMermaid(`$escapedCode`, '$theme');
        }
    """
    webViewNavigator.evaluateJavaScript(jsCode)
}
```

## 测试结果

✅ 所有测试通过：
- `mpp-viewer-web:build` - SUCCESS
- `mpp-ui:compileKotlinJvm` - SUCCESS
- `mpp-ui:run` - SUCCESS (Desktop 应用正常启动)
- `mpp-viewer-web:run` - SUCCESS (主题切换正常工作)

## 配色优化建议

基于 IntelliJ IDEA 插件的最佳实践：

1. **节点颜色**: 使用主色调和边框颜色区分不同类型的节点
2. **边和线**: 使用对比色（如 Teal/Cyan）使流程更清晰
3. **注释**: 使用背景色突出显示注释块
4. **文本**: 确保文本在深色和浅色背景上都清晰可读
5. **透明背景**: 让图表无缝集成到任何 UI 中

## 未来增强

- [ ] 添加更多预设主题（GitHub Dark, Solarized 等）
- [ ] 支持自定义主题颜色
- [ ] 添加主题配置 UI
- [ ] 支持主题实时预览
- [ ] 导出带主题的 SVG

