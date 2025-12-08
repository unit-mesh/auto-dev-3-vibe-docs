# 类 Temporal 持久化工作流编排系统 - 实施计划

> **目标**: 在不使用 Temporal 库的前提下，基于 mpp-server 和 mpp-core，实现类似 Temporal 的持久化工作流编排能力，用于 Agentic AI 架构

**文档日期**: 2025-12-08  
**参考文档**: `docs/sync/Temporal 助力 AI Agent 架构.md`

---

## 📋 目录

- [一、核心需求分析](#一核心需求分析)
- [二、现有能力盘点](#二现有能力盘点)
- [三、架构设计](#三架构设计)
- [四、实施路线图](#四实施路线图)
- [五、技术选型与依赖](#五技术选型与依赖)
- [六、风险评估与对策](#六风险评估与对策)

---

## 一、核心需求分析

### 1.1 Temporal 核心特性映射

根据参考文档，我们需要实现以下核心能力：

| Temporal 特性                        | 业务价值               | 实现优先级 |
|------------------------------------|--------------------|-------|
| **持久化执行 (Durable Execution)**      | Agent 执行过程崩溃后可恢复   | 🔴 P0 |
| **事件溯源 (Event Sourcing)**          | 记录所有执行步骤，支持审计和重放   | 🔴 P0 |
| **确定性重放 (Deterministic Replay)**   | 通过重放历史恢复到确切状态      | 🟡 P1 |
| **长时间运行 (Long-running Workflows)** | 支持天/周级别的任务（如等待审批）  | 🟡 P1 |
| **Signal/Query/Update**            | 外部与运行中 Workflow 交互 | 🔴 P0 |
| **人机回环 (Human-in-the-Loop)**       | 任务暂停等待人类决策         | 🟢 P2 |
| **多智能体协同 (Multi-Agent Swarm)**     | 父子任务、并行执行          | 🟢 P2 |
| **侧信道流式传输**                        | LLM 流式输出与持久化分离     | 🔵 P3 |

### 1.2 典型使用场景

#### 场景 1: 代码审查 Agent（长时间运行）
```
1. Agent 分析代码，生成审查报告
2. 发送通知给人类审查员，进入休眠状态（可能数小时/数天）
3. 人类通过 API 发送 Signal（批准/拒绝）
4. Agent 被唤醒，根据决策执行后续操作（合并/重构）
5. 整个过程可审计、可恢复
```

#### 场景 2: 多 Agent 协作开发
```
1. Master Agent 拆解任务为 3 个子任务
2. 并行启动 3 个 Worker Agent（子 Workflow）
3. Worker-1 编写代码，Worker-2 编写测试，Worker-3 更新文档
4. Master Agent 等待所有子任务完成
5. 如果 Worker-2 失败，自动重试或启动备用 Agent
6. 汇总结果，生成最终报告
```

#### 场景 3: 崩溃恢复
```
1. Agent 执行到第 50 步，调用 LLM 获得响应 X
2. 服务器崩溃重启
3. 系统从事件历史中恢复：
   - 重放前 49 步的事件
   - 读取第 50 步 LLM 的历史响应 X（不重新调用）
   - 从第 51 步继续执行
```

---

## 二、现有能力盘点

### 2.1 ✅ 已有的优秀基础

#### 2.1.1 会话管理（Session）
**位置**: `mpp-server/src/main/kotlin/cc/unitmesh/server/session/SessionManager.kt`

**现有能力**:
- ✅ Session 模型（状态、元数据）
- ✅ 事件包装器 `SessionEventEnvelope`（包含序列号）
- ✅ 事件存储 `eventStore`（内存 ConcurrentHashMap）
- ✅ 事件广播到 SSE 订阅者
- ✅ 会话状态快照 `SessionState`

**优点**:
- 已经有事件序列化和序列号机制
- 支持历史事件重播给新订阅者
- 线程安全的并发控制

**不足**:
- ❌ 事件仅存储在内存中，重启后丢失
- ❌ 没有检查点（Checkpoint）机制
- ❌ 没有工作流恢复逻辑

#### 2.1.2 Agent 执行器（CodingAgentExecutor）
**位置**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/executor/CodingAgentExecutor.kt`

**现有能力**:
- ✅ 迭代式执行循环
- ✅ AgentStep 和 AgentEdit 记录
- ✅ 对话管理 `ConversationManager`
- ✅ 工具调用和结果记录

**不足**:
- ❌ 步骤不持久化
- ❌ 崩溃后无法恢复
- ❌ 没有暂停/恢复机制

#### 2.1.3 事件系统（AgentEvent）
**位置**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/AgentEvent.kt`

**现有能力**:
- ✅ 丰富的事件类型（IterationStart, ToolCall, ToolResult, Error, Complete）
- ✅ 已序列化（@Serializable）

**优点**: 直接可用作事件溯源的事件流

#### 2.1.4 持久化能力
**位置**: 
- `mpp-ui/src/commonMain/sqldelight/` (SQLDelight)
- `mpp-core/src/*/kotlin/cc/unitmesh/devins/llm/SessionStorage.*.kt` (多平台文件系统)

**现有能力**:
- ✅ SQLDelight 数据库（JVM/Android）
- ✅ IndexedDB（WASM）
- ✅ 文件系统（JVM）
- ✅ LocalStorage（JS）

#### 2.1.5 协程与异步
**现有能力**:
- ✅ 大量使用 Kotlin Coroutines
- ✅ `ShellSessionManager` - 管理长时间运行的 Shell 会话
- ✅ `ToolOrchestrator` - 支持异步工具执行

### 2.2 ❌ 缺失的关键能力

| 能力 | 重要性 | 现状 |
|-----|-------|-----|
| **事件持久化** | 🔴 必需 | 仅内存存储 |
| **检查点 (Checkpoint)** | 🔴 必需 | 不存在 |
| **工作流恢复** | 🔴 必需 | 不存在 |
| **暂停/恢复** | 🟡 重要 | 不存在 |
| **Signal/Query 原语** | 🟡 重要 | 基础的事件广播存在 |
| **子工作流管理** | 🟢 次要 | 有 SubAgentManager，但不是工作流级别 |
| **版本控制** | 🔵 增强 | 不存在 |

---

## 三、架构设计

### 3.1 核心概念映射

| Temporal 概念 | 我们的实现 | 说明 |
|--------------|-----------|-----|
| **Workflow** | `DurableSession` | 持久化的执行会话 |
| **Activity** | `ToolExecution` | 工具调用（非确定性操作） |
| **Worker** | `AgentExecutor` | 执行 Agent 逻辑 |
| **Event History** | `SessionEventLog` | 事件溯源日志 |
| **Signal** | `SessionSignal` | 外部发送消息给会话 |
| **Query** | `SessionQuery` | 查询会话当前状态 |
| **Update** | `SessionUpdate` | 同步修改并返回结果 |

### 3.2 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                      Client (Android/Web/CLI)                   │
│              HTTP API + SSE (流式事件)                           │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                       mpp-server (Ktor)                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │   WorkflowController (REST API)                          │  │
│  │   - POST /api/workflows/start                            │  │
│  │   - POST /api/workflows/{id}/signal                      │  │
│  │   - GET  /api/workflows/{id}/query                       │  │
│  │   - POST /api/workflows/{id}/update                      │  │
│  │   - GET  /api/workflows/{id}/events (SSE)                │  │
│  └──────────────────────────────────────────────────────────┘  │
│                         │                                        │
│                         ▼                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │   WorkflowEngine (核心引擎)                               │  │
│  │   - startWorkflow()                                      │  │
│  │   - resumeWorkflow()                                     │  │
│  │   - sendSignal()                                         │  │
│  │   - executeQuery()                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│            │                    │                    │           │
│            ▼                    ▼                    ▼           │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐     │
│  │ EventStore   │    │ Checkpoint   │    │ SignalQueue  │     │
│  │ (事件溯源)    │    │ (状态快照)    │    │ (信号队列)    │     │
│  └──────────────┘    └──────────────┘    └──────────────┘     │
│            │                    │                    │           │
│            └────────────────────┴────────────────────┘           │
│                                 │                                │
│                                 ▼                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │   SQLite/PostgreSQL Database                             │  │
│  │   - workflow_events (事件日志)                            │  │
│  │   - workflow_checkpoints (检查点)                         │  │
│  │   - workflow_signals (信号队列)                           │  │
│  │   - workflow_metadata (工作流元数据)                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      mpp-core (Agent Logic)                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │   DurableAgentExecutor (持久化执行器)                      │  │
│  │   - 执行 Agent 逻辑                                        │  │
│  │   - 在关键点创建 Checkpoint                                │  │
│  │   - 支持暂停/恢复                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                         │                                        │
│                         ▼                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │   CodingAgent (现有 Agent)                                │  │
│  │   - LLM 调用（通过 Activity 包装）                         │  │
│  │   - Tool 执行（通过 Activity 包装）                        │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 核心模块设计

#### 3.3.1 模块一：EventStore（事件溯源存储）

**职责**: 
- 持久化所有 AgentEvent
- 提供事件流查询（按 sessionId）
- 保证事件顺序（sequenceNumber）

**数据模型**:
```kotlin
// 表：workflow_events
data class WorkflowEvent(
    val id: String,                  // UUID
    val workflowId: String,          // 工作流 ID
    val sequenceNumber: Long,        // 序列号（从 1 开始）
    val eventType: String,           // 事件类型
    val eventData: String,           // JSON 序列化的 AgentEvent
    val timestamp: Long,             // 时间戳
    val checkpointId: String?        // 关联的检查点 ID（如果有）
)
```

**接口**:
```kotlin
interface EventStore {
    suspend fun appendEvent(event: WorkflowEvent): Long  // 返回序列号
    suspend fun getEvents(workflowId: String, fromSequence: Long = 0): List<WorkflowEvent>
    suspend fun getLatestSequence(workflowId: String): Long
}
```

**实现方式**:
- **JVM/Server**: PostgreSQL 或 SQLite（推荐 PostgreSQL 用于生产）
- **Android**: SQLDelight（已有基础设施）
- **WASM**: IndexedDB（已有封装）
- **跨平台**: 使用 `expect`/`actual` 模式

#### 3.3.2 模块二：CheckpointManager（检查点管理）

**职责**:
- 在关键节点保存工作流状态快照
- 加速恢复（无需重放所有事件）
- 定期清理旧检查点

**数据模型**:
```kotlin
// 表：workflow_checkpoints
data class WorkflowCheckpoint(
    val id: String,                  // UUID
    val workflowId: String,
    val sequenceNumber: Long,        // 对应的事件序列号
    val state: String,               // JSON 序列化的状态
    val createdAt: Long
)

// 工作流状态
@Serializable
data class WorkflowState(
    val workflowId: String,
    val status: WorkflowStatus,
    val currentIteration: Int,
    val maxIterations: Int,
    val conversationHistory: List<Message>,  // 对话历史
    val agentSteps: List<AgentStep>,         // 已执行的步骤
    val agentEdits: List<AgentEdit>,         // 已执行的编辑
    val pendingSignals: List<String>,        // 待处理的信号
    val customState: Map<String, Any>        // 自定义状态
)
```

**检查点策略**:
```kotlin
// 何时创建检查点？
- 每 10 个事件
- 每次 LLM 调用后
- 每次 Tool 执行后
- 显式调用 checkpoint()
```

#### 3.3.3 模块三：WorkflowEngine（工作流引擎）

**职责**:
- 启动新工作流
- 从检查点恢复工作流
- 处理 Signal/Query/Update
- 调度 Agent 执行

**核心方法**:
```kotlin
class WorkflowEngine(
    private val eventStore: EventStore,
    private val checkpointManager: CheckpointManager,
    private val signalQueue: SignalQueue
) {
    suspend fun startWorkflow(request: StartWorkflowRequest): String {
        // 1. 创建 Workflow
        val workflowId = UUID.randomUUID().toString()
        
        // 2. 记录 WorkflowStarted 事件
        eventStore.appendEvent(
            WorkflowEvent(
                workflowId = workflowId,
                eventType = "WorkflowStarted",
                eventData = Json.encodeToString(request)
            )
        )
        
        // 3. 启动 Agent 执行（协程）
        launch {
            executeWorkflow(workflowId)
        }
        
        return workflowId
    }
    
    private suspend fun executeWorkflow(workflowId: String) {
        // 1. 恢复状态（从检查点 + 增量事件）
        val state = recoverState(workflowId)
        
        // 2. 创建 DurableAgentExecutor
        val executor = DurableAgentExecutor(
            workflowId = workflowId,
            initialState = state,
            eventStore = eventStore,
            checkpointManager = checkpointManager
        )
        
        // 3. 执行 Agent 逻辑
        try {
            executor.execute()
        } catch (e: Exception) {
            // 记录错误事件
            eventStore.appendEvent(
                WorkflowEvent(
                    workflowId = workflowId,
                    eventType = "WorkflowFailed",
                    eventData = Json.encodeToString(mapOf("error" to e.message))
                )
            )
        }
    }
    
    suspend fun sendSignal(workflowId: String, signal: WorkflowSignal) {
        // 1. 记录 Signal 事件
        eventStore.appendEvent(
            WorkflowEvent(
                workflowId = workflowId,
                eventType = "SignalReceived",
                eventData = Json.encodeToString(signal)
            )
        )
        
        // 2. 唤醒正在等待的工作流
        signalQueue.enqueue(workflowId, signal)
    }
}
```

#### 3.3.4 模块四：DurableAgentExecutor（持久化执行器）

**职责**:
- 包装现有的 `CodingAgentExecutor`
- 在每个关键步骤后记录事件
- 支持暂停/恢复
- 支持 `waitForSignal()` 原语

**关键设计**:
```kotlin
class DurableAgentExecutor(
    private val workflowId: String,
    private val initialState: WorkflowState,
    private val eventStore: EventStore,
    private val checkpointManager: CheckpointManager,
    private val signalQueue: SignalQueue
) {
    private var currentState = initialState
    
    suspend fun execute() {
        // 重放模式：判断是否在恢复
        val isRecovery = currentState.agentSteps.isNotEmpty()
        
        if (isRecovery) {
            logger.info { "Recovering workflow $workflowId from checkpoint" }
        }
        
        // 执行主循环
        while (shouldContinue()) {
            // 检查是否有待处理的信号
            val signal = signalQueue.poll(workflowId)
            if (signal != null) {
                handleSignal(signal)
            }
            
            // 执行下一步（委托给 CodingAgentExecutor）
            val step = executeNextStep()
            
            // 记录事件
            recordEvent("StepCompleted", step)
            
            // 创建检查点（每 N 步）
            if (shouldCheckpoint()) {
                createCheckpoint()
            }
        }
    }
    
    // 等待外部信号（暂停执行）
    suspend fun waitForSignal(signalName: String, timeoutMs: Long): WorkflowSignal {
        recordEvent("WaitingForSignal", mapOf("signalName" to signalName))
        
        // 创建检查点（进入休眠状态）
        createCheckpoint()
        
        // 阻塞等待信号（通过 Channel 或 suspend）
        val signal = withTimeout(timeoutMs) {
            signalQueue.await(workflowId, signalName)
        }
        
        recordEvent("SignalReceived", signal)
        return signal
    }
    
    private suspend fun recordEvent(type: String, data: Any) {
        val event = WorkflowEvent(
            workflowId = workflowId,
            eventType = type,
            eventData = Json.encodeToString(data),
            timestamp = System.currentTimeMillis()
        )
        eventStore.appendEvent(event)
    }
    
    private suspend fun createCheckpoint() {
        val checkpoint = WorkflowCheckpoint(
            workflowId = workflowId,
            sequenceNumber = eventStore.getLatestSequence(workflowId),
            state = Json.encodeToString(currentState),
            createdAt = System.currentTimeMillis()
        )
        checkpointManager.save(checkpoint)
    }
}
```

### 3.4 确定性重放的关键设计

**问题**: LLM 调用是非确定性的，如何实现确定性重放？

**解决方案**: Activity 模式

```kotlin
// 1. 将 LLM 调用封装为 Activity
suspend fun callLLM(prompt: String): String {
    // 检查是否在重放模式
    if (isReplaying()) {
        // 从事件历史中读取结果
        return getHistoricalResult("LLMCall")
    }
    
    // 实际调用 LLM
    val result = llmService.chat(prompt)
    
    // 记录结果事件
    recordActivityResult("LLMCall", result)
    
    return result
}

// 2. Tool 调用也是 Activity
suspend fun executeTool(toolName: String, params: Map<String, Any>): ToolResult {
    if (isReplaying()) {
        return getHistoricalResult("ToolCall:$toolName")
    }
    
    val result = toolRegistry.execute(toolName, params)
    recordActivityResult("ToolCall:$toolName", result)
    
    return result
}
```

**重放逻辑**:
```kotlin
private fun isReplaying(): Boolean {
    // 当前执行的步骤数 < 历史事件中记录的步骤数
    return currentState.agentSteps.size < historicalSteps.size
}

private fun getHistoricalResult(activityType: String): Any {
    // 从事件历史中查找对应的结果
    val stepIndex = currentState.agentSteps.size
    val historicalEvent = historicalEvents.find { 
        it.sequenceNumber == stepIndex && it.eventType == activityType 
    }
    return Json.decodeFromString(historicalEvent.eventData)
}
```

---

## 四、实施路线图

### Phase 1: 基础设施（2-3 周）

#### 1.1 数据库设计与实现
**优先级**: 🔴 P0  
**工作量**: 5 天

**任务**:
- [ ] 设计数据库 Schema（workflow_events, workflow_checkpoints, workflow_signals, workflow_metadata）
- [ ] 实现 `EventStore` 接口（SQLite 版本，用于 mpp-server）
- [ ] 实现 `CheckpointManager`
- [ ] 编写单元测试

**文件位置**:
```
mpp-core/src/commonMain/kotlin/cc/unitmesh/workflow/
├── EventStore.kt                 # 接口定义
├── CheckpointManager.kt
├── models/
│   ├── WorkflowEvent.kt
│   ├── WorkflowCheckpoint.kt
│   └── WorkflowState.kt
```

```
mpp-core/src/jvmMain/kotlin/cc/unitmesh/workflow/
├── EventStoreImpl.kt             # SQLite 实现
└── CheckpointManagerImpl.kt
```

**Schema 示例**:
```sql
-- mpp-server/src/main/resources/db/migration/V1__workflow_tables.sql

CREATE TABLE workflow_events (
    id TEXT PRIMARY KEY,
    workflow_id TEXT NOT NULL,
    sequence_number INTEGER NOT NULL,
    event_type TEXT NOT NULL,
    event_data TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    checkpoint_id TEXT,
    UNIQUE(workflow_id, sequence_number)
);

CREATE INDEX idx_workflow_events_workflow_id ON workflow_events(workflow_id);
CREATE INDEX idx_workflow_events_sequence ON workflow_events(workflow_id, sequence_number);

CREATE TABLE workflow_checkpoints (
    id TEXT PRIMARY KEY,
    workflow_id TEXT NOT NULL,
    sequence_number INTEGER NOT NULL,
    state TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE INDEX idx_workflow_checkpoints_workflow_id ON workflow_checkpoints(workflow_id);

CREATE TABLE workflow_signals (
    id TEXT PRIMARY KEY,
    workflow_id TEXT NOT NULL,
    signal_name TEXT NOT NULL,
    signal_data TEXT NOT NULL,
    received_at INTEGER NOT NULL,
    processed BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_workflow_signals_workflow_id ON workflow_signals(workflow_id);

CREATE TABLE workflow_metadata (
    workflow_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    task TEXT NOT NULL,
    status TEXT NOT NULL,
    owner_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    metadata TEXT
);
```

#### 1.2 SignalQueue 实现
**优先级**: 🔴 P0  
**工作量**: 3 天

**任务**:
- [ ] 实现信号队列（基于数据库 + 内存 Channel）
- [ ] 支持 `enqueue()`, `poll()`, `await()` 方法
- [ ] 支持超时机制

**设计**:
```kotlin
// mpp-core/src/commonMain/kotlin/cc/unitmesh/workflow/SignalQueue.kt

interface SignalQueue {
    suspend fun enqueue(workflowId: String, signal: WorkflowSignal)
    suspend fun poll(workflowId: String): WorkflowSignal?
    suspend fun await(workflowId: String, signalName: String, timeoutMs: Long): WorkflowSignal
}

// 实现：混合模式（DB 持久化 + 内存 Channel 通知）
class HybridSignalQueue(
    private val db: Database,
    private val channels: ConcurrentHashMap<String, Channel<WorkflowSignal>>
) : SignalQueue {
    
    override suspend fun enqueue(workflowId: String, signal: WorkflowSignal) {
        // 1. 持久化到数据库
        db.insertSignal(signal)
        
        // 2. 如果有监听者，立即通知（通过 Channel）
        channels[workflowId]?.send(signal)
    }
    
    override suspend fun await(workflowId: String, signalName: String, timeoutMs: Long): WorkflowSignal {
        // 1. 先检查数据库中是否已有信号
        val existingSignal = db.getUnprocessedSignal(workflowId, signalName)
        if (existingSignal != null) {
            db.markSignalAsProcessed(existingSignal.id)
            return existingSignal
        }
        
        // 2. 创建 Channel 监听
        val channel = channels.getOrPut(workflowId) { Channel(Channel.BUFFERED) }
        
        // 3. 等待信号（带超时）
        return withTimeout(timeoutMs) {
            while (true) {
                val signal = channel.receive()
                if (signal.name == signalName) {
                    db.markSignalAsProcessed(signal.id)
                    return@withTimeout signal
                }
            }
        }
    }
}
```

#### 1.3 WorkflowEngine 核心
**优先级**: 🔴 P0  
**工作量**: 5 天

**任务**:
- [ ] 实现 `WorkflowEngine` 类
- [ ] 实现 `startWorkflow()`, `resumeWorkflow()`, `sendSignal()`
- [ ] 实现状态恢复逻辑（从 Checkpoint + 增量事件）
- [ ] 集成到 mpp-server 的 Ktor 路由

**关键代码**:
```kotlin
// mpp-server/src/main/kotlin/cc/unitmesh/server/workflow/WorkflowEngine.kt

class WorkflowEngine(
    private val eventStore: EventStore,
    private val checkpointManager: CheckpointManager,
    private val signalQueue: SignalQueue
) {
    private val activeWorkflows = ConcurrentHashMap<String, Job>()
    
    suspend fun startWorkflow(request: StartWorkflowRequest): String {
        val workflowId = UUID.randomUUID().toString()
        
        // 记录启动事件
        eventStore.appendEvent(
            WorkflowEvent(
                id = UUID.randomUUID().toString(),
                workflowId = workflowId,
                sequenceNumber = 1,
                eventType = "WorkflowStarted",
                eventData = Json.encodeToString(request),
                timestamp = System.currentTimeMillis()
            )
        )
        
        // 启动协程执行
        val job = GlobalScope.launch {
            executeWorkflow(workflowId, request)
        }
        
        activeWorkflows[workflowId] = job
        
        return workflowId
    }
    
    private suspend fun executeWorkflow(workflowId: String, request: StartWorkflowRequest) {
        try {
            // 恢复或创建初始状态
            val state = recoverState(workflowId)
            
            // 创建持久化执行器
            val executor = DurableAgentExecutor(
                workflowId = workflowId,
                initialState = state,
                eventStore = eventStore,
                checkpointManager = checkpointManager,
                signalQueue = signalQueue
            )
            
            // 执行
            executor.execute(request)
            
        } catch (e: Exception) {
            logger.error(e) { "Workflow $workflowId failed" }
            eventStore.appendEvent(
                WorkflowEvent(
                    id = UUID.randomUUID().toString(),
                    workflowId = workflowId,
                    sequenceNumber = eventStore.getLatestSequence(workflowId) + 1,
                    eventType = "WorkflowFailed",
                    eventData = Json.encodeToString(mapOf("error" to e.message)),
                    timestamp = System.currentTimeMillis()
                )
            )
        } finally {
            activeWorkflows.remove(workflowId)
        }
    }
    
    private suspend fun recoverState(workflowId: String): WorkflowState {
        // 1. 获取最新检查点
        val checkpoint = checkpointManager.getLatest(workflowId)
        
        if (checkpoint == null) {
            // 全新工作流
            return WorkflowState.initial(workflowId)
        }
        
        // 2. 从检查点恢复
        val state = Json.decodeFromString<WorkflowState>(checkpoint.state)
        
        // 3. 重放检查点之后的事件
        val events = eventStore.getEvents(workflowId, fromSequence = checkpoint.sequenceNumber + 1)
        
        return applyEvents(state, events)
    }
    
    private fun applyEvents(initialState: WorkflowState, events: List<WorkflowEvent>): WorkflowState {
        var state = initialState
        
        events.forEach { event ->
            state = when (event.eventType) {
                "StepCompleted" -> {
                    val step = Json.decodeFromString<AgentStep>(event.eventData)
                    state.copy(agentSteps = state.agentSteps + step)
                }
                "IterationCompleted" -> {
                    state.copy(currentIteration = state.currentIteration + 1)
                }
                // ... 其他事件类型
                else -> state
            }
        }
        
        return state
    }
}
```

### Phase 2: DurableAgentExecutor（2-3 周）

#### 2.1 包装 CodingAgentExecutor
**优先级**: 🔴 P0  
**工作量**: 5 天

**任务**:
- [ ] 创建 `DurableAgentExecutor` 包装器
- [ ] 在每个关键步骤后调用 `recordEvent()`
- [ ] 实现重放模式（`isReplaying()` 判断）

**设计思路**:
```kotlin
// mpp-core/src/commonMain/kotlin/cc/unitmesh/workflow/DurableAgentExecutor.kt

class DurableAgentExecutor(
    private val workflowId: String,
    private val initialState: WorkflowState,
    private val eventStore: EventStore,
    private val checkpointManager: CheckpointManager,
    private val signalQueue: SignalQueue
) {
    private var currentState = initialState
    private val underlyingExecutor = CodingAgentExecutor(...)
    
    // 历史事件（用于判断是否在重放）
    private lateinit var historicalEvents: List<WorkflowEvent>
    private var replayIndex = 0
    
    suspend fun execute(request: StartWorkflowRequest) {
        // 1. 加载历史事件
        historicalEvents = eventStore.getEvents(workflowId)
        
        // 2. 判断是否在恢复模式
        val isRecovery = historicalEvents.any { it.eventType == "StepCompleted" }
        
        if (isRecovery) {
            logger.info { "Recovering workflow $workflowId from ${historicalEvents.size} events" }
        }
        
        // 3. 委托给 CodingAgentExecutor，但拦截关键调用
        val task = AgentTask(
            requirement = request.task,
            projectPath = request.projectPath
        )
        
        // 执行主循环（修改 CodingAgentExecutor 以支持步骤级回调）
        underlyingExecutor.executeWithCallback(task) { step ->
            handleStep(step)
        }
    }
    
    private suspend fun handleStep(step: AgentStep) {
        // 如果在重放模式，跳过实际执行
        if (isReplaying()) {
            replayIndex++
            return
        }
        
        // 记录事件
        recordEvent("StepCompleted", step)
        currentState = currentState.copy(
            agentSteps = currentState.agentSteps + step
        )
        
        // 创建检查点
        if (shouldCheckpoint()) {
            createCheckpoint()
        }
    }
    
    private fun isReplaying(): Boolean {
        return replayIndex < historicalEvents.count { it.eventType == "StepCompleted" }
    }
}
```

**注意**: 需要修改 `CodingAgentExecutor` 以支持步骤级回调，或者完全重写执行逻辑。

#### 2.2 Activity 包装器
**优先级**: 🔴 P0  
**工作量**: 4 天

**任务**:
- [ ] 创建 `DurableActivity` 基类
- [ ] 包装 LLM 调用为 `LLMActivity`
- [ ] 包装 Tool 调用为 `ToolActivity`
- [ ] 实现确定性重放逻辑

**设计**:
```kotlin
// mpp-core/src/commonMain/kotlin/cc/unitmesh/workflow/activity/DurableActivity.kt

abstract class DurableActivity<I, O>(
    private val activityName: String,
    private val executor: DurableAgentExecutor
) {
    suspend fun execute(input: I): O {
        // 1. 检查是否在重放模式
        if (executor.isReplaying()) {
            // 从历史中读取结果
            val historicalResult = executor.getHistoricalResult(activityName)
            return Json.decodeFromString(historicalResult)
        }
        
        // 2. 实际执行
        val result = executeInternal(input)
        
        // 3. 记录结果
        executor.recordActivityResult(activityName, result)
        
        return result
    }
    
    protected abstract suspend fun executeInternal(input: I): O
}

// LLM 调用
class LLMActivity(executor: DurableAgentExecutor) : DurableActivity<String, String>("LLMCall", executor) {
    override suspend fun executeInternal(input: String): String {
        return llmService.chat(input)
    }
}

// Tool 调用
class ToolActivity(
    private val toolName: String,
    executor: DurableAgentExecutor
) : DurableActivity<Map<String, Any>, ToolResult>("Tool:$toolName", executor) {
    override suspend fun executeInternal(input: Map<String, Any>): ToolResult {
        return toolRegistry.execute(toolName, input)
    }
}
```

#### 2.3 暂停/恢复机制
**优先级**: 🟡 P1  
**工作量**: 3 天

**任务**:
- [ ] 实现 `waitForSignal()` 原语
- [ ] 支持超时机制
- [ ] 测试暂停/恢复流程

**示例用法**:
```kotlin
// 在 Agent 代码中使用
suspend fun executeCodeReview(code: String) {
    // 1. 生成审查报告
    val report = llmActivity.execute("Review this code: $code")
    
    // 2. 发送通知
    sendNotificationActivity.execute("Code review ready for approval")
    
    // 3. 等待人类审批（可能数天）
    val approval = waitForSignal("CodeReviewApproval", timeoutMs = 7 * 24 * 3600 * 1000L)
    
    // 4. 根据审批结果执行后续操作
    if (approval.data["approved"] == true) {
        mergeCodeActivity.execute(code)
    } else {
        refactorCodeActivity.execute(code, approval.data["feedback"] as String)
    }
}
```

### Phase 3: API 与集成（1-2 周）

#### 3.1 REST API 实现
**优先级**: 🔴 P0  
**工作量**: 4 天

**任务**:
- [ ] 实现 WorkflowController
- [ ] 添加路由：`POST /api/workflows/start`
- [ ] 添加路由：`POST /api/workflows/{id}/signal`
- [ ] 添加路由：`GET /api/workflows/{id}/query`
- [ ] 添加路由：`GET /api/workflows/{id}/events` (SSE)

**代码示例**:
```kotlin
// mpp-server/src/main/kotlin/cc/unitmesh/server/workflow/WorkflowController.kt

fun Route.workflowRoutes(engine: WorkflowEngine) {
    route("/api/workflows") {
        // 启动工作流
        post("/start") {
            val request = call.receive<StartWorkflowRequest>()
            val workflowId = engine.startWorkflow(request)
            call.respond(HttpStatusCode.Created, mapOf("workflowId" to workflowId))
        }
        
        // 发送信号
        post("/{id}/signal") {
            val workflowId = call.parameters["id"]!!
            val signal = call.receive<WorkflowSignal>()
            engine.sendSignal(workflowId, signal)
            call.respond(HttpStatusCode.OK)
        }
        
        // 查询状态
        get("/{id}/query") {
            val workflowId = call.parameters["id"]!!
            val state = engine.queryState(workflowId)
            call.respond(state)
        }
        
        // SSE 事件流
        get("/{id}/events") {
            val workflowId = call.parameters["id"]!!
            call.respondSse {
                engine.subscribeToEvents(workflowId).collect { event ->
                    send(event)
                }
            }
        }
    }
}
```

#### 3.2 与现有 SessionManager 集成
**优先级**: 🟡 P1  
**工作量**: 2 天

**策略**: 
- 保留现有 SessionManager 用于简单的会话管理
- WorkflowEngine 用于需要持久化的复杂任务
- 逐步迁移现有 API 到 WorkflowEngine

### Phase 4: 高级特性（2-3 周）

#### 4.1 子工作流支持
**优先级**: 🟢 P2  
**工作量**: 4 天

**设计**:
```kotlin
// 启动子工作流
suspend fun startChildWorkflow(
    childRequest: StartWorkflowRequest,
    parentWorkflowId: String
): String {
    val childWorkflowId = engine.startWorkflow(childRequest)
    
    // 记录父子关系
    recordEvent("ChildWorkflowStarted", mapOf(
        "childWorkflowId" to childWorkflowId,
        "parentWorkflowId" to parentWorkflowId
    ))
    
    return childWorkflowId
}

// 等待子工作流完成
suspend fun awaitChildWorkflow(childWorkflowId: String): WorkflowResult {
    // 监听子工作流的完成事件
    return engine.awaitWorkflowCompletion(childWorkflowId)
}
```

#### 4.2 侧信道流式传输
**优先级**: 🔵 P3  
**工作量**: 3 天

**设计**:
```kotlin
// 使用 Redis Pub/Sub（或内存 Channel）
class StreamingLLMActivity(
    executor: DurableAgentExecutor,
    private val redisPublisher: RedisPublisher
) : DurableActivity<String, String>("StreamingLLM", executor) {
    
    override suspend fun executeInternal(input: String): String {
        val channelId = "llm:${executor.workflowId}"
        val fullResponse = StringBuilder()
        
        // 流式调用 LLM
        llmService.chatStreaming(input) { token ->
            // 实时发布到 Redis
            redisPublisher.publish(channelId, token)
            fullResponse.append(token)
        }
        
        // 返回完整结果（持久化）
        return fullResponse.toString()
    }
}
```

#### 4.3 版本控制与灰度发布
**优先级**: 🔵 P3  
**工作量**: 5 天

**设计**:
- 工作流代码带版本号
- 新版本代码通过不同的 Worker 池执行
- 旧工作流继续在旧版本上运行

### Phase 5: 测试与优化（1-2 周）

#### 5.1 单元测试
- [ ] EventStore 测试
- [ ] CheckpointManager 测试
- [ ] WorkflowEngine 测试
- [ ] DurableAgentExecutor 测试

#### 5.2 集成测试
- [ ] 端到端工作流测试
- [ ] 崩溃恢复测试
- [ ] Signal/Query 测试
- [ ] 并发测试

#### 5.3 性能优化
- [ ] 数据库索引优化
- [ ] 检查点压缩
- [ ] 事件批量写入

---

## 五、技术选型与依赖

### 5.1 数据库选型

| 方案 | 优点 | 缺点 | 推荐场景 |
|-----|-----|-----|---------|
| **SQLite** | 零配置、轻量、适合单机 | 并发写入性能有限 | 开发/测试/单机部署 |
| **PostgreSQL** | 高性能、支持高并发、ACID | 需要独立部署和维护 | 生产环境（推荐） |
| **内存 + 定期快照** | 极高性能 | 数据丢失风险 | 临时任务、可重放场景 |

**推荐**: 
- **Phase 1-2**: SQLite（快速开发）
- **Phase 3+**: PostgreSQL（生产级）

### 5.2 新增依赖

```kotlin
// mpp-server/build.gradle.kts
dependencies {
    // 数据库
    implementation("org.jetbrains.exposed:exposed-core:0.47.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.47.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.47.0")
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")  // 或 PostgreSQL
    
    // 数据库迁移（可选）
    implementation("org.flywaydb:flyway-core:9.22.3")
    
    // Redis（可选，用于侧信道）
    implementation("io.lettuce:lettuce-core:6.3.0")
}
```

### 5.3 无需新增的能力

✅ 已有且可复用：
- Kotlin Coroutines（异步执行）
- Kotlinx Serialization（事件序列化）
- Ktor SSE（事件流）
- SQLDelight（多平台数据库，可替代 Exposed）

---

## 六、风险评估与对策

### 6.1 技术风险

| 风险 | 影响 | 概率 | 对策 |
|-----|-----|-----|-----|
| **确定性重放实现复杂** | 🔴 高 | 中 | 先实现简单版本（只重放成功的步骤），逐步完善 |
| **数据库性能瓶颈** | 🟡 中 | 低 | 使用 PostgreSQL + 批量写入 + 索引优化 |
| **状态快照过大** | 🟡 中 | 中 | 限制对话历史长度 + 压缩 + 只保留关键状态 |
| **并发控制复杂** | 🟡 中 | 低 | 使用数据库事务 + 乐观锁 |

### 6.2 工程风险

| 风险 | 影响 | 概率 | 对策 |
|-----|-----|-----|-----|
| **开发周期超预期** | 🟡 中 | 高 | 采用迭代式开发，Phase 1 完成后即可小范围使用 |
| **与现有代码冲突** | 🟡 中 | 中 | 通过包装器模式最小化侵入性 |
| **测试覆盖不足** | 🟡 中 | 中 | 优先编写集成测试，覆盖关键路径 |

### 6.3 业务风险

| 风险 | 影响 | 概率 | 对策 |
|-----|-----|-----|-----|
| **功能复杂度影响用户体验** | 🟢 低 | 低 | API 设计保持简洁，复杂性隐藏在内部 |
| **迁移成本高** | 🟡 中 | 低 | 新旧系统并行，逐步迁移 |

---

## 七、成功标准

### 7.1 Phase 1 成功标准
- [ ] 能够持久化事件到数据库
- [ ] 能够创建和恢复检查点
- [ ] 服务器重启后工作流自动恢复

### 7.2 Phase 2 成功标准
- [ ] DurableAgentExecutor 能够执行完整的 Agent 任务
- [ ] LLM 调用结果能够确定性重放
- [ ] 支持 `waitForSignal()` 暂停/恢复

### 7.3 Phase 3 成功标准
- [ ] REST API 全部实现并通过测试
- [ ] 前端（Android/Web）能够通过 API 控制工作流

### 7.4 最终成功标准
- [ ] 代码审查 Agent 能够运行数天并恢复
- [ ] 多 Agent 协作任务能够正确执行
- [ ] 系统崩溃后所有工作流自动恢复并继续执行

---

## 八、后续扩展计划

### 8.1 短期（3 个月内）
- [ ] 实现 Worker 池，支持多服务器部署
- [ ] 添加工作流监控面板（Temporal UI 风格）
- [ ] 支持工作流取消和暂停

### 8.2 中期（6 个月内）
- [ ] 实现 Cron 定时工作流
- [ ] 支持工作流模板和复用
- [ ] 添加 Metrics 和 Tracing

### 8.3 长期（1 年内）
- [ ] 支持分布式工作流（跨多个 Server）
- [ ] 实现类似 Temporal Cloud 的 SaaS 版本
- [ ] 支持 Workflow-as-Code（DSL）

---

## 九、参考资料

### 9.1 内部文档
- `docs/sync/Temporal 助力 AI Agent 架构.md` - Temporal 机制详解
- `mpp-server/README.md` - mpp-server 架构说明
- `mpp-core/README.md` - mpp-core 核心能力

### 9.2 外部参考
- [Temporal Documentation](https://docs.temporal.io/)
- [Event Sourcing Pattern](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)

---

## 附录 A: 数据库 Schema 完整定义

```sql
-- workflow_events: 事件溯源存储
CREATE TABLE workflow_events (
    id TEXT PRIMARY KEY,
    workflow_id TEXT NOT NULL,
    sequence_number INTEGER NOT NULL,
    event_type TEXT NOT NULL,
    event_data TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    checkpoint_id TEXT,
    created_by TEXT,
    UNIQUE(workflow_id, sequence_number)
);

CREATE INDEX idx_workflow_events_workflow_id ON workflow_events(workflow_id);
CREATE INDEX idx_workflow_events_sequence ON workflow_events(workflow_id, sequence_number);
CREATE INDEX idx_workflow_events_timestamp ON workflow_events(timestamp);

-- workflow_checkpoints: 检查点存储
CREATE TABLE workflow_checkpoints (
    id TEXT PRIMARY KEY,
    workflow_id TEXT NOT NULL,
    sequence_number INTEGER NOT NULL,
    state TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    size_bytes INTEGER NOT NULL
);

CREATE INDEX idx_workflow_checkpoints_workflow_id ON workflow_checkpoints(workflow_id);
CREATE INDEX idx_workflow_checkpoints_sequence ON workflow_checkpoints(workflow_id, sequence_number DESC);

-- workflow_signals: 信号队列
CREATE TABLE workflow_signals (
    id TEXT PRIMARY KEY,
    workflow_id TEXT NOT NULL,
    signal_name TEXT NOT NULL,
    signal_data TEXT NOT NULL,
    received_at INTEGER NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    processed_at INTEGER
);

CREATE INDEX idx_workflow_signals_workflow_id ON workflow_signals(workflow_id, processed);

-- workflow_metadata: 工作流元数据
CREATE TABLE workflow_metadata (
    workflow_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    task TEXT NOT NULL,
    status TEXT NOT NULL,
    owner_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    completed_at INTEGER,
    metadata TEXT,
    parent_workflow_id TEXT,
    version TEXT
);

CREATE INDEX idx_workflow_metadata_status ON workflow_metadata(status);
CREATE INDEX idx_workflow_metadata_owner ON workflow_metadata(owner_id);
CREATE INDEX idx_workflow_metadata_parent ON workflow_metadata(parent_workflow_id);

-- workflow_children: 子工作流关系
CREATE TABLE workflow_children (
    parent_workflow_id TEXT NOT NULL,
    child_workflow_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    PRIMARY KEY (parent_workflow_id, child_workflow_id)
);
```

---

## 附录 B: API 接口定义

### B.1 启动工作流
```http
POST /api/workflows/start
Content-Type: application/json

{
  "projectId": "proj-123",
  "task": "Implement user authentication",
  "userId": "user-456",
  "metadata": {
    "maxIterations": 100,
    "llmConfig": "{...}"
  }
}

Response 201:
{
  "workflowId": "wf-789",
  "status": "PENDING",
  "createdAt": 1733673600000
}
```

### B.2 发送信号
```http
POST /api/workflows/{workflowId}/signal
Content-Type: application/json

{
  "signalName": "CodeReviewApproval",
  "data": {
    "approved": true,
    "feedback": "Looks good!"
  }
}

Response 200:
{
  "success": true
}
```

### B.3 查询状态
```http
GET /api/workflows/{workflowId}/query

Response 200:
{
  "workflowId": "wf-789",
  "status": "RUNNING",
  "currentIteration": 15,
  "maxIterations": 100,
  "agentSteps": [...],
  "lastUpdate": 1733673700000
}
```

### B.4 订阅事件流（SSE）
```http
GET /api/workflows/{workflowId}/events

Response:
event: iteration
data: {"current": 1, "max": 100}

event: llm_chunk
data: {"chunk": "Based on your requirements..."}

event: tool_call
data: {"toolName": "read_file", "params": "{...}"}

event: complete
data: {"success": true, "iterations": 25}
```

---

## 总结

这份实施计划提供了一个**渐进式、低风险**的路径，在不引入 Temporal 库的前提下，实现核心的持久化工作流编排能力。

**关键优势**:
1. ✅ **复用现有基础设施**: SessionManager、AgentEvent、SQLDelight
2. ✅ **最小化侵入性**: 通过包装器模式，无需大规模重构现有代码
3. ✅ **灵活的实施节奏**: Phase 1 完成后即可小范围试用，逐步推广
4. ✅ **与 KMP 架构匹配**: 使用 `expect`/`actual` 支持跨平台

**核心挑战**:
1. ⚠️ **确定性重放的工程实现**（需要仔细设计 Activity 包装器）
2. ⚠️ **数据库性能优化**（建议生产环境使用 PostgreSQL）

**下一步行动**:
- 确认技术选型（SQLite vs PostgreSQL）
- 启动 Phase 1 开发（2-3 周）
- 设计详细的数据库 Schema
- 搭建开发环境和测试框架

---

**文档版本**: v1.0  
**最后更新**: 2025-12-08  
**作者**: AI Architect  
**审阅**: 待审阅

