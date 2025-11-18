# WASM UTF-8 字体支持实现总结

## 🎯 实现目标

为 Kotlin WASM JS 应用添加完整的 UTF-8 字符支持，包括：
- 中文（简体/繁体）
- 日文（平假名、片假名、汉字）
- 韩文（谚文）
- Emoji 表情符号
- 拉丁、西里尔、希腊等字符

## ✅ 完成的工作

### 1. 字体下载与管理

**文件：`mpp-ui/build.gradle.kts`**

创建了 `DownloadWasmFontsTask` Gradle 任务：
- ✅ 自动下载 Noto Sans SC Variable Font TTF (~17MB)
- ✅ 支持配置 `-PuseCJKFont=false` 下载轻量级字体 (~500KB)
- ✅ 配置 Gradle 依赖确保字体在资源处理前下载
- ✅ 兼容 Gradle Configuration Cache
- ✅ 字体文件**不提交到 Git**（通过 `.gitignore` 配置）

```bash
# 下载完整 CJK 字体（默认）
./gradlew :mpp-ui:downloadWasmFonts

# 下载轻量级字体
./gradlew :mpp-ui:downloadWasmFonts -PuseCJKFont=false
```

### 2. WASM 入口点字体加载

**文件：`mpp-ui/src/wasmJsMain/kotlin/Main.kt`**

实现了官方推荐的字体预加载机制：
- ✅ 使用 `preloadFont()` API 异步加载字体
- ✅ 使用 `FontFamilyResolver.preload()` 注册字体
- ✅ 显示加载指示器直到字体就绪
- ✅ 配置 `configureWebResources` 资源路径映射

```kotlin
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class, InternalComposeUiApi::class)
fun main() {
    configureWebResources {
        resourcePathMapping { path -> "./$path" }
    }

    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        val utf8Font = preloadFont(Res.font.NotoSansSC_Regular).value
        var fontsFallbackInitialized by remember { mutableStateOf(false) }

        if (utf8Font != null && fontsFallbackInitialized) {
            AutoDevApp()
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.8f))) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        val fontFamilyResolver = LocalFontFamilyResolver.current
        LaunchedEffect(fontFamilyResolver, utf8Font) {
            if (utf8Font != null) {
                fontFamilyResolver.preload(FontFamily(listOf(utf8Font)))
                fontsFallbackInitialized = true
            }
        }
    }
}
```

### 3. GitHub Actions 集成

**文件：`.github/workflows/deploy-wasm.yml`**

在 CI/CD 流水线中添加字体下载步骤：
```yaml
- name: Download WASM Fonts for UTF-8 Support
  run: |
    echo "📦 Downloading Noto Sans SC TTF for full UTF-8 support..."
    ./gradlew :mpp-ui:downloadWasmFonts --no-daemon --info
```

### 4. 文档更新

**文件：`docs/wasm-emoji-font-setup.md`**

更新文档说明：
- ✅ 强调必须使用 TTF 格式（WASM 不支持 OTF）
- ✅ 说明 Gradle 自动下载机制
- ✅ 提供手动下载备选方案
- ✅ 添加故障排除指南

## 🔑 关键技术要点

### 1. 字体格式要求

⚠️ **WASM 只支持 TTF 格式，不支持 OTF 格式！**

这是 Skiko 引擎的限制，使用 OTF 会导致字体无法加载。

### 2. 字体文件位置

字体必须放在 Compose Resources 目录：
```
mpp-ui/src/commonMain/composeResources/font/
```

而不是旧的 resources 目录。

### 3. 资源生成

Compose Multiplatform 会自动生成资源访问代码：
```kotlin
// 自动生成在：
// build/generated/compose/resourceGenerator/kotlin/commonMain/autodev_intellij/mpp_ui/generated/resources/Res.kt

val Res.font.NotoSansSC_Regular: FontResource
```

### 4. 字体加载流程

1. **配置资源路径** → `configureWebResources`
2. **异步加载字体** → `preloadFont()`
3. **注册字体家族** → `fontFamilyResolver.preload()`
4. **显示加载指示器** → 直到 `fontsFallbackInitialized = true`
5. **渲染应用** → `AutoDevApp()`

## 📦 字体文件信息

| 字体 | 大小 | 格式 | 支持语言 | 用途 |
|-----|------|------|---------|-----|
| NotoSansSC-Regular.ttf | ~17MB | TTF (Variable Font) | 中日韩+Emoji | 生产环境（推荐） |
| NotoColorEmoji.ttf | ~10MB | TTF | 仅 Emoji | 仅需 Emoji 支持 |
| NotoSans-Regular.ttf | ~500KB | TTF | 基础拉丁字符 | 轻量级测试 |

## 🚀 使用方法

### 本地开发

```bash
# 1. 下载字体
./gradlew :mpp-ui:downloadWasmFonts

# 2. 清理并构建
./gradlew :mpp-ui:clean :mpp-ui:compileKotlinWasmJs

# 3. 构建发行版
./gradlew :mpp-ui:wasmJsBrowserDistribution

# 4. 运行开发服务器
cd mpp-ui/build/dist/wasmJs/productionExecutable
python3 -m http.server 8080
```

### CI/CD 部署

GitHub Actions 会自动：
1. 下载字体（通过 `downloadWasmFonts` 任务）
2. 构建 WASM 应用
3. 部署到 GitHub Pages

## 🔧 故障排除

### 问题 1：字体显示为方框

**检查项：**
1. ✅ 确认字体格式是 TTF（不是 OTF）
2. ✅ 确认字体已下载：`ls -lh mpp-ui/src/commonMain/composeResources/font/`
3. ✅ 检查浏览器控制台错误
4. ✅ 重新构建：`./gradlew :mpp-ui:clean :mpp-ui:compileKotlinWasmJs`

### 问题 2：Gradle 任务失败

**检查项：**
1. ✅ 网络连接正常
2. ✅ GitHub 访问正常
3. ✅ 手动下载字体并放置到 `composeResources/font/`

### 问题 3：配置缓存错误

**解决方案：**
```bash
# 清除配置缓存
./gradlew --stop
rm -rf ~/.gradle/caches/configuration-cache
./gradlew :mpp-ui:downloadWasmFonts
```

## 📚 参考资料

- [Compose Multiplatform Resources](https://github.com/JetBrains/compose-multiplatform/tree/master/components/resources)
- [官方 WASM 字体示例](https://github.com/JetBrains/compose-multiplatform/blob/master/components/resources/demo/shared/src/webMain/kotlin/main.wasm.kt)
- [Noto CJK Fonts](https://github.com/notofonts/noto-cjk)
- [Skiko Engine](https://github.com/JetBrains/skiko)

## 📝 许可证

Noto 字体使用 **SIL Open Font License 1.1**，可自由用于商业和非商业项目。

## ✨ 下一步优化

可选的后续改进：

1. **字体子集化**：提取项目实际使用的字符，减小字体文件大小
2. **CDN 加载**：从 CDN 动态加载字体（需要研究 Skiko 支持）
3. **多字体回退**：配置字体回退链（Primary → CJK → Emoji）
4. **性能监控**：添加字体加载时间监控

---

**实现日期：** 2025-11-18  
**WASM 版本：** Kotlin 2.x + Compose Multiplatform  
**字体版本：** Noto Sans CJK SC Variable Font (Sans2.004)

