# Navigation Architecture Design

## 概述

AutoDev 的跨平台导航系统，基于 Compose Multiplatform 构建，支持 Android、iOS/macOS 和 Desktop 平台。

## 设计原则

1. **统一但灵活**：核心逻辑统一，UI 适配各平台特性
2. **功能导向**：基于核心 AI Agent 功能组织导航
3. **平台原生感**：遵循各平台的设计规范

## 功能模块

### 主要功能（Main Features）
显示在底部导航/TabBar：

1. **Chat（对话）** - `AppScreen.CHAT`
   - 本地 AI 对话（Local Chat）
   - AgentType: `LOCAL_CHAT`

2. **Coding（编码）** - `AppScreen.CODING`
   - Coding Agent
   - AgentType: `CODING`

3. **Review（审查）** - `AppScreen.CODE_REVIEW`
   - 代码审查
   - AgentType: `CODE_REVIEW`

4. **Remote（远程）** - `AppScreen.REMOTE`
   - 远程服务器
   - AgentType: `REMOTE`

5. **Profile（我的）** - `AppScreen.PROFILE`
   - 个人中心
   - 设置和配置

### 次要功能（Secondary Features）
仅在 Drawer/Sheet 中显示：

1. **Projects（项目）** - `AppScreen.PROJECTS`
   - 项目管理
   - ProjectViewModel

2. **Tasks（任务）** - `AppScreen.TASKS`
   - 任务管理
   - TaskViewModel

3. **Sessions（会话）** - `AppScreen.SESSIONS`
   - 会话历史
   - SessionViewModel

## 平台实现

### Android 平台

#### 导航组件
- **TopBar**: 标题 + 汉堡菜单 + 操作按钮
- **BottomNavigation**: 5个主要入口（Chat、Coding、Review、Remote、Profile）
- **NavigationDrawer**: 
  - 用户信息
  - 主要功能（带描述）
  - 次要功能
  - 设置和工具
  - 退出登录

#### 使用方式
```kotlin
AndroidNavLayout(
    currentScreen = currentScreen,
    onScreenChange = { screen -> /* ... */ },
    sessionViewModel = sessionViewModel,
    onShowSettings = { /* ... */ },
    onShowTools = { /* ... */ },
    onShowDebug = { /* ... */ }
) { paddingValues ->
    // 内容区域
}
```

### iOS/macOS 平台

#### 导航组件
- **TopBar**: 标题 + 操作按钮 + 设置图标
- **NavigationBar** (模拟 TabBar): 5个主要入口
- **ModalBottomSheet**: 
  - 用户信息
  - 次要功能
  - 设置和工具
  - 退出登录

#### 使用方式
```kotlin
AppleNavLayout(
    currentScreen = currentScreen,
    onScreenChange = { screen -> /* ... */ },
    sessionViewModel = sessionViewModel,
    onShowSettings = { /* ... */ },
    onShowTools = { /* ... */ }
) { paddingValues ->
    // 内容区域
}
```

### Desktop 平台

#### 导航组件
- **NavigationRail**: 垂直侧边栏
- **设置按钮**: 在 NavigationRail 顶部

#### 使用方式
```kotlin
DesktopNavLayout(
    currentScreen = currentScreen,
    onScreenChange = { screen -> /* ... */ },
    sessionViewModel = sessionViewModel,
    onShowSettings = { /* ... */ }
) {
    // 内容区域（无 padding）
}
```

## 核心数据结构

### AppScreen 枚举
```kotlin
enum class AppScreen {
    LOGIN,          // 登录
    CHAT,           // 本地对话
    CODING,         // 编码 Agent
    CODE_REVIEW,    // 代码审查
    REMOTE,         // 远程服务器
    PROJECTS,       // 项目管理
    TASKS,          // 任务管理
    SESSIONS,       // 会话历史
    PROFILE,        // 个人中心
    HOME            // 兼容旧代码
}
```

### NavItem 配置
```kotlin
data class NavItem(
    val screen: AppScreen,
    val icon: ImageVector,
    val label: String,
    val description: String = "",
    val showInBottomNav: Boolean = true,    // Android BottomNavigation
    val showInTabBar: Boolean = true,        // iOS TabBar
    val showInDrawer: Boolean = true,        // Android Drawer
    val showInRail: Boolean = true           // Desktop NavigationRail
)
```

