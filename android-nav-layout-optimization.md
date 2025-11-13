# Android APP 布局优化总结

## 概述

优化了 Android APP 的布局，设计了新的 `AndroidNavLayout` 来提供更好的 Session 管理体验，并将原来的 `SessionAppContent` 功能集成到新的架构中。

## 主要变更

### 1. 新增 AndroidNavLayout (Android 专用导航布局)

**文件**: `mpp-ui/src/androidMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayout.kt`

**功能**:
- ✅ 底部导航栏，包含 4 个标签页：
  - **本地 Chat**: 本地 AI 对话
  - **远程 Session**: 远程会话管理（需登录）
  - **项目**: 项目管理
  - **任务**: 任务管理
- ✅ 侧边栏式 Session 列表（类似 SessionSidebar）
- ✅ Session 详情展示
- ✅ 项目和任务的 CRUD 操作
- ✅ 登录/登出功能
- ✅ Material 3 设计风格

**布局结构**:
```
┌─────────────────────────────┐
│   Android APP               │
├─────────────────────────────┤
│                             │
│   Content Area              │
│   (根据选中的 Tab 变化)      │
│                             │
├─────────────────────────────┤
│  本地 │ 远程 │ 项目 │ 任务  │  ← 底部导航栏
└─────────────────────────────┘
```

远程 Session 页面布局:
```
┌──────────┬──────────────────┐
│  Session │                  │
│  列表    │   Session 详情    │
│  (侧边栏) │                  │
│          │                  │
└──────────┴──────────────────┘
```

### 2. 集成到 AutoDevApp

**文件**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/AutoDevApp.kt`

**变更**:
- ✅ 添加 Android 平台检测，自动使用 `AndroidNavLayoutWrapper`
- ✅ 创建 `expect`/`actual` 声明，实现跨平台兼容
- ✅ Android 平台使用优化的导航布局
- ✅ 其他平台保持原有布局不变

**代码片段**:
```kotlin
// Android 平台使用优化的导航布局
if (Platform.isAndroid) {
    AndroidNavLayoutWrapper(
        chatHistoryManager = chatHistoryManager,
        serverUrl = serverUrl,
        // ... 其他参数
    )
    return
}
```

### 3. AndroidNavLayoutWrapper (跨平台适配)

**文件**:
- `mpp-ui/src/androidMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayoutWrapper.android.kt` (实际实现)
- `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayoutWrapper.jvm.kt` (占位)
- `mpp-ui/src/iosMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayoutWrapper.ios.kt` (占位)
- `mpp-ui/src/jsMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayoutWrapper.js.kt` (占位)
- `mpp-ui/src/wasmJsMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayoutWrapper.wasm.kt` (占位)

**作用**:
- 提供跨平台的 `expect`/`actual` 机制
- Android 平台有完整实现
- 其他平台提供空实现（不会被调用）

### 4. 弃用 SessionApp

**文件**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/app/SessionApp.kt`

**变更**:
- ✅ 删除了所有旧代码（SessionAppContent、AndroidNavigationLayout、DesktopNavigationLayout 等）
- ✅ 添加了弃用说明和迁移指引
- ✅ 标记为 `@Deprecated`

**迁移路径**:
- Android → `AndroidNavLayout`
- Desktop/其他平台 → `UnifiedAppContent`

## 设计优势

### 1. **平台优化**
- Android 使用底部导航（符合 Material Design 规范）
- Desktop 保持侧边栏导航（更适合大屏幕）

### 2. **统一架构**
- 本地 Chat 和远程 Session 统一在一个 APP 中
- 无缝切换不同模式

### 3. **模块化**
- `AndroidNavLayout`: 纯 Android UI 组件
- `AndroidNavLayoutWrapper`: 跨平台适配层
- `AutoDevApp`: 主应用入口

### 4. **可扩展性**
- 易于添加新的标签页
- 支持更多远程功能（项目、任务等）

## 使用方式

### Android APP
直接使用 `AutoDevApp()`，会自动检测平台并使用优化的布局：

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoDevApp() // 自动使用 AndroidNavLayout
        }
    }
}
```

### Desktop APP
仍然使用原有的布局（Scaffold + SessionSidebar）：

```kotlin
fun main() = application {
    Window(/*...*/) {
        AutoDevApp() // 使用桌面布局
    }
}
```

## 技术要点

### 1. Kotlin Multiplatform
- 使用 `expect`/`actual` 实现平台特定代码
- 保持公共代码的可移植性

### 2. Compose Multiplatform
- Material 3 组件
- 响应式状态管理 (`State`, `StateFlow`)

### 3. 导航模式
- Android: `NavigationBar` (底部)
- Desktop: `NavigationRail` (侧边)

### 4. 会话管理
- 本地会话: `ChatHistoryManager`
- 远程会话: `SessionViewModel` + `SessionClient`

## 文件清单

### 新增文件
1. `mpp-ui/src/androidMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayout.kt` (620+ 行)
2. `mpp-ui/src/androidMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayoutWrapper.android.kt`
3. `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayoutWrapper.jvm.kt`
4. `mpp-ui/src/iosMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayoutWrapper.ios.kt`
5. `mpp-ui/src/jsMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayoutWrapper.js.kt`
6. `mpp-ui/src/wasmJsMain/kotlin/cc/unitmesh/devins/ui/compose/AndroidNavLayoutWrapper.wasm.kt`

### 修改文件
1. `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/AutoDevApp.kt`
   - 添加 Android 平台检测
   - 添加 `AndroidNavLayoutWrapper` expect 声明

2. `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/app/SessionApp.kt`
   - 删除所有旧代码
   - 标记为弃用

## 测试建议

### Android
1. ✅ 编译通过（无错误）
2. 🔲 启动 APP，检查底部导航栏
3. 🔲 测试本地 Chat 功能
4. 🔲 测试远程 Session 登录
5. 🔲 测试项目和任务管理

### Desktop
1. 🔲 确认布局未受影响
2. 🔲 SessionSidebar 功能正常

## 后续改进

1. **状态持久化**: 保存当前选中的 Tab
2. **动画优化**: 添加页面切换动画
3. **主题支持**: 深色/浅色主题切换
4. **性能优化**: 懒加载 Session 列表
5. **错误处理**: 网络错误、认证失败等

## 参考
- Material Design 3: https://m3.material.io/
- Compose Multiplatform: https://www.jetbrains.com/lp/compose-multiplatform/
- Android Navigation: https://developer.android.com/guide/navigation
