# UI 改进总结 - Sidebar & Workspace 集成

## 实施的改进

### 1. DesktopTitleBarTabs 左侧添加 Sidebar Toggle

**变更**：在标题栏最左侧添加了 Sidebar 切换按钮

**实现**：
```kotlin
// 左侧第一个按钮
IconButton(
    onClick = { UIStateManager.toggleSessionSidebar() },
    modifier = Modifier.size(28.dp)
) {
    Icon(
        imageVector = if (isSessionSidebarVisible) 
            AutoDevComposeIcons.MenuOpen 
        else 
            AutoDevComposeIcons.Menu,
        contentDescription = if (isSessionSidebarVisible) 
            "Hide Sidebar" 
        else 
            "Show Sidebar",
        tint = if (isSessionSidebarVisible) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    )
}
```

**布局**：
```
[Sidebar Toggle] | [Coding] [Code Review] [Remote] ... [Workspace] ... [Remote Config] [TreeView]
     ↑                                                                                      ↑
  新增按钮                                                                               原有按钮
```

**特性**：
- 使用全局状态：`UIStateManager.isSessionSidebarVisible`
- 图标自动切换：MenuOpen (展开) / Menu (收起)
- 颜色高亮：展开时显示 primary 颜色

---

### 2. Code Review 模式自动隐藏 SessionSidebar

**需求**：切换到 Code Review 类型时，自动隐藏左侧的 Session 历史面板

**实现**：
```kotlin
@Composable
fun DesktopTitleBarTabs(...) {
    // 当切换到 Code Review 时自动隐藏 SessionSidebar
    LaunchedEffect(currentAgentType) {
        if (currentAgentType == AgentType.CODE_REVIEW) {
            UIStateManager.setSessionSidebarVisible(false)
        } else {
            UIStateManager.setSessionSidebarVisible(true)
        }
    }
    ...
}
```

**行为**：
- `AgentType.CODE_REVIEW` → 自动隐藏 SessionSidebar
- `AgentType.CODING/REMOTE/LOCAL_CHAT` → 自动显示 SessionSidebar
- 用户仍可手动点击 Sidebar Toggle 切换

**原因**：
- Code Review 通常是一次性任务，不需要会话历史
- 给代码对比视图提供更多空间
- 优化专注度

---

### 3. SessionSidebar UI 改进

#### 3.1 移除右上角 "+" 按钮

**Before**:
```
┌─────────────────────────┐
│ Sessions            [+] │  ← 右上角有 + 按钮
├─────────────────────────┤
│ ...                     │
```

**After**:
```
┌─────────────────────────┐
│ Recent                  │  ← 标题改为 Recent，移除 + 按钮
├─────────────────────────┤
│ ...                     │
```

#### 3.2 标题改为 "Recent"

```kotlin
Text(
    text = "Recent",  // 之前是 "Sessions"
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurface
)
```

**理由**：
- "Recent" 更准确描述内容（最近的会话）
- 与 IDE 的命名习惯一致（Recent Files, Recent Projects）
- 更简洁直观

#### 3.3 底部添加 "New Agent" 按钮

**新增**：在底部 Settings 按钮上方添加完整的 "New Agent" 按钮

```kotlin
// New Agent Button
Button(
    onClick = onNewChat,
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
) {
    Icon(
        imageVector = AutoDevComposeIcons.Add,
        contentDescription = null,
        modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
        text = "New Agent",
        style = MaterialTheme.typography.labelLarge
    )
}
```

**布局**：
```
┌─────────────────────────┐
│ Recent                  │
├─────────────────────────┤
│ Session 1               │
│ Session 2               │
│ ...                     │
├─────────────────────────┤
│ [+] New Agent          │  ← 新增：完整按钮
├─────────────────────────┤
│ [⚙️] [🔧] [🐛]          │  ← Settings / Tools / Debug
└─────────────────────────┘
```

**优势**：
- 更显眼，易于发现
- 更大的点击区域
- 文字标签明确说明功能
- 视觉上与底部工具栏区分开

