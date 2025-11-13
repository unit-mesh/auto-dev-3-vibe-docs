# Session 管理架构分析 - Chat vs Agent 模式

## 问题诊断

### 当前状态
SessionSidebar 只在 **Chat 模式**下显示，但用户期望在 **Desktop 上始终显示**，包括 Agent 模式。

## 核心问题：会话管理系统不一致

### 1. Chat 模式的会话管理 ✅

#### 使用的组件
- **ChatHistoryManager** (`mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/llm/ChatHistoryManager.kt`)
- **SessionStorage** (平台实现)

#### 特性
```kotlin
class ChatHistoryManager {
    private val sessions = mutableMapOf<String, ChatSession>()  // 多会话管理
    private var currentSessionId: String? = null
    
    // ✅ 持久化支持
    suspend fun initialize() {
        val loadedSessions = SessionStorage.loadSessions()
        // 从磁盘加载：~/.autodev/sessions/chat-sessions.json
    }
    
    // ✅ 自动保存
    private fun saveSessionsAsync() {
        SessionStorage.saveSessions(sessions.values.toList())
    }
    
    // ✅ 多会话支持
    fun createSession()
    fun switchSession(sessionId: String)
    fun deleteSession(sessionId: String)
    fun getAllSessions(): List<ChatSession>
}
```

#### 存储位置
- **JVM**: `~/.autodev/sessions/chat-sessions.json`
- **Android**: 内存缓存
- **WASM**: `localStorage`

### 2. Agent 模式的会话管理 ❌

#### 使用的组件
- **CodingAgentViewModel** + **ComposeRenderer**
- **NO SessionStorage!**

#### 当前实现
```kotlin
// ComposeRenderer.kt
class ComposeRenderer : CodingAgentRenderer {
    // ❌ 只有内存状态，没有持久化！
    private val _timeline = mutableStateListOf<TimelineItem>()
    
    fun clearMessages() {
        _timeline.clear()  // 清空就丢失了
    }
}

// CodingAgentViewModel.kt
class CodingAgentViewModel(
    llmService: KoogLLMService?,
    projectPath: String,
    maxIterations: Int = 100
) {
    // ❌ 每次创建都是新的 Renderer，之前的历史丢失
    val renderer = ComposeRenderer()
}
```

#### 问题总结
| 特性 | Chat 模式 | Agent 模式 |
|------|----------|-----------|
| 多会话支持 | ✅ 是 | ❌ 否 |
| 持久化存储 | ✅ 是 | ❌ 否 |
| 历史记录 | ✅ 保存到磁盘 | ❌ 应用关闭即丢失 |
| 会话切换 | ✅ 支持 | ❌ 不支持 |
| SessionSidebar 集成 | ✅ 完全支持 | ❌ 无法集成 |

## 架构对比

### Chat 模式架构
```
┌─────────────────────┐
│   SessionSidebar    │ ← 显示所有会话
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│ ChatHistoryManager  │ ← 管理多会话
├─────────────────────┤
│ • createSession()   │
│ • switchSession()   │
│ • getAllSessions()  │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│  SessionStorage     │ ← 持久化
├─────────────────────┤
│ JVM: ~/.autodev/    │
│ Android: Memory     │
│ WASM: localStorage  │
└─────────────────────┘
```

### Agent 模式架构（当前）
```
┌─────────────────────┐
│ AgentChatInterface  │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│CodingAgentViewModel │ ← 每次创建都是新的
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│  ComposeRenderer    │ ← 内存状态（_timeline）
├─────────────────────┤
│ ❌ 无持久化          │
│ ❌ 无会话管理        │
│ ❌ 关闭即丢失        │
└─────────────────────┘
```

## 问题根源

### 1. 显示条件过严
```kotlin
// AutoDevApp.kt:530
if (showSessionSidebar && Platform.isJvm && !useAgentMode) {
    SessionSidebar(...)
}
```

**问题**：`!useAgentMode` 条件导致 Agent 模式下不显示

### 2. Agent 模式无会话系统
- `ComposeRenderer._timeline` 只是内存状态
- 没有类似 `ChatHistoryManager` 的会话管理
- 没有持久化机制
- 应用关闭后所有对话历史丢失

