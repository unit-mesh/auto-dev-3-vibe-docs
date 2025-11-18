# WASM UTF-8/Emoji 字体支持实现总结

## 问题描述

Kotlin WASM JS 平台默认不支持 UTF-8 字符（emoji、中文、日文、韩文等）的正确显示。需要通过 Skiko 引擎预加载字体来解决此问题。

## 解决方案

参考 JetBrains 官方示例实现字体预加载机制。

**官方示例：** https://github.com/JetBrains/compose-multiplatform/blob/master/components/resources/demo/shared/src/webMain/kotlin/main.wasm.kt

## 已完成的工作

### 1. 字体文件配置 ✅

- **下载字体：** NotoColorEmoji.ttf (~10MB)
- **位置：** `mpp-ui/src/commonMain/composeResources/font/NotoColorEmoji.ttf`
- **说明：** 字体必须放在 `composeResources/font/` 目录下（不是普通的 `resources/` 目录）

### 2. 代码实现 ✅

#### Main.kt (`mpp-ui/src/wasmJsMain/kotlin/Main.kt`)

```kotlin
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class, InternalComposeUiApi::class)
fun main() {
    // 配置资源路径映射
    configureWebResources {
        resourcePathMapping { path -> "./$path" }
    }
    
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        // 预加载 emoji 字体
        val emojiFont = preloadFont(Res.font.NotoColorEmoji).value
        var fontsFallbackInitialized by remember { mutableStateOf(false) }

        // 字体加载完成后显示应用，否则显示加载指示器
        if (emojiFont != null && fontsFallbackInitialized) {
            AutoDevApp()
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.8f))) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        // 注册字体到 FontFamilyResolver
        val fontFamilyResolver = LocalFontFamilyResolver.current
        LaunchedEffect(fontFamilyResolver, emojiFont) {
            if (emojiFont != null) {
                fontFamilyResolver.preload(FontFamily(listOf(emojiFont)))
                fontsFallbackInitialized = true
            }
        }
    }
}
```

**关键点：**
- 需要 `@OptIn(InternalComposeUiApi::class)` 注解
- 使用 `preloadFont()` API 加载字体
- 使用 `fontFamilyResolver.preload()` 注册字体
- 在字体加载期间显示加载指示器

#### CodeFont.wasmJs.kt

```kotlin
actual fun getFiraCodeFontFamily(): FontFamily {
    return FontFamily.Monospace
}
```

简化实现，WASM 平台暂时使用系统默认等宽字体。

### 3. 辅助工具 ✅

#### 自动下载脚本

`docs/test-scripts/download-emoji-font.sh`

```bash
./docs/test-scripts/download-emoji-font.sh
```

自动下载 NotoColorEmoji.ttf 到正确的目录。

### 4. 文档 ✅

- `docs/wasm-emoji-font-setup.md` - 完整设置指南
- `docs/wasm-utf8-support-summary.md` - 本文档（实现总结）

## 构建和测试

### 编译

```bash
cd /Volumes/source/ai/autocrud

# 清理旧构建
./gradlew :mpp-ui:clean

# 编译 WASM JS 目标
./gradlew :mpp-ui:compileKotlinWasmJs

# 完整构建（包含 webpack）
./gradlew :mpp-ui:wasmJsBrowserDistribution
```

### 验证

编译成功，字体文件正确加载：

```
✅ 编译成功（无错误）
✅ 字体文件生成：composeResources/.../font/NotoColorEmoji.ttf
✅ 资源访问器生成：Font0.commonMain.kt
✅ 字体加载 URL：http://localhost:8080/composeResources/.../font/NotoColorEmoji.ttf
```

## 技术细节

### 资源生成

Compose Multiplatform 自动生成：

```kotlin
// 生成的资源访问器
internal val Res.font.NotoColorEmoji: FontResource by lazy {
    FontResource("font:NotoColorEmoji", setOf(
        ResourceItem(setOf(), "${MD}font/NotoColorEmoji.ttf", -1, -1),
    ))
}
```

### 字体加载流程

1. `configureWebResources` 配置资源路径
2. `preloadFont` 异步加载字体文件
3. `fontFamilyResolver.preload` 注册字体到渲染引擎
4. Skiko 引擎在需要时使用预加载的字体

### 与官方示例的差异

| 项目 | 官方示例 | 本实现 |
|------|----------|--------|
| 容器组件 | ComposeViewport | CanvasBasedWindow |
| HTML 元素 | `composeApplication` | `ComposeTarget` |
| 字体数量 | 3个（Workbench, FontAwesome, NotoColorEmoji） | 1个（NotoColorEmoji） |
| 内容组件 | UseResources() | AutoDevApp() |

## 故障排除

### 问题：WebAssembly.Exception / Native 转换错误

**原因：** 早期尝试使用 `try-catch` 包裹 Composable 函数调用

**解决：** 移除 try-catch，让 Compose 自然处理异常

### 问题：字体资源未生成

**原因：** 字体放在 `resources/fonts/` 而不是 `composeResources/font/`

**解决：** 移动到正确目录并重新生成资源

### 问题：Unresolved reference 'Res'

**原因：** 使用了错误的包名

**解决：** 使用生成的包名：`autodev_intellij.mpp_ui.generated.resources.Res`

## 后续建议

### 1. 性能优化

- 考虑字体子集化以减小文件大小（目前 10MB）
- 可以只包含常用 emoji 和字符

### 2. 中文字体支持

如需更好的中文显示，可以添加 Noto Sans CJK：

```bash
./docs/test-scripts/download-emoji-font.sh
# 在提示时选择下载 CJK 字体
```

### 3. 字体回退链

未来可以配置多字体回退链：

```kotlin
FontFamily(
    Font(Res.font.FiraCode),      // 代码字体
    Font(Res.font.NotoSansCJK),    // 中日韩字体
    Font(Res.font.NotoColorEmoji)  // Emoji
)
```

## 测试建议

在应用中测试以下字符是否正确显示：

- **Emoji：** 😀 🎉 ✅ ❌ 🚀 💻 🌟 ⚡
- **中文：** 你好，世界！欢迎使用 AutoDev
- **日文：** こんにちは、世界！
- **韩文：** 안녕하세요, 세계!
- **特殊符号：** ©️ ®️ ™️ ⚡ ⭐ ✨

## 参考资料

- [Compose Multiplatform Resources 官方文档](https://github.com/JetBrains/compose-multiplatform/tree/master/components/resources)
- [官方 WASM 示例](https://github.com/JetBrains/compose-multiplatform/blob/master/components/resources/demo/shared/src/webMain/kotlin/main.wasm.kt)
- [Noto Emoji 项目](https://github.com/googlefonts/noto-emoji)
- [Noto CJK 字体](https://github.com/googlefonts/noto-cjk)
- [Skiko 引擎](https://github.com/JetBrains/skiko)

## 许可证

Noto 字体使用 [SIL Open Font License 1.1](https://scripts.sil.org/OFL)，可以自由用于商业和非商业项目。

---

**状态：** ✅ 已完成并验证  
**最后更新：** 2025-11-18  
**作者：** AutoDev Team