## Agent 类型映射

### Screen → AgentType
```kotlin
fun screenToAgentType(screen: AppScreen): AgentType? {
    return when (screen) {
        AppScreen.CHAT -> AgentType.LOCAL_CHAT
        AppScreen.CODING -> AgentType.CODING
        AppScreen.CODE_REVIEW -> AgentType.CODE_REVIEW
        AppScreen.REMOTE -> AgentType.REMOTE
        else -> null
    }
}
```

### AgentType → Screen
```kotlin
fun agentTypeToScreen(agentType: AgentType): AppScreen {
    return when (agentType) {
        AgentType.LOCAL_CHAT -> AppScreen.CHAT
        AgentType.CODING -> AppScreen.CODING
        AgentType.CODE_REVIEW -> AppScreen.CODE_REVIEW
        AgentType.REMOTE -> AppScreen.REMOTE
        else -> AppScreen.CHAT
    }
}
```

## 屏幕标题

```kotlin
fun getScreenTitle(screen: AppScreen): String {
    return when (screen) {
        AppScreen.LOGIN -> "登录"
        AppScreen.HOME -> "首页"
        AppScreen.CHAT -> "AI 对话"
        AppScreen.CODING -> "编码 Agent"
        AppScreen.CODE_REVIEW -> "代码审查"
        AppScreen.REMOTE -> "远程服务器"
        AppScreen.PROJECTS -> "项目管理"
        AppScreen.TASKS -> "任务管理"
        AppScreen.SESSIONS -> "会话历史"
        AppScreen.PROFILE -> "个人中心"
    }
}
```

## 平台差异总结

| 特性 | Android | iOS/macOS | Desktop |
|-----|---------|-----------|---------|
| 主导航 | BottomNavigation | NavigationBar | NavigationRail |
| 次导航 | Drawer | BottomSheet | - |
| 顶部栏 | TopBar + Menu | TopBar | - |
| 设置入口 | Drawer | TopBar Icon | Rail Header |
| 导航位置 | 底部 + 侧边 | 底部 + Sheet | 左侧 |
| 视觉风格 | Material 3 | Material 3* | Material 3 |

*注：iOS/macOS 使用 Material 3，未来可考虑原生 SwiftUI

## 迁移指南

### 从旧版本迁移

1. **移除重复的 AppScreen 定义**
   - 删除 `SessionApp.kt` 中的 enum 定义
   - 使用 `NavLayout.kt` 中的统一定义

2. **更新屏幕切换逻辑**
   ```kotlin
   // 旧版
   currentScreen = AppScreen.HOME
   
   // 新版
   currentScreen = AppScreen.CHAT
   ```

3. **更新 when 分支**
   ```kotlin
   // 旧版
   when (currentScreen) {
       AppScreen.HOME, AppScreen.CHAT -> { /* ... */ }
   }
   
   // 新版
   when (currentScreen) {
       AppScreen.CHAT, AppScreen.CODING, 
       AppScreen.CODE_REVIEW, AppScreen.REMOTE -> { /* ... */ }
   }
   ```

## 文件结构

```
mpp-ui/src/
├── commonMain/kotlin/cc/unitmesh/devins/ui/app/
│   └── NavLayout.kt                      # 核心导航组件
├── androidMain/kotlin/cc/unitmesh/devins/ui/compose/
│   └── AutoDevApp.android.kt             # Android 实现
├── appleMain/kotlin/cc/unitmesh/devins/ui/compose/
│   └── AutoDevApp.apple.kt               # iOS/macOS 实现
└── jvmMain/kotlin/cc/unitmesh/devins/ui/compose/
    └── AutoDevApp.jvm.kt                 # Desktop 实现
```

## 未来优化

1. **iOS 原生集成**
   - 考虑使用 SwiftUI NavigationStack
   - 原生 TabBar 实现

2. **动画过渡**
   - 屏幕切换动画
   - Drawer 滑动动画

3. **深度链接**
   - 支持外部链接跳转
   - 状态恢复

4. **多窗口支持**
   - iPad 分屏
   - macOS 多窗口

## 相关文档

- [Design System - Compose](./design-system-compose.md)
- [KMP Platform Analysis](../kmp-platform-analysis.md)
- [Architecture Overview](../architecture/)
