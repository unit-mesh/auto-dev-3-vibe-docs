# 全局 StateFlow 重构总结

## 重构完成

已成功将 TreeView 和其他 UI 状态从 Props Drilling 模式迁移到全局 StateFlow 模式。

## 文件变更

### 新增文件

1. **`mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/state/UIStateManager.kt`**
   - 全局 UI 状态管理器
   - 使用 StateFlow 管理：
     - `isTreeViewVisible`: TreeView 显示状态
     - `isSessionSidebarVisible`: Session Sidebar 显示状态  
     - `workspacePath`: 工作空间路径
     - `hasHistory`: 历史记录状态

### 修改文件

2. **`mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/chat/DesktopTitleBarTabs.kt`**
   - 移除 `workspacePath`, `isTreeViewVisible`, `onToggleTreeView` 参数
   - 直接从 `UIStateManager` 获取状态
   - 直接调用 `UIStateManager.toggleTreeView()` 切换状态

3. **`mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/AutoDevApp.kt`**
   - 添加 `UIStateManager` 导入
   - 从本地 `mutableStateOf` 改为 `UIStateManager.*.collectAsState()`
   - 初始化全局状态: `UIStateManager.setTreeViewVisible(initialTreeViewVisible)`
   - 简化回调: `onToggleTreeView = { UIStateManager.toggleTreeView() }`
   - 同步状态到全局: `UIStateManager.setWorkspacePath(path)`, `UIStateManager.setHasHistory(hasHistory)`

4. **`mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/AgentChatInterface.kt`**
   - 添加 `UIStateManager` 导入
   - 从全局状态获取: `val isTreeViewVisibleState by UIStateManager.isTreeViewVisible.collectAsState()`
   - 移除复杂的双向状态同步逻辑（原来的 `LaunchedEffect` 循环调用）
   - 直接使用 `UIStateManager.toggleTreeView()` 和 `UIStateManager.setTreeViewVisible(false)`

5. **`mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/state/DesktopUiState.kt`**
   - 添加 `UIStateManager` 导入
   - 改为从全局状态读取:
     ```kotlin
     val showSessionSidebar: Boolean
         get() = UIStateManager.isSessionSidebarVisible.value
     
     val isTreeViewVisible: Boolean
         get() = UIStateManager.isTreeViewVisible.value
     
     val workspacePath: String
         get() = UIStateManager.workspacePath.value
     ```
   - 所有修改方法委托给全局管理器:
     ```kotlin
     fun toggleTreeView() {
         UIStateManager.toggleTreeView()
     }
     ```

6. **`mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/Main.kt`**
   - 简化 `DesktopTitleBarTabs` 调用，移除不必要的参数
   - 简化回调函数，状态由全局管理器处理

## 架构优势

### Before (Props Drilling)
```
Main.kt (uiState)
  ↓ props
DesktopTitleBarTabs (workspacePath, isTreeViewVisible, onToggleTreeView)
  ↓ callback
AutoDevApp (isTreeViewVisible)
  ↓ props
AgentInterfaceRouter (isTreeViewVisible, onToggleTreeView)
  ↓ props
AgentChatInterface (isTreeViewVisible, onToggleTreeView)
  ↓ sync
CodingAgentViewModel (isTreeViewVisible)
  ↓ render
FileSystemTreeView
```

### After (Global StateFlow)
```
UIStateManager (Single Source of Truth)
  ↓ StateFlow
  ├─> Main.kt (collectAsState)
  ├─> DesktopTitleBarTabs (collectAsState)
  ├─> AutoDevApp (collectAsState)
  ├─> AgentChatInterface (collectAsState)
  └─> FileSystemTreeView

Any component can:
- Read: UIStateManager.isTreeViewVisible.collectAsState()
- Write: UIStateManager.toggleTreeView()
```

## 关键改进

### 1. 消除状态传递层级
- ❌ 之前：需要通过 5-6 层组件传递 props 和回调
- ✅ 现在：任何组件直接访问全局状态

### 2. 避免循环调用
- ❌ 之前：`LaunchedEffect` 监听状态变化，导致循环调用
- ✅ 现在：单向数据流，状态变化自动触发 UI 更新

### 3. 代码更简洁
- ❌ 之前：每个组件需要声明 `isTreeViewVisible` 和 `onToggleTreeView` 参数
- ✅ 现在：直接调用 `UIStateManager.toggleTreeView()`

### 4. 易于调试
- ✅ 所有状态变化都在 `UIStateManager` 中有日志输出
- ✅ 单一数据源，状态变化路径清晰

### 5. 性能优化
- ✅ StateFlow 只在值真正改变时触发 recomposition
- ✅ 避免不必要的回调传递和重新创建

## 使用示例

### 读取状态
```kotlin
@Composable
fun MyComponent() {
    val isTreeViewVisible by UIStateManager.isTreeViewVisible.collectAsState()
    val workspacePath by UIStateManager.workspacePath.collectAsState()
    
    Text("TreeView is ${if (isTreeViewVisible) "visible" else "hidden"}")
    Text("Workspace: $workspacePath")
}
```

### 修改状态
```kotlin
// 切换 TreeView
IconButton(onClick = { UIStateManager.toggleTreeView() }) {
    Icon(...)
}

// 设置工作空间路径
UIStateManager.setWorkspacePath("/path/to/workspace")

// 设置 Sidebar 显示状态
UIStateManager.setSessionSidebarVisible(true)
```

## 测试验证

✅ 编译通过
- 无编译错误
- 只有预期的废弃警告（与本次重构无关）

## 下一步优化建议

1. **添加单元测试**
   ```kotlin
   @Test
   fun testToggleTreeView() {
       UIStateManager.reset()
       assertFalse(UIStateManager.isTreeViewVisible.value)
       UIStateManager.toggleTreeView()
       assertTrue(UIStateManager.isTreeViewVisible.value)
   }
   ```

2. **考虑添加持久化**
   - 将 UI 状态保存到本地存储
   - 应用重启后恢复上次的状态

3. **扩展到更多状态**
   - 将其他全局 UI 状态迁移到 `UIStateManager`
   - 例如：`selectedAgent`, `useAgentMode`, `showConfigDialog` 等

4. **性能监控**
   - 添加性能追踪，确保 StateFlow 不会导致过度 recomposition
   - 使用 Compose Compiler Metrics 分析

## 参考文档

- `docs/fix-treeview-state-propagation.md` - 原问题分析和 StateFlow 方案设计
- [Kotlin StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)
- [Jetpack Compose State Management](https://developer.android.com/jetpack/compose/state)