### 3. 两套独立系统
- Chat 模式：`ChatHistoryManager` → `SessionStorage`
- Agent 模式：`ComposeRenderer._timeline` → 内存

## 解决方案

### 方案 A：最小改动（推荐短期）

#### 1. 修改显示条件
```kotlin
// AutoDevApp.kt:530
// 修改前
if (showSessionSidebar && Platform.isJvm && !useAgentMode)

// 修改后
if (showSessionSidebar && Platform.isJvm)  // 移除 Agent 模式限制
```

#### 2. 在 Agent 模式下禁用会话操作
```kotlin
SessionSidebar(
    chatHistoryManager = chatHistoryManager,
    currentSessionId = if (useAgentMode) null else chatHistoryManager.getCurrentSession().id,
    onSessionSelected = { sessionId ->
        if (!useAgentMode) {
            chatHistoryManager.switchSession(sessionId)
            messages = chatHistoryManager.getMessages()
        }
    },
    // Agent 模式下显示提示信息
    showAgentModeWarning = useAgentMode,
    // ...
)
```

#### 3. 添加 UI 提示
在 SessionSidebar 中添加：
```kotlin
if (showAgentModeWarning) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.warningContainer
        )
    ) {
        Text(
            text = "⚠️ Agent mode doesn't support session history yet",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
        )
    }
}
```

**优点**：
- ✅ 最小改动
- ✅ 保持 UI 一致性
- ✅ 明确告知用户限制

**缺点**：
- ❌ Agent 模式仍无会话管理
- ❌ 功能不完整

### 方案 B：统一会话管理（推荐长期）

#### 1. 让 Agent 模式也使用 ChatHistoryManager

```kotlin
// CodingAgentViewModel.kt
class CodingAgentViewModel(
    llmService: KoogLLMService?,
    projectPath: String,
    maxIterations: Int = 100,
    // 新增：注入 ChatHistoryManager
    private val chatHistoryManager: ChatHistoryManager? = null
) {
    val renderer = ComposeRenderer()
    
    // 新增：保存到会话
    private fun saveToHistory(role: MessageRole, content: String) {
        when (role) {
            MessageRole.USER -> chatHistoryManager?.addUserMessage(content)
            MessageRole.ASSISTANT -> chatHistoryManager?.addAssistantMessage(content)
            else -> {}
        }
    }
    
    suspend fun executeTask(task: String) {
        saveToHistory(MessageRole.USER, task)  // 保存用户输入
        
        // ... 执行任务 ...
        
        val result = agent.executeTask(...)
        saveToHistory(MessageRole.ASSISTANT, result)  // 保存 Agent 输出
    }
    
    // 新增：从历史恢复
    fun restoreFromHistory(sessionId: String) {
        val messages = chatHistoryManager?.getMessages() ?: return
        renderer.clearMessages()
        messages.forEach { msg ->
            renderer._timeline.add(TimelineItem.MessageItem(msg))
        }
    }
}
```

#### 2. 修改 AgentChatInterface
```kotlin
@Composable
fun AgentChatInterface(
    llmService: KoogLLMService?,
    chatHistoryManager: ChatHistoryManager,  // 新增
    currentSessionId: String?,               // 新增
    // ...
) {
    val viewModel = remember(llmService, currentWorkspace?.rootPath, currentSessionId) {
        CodingAgentViewModel(
            llmService = llmService,
            projectPath = rootPath,
            chatHistoryManager = chatHistoryManager
        ).apply {
            // 恢复历史
            currentSessionId?.let { restoreFromHistory(it) }
        }
    }
    // ...
}
```

#### 3. 统一会话数据结构
```kotlin
// 扩展 ChatSession 支持 Agent 模式
data class ChatSession(
    val id: String,
    val messages: MutableList<Message>,
    val createdAt: Long,
    val updatedAt: Long,
    // 新增：标记会话类型
    val sessionType: SessionType = SessionType.CHAT
)

enum class SessionType {
    CHAT,    // 简单聊天
    AGENT    // Agent 任务执行
}
```

**优点**：
- ✅ 统一的会话管理
- ✅ Agent 模式支持历史记录
- ✅ 可以切换 Agent 会话
- ✅ 所有对话持久化

