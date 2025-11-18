# WASM 全局字体使用指南

## 简介

在 WASM 平台上，为了支持中文、日文、韩文、Emoji 等 UTF-8 字符，我们集成了 Noto Sans CJK SC 字体。

## 字体资源

字体自动下载并集成在项目中：

- **文件：** `NotoSansSC-Regular.otf`
- **大小：** ~15MB
- **支持：** 中文（简繁）、日文、韩文、Latin、Emoji
- **位置：** `composeResources/font/` （不提交到 Git）

## 使用方法

### 方式 1：在 Composable 函数中使用（推荐）

```kotlin
import autodev_intellij.mpp_ui.generated.resources.Res
import autodev_intellij.mpp_ui.generated.resources.NotoSansSC_Regular
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import cc.unitmesh.devins.ui.platform.Platform

@Composable
fun MyText() {
    val fontFamily = if (Platform.isWasm) {
        FontFamily(Font(Res.font.NotoSansSC_Regular, FontWeight.Normal))
    } else {
        FontFamily.Default
    }
    
    Text(
        text = "你好世界 Hello World 🎉",
        fontFamily = fontFamily
    )
}
```

### 方式 2：创建平台特定的字体 Helper

#### 在 `wasmJsMain` 中：

```kotlin
// src/wasmJsMain/kotlin/YourPackage/WasmFonts.kt
package your.package

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import autodev_intellij.mpp_ui.generated.resources.Res
import autodev_intellij.mpp_ui.generated.resources.NotoSansSC_Regular
import org.jetbrains.compose.resources.Font

@Composable
fun getUtf8FontFamily(): FontFamily {
    return FontFamily(Font(Res.font.NotoSansSC_Regular, FontWeight.Normal))
}
```

#### 在其他平台：

```kotlin
// src/jvmMain/kotlin/YourPackage/WasmFonts.kt
@Composable
fun getUtf8FontFamily(): FontFamily {
    return FontFamily.Default
}
```

### 方式 3：在 Material Theme 中全局配置

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import autodev_intellij.mpp_ui.generated.resources.Res
import autodev_intellij.mpp_ui.generated.resources.NotoSansSC_Regular
import org.jetbrains.compose.resources.Font
import cc.unitmesh.devins.ui.platform.Platform

@Composable
fun MyAppTheme(content: @Composable () -> Unit) {
    val defaultFontFamily = if (Platform.isWasm) {
        FontFamily(Font(Res.font.NotoSansSC_Regular))
    } else {
        FontFamily.Default
    }
    
    val typography = Typography(
        bodyLarge = TextStyle(fontFamily = defaultFontFamily),
        bodyMedium = TextStyle(fontFamily = defaultFontFamily),
        bodySmall = TextStyle(fontFamily = defaultFontFamily),
        // ... 其他文本样式
    )
    
    MaterialTheme(
        typography = typography,
        content = content
    )
}
```

## Platform 工具类

项目提供了 `Platform` 工具类用于平台检测：

```kotlin
import cc.unitmesh.devins.ui.platform.Platform

// 检查平台
if (Platform.isWasm) {
    // WASM 特定代码
}

if (Platform.isJvm) {
    // JVM (Desktop) 特定代码
}

if (Platform.isAndroid) {
    // Android 特定代码
}

