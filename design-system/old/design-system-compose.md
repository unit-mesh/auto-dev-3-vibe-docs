# AutoDev 设计系统 - Compose 色彩实现

## 概述

本文档说明如何在 Kotlin Compose Multiplatform (Desktop/Android) 中使用 AutoDev 设计系统的色彩。

## 核心文件

### 1. AutoDevColors.kt

完整的色彩定义文件，包含所有色阶和语义化颜色：

**位置**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/theme/AutoDevColors.kt`

```kotlin
object AutoDevColors {
    // 色阶
    object Indigo { ... }
    object Cyan { ... }
    object Neutral { ... }
    object Green { ... }
    object Amber { ... }
    object Red { ... }
    object Blue { ... }
    
    // 代码高亮专用颜色
    object Syntax {
        object Dark { ... }
        object Light { ... }
    }
    
    // Diff 显示专用颜色
    object Diff {
        object Dark { ... }
        object Light { ... }
    }
}
```

### 2. AutoDevTheme.kt

Material 3 主题适配，将 AutoDevColors 映射到 Material 3 的 ColorScheme：

**位置**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/theme/AutoDevTheme.kt`

```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = AutoDevColors.Indigo.c300,
    secondary = AutoDevColors.Cyan.c400,
    tertiary = AutoDevColors.Green.c300,
    background = AutoDevColors.Neutral.c900,
    surface = AutoDevColors.Neutral.c800,
    // ...
)

private val LightColorScheme = lightColorScheme(
    primary = AutoDevColors.Indigo.c600,
    secondary = AutoDevColors.Cyan.c500,
    tertiary = AutoDevColors.Green.c600,
    background = AutoDevColors.Neutral.c50,
    surface = Color.White,
    // ...
)
```

## 使用指南

### 1. 在 Compose UI 中使用主题颜色

```kotlin
import androidx.compose.material3.MaterialTheme

@Composable
fun MyComponent() {
    // 使用主题颜色（推荐）
    Text(
        text = "主要文本",
        color = MaterialTheme.colorScheme.primary
    )
    
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
    )
}
```

### 2. 直接使用 AutoDevColors

对于特殊场景（如语法高亮、Diff 显示），可以直接使用 AutoDevColors：

```kotlin
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

@Composable
fun SyntaxHighlighter() {
    val colors = AutoDevColors.Syntax.Dark
    
    Text(
        text = "@agent",
        color = colors.agent  // 青色的 Agent 标识
    )
    
    Text(
        text = "/command",
        color = colors.command  // 绿色的命令
    )
    
    Text(
        text = "$variable",
        color = colors.variable  // 洋红色的变量
    )
}
```

### 3. Diff 显示

```kotlin
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

@Composable
fun DiffView() {
    // 使用 Diff 专用颜色
    val addedBg = AutoDevColors.Diff.Dark.addedBg
    val deletedBg = AutoDevColors.Diff.Dark.deletedBg
    
    Row(
        modifier = Modifier.background(addedBg)
    ) {
        Text("+ 新增的行", color = AutoDevColors.Green.c300)
    }
    
    Row(
        modifier = Modifier.background(deletedBg)
    ) {
        Text("- 删除的行", color = AutoDevColors.Red.c300)
    }
}
```

## 已更新的文件

以下文件已更新为使用新的设计系统颜色：

- ✅ `AutoDevTheme.kt` - Material 3 主题配色
- ✅ `DiffSketchRenderer.kt` - Diff 显示颜色
- ✅ `DevInSyntaxHighlighter.kt` - 语法高亮颜色

## 主题切换

AutoDevTheme 支持三种模式：

```kotlin
enum class ThemeMode {
    LIGHT,   // 亮色模式
    DARK,    // 暗色模式
    SYSTEM   // 跟随系统
}

@Composable
fun App() {
    AutoDevTheme(
        themeMode = ThemeManager.currentTheme  // 动态主题切换
    ) {
        // 你的 UI 内容
    }
}
```

## Material 3 颜色映射

### 暗色模式

| Material 3 角色 | AutoDev 颜色 | 用途 |
|----------------|-------------|------|
| `primary` | Indigo c300 | 主要操作按钮、强调元素 |
| `secondary` | Cyan c400 | 辅助操作、AI 相关功能 |
| `tertiary` | Green c300 | 成功状态、完成标记 |
| `background` | Neutral c900 | 应用背景 |
| `surface` | Neutral c800 | 卡片、对话框背景 |
| `error` | Red c300 | 错误提示 |

### 亮色模式

| Material 3 角色 | AutoDev 颜色 | 用途 |
|----------------|-------------|------|
| `primary` | Indigo c600 | 主要操作按钮、强调元素 |
| `secondary` | Cyan c500 | 辅助操作、AI 相关功能 |
| `tertiary` | Green c600 | 成功状态、完成标记 |
| `background` | Neutral c50 | 应用背景 |
| `surface` | White | 卡片、对话框背景 |
| `error` | Red c600 | 错误提示 |

## 最佳实践

### ✅ 推荐做法

1. **优先使用 MaterialTheme.colorScheme**
   ```kotlin
   Text(color = MaterialTheme.colorScheme.primary)
   ```

2. **特殊场景使用 AutoDevColors**
   ```kotlin
   // 代码高亮
   Text(color = AutoDevColors.Syntax.Dark.keyword)
   
   // Diff 显示
   Box(modifier = Modifier.background(AutoDevColors.Diff.Dark.addedBg))
   ```

3. **避免硬编码颜色**
   ```kotlin
   // ❌ 不要这样
   Text(color = Color(0xFF6750A4))
   
   // ✅ 应该这样
   Text(color = MaterialTheme.colorScheme.primary)
   ```

### 🎨 语义化使用

```kotlin
// 状态颜色
val successColor = AutoDevColors.Green.c300
val warningColor = AutoDevColors.Amber.c300
val errorColor = AutoDevColors.Red.c300
val infoColor = AutoDevColors.Blue.c300

// 文本颜色
val primaryText = MaterialTheme.colorScheme.onSurface
val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
```

## 主题响应式

如果需要根据主题模式动态选择颜色：

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun DynamicColorComponent() {
    val isDark = isSystemInDarkTheme()
    
    val syntaxColors = if (isDark) {
        AutoDevColors.Syntax.Dark
    } else {
        AutoDevColors.Syntax.Light
    }
    
    Text(
        text = "fun main()",
        color = syntaxColors.keyword
    )
}
```

## TODO

以下功能计划在未来版本中实现：

- [ ] 自动根据主题模式切换 Syntax 颜色
- [ ] 自动根据主题模式切换 Diff 颜色
- [ ] 提供 CompositionLocal 以便更方便地访问当前主题
- [ ] 添加颜色过渡动画

## 参考

- [TypeScript 色彩系统](design-system-color.md)
- [Material 3 Color System](https://m3.material.io/styles/color/overview)
- [Compose Material 3 文档](https://developer.android.com/jetpack/compose/designsystems/material3)

---

**版本**: 1.0.0  
**最后更新**: 2025-11-07  
**维护者**: AutoDev Team