**缺点**：
- ❌ 需要较大重构
- ❌ Agent 的 Timeline 结构复杂（包含工具调用、文件查看等）

### 方案 C：创建 AgentSessionManager（最佳长期方案）

#### 1. 创建独立的 Agent 会话管理器
```kotlin
// AgentSessionManager.kt
class AgentSessionManager {
    private val sessions = mutableMapOf<String, AgentSession>()
    private var currentSessionId: String? = null
    
    // 使用类似 ChatHistoryManager 的架构
    suspend fun initialize() {
        val loadedSessions = AgentSessionStorage.loadSessions()
        // ...
    }
    
    fun createSession(task: String): AgentSession
    fun switchSession(sessionId: String): AgentSession?
    fun getAllSessions(): List<AgentSession>
    
    // 保存 Timeline 状态
    fun saveTimeline(sessionId: String, timeline: List<TimelineItem>)
    fun loadTimeline(sessionId: String): List<TimelineItem>
}

data class AgentSession(
    val id: String,
    val task: String,
    val timeline: List<TimelineItem>,  // 完整的 Agent 执行历史
    val status: AgentStatus,
    val createdAt: Long,
    val updatedAt: Long
)

enum class AgentStatus {
    PENDING, RUNNING, COMPLETED, FAILED
}
```

#### 2. SessionSidebar 统一显示
```kotlin
SessionSidebar(
    chatHistoryManager = chatHistoryManager,
    agentSessionManager = agentSessionManager,  // 新增
    currentMode = if (useAgentMode) "agent" else "chat",
    currentSessionId = currentSessionId,
    // ...
)
```

**优点**：
- ✅ 保持两种模式的独立性
- ✅ Agent 模式有完整的会话管理
- ✅ 可以保存复杂的 Timeline 结构
- ✅ 架构清晰，易于维护

**缺点**：
- ❌ 需要创建新的管理器
- ❌ 需要新的存储格式

## 推荐实施路径

### 阶段 1：快速修复（本次完成）
1. ✅ 修改 `AutoDevApp.kt` 显示条件，移除 `!useAgentMode`
2. ✅ SessionSidebar 在 Agent 模式下显示提示信息
3. ✅ 文档说明当前限制

### 阶段 2：最小集成（1-2 周）
1. CodingAgentViewModel 集成 ChatHistoryManager
2. 简化 Timeline 为纯文本消息保存
3. 支持 Agent 会话切换

### 阶段 3：完整方案（1-2 月）
1. 创建 AgentSessionManager
2. 支持完整 Timeline 持久化
3. 统一 SessionSidebar 显示

## 实施代码

### 立即修复（方案 A）

```kotlin
// 1. AutoDevApp.kt:530
// 修改显示条件
if (showSessionSidebar && Platform.isJvm) {  // 移除 !useAgentMode
    SessionSidebar(
        chatHistoryManager = chatHistoryManager,
        currentSessionId = if (useAgentMode) null else chatHistoryManager.getCurrentSession().id,
        isAgentMode = useAgentMode,  // 新增标志
        // ...
    )
}
```

```kotlin
// 2. SessionSidebar.kt
@Composable
fun SessionSidebar(
    chatHistoryManager: ChatHistoryManager,
    currentSessionId: String?,
    isAgentMode: Boolean = false,  // 新增
    // ...
) {
    // 在会话列表前显示警告
    if (isAgentMode) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Agent Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Session history is only available in Chat mode. " +
                          "Switch to Chat mode to access your saved conversations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
    
    // 现有的会话列表（Agent 模式下可能为空或禁用）
}
```

## 总结

**当前问题**：
- ❌ SessionSidebar 只在 Chat 模式显示
- ❌ Agent 模式完全没有会话管理系统
- ❌ Agent 对话历史应用关闭即丢失

**根本原因**：
- Chat 和 Agent 使用两套独立的消息管理系统
- Agent 模式的 `ComposeRenderer` 只有内存状态，无持久化

**建议方案**：
- **短期**：修改显示条件 + 添加提示（方案 A）
- **长期**：创建 AgentSessionManager（方案 C）


