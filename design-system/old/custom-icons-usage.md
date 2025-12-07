# 自定义 SVG 图标使用指南

## 概述

我们已经成功将 `ai.svg` 和 `mcp.svg` 转换为可用的 Compose ImageVector 图标。

## 文件结构

```
mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/icons/
├── AutoDevComposeIcons.kt   # 统一的图标管理入口
└── CustomIcons.kt            # 自定义图标实现（从 SVG 转换）
```

## 可用的自定义图标

### 1. AI 图标 (星形/闪光)
- **来源**: `resources/ai.svg`
- **描述**: 代表 AI 功能的星形图标
- **颜色**: Indigo-500 (#6366F1)
- **尺寸**: 24x24 dp (原始 256x256 已缩放)

### 2. MCP 图标
- **来源**: `resources/mcp.svg`
- **描述**: Model Context Protocol 标识
- **颜色**: 黑色（可通过 tint 修改）
- **尺寸**: 24x24 dp

## 使用方法

### 基础使用

```kotlin
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

// 使用 AI 图标
Icon(
    imageVector = AutoDevComposeIcons.Custom.AI,
    contentDescription = "AI Feature",
    tint = MaterialTheme.colorScheme.primary
)

// 使用 MCP 图标
Icon(
    imageVector = AutoDevComposeIcons.Custom.MCP,
    contentDescription = "MCP Integration",
    tint = MaterialTheme.colorScheme.onSurface
)
```

### 在按钮中使用

```kotlin
IconButton(
    onClick = { /* AI 功能 */ }
) {
    Icon(
        imageVector = AutoDevComposeIcons.Custom.AI,
        contentDescription = "Open AI Assistant",
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}
```

### 在列表项中使用

```kotlin
ListItem(
    headlineContent = { Text("MCP Server") },
    leadingContent = {
        Icon(
            imageVector = AutoDevComposeIcons.Custom.MCP,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
    }
)
```

### 自定义大小和颜色

```kotlin
Icon(
    imageVector = AutoDevComposeIcons.Custom.AI,
    contentDescription = "AI",
    modifier = Modifier.size(32.dp),
    tint = Color(0xFF06B6D4) // Cyan-500
)
```

## 技术细节

### SVG 到 ImageVector 转换

这些图标是通过以下步骤从 SVG 转换而来：

1. **解析 SVG 路径数据**: 提取 `<path>` 元素的 `d` 属性
2. **转换为 Compose Path API**: 将 SVG 命令转换为 Compose 的 `path {}` DSL
3. **设置视口和尺寸**: 配置 `viewportWidth/Height` 和 `defaultWidth/Height`
4. **应用颜色和样式**: 设置 `fill`, `stroke` 等属性

### AI 图标简化说明

原始 AI SVG 包含渐变色，在转换过程中我们做了以下简化：
- 使用单色 (Indigo-500) 替代渐变
- 保留主要的星形轮廓
- 可以通过 `tint` 参数动态修改颜色

### 扩展新的 SVG 图标

如果需要添加新的 SVG 图标：

1. 将 SVG 文件放入 `mpp-ui/src/commonMain/resources/`
2. 在 `CustomIcons.kt` 中添加新的 `ImageVector`:

```kotlin
val YourIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "YourIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            // ... SVG 路径数据
        ) {
            // 路径命令
        }
    }.build()
}
```

3. 在 `AutoDevComposeIcons.Custom` 中暴露:

```kotlin
val YourIcon: ImageVector get() = CustomIcons.YourIcon
```

## 性能考虑

- ✅ **延迟初始化**: 使用 `by lazy` 确保图标只在首次使用时创建
- ✅ **内存效率**: ImageVector 是轻量级的不可变对象
- ✅ **可复用**: 同一个 ImageVector 实例可以在多处使用
- ✅ **无运行时解析**: 编译时已转换为代码，无需运行时解析 SVG

## 优势对比

| 方法 | 优点 | 缺点 |
|------|------|------|
| **ImageVector (当前)** | ✅ 类型安全<br>✅ 可动态着色<br>✅ 性能最优<br>✅ 跨平台支持 | ⚠️ 需要手动转换<br>⚠️ 复杂图形代码较长 |
| **Painter Resource** | ✅ 支持 SVG 原生特性<br>✅ 无需转换 | ❌ 跨平台支持有限<br>❌ 不能动态着色 |
| **Material Icons** | ✅ 开箱即用<br>✅ 标准化 | ❌ 受限于预定义图标 |

## 示例应用场景

```kotlin
// 1. 工具栏 AI 按钮
TopAppBar(
    title = { Text("AutoDev") },
    actions = {
        IconButton(onClick = { openAI() }) {
            Icon(
                imageVector = AutoDevComposeIcons.Custom.AI,
                contentDescription = "AI Assistant"
            )
        }
    }
)

// 2. MCP 服务器状态指示
Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    Icon(
        imageVector = AutoDevComposeIcons.Custom.MCP,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = if (connected) Color.Green else Color.Gray
    )
    Text("MCP: ${if (connected) "Connected" else "Disconnected"}")
}

// 3. 功能卡片
Card {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = AutoDevComposeIcons.Custom.AI,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text("AI Powered", style = MaterialTheme.typography.titleMedium)
    }
}
```

## 常见问题

**Q: 为什么不直接使用 SVG 文件？**  
A: Compose Multiplatform 的 SVG 支持在不同平台有差异，使用 ImageVector 可以确保跨平台一致性和最佳性能。

**Q: 可以修改图标颜色吗？**  
A: 可以！使用 `tint` 参数可以动态修改图标颜色。

**Q: 图标质量会损失吗？**  
A: 不会。ImageVector 是矢量格式，可以无损缩放到任意大小。

**Q: 渐变色怎么办？**  
A: ImageVector 支持渐变，可以在 `path {}` 中使用 `Brush.linearGradient()` 或 `Brush.radialGradient()`，但当前实现为了简化使用了单色。



