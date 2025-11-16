# Desktop Window Layout 设计

## 概述

实现了一个现代化的 Desktop 应用窗口布局，类似于 Chrome、VS Code 等桌面应用。

## 布局结构

```
┌─────────────────────────────────────────────────────┐
│ ●●●  [Local] [Coding✓] [Review] [Remote]  [⚙ 🔧] │ ← 自定义标题栏（可拖拽）
├─────────────────────────────────────────────────────┤
│ File Edit View Help                                 │ ← macOS 菜单栏（可选）
├─────────────────────────────────────────────────────┤
│                                                     │
│                                                     │
│              主内容区域                              │
│              (AutoDevApp)                           │
│                                                     │
│                                                     │
└─────────────────────────────────────────────────────┘
```

## 关键特性

### 1. 自定义标题栏
- **位置**: 窗口最顶部，高度 48dp
- **功能**: 可拖拽移动窗口
- **内容**:
  - 左侧：窗口控制按钮（关闭、最小化、最大化）
  - 右侧：Agent Type Tabs（横向排列）

### 2. 窗口控制按钮

#### macOS 风格（默认）
- 🔴 关闭（红色）
- 🟡 最小化（黄色）
- 🟢 最大化/还原（绿色）
- Hover 时显示图标
- 圆形按钮设计

#### Windows 风格（可选）
- 图标按钮
- Hover 时背景高亮
- 关闭按钮特殊红色

### 3. 圆角和边框
- **圆角**: 12dp 的 RoundedCornerShape
- **边框**: 1dp 的半透明边框
- **阴影**: 8dp 的阴影效果
- **透明窗口**: 支持圆角显示

### 4. Agent Type Tabs
- 横向排列在标题栏
- 每个 Tab 包含图标 + 文字
- 选中状态有视觉高亮
- 四种类型：
  - 💬 Local Chat
  - 💻 Coding Agent
  - 📝 Code Review
  - ☁️ Remote Agent

## 文件结构

```
mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/desktop/
├── DesktopWindowLayout.kt    # 主布局组件
├── WindowControls.kt          # 窗口控制按钮
├── AutoDevMenuBar.kt          # macOS 菜单栏
└── AutoDevTray.kt             # 系统托盘

mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/chat/
├── TopBarMenu.kt              # 跨平台 TopBar
├── TopBarMenuDesktop.kt       # Desktop Tab 实现
└── TopBarMenuMobile.kt        # Mobile Dropdown 实现
```

## 使用方式

### Main.kt 配置

```kotlin
Window(
    undecorated = true,   // 移除系统标题栏
    transparent = true    // 支持圆角和透明度
) {
    DesktopWindowLayout(
        title = "AutoDev",
        showWindowControls = true,
        onMinimize = { windowState.isMinimized = true },
        onMaximize = { /* 切换最大化状态 */ },
        onClose = { /* 关闭窗口 */ },
        titleBarContent = {
            // 可以在这里添加更多标题栏内容
        }
    ) {
        // 主应用内容
        AutoDevApp(...)
    }
}
```

## 平台适配

### Desktop (JVM)
- ✅ 自定义标题栏 + 窗口控制
- ✅ 横向 Agent Type Tabs
- ✅ 圆角窗口 + 阴影
- ✅ 可拖拽标题栏

### Mobile (Android)
- ✅ Dropdown Menu 风格
- ✅ 系统状态栏
- ✅ 标准 Android UI

### Web (JS/WASM)
- ✅ 标准浏览器 UI
- ✅ 适配小屏幕

## 视觉效果

### 窗口外观
- 12dp 圆角，现代感
- 8dp 阴影，立体感
- 1dp 边框，精致感

### 交互效果
- 标题栏可拖拽
- 按钮 Hover 高亮
- Tab 切换动画（Material3）

## 性能优化
- 使用 `remember` 缓存交互状态
- Surface 组件优化渲染
- Modifier 链式调用优化

## 未来扩展

### 支持的功能
- [x] macOS 风格窗口控制
- [x] Windows 风格窗口控制
- [x] Agent Type Tabs
- [ ] 双击标题栏最大化
- [ ] 窗口吸附边缘
- [ ] 自定义窗口大小调整
- [ ] 全屏模式优化

### 可配置项
- 窗口控制样式（macOS/Windows）
- 圆角大小
- 阴影强度
- 标题栏高度
- 边框颜色

## 技术细节

### WindowDraggableArea
- Compose Desktop 提供的特殊组件
- 必须在 `FrameWindowScope` 中使用
- 自动处理拖拽逻辑
- 不影响子组件的点击事件

### 透明窗口
- `undecorated = true` 移除系统装饰
- `transparent = true` 支持透明和圆角
- 需要自行实现所有窗口功能

### Material3 集成
- 使用 Material3 的颜色系统
- 自适应主题（Light/Dark）
- 遵循 Material Design 规范

## 相关文档
- [Kotlin Compose Desktop Window Management](https://kotlinlang.org/docs/multiplatform/compose-desktop-top-level-windows-management.html)
- [TopBarMenu 重构说明](./topbar-refactoring.md)

