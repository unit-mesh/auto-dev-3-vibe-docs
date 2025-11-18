# WASM UTF-8 字体支持设置指南

## 背景

Kotlin WASM JS 不原生支持 UTF-8 字符（特别是 emoji 和中文字符）。由于使用的是 Skiko 引擎，我们需要将字体文件打包到 Compose Resources 中，并使用 `preloadFont` API 预加载字体。

⚠️ **重要：WASM 只支持 TTF 格式，不支持 OTF 格式！**

## 实现参考

官方实现示例：https://github.com/JetBrains/compose-multiplatform/blob/master/components/resources/demo/shared/src/webMain/kotlin/main.wasm.kt

## 快速开始

### 自动下载（推荐）

项目已配置 Gradle 任务自动下载 UTF-8 字体（**不会提交到 Git**）：

```bash
# 下载完整的中日韩 UTF-8 字体 (默认，~17MB TTF)
./gradlew :mpp-ui:downloadWasmFonts

# 或下载轻量级字体 (~500KB)
./gradlew :mpp-ui:downloadWasmFonts -PuseCJKFont=false
```

字体会自动下载到 `mpp-ui/src/commonMain/composeResources/font/` 目录。

### GitHub Actions

GitHub Actions 会在部署前自动下载字体：

```yaml
- name: Download WASM Fonts for UTF-8 Support
  run: |
    echo "📦 Downloading Noto Sans SC TTF for full UTF-8 support..."
    ./gradlew :mpp-ui:downloadWasmFonts --no-daemon --info
```

## 手动设置步骤（可选）

### 1. 下载 Noto Sans SC 字体（TTF 格式）

⚠️ **必须使用 TTF 格式，OTF 格式不兼容 WASM！**

**选项 A: Noto Sans SC Variable Font (推荐，支持完整 UTF-8)**
```bash
# 下载 TTF 格式的 Noto Sans SC Variable Font (~17MB)
curl -L -o NotoSansSC-Regular.ttf \
  "https://github.com/notofonts/noto-cjk/raw/main/Sans/Variable/TTF/Subset/NotoSansSC-VF.ttf"
```

**选项 B: Noto Color Emoji (仅 Emoji)**
```bash
# 下载 Noto Color Emoji TTF (~10MB)
curl -L -o NotoColorEmoji.ttf \
  "https://github.com/googlefonts/noto-emoji/raw/main/fonts/NotoColorEmoji.ttf"
```

**选项 C: Noto Sans (轻量级，基础 UTF-8)**
```bash
# 下载 Noto Sans TTF (~500KB)
curl -L -o NotoSans-Regular.ttf \
  "https://github.com/googlefonts/noto-fonts/raw/main/hinted/ttf/NotoSans/NotoSans-Regular.ttf"
```

### 2. 添加字体文件到项目

将下载的字体文件放到 Compose Resources 目录：

```bash
# 进入项目目录
cd /Volumes/source/ai/autocrud/mpp-ui

# 将字体文件复制到 Compose Resources 目录（注意：是 composeResources，不是 resources）
cp ~/Downloads/NotoSansSC-Regular.ttf src/commonMain/composeResources/font/

# 确认文件已添加
ls -lh src/commonMain/composeResources/font/
```

### 3. 验证实现

代码已更新 (`src/wasmJsMain/kotlin/Main.kt`)，实现了：

- ✅ 字体预加载机制
- ✅ 加载期间显示进度指示器
- ✅ 字体就绪后启动应用
- ✅ 使用 Compose Resources API 管理字体

### 4. 构建和测试

```bash
# 清理旧构建
./gradlew :mpp-ui:clean

# 构建 WASM JS 目标
./gradlew :mpp-ui:wasmJsBrowserDistribution

# 或者开发模式（更快）
./gradlew :mpp-ui:wasmJsBrowserDevelopmentWebpack --continuous

# 运行开发服务器（如果配置了）
./gradlew :mpp-ui:wasmJsBrowserRun
```

### 5. 验证 UTF-8 支持

在应用中测试以下内容是否正确显示：

- Emoji: 😀 🎉 ✅ ❌ 🚀
- 中文: 你好，世界！
- 日文: こんにちは
- 韩文: 안녕하세요
- 特殊符号: ©️ ®️ ™️ ⚡ ⭐

## 技术细节

### 字体预加载流程

1. **配置资源路径映射**
   ```kotlin
   configureWebResources {
       resourcePathMapping { path -> "./$path" }
   }
   ```

2. **预加载字体**
   ```kotlin
   val emojiFont = preloadFont(Res.font.NotoColorEmoji).value
   ```

3. **注册字体家族**
   ```kotlin
   fontFamilyResolver.preload(FontFamily(listOf(emojiFont)))
   ```

4. **等待字体就绪**
   - 显示加载指示器
   - 字体加载完成后渲染应用

### Webpack 配置

如果需要自定义 webpack 配置，可以在 `webpack.config.d/` 目录下添加配置文件。

### 文件大小考虑

- **NotoSansSC-Regular.ttf (Variable Font)**: ~17MB (包含中日韩字符 + Emoji)
- **NotoColorEmoji.ttf**: ~10MB (仅包含所有 emoji)
- **NotoSans-Regular.ttf**: ~500KB (基础拉丁、西里尔、希腊字符)

**优化建议：**
- ✅ 字体文件通过 Gradle 自动下载，**不提交到 Git**
- ✅ 只在 WASM 构建中包含字体
- 考虑使用字体子集化工具进一步减小文件大小
- 使用 lazy loading 延迟加载字体（已实现）

## 故障排除

### 问题 1: 字体文件未找到

**错误信息：**
```
Resource not found: fonts/NotoColorEmoji.ttf
```

**解决方案：**
1. 确认字体文件在正确的目录：`src/commonMain/resources/fonts/`
2. 重新构建项目：`./gradlew :mpp-ui:clean :mpp-ui:wasmJsBrowserDistribution`
3. 检查 Compose Resources 生成的文件：`build/generated/compose/resourceGenerator/`

### 问题 2: Emoji 或中文仍然显示为方框

**可能原因：**
- 字体未正确加载
- 使用了 OTF 格式（WASM 不支持）
- 浏览器不支持该字体格式

**解决方案：**
1. 检查浏览器控制台是否有错误
2. **确保使用 TTF 格式，不是 OTF 格式**
3. 运行 `./gradlew :mpp-ui:downloadWasmFonts` 确保字体已下载
4. 确认浏览器支持 Color Emoji (Chrome 支持，Firefox 部分支持)

### 问题 3: 构建失败

**错误信息：**
```
Font resource compilation failed
```

**解决方案：**
1. 确认字体文件完整且未损坏
2. 检查文件权限
3. 尝试使用较小的字体文件进行测试

## 参考资料

- [Compose Multiplatform Resources](https://github.com/JetBrains/compose-multiplatform/tree/master/components/resources)
- [Noto Emoji Project](https://github.com/googlefonts/noto-emoji)
- [Noto CJK Fonts](https://github.com/googlefonts/noto-cjk)
- [Skiko Engine Documentation](https://github.com/JetBrains/skiko)

## 许可证

Noto 字体使用 SIL Open Font License 1.1，可以自由使用于商业和非商业项目。

