# Android UI 快速开始

## 🎯 快速总结

✅ **已完成的工作**

1. ✅ 创建了 Android 专属的 UI 实现（`AutoDevApp.android.kt`）
2. ✅ 使用 Kotlin Multiplatform 的 expect/actual 模式
3. ✅ 实现了完整的 Android Material 3 设计
4. ✅ 增强了 `AndroidNavLayout` 支持 Drawer 设置和工具
5. ✅ 添加了 HOME/CHAT 屏幕到 AppScreen 枚举
6. ✅ 创建了所有必要的平台 actual 实现（JVM/JS/WASM）

## 🏗️ 架构图

```
┌─────────────────────────────────────────────────────┐
│              PlatformAutoDevApp (expect)            │
│                  (commonMain)                        │
└────────────────────┬────────────────────────────────┘
                     │
        ┌────────────┴────────────┬──────────────┐
        │                         │              │
        ▼                         ▼              ▼
┌───────────────┐        ┌──────────────┐  ┌──────────┐
│  Android      │        │   Desktop    │  │   WASM   │
│  actual       │        │   actual     │  │  actual  │
│  实现         │        │   实现       │  │   实现   │
│               │        │              │  │          │
│ BottomNav +   │        │ SessionBar + │  │ Vertical │
│ Drawer        │        │ TopBar       │  │ Menu     │
└───────────────┘        └──────────────┘  └──────────┘
```

## 📱 Android UI 结构

```
AndroidAutoDevContent
├── SessionViewModel (会话管理)
├── ChatHistoryManager (聊天历史)
├── WorkspaceManager (工作空间)
├── ConfigManager (配置管理)
│
├── AndroidNavLayout
│   ├── Drawer (滑出菜单)
│   │   ├── 用户信息
│   │   ├── 导航项 (Home/Chat/Projects/Tasks/Profile)
│   │   ├── 设置 (Model/Tool/Debug)
│   │   └── 退出登录
│   │
│   ├── TopBar (标题栏)
│   │   ├── 汉堡菜单按钮
│   │   ├── 屏幕标题
│   │   └── 操作按钮 (根据屏幕变化)
│   │
│   ├── Main Content (主内容区)
│   │   ├── HOME → HomeScreen
│   │   ├── CHAT → ChatScreen
│   │   ├── TASKS → TasksPlaceholderScreen
│   │   └── PROFILE → ProfileScreen
│   │
│   └── BottomNavigation (底部导航)
│       ├── 🏠 首页
│       ├── 💬 对话
│       ├── 📋 任务
│       └── 👤 我的
│
└── Dialogs (对话框)
    ├── ModelConfigDialog (模型配置)
    ├── ToolConfigDialog (工具配置)
    ├── DebugDialog (调试信息)
    └── ErrorDialog (错误提示)
```

## 🚀 如何使用

### 方式 1: 使用 PlatformAutoDevApp（推荐）

```kotlin
import cc.unitmesh.devins.ui.compose.PlatformAutoDevApp

@Composable
fun App() {
    // 自动使用平台特定的实现
    // Android → AndroidAutoDevContent
    // Desktop → 原有 AutoDevApp
    // WASM → 原有 AutoDevApp
    PlatformAutoDevApp(
        triggerFileChooser = false,
        onFileChooserHandled = {},
        initialMode = "auto"
    )
}
```

### 方式 2: 直接调用原有 AutoDevApp

```kotlin
import cc.unitmesh.devins.ui.compose.AutoDevApp

@Composable
fun App() {
    // 原有实现（所有平台共享）
    AutoDevApp(
        triggerFileChooser = false,
        onFileChooserHandled = {},
        initialMode = "auto"
    )
}
```

## 🎨 Android 专属特性

### 1. BottomNavigation

- 4 个主要入口：Home/Chat/Tasks/Profile
- 图标 + 文字标签
- 当前屏幕高亮（Primary Color）
- Material You 自适应颜色

### 2. Drawer 菜单

- 从左侧滑出
- 显示用户信息（头像 + 名称）
- 完整的导航项列表
- 设置和工具快捷入口
- 退出登录（红色警告色）
- 版本信息

### 3. TopBar Actions

根据当前屏幕动态显示操作按钮：

```kotlin
actions = {
    when (currentScreen) {
        AppScreen.CHAT -> {
            // 切换 Agent 模式按钮
            IconButton(onClick = { /* toggle */ }) {
                Icon(Icons.Default.SmartToy, ...)
            }
        }
        AppScreen.HOME -> {
            // 搜索按钮
            IconButton(onClick = { /* search */ }) {
                Icon(Icons.Default.Search, ...)
            }
        }
        else -> {}
    }
}
```

### 4. HomeScreen 特性

- **欢迎卡片**: Primary Container 突出显示
- **快速操作**: AI 对话 + 项目管理（2列布局）
- **最近会话**: 显示最近 5 条会话记录
- **点击交互**: 所有卡片支持点击跳转

### 5. ChatScreen 特性

- **Agent 模式**: 全屏显示，支持 TreeView
- **Chat 模式**: 消息列表 + 输入框
- **空状态**: 居中显示输入框（优雅的初始状态）
- **键盘适配**: 使用 `imePadding()` 自动适配软键盘

### 6. ProfileScreen 特性

- **配置卡片**: 模型配置 + 工具配置
- **点击跳转**: 点击卡片打开对应的 Dialog
- **当前状态显示**: 显示当前模型（Provider / Model）
- **关于信息**: 版本号、应用描述

## 📋 文件清单

