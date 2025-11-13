# Android UI 实现文档

## 📱 概述

基于设计文档实现了 Android 专属的 UI，使用 Kotlin Multiplatform 的 expect/actual 模式，实现了平台特定的用户体验。

## 🏗️ 架构设计

### 文件结构

```
mpp-ui/src/
├── commonMain/kotlin/cc/unitmesh/devins/ui/
│   ├── compose/
│   │   ├── AutoDevApp.kt              # 原有实现（Desktop/WASM）
│   │   └── AutoDevAppPlatform.kt      # expect 声明
│   └── app/
│       ├── NavLayout.kt                # 增强的导航布局（支持 Drawer）
│       └── SessionApp.kt               # AppScreen 枚举定义
│
├── androidMain/kotlin/cc/unitmesh/devins/ui/
│   └── compose/
│       └── AutoDevApp.android.kt       # Android actual 实现 ✨
│
├── jvmMain/kotlin/cc/unitmesh/devins/ui/
│   └── compose/
│       └── AutoDevApp.jvm.kt           # JVM actual 实现
│
├── jsMain/kotlin/cc/unitmesh/devins/ui/
│   └── compose/
│       └── AutoDevApp.js.kt            # JS actual 实现
│
└── wasmJsMain/kotlin/cc/unitmesh/devins/ui/
    └── compose/
        └── AutoDevApp.wasm.kt          # WASM actual 实现
```

## 🎨 Android UI 设计

### 布局结构

```
┌───────────────────────────────────┐
│  [TopBar with Menu & Actions]    │
├───────────────────────────────────┤
│                                   │
│        Main Content               │
│        (根据 currentScreen)        │
│                                   │
├───────────────────────────────────┤
│  [Bottom Navigation]              │
│   🏠 Home  💬 Chat  📋 Tasks  👤  │
└───────────────────────────────────┘
```

### 屏幕类型

| 屏幕 | 入口 | 功能 |
|-----|------|------|
| **HOME** | BottomNav + Drawer | 欢迎页、快速操作、最近会话 |
| **CHAT** | BottomNav + Drawer | AI 对话（支持 Agent 模式）|
| **TASKS** | BottomNav + Drawer | 任务管理（开发中）|
| **PROFILE** | BottomNav + Drawer | 设置和配置 |

### Drawer 菜单

```
┌───────────────────────┐
│  👤 User Profile      │
│  本地用户 / AutoDev    │
│  ────────────────────  │
│  🏠 首页              │
│  💬 对话              │
│  📁 项目（仅 Drawer）  │
│  📋 任务              │
│  👤 我的              │
│  ────────────────────  │
│  ⚙️ 模型设置          │
│  🔧 工具配置          │
│  🐛 调试信息*         │
│  ────────────────────  │
│  🚪 退出登录          │
│  ────────────────────  │
│  AutoDev v0.1.5       │
└───────────────────────┘

* 调试信息仅在有调试数据时显示
```

## 💻 代码示例

### 使用 PlatformAutoDevApp

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Android 平台会自动使用 Android 专属实现
            PlatformAutoDevApp(
                triggerFileChooser = false,
                onFileChooserHandled = {},
                initialMode = "auto"
            )
        }
    }
}
```

### 屏幕切换逻辑

```kotlin
// AndroidAutoDevContent 内部
var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

AndroidNavLayout(
    currentScreen = currentScreen,
    onScreenChange = { currentScreen = it },
    sessionViewModel = sessionViewModel,
    onShowSettings = { showModelConfigDialog = true },
    onShowTools = { showToolConfigDialog = true },
    onShowDebug = { showDebugDialog = true },
    hasDebugInfo = compilerOutput.isNotEmpty()
) { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
        when (currentScreen) {
            AppScreen.HOME -> HomeScreen(...)
            AppScreen.CHAT -> ChatScreen(...)
            AppScreen.TASKS -> TasksPlaceholderScreen()
            AppScreen.PROFILE -> ProfileScreen(...)
            else -> Text("开发中...")
        }
    }
}
```

## 🎯 主要组件

### 1. HomeScreen

- 欢迎卡片（Primary Container）
- 快速操作（AI 对话 + 项目管理）
- 最近会话列表（最多显示 5 条）

```kotlin
@Composable
private fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToProjects: () -> Unit,
    recentSessions: List<ChatSession>
)
```

### 2. ChatScreen

- 支持 Chat 模式和 Agent 模式
- 集成 `AgentChatInterface`（Agent 模式）
- 集成 `MessageList` + `DevInEditorInput`（Chat 模式）
- 全屏沉浸式体验（隐藏平台 TopBar）

```kotlin
@Composable
private fun ChatScreen(
    messages: List<Message>,
    currentStreamingOutput: String,
    isLLMProcessing: Boolean,
    llmService: KoogLLMService?,
    chatHistoryManager: ChatHistoryManager,
    callbacks: Any,
    completionManager: Any,
    projectPath: String,
    fileSystem: DefaultFileSystem,
    useAgentMode: Boolean,
    isTreeViewVisible: Boolean,
    selectedAgentType: String,
    currentModelConfig: ModelConfig?,
    onConfigWarning: () -> Unit,
    onModelConfigChange: (ModelConfig) -> Unit
)
```

### 3. ProfileScreen

- 模型配置入口（显示当前配置）
- 工具配置入口
- 关于信息（版本号等）

```kotlin
@Composable
private fun ProfileScreen(
    currentModelConfig: ModelConfig?,
    onShowModelConfig: () -> Unit,
    onShowToolConfig: () -> Unit
)
```

### 4. TasksPlaceholderScreen

- 占位屏幕，显示"即将推出"

## 🔧 配置管理

### 配置 Dialog

所有配置通过 Dialog 管理，支持：

1. **ModelConfigDialog**: 模型配置（API Key、Provider、Model）
2. **ToolConfigDialog**: 工具配置（MCP Tools、Builtin Tools）
3. **DebugDialog**: 调试信息（Compiler Output）

### 配置入口

- **Drawer 菜单**: ⚙️ 模型设置、🔧 工具配置
- **Profile 屏幕**: 配置卡片（点击打开 Dialog）

## 🎨 设计规范

### 颜色使用

```kotlin
// 使用 MaterialTheme 色彩系统
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.primaryContainer
MaterialTheme.colorScheme.surface
MaterialTheme.colorScheme.error