---

## 全局状态集成

所有改动都使用 `UIStateManager` 全局状态管理：

### 读取状态
```kotlin
// 在任何 Composable 中
val isSessionSidebarVisible by UIStateManager.isSessionSidebarVisible.collectAsState()
val workspacePath by UIStateManager.workspacePath.collectAsState()
val isTreeViewVisible by UIStateManager.isTreeViewVisible.collectAsState()
```

### 修改状态
```kotlin
// 切换 Sidebar
UIStateManager.toggleSessionSidebar()

// 设置 Sidebar 显示状态
UIStateManager.setSessionSidebarVisible(false)

// 设置工作空间路径
UIStateManager.setWorkspacePath(path)
```

### 自动同步
- 状态变化自动触发所有订阅组件的 recomposition
- 无需手动传递回调
- 无需担心状态不同步

---

## 测试验证

### 启动日志分析
```
✅ 加载上次工作空间: autodev-lotus (/Users/phodal/ai/autodev-lotus)
📁 [UIStateManager] Workspace path set to: /Users/phodal/ai/autodev-lotus

🔄 [UIStateManager] Session Sidebar toggled to: false
🔄 [UIStateManager] Session Sidebar toggled to: true

🔄 Switch Agent Type: CODE_REVIEW
```

**验证结果**：
1. ✅ Workspace 路径正确加载并显示在标题栏
2. ✅ Sidebar Toggle 按钮工作正常
3. ✅ 切换到 Code Review 时自动控制 Sidebar

---

## 用户体验改进

### Before
```
[Sessions          +]
- 标题通用性太强
- + 按钮不够显眼
- 无法快速切换 Sidebar
- Code Review 模式下仍显示历史
```

### After
```
[☰] [Coding] [Code Review] ...
- 左侧可快速切换 Sidebar
- Recent 更准确描述内容
- 底部大按钮创建新会话
- Code Review 自动隐藏历史
```

---

## 文件变更

1. **DesktopTitleBarTabs.kt**
   - 添加 Sidebar Toggle 按钮
   - 添加 `LaunchedEffect` 监听 AgentType 变化
   - 从全局状态读取 `isSessionSidebarVisible`

2. **SessionSidebar.kt**
   - Header 标题改为 "Recent"
   - 移除右上角 + 按钮
   - 底部添加 "New Agent" 完整按钮

3. **UIStateManager.kt**
   - 已包含 `isSessionSidebarVisible` 状态管理
   - 提供 `toggleSessionSidebar()` 和 `setSessionSidebarVisible()` 方法

---

## 技术亮点

### 1. 响应式设计
```kotlin
LaunchedEffect(currentAgentType) {
    if (currentAgentType == AgentType.CODE_REVIEW) {
        UIStateManager.setSessionSidebarVisible(false)
    } else {
        UIStateManager.setSessionSidebarVisible(true)
    }
}
```
- 自动响应 AgentType 变化
- 无需手动管理状态同步

### 2. 状态集中管理
```kotlin
object UIStateManager {
    private val _isSessionSidebarVisible = MutableStateFlow(true)
    val isSessionSidebarVisible: StateFlow<Boolean> = _isSessionSidebarVisible.asStateFlow()
}
```
- 单一数据源
- 类型安全
- 易于测试

### 3. Material Design 3
```kotlin
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
)
```
- 遵循 Material 3 设计规范
- 主题自适应
- 一致的视觉语言

---

## 下一步建议

1. **添加动画**
   ```kotlin
   AnimatedVisibility(visible = isSessionSidebarVisible) {
       SessionSidebar(...)
   }
   ```

2. **记住用户偏好**
   - 保存 Sidebar 展开/收起状态
   - 下次启动时恢复

3. **键盘快捷键**
   - `Cmd/Ctrl + B` 切换 Sidebar
   - `Cmd/Ctrl + N` 新建 Agent

4. **性能优化**
   - 只在可见时加载 Session 列表
   - 虚拟滚动优化大量历史记录