// 获取平台名称
println("Running on: ${Platform.name}")
```

### Platform 属性：

- `Platform.isWasm: Boolean` - WASM JS 平台
- `Platform.isJvm: Boolean` - JVM (Desktop) 平台
- `Platform.isAndroid: Boolean` - Android 平台
- `Platform.isIos: Boolean` - iOS 平台
- `Platform.isJs: Boolean` - JS (Node.js) 平台
- `Platform.name: String` - 平台名称字符串

## 示例代码

### 示例 1：聊天消息显示

```kotlin
@Composable
fun ChatMessage(message: String) {
    val fontFamily = remember {
        if (Platform.isWasm) {
            FontFamily(Font(Res.font.NotoSansSC_Regular))
        } else {
            FontFamily.Default
        }
    }
    
    Text(
        text = message, // 可以包含中文、emoji等
        fontFamily = fontFamily,
        style = MaterialTheme.typography.bodyMedium
    )
}
```

### 示例 2：代码块（带中文注释）

```kotlin
@Composable
fun CodeBlock(code: String) {
    val monospaceFontFamily = remember {
        if (Platform.isWasm) {
            // WASM使用Noto Sans以支持中文注释
            FontFamily(Font(Res.font.NotoSansSC_Regular))
        } else {
            FontFamily.Monospace
        }
    }
    
    Text(
        text = code, // 代码中可能包含中文注释
        fontFamily = monospaceFontFamily,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = monospaceFontFamily
        )
    )
}
```

### 示例 3：自适应字体选择

```kotlin
@Composable
fun rememberPlatformFontFamily(): FontFamily {
    return remember {
        if (Platform.isWasm) {
            FontFamily(Font(Res.font.NotoSansSC_Regular))
        } else {
            FontFamily.Default
        }
    }
}

@Composable
fun MyComponent() {
    val fontFamily = rememberPlatformFontFamily()
    
    Column {
        Text("Hello World", fontFamily = fontFamily)
        Text("你好世界", fontFamily = fontFamily)
        Text("🎉✨🚀", fontFamily = fontFamily)
    }
}
```

## 注意事项

1. **只在 WASM 平台使用自定义字体**
   - 其他平台使用系统默认字体即可
   - 使用 `Platform.isWasm` 进行条件判断

2. **资源访问限制**
   - Compose Resources 生成的字体只能在平台特定代码或 Composable 函数中访问
   - 不能在 commonMain 的非 Composable 函数中直接访问

3. **性能考虑**
   - 使用 `remember` 缓存字体实例，避免重复创建
   - 字体在 Main.kt 中已经预加载，无需再次加载

4. **字体加载状态**
   - Main.kt 中已实现字体预加载
   - 字体加载期间会显示 Loading 指示器
   - 加载完成后应用才会显示

## 构建和部署

### 开发环境

```bash
# 字体会自动下载（如果不存在）
./gradlew :mpp-ui:compileKotlinWasmJs

# 或手动下载
./gradlew :mpp-ui:downloadWasmFonts
```

### CI/CD 环境

字体下载任务会在编译前自动执行，无需额外配置：

```yaml
# GitHub Actions示例
- name: Build WASM
  run: ./gradlew :mpp-ui:wasmJsBrowserDistribution
  # 字体会自动下载
```

### 切换字体

如果需要使用更轻量的字体（不支持 CJK）：

```bash
./gradlew :mpp-ui:downloadWasmFonts -PuseCJKFont=false
```

这会下载 Noto Sans (500KB) 而不是 Noto Sans CJK (15MB)。

## 疑难解答

### 问题：字体未生成

**解决方案：**
```bash
./gradlew :mpp-ui:clean
./gradlew :mpp-ui:downloadWasmFonts
./gradlew :mpp-ui:compileKotlinWasmJs
```

### 问题：中文/Emoji 仍然无法显示

**可能原因：**
1. 字体未正确加载
2. 未使用正确的 FontFamily

**解决方案：**
检查 `Main.kt` 中的字体预加载逻辑，确保使用了 `Platform.isWasm` 条件。

### 问题：编译错误 "Unresolved reference"

**原因：** 在 commonMain 中直接访问字体资源

**解决方案：** 将字体相关代码移到平台特定目录（如 wasmJsMain）或在 Composable 函数中使用。

## 参考资料

- [Platform 工具类](../mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/platform/Platform.kt)
- [Main.kt 字体预加载](../mpp-ui/src/wasmJsMain/kotlin/Main.kt)
- [Compose Resources 文档](https://github.com/JetBrains/compose-multiplatform/tree/master/components/resources)
- [Noto Fonts 项目](https://github.com/googlefonts/noto-cjk)