// ❌ 避免硬编码颜色
// Color(0xFF...) 或 Color.Red
```

### 间距规范

```kotlin
// 组件间距
Arrangement.spacedBy(8.dp)  // 小间距
Arrangement.spacedBy(12.dp) // 中间距
Arrangement.spacedBy(16.dp) // 大间距

// 内容边距
Modifier.padding(16.dp)              // 标准边距
Modifier.padding(horizontal = 12.dp) // 水平边距
Modifier.padding(vertical = 8.dp)    // 垂直边距
```

### 组件大小

```kotlin
// 图标大小
Modifier.size(16.dp)  // 小图标（按钮内）
Modifier.size(24.dp)  // 标准图标
Modifier.size(32.dp)  // 大图标（快速操作）
Modifier.size(48.dp)  // 超大图标（用户头像）
Modifier.size(64.dp)  // 特大图标（占位图标）
```

## 📦 依赖关系

### Android 依赖的 Common 组件

- `AndroidNavLayout` (commonMain)
- `SessionViewModel` (commonMain)
- `ChatHistoryManager` (commonMain)
- `ConfigManager` (commonMain)
- `WorkspaceManager` (commonMain)
- `AgentChatInterface` (commonMain)
- `MessageList` (commonMain)
- `DevInEditorInput` (commonMain)

### Android 特定组件

- `PlatformAutoDevApp` (actual 实现)
- `AndroidAutoDevContent` (private)
- `HomeScreen` (private)
- `ChatScreen` (private)
- `ProfileScreen` (private)
- `TasksPlaceholderScreen` (private)

## 🚀 测试指南

### 1. 编译 Android 应用

```bash
cd /Volumes/source/ai/autocrud
./gradlew :mpp-ui:assembleDebug
```

### 2. 运行在模拟器

```bash
./gradlew :mpp-ui:installDebug
adb shell am start -n cc.unitmesh.devins.ui/.MainActivity
```

### 3. 测试流程

#### 首次启动
1. ✅ 显示 HOME 屏幕
2. ✅ 显示欢迎卡片
3. ✅ 快速操作可点击
4. ✅ BottomNavigation 正确显示

#### Chat 功能
1. 点击"对话"按钮或 BottomNav 的 Chat 图标
2. ✅ 切换到 CHAT 屏幕
3. ✅ 显示输入框（居中）
4. ✅ 输入消息后显示消息列表
5. ✅ AI 回复正常显示

#### Drawer 菜单
1. 点击 TopBar 左侧的汉堡菜单
2. ✅ Drawer 从左侧滑出
3. ✅ 显示用户信息
4. ✅ 显示所有导航项
5. ✅ 显示设置和工具选项
6. ✅ 点击导航项切换屏幕
7. ✅ 点击设置打开配置 Dialog

#### 配置管理
1. 打开 Drawer → 点击"模型设置"
2. ✅ 显示 ModelConfigDialog
3. ✅ 输入配置并保存
4. ✅ 配置生效（可以发送消息）

## 🐛 已知问题

### 问题 1: TreeView 在 Android 上显示异常
**状态**: 待修复  
**原因**: Android 的 SplitPane 实现可能有问题  
**临时方案**: 暂时禁用 TreeView 或使用全屏模式

### 问题 2: 键盘弹出时布局调整
**状态**: 部分解决  
**解决方案**: 使用 `Modifier.imePadding()` 和 `Modifier.navigationBarsPadding()`

### 问题 3: Drawer 滑动手势冲突
**状态**: 待测试  
**潜在问题**: 可能与 Chat 消息列表的滑动冲突

## 📝 开发笔记

### 设计决策

1. **BottomNavigation 只显示 4 个入口**
   - Home、Chat、Tasks、Profile
   - Projects 放在 Drawer 中（Android 屏幕有限）

2. **Chat 屏幕不显示 TopBar（Agent 模式）**
   - 全屏沉浸式体验
   - 更多空间显示 Agent 执行过程

3. **Settings 放在 Drawer 和 Profile 双入口**
   - Drawer: 快速访问（无需切换屏幕）
   - Profile: 统一配置界面

4. **使用 expect/actual 模式**
   - Android 有独立实现
   - Desktop/WASM 共享原有实现
   - 易于维护和扩展

### 未来优化

1. **Android 手势优化**
   - 侧滑返回
   - 长按菜单
   - 双击滚动到顶部

2. **性能优化**
   - LazyColumn 优化
   - 图片缓存
   - 状态持久化

3. **UI 细节优化**
   - 动画过渡
   - 加载状态
   - 错误提示

4. **无障碍支持**
   - Content Description
   - Semantic Properties
   - Keyboard Navigation

## 🔗 相关文档

- [重构设计方案](./refactoring-autodev-app-design.md)
- [NavLayout 文档](../mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/app/NavLayout.kt)
- [Android Material 3](https://m3.material.io/)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

---

**文档版本**: v1.0  
**创建时间**: 2025-11-13  
**作者**: AI Assistant  
**状态**: 实现完成，待测试