| 文件 | 路径 | 描述 |
|------|------|------|
| expect 声明 | `commonMain/.../AutoDevAppPlatform.kt` | 平台接口定义 |
| Android 实现 | `androidMain/.../AutoDevApp.android.kt` | Android 专属 UI ✨ |
| JVM 实现 | `jvmMain/.../AutoDevApp.jvm.kt` | Desktop 委托 |
| JS 实现 | `jsMain/.../AutoDevApp.js.kt` | JS 委托 |
| WASM 实现 | `wasmJsMain/.../AutoDevApp.wasm.kt` | WASM 委托 |
| NavLayout 增强 | `commonMain/.../NavLayout.kt` | 导航布局组件 |
| AppScreen 枚举 | `commonMain/.../SessionApp.kt` | 屏幕类型定义 |

## 🧪 测试步骤

### 1. 构建 Android 应用

```bash
cd /Volumes/source/ai/autocrud

# 清理构建
./gradlew clean

# 编译 Android 应用
./gradlew :mpp-ui:assembleDebug
```

### 2. 安装到设备/模拟器

```bash
# 安装 Debug APK
./gradlew :mpp-ui:installDebug

# 或者直接运行
./gradlew :mpp-ui:installDebug && adb shell am start -n cc.unitmesh.devins.ui/.MainActivity
```

### 3. 测试清单

#### UI 导航测试
- [ ] 启动应用，显示 HOME 屏幕
- [ ] 点击 BottomNavigation 的"对话"，切换到 CHAT 屏幕
- [ ] 点击 BottomNavigation 的"任务"，切换到 TASKS 屏幕
- [ ] 点击 BottomNavigation 的"我的"，切换到 PROFILE 屏幕
- [ ] 点击 TopBar 的汉堡菜单，打开 Drawer
- [ ] 在 Drawer 中点击各个导航项，切换屏幕

#### Chat 功能测试
- [ ] 进入 CHAT 屏幕，显示居中输入框
- [ ] 输入消息，点击发送
- [ ] 消息列表正确显示用户消息
- [ ] AI 回复正常显示
- [ ] 滚动消息列表流畅

#### 配置测试
- [ ] 打开 Drawer → 点击"模型设置"
- [ ] ModelConfigDialog 正确显示
- [ ] 输入配置信息（API Key、Provider、Model）
- [ ] 保存配置成功
- [ ] 返回 CHAT 屏幕，发送消息测试 LLM 是否生效

#### Drawer 测试
- [ ] 显示用户信息（本地用户 / AutoDev）
- [ ] 所有导航项正确显示
- [ ] 设置和工具选项正确显示
- [ ] 点击"模型设置"打开 ModelConfigDialog
- [ ] 点击"工具配置"打开 ToolConfigDialog
- [ ] 点击"调试信息"打开 DebugDialog（如果有调试数据）
- [ ] 点击"退出登录"（测试登出逻辑）

#### HomeScreen 测试
- [ ] 欢迎卡片正确显示
- [ ] 快速操作卡片可点击
- [ ] 点击"AI 对话"跳转到 CHAT 屏幕
- [ ] 点击"项目管理"跳转到 PROJECTS 屏幕
- [ ] 最近会话列表显示（如果有历史）

#### ProfileScreen 测试
- [ ] 配置卡片正确显示
- [ ] 显示当前模型配置
- [ ] 点击"模型配置"打开 ModelConfigDialog
- [ ] 点击"工具配置"打开 ToolConfigDialog
- [ ] 关于信息正确显示

## 🔧 调试技巧

### 查看日志

```bash
# 实时查看应用日志
adb logcat | grep "AutoDev\|ChatHistory\|ConfigManager"

# 清除日志后重新启动
adb logcat -c && adb shell am start -n cc.unitmesh.devins.ui/.MainActivity && adb logcat | grep "AutoDev"
```

### 常见问题

#### 问题 1: 应用闪退
**检查**: 
```bash
adb logcat *:E
```
**可能原因**: 配置初始化失败、权限不足

#### 问题 2: Drawer 无法打开
**检查**: TopBar 的汉堡菜单是否显示  
**解决**: 确认 `AndroidNavLayout` 正确传递了 `drawerState`

#### 问题 3: BottomNavigation 不响应
**检查**: `onScreenChange` 回调是否正确  
**解决**: 确认 `currentScreen` 状态正确更新

#### 问题 4: Chat 输入框不显示
**检查**: `DevInEditorInput` 是否正确初始化  
**解决**: 确认 `callbacks` 和 `completionManager` 已传递

## 📚 扩展阅读

- [重构设计方案](./refactoring-autodev-app-design.md)
- [Android UI 实现文档](./android-ui-implementation.md)
- [Material 3 Guidelines](https://m3.material.io/components)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

## 🎉 下一步

### 立即可做
1. ✅ 测试 Android 应用
2. ✅ 验证所有屏幕切换
3. ✅ 测试配置管理
4. ✅ 测试 Chat 功能

### 后续优化
1. 添加 Agent 模式切换动画
2. 优化 TreeView 在 Android 上的显示
3. 添加侧滑手势支持
4. 优化键盘弹出时的布局
5. 添加更多动画和过渡效果

### 长期规划
1. 实现完整的 TASKS 屏幕
2. 添加搜索功能
3. 支持多语言（i18n）
4. 性能优化（LazyColumn、图片缓存）
5. 无障碍支持

---

**快速开始版本**: v1.0  
**创建时间**: 2025-11-13  
**作者**: AI Assistant  
**状态**: ✅ 可以开始测试

