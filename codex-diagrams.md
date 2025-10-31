# Codex 架构分析 - 核心图表集

本文件包含 Codex 架构分析的关键 Mermaid 图表，供快速参考。

## 1. 系统架构全景图

```mermaid
flowchart TB
    subgraph "Client Layer"
        CLI[CLI Entry]
        TUI[TUI Interface]
        EXEC[Exec Mode]
    end
    
    subgraph "Core Engine - codex-core"
        CODEX[Codex Session]
        CONV[ConversationManager]
        SUB[Submission Loop]
        EVENT[Event Stream]
    end
    
    subgraph "Agent Orchestration"
        ORCH[ToolOrchestrator]
        ROUTER[ToolRouter]
        REGISTRY[ToolRegistry]
        PARALLEL[ToolCallRuntime]
    end
    
    subgraph "Tool System"
        SHELL[ShellHandler]
        PATCH[ApplyPatchHandler]
        UNIFIED[UnifiedExecHandler]
        GREP[GrepHandler]
        READ[ReadFileHandler]
        MCP[McpHandler]
    end
    
    subgraph "Execution & Sandbox"
        SANDBOX[SandboxManager]
        EXEC_ENV[ExecEnv]
        APPROVAL[ApprovalStore]
        MACOS[macOS Seatbelt]
        LINUX[Linux Seccomp]
        WINDOWS[Windows Token]
    end
    
    subgraph "Context & State"
        STATE[SessionState]
        HISTORY[ConversationHistory]
        TRACKER[TurnDiffTracker]
        MCP_MGR[McpConnectionManager]
    end
    
    CLI --> CODEX
    TUI --> CODEX
    EXEC --> CODEX
    
    CODEX --> CONV
    CODEX --> SUB
    CODEX --> EVENT
    
    SUB --> ORCH
    ORCH --> ROUTER
    ROUTER --> REGISTRY
    REGISTRY --> PARALLEL
    
    PARALLEL --> SHELL
    PARALLEL --> PATCH
    PARALLEL --> UNIFIED
    PARALLEL --> GREP
    PARALLEL --> READ
    PARALLEL --> MCP
    
    SHELL --> SANDBOX
    SANDBOX --> EXEC_ENV
    EXEC_ENV --> APPROVAL
    
    APPROVAL --> MACOS
    APPROVAL --> LINUX
    APPROVAL --> WINDOWS
    
    CODEX --> STATE
    STATE --> HISTORY
    STATE --> TRACKER
    STATE --> MCP_MGR
```

## 2. Queue Pair 通信模式

```mermaid
flowchart LR
    subgraph "Client"
        UI[User Interface]
    end
    
    subgraph "Codex Instance"
        TX_SUB[tx_sub<br/>Submission Sender]
        RX_EVENT[rx_event<br/>Event Receiver]
    end
    
    subgraph "Session Loop"
        RX_SUB[rx_sub<br/>Submission Receiver]
        TX_EVENT[tx_event<br/>Event Sender]
        PROCESS[Processing Logic]
    end
    
    UI -->|submit Op| TX_SUB
    TX_SUB -.->|async_channel| RX_SUB
    RX_SUB --> PROCESS
    PROCESS --> TX_EVENT
    TX_EVENT -.->|async_channel| RX_EVENT
    RX_EVENT -->|next_event| UI
```

## 3. Tool Orchestrator 执行流程

```mermaid
flowchart TD
    START([Tool Call Request]) --> APPROVAL{需要审批?}
    
    APPROVAL -->|是| ASK_USER[请求用户审批]
    APPROVAL -->|否| SELECT_SANDBOX[选择沙箱策略]
    
    ASK_USER --> CHECK_DECISION{审批结果}
    CHECK_DECISION -->|拒绝| REJECT([返回拒绝错误])
    CHECK_DECISION -->|批准| SELECT_SANDBOX
    
    SELECT_SANDBOX --> FIRST_TRY[第一次尝试执行]
    
    FIRST_TRY --> CHECK_RESULT{执行结果}
    CHECK_RESULT -->|成功| SUCCESS([返回成功结果])
    CHECK_RESULT -->|其他错误| ERROR([返回错误])
    CHECK_RESULT -->|沙箱拒绝| CHECK_ESCALATE{允许升级?}
    
    CHECK_ESCALATE -->|否| SANDBOX_ERROR([返回沙箱错误])
    CHECK_ESCALATE -->|是| ASK_RETRY[请求无沙箱重试审批]
    
    ASK_RETRY --> CHECK_RETRY{审批结果}
    CHECK_RETRY -->|拒绝| REJECT
    CHECK_RETRY -->|批准| RETRY[无沙箱重试]
    
    RETRY --> FINAL_RESULT{执行结果}
    FINAL_RESULT -->|成功| SUCCESS
    FINAL_RESULT -->|失败| ERROR
```

## 4. 并行工具执行

```mermaid
flowchart TB
    subgraph "ToolCallRuntime"
        GATE[Tool Call Gate<br/>ReadinessFlag]
        LOCK[RwLock Parallel Execution]
    end
    
    subgraph "Parallel Tools"
        P1[read_file]
        P2[list_dir]
        P3[grep_files]
    end
    
    subgraph "Serial Tools"
        S1[local_shell]
        S2[apply_patch]
        S3[unified_exec]
    end
    
    GATE --> CHECK{支持并行?}
    CHECK -->|是| READ_LOCK[获取读锁<br/>允许多个并发]
    CHECK -->|否| WRITE_LOCK[获取写锁<br/>独占执行]
    
    READ_LOCK --> P1
    READ_LOCK --> P2
    READ_LOCK --> P3
    
    WRITE_LOCK --> S1
    WRITE_LOCK --> S2
    WRITE_LOCK --> S3
```

## 5. MCP 工具管理

```mermaid
flowchart TB
    subgraph "McpConnectionManager"
        CLIENTS[clients<br/>HashMap Server to Client]
        TOOLS[tools<br/>HashMap Qualified Name to ToolInfo]
        FILTERS[tool_filters<br/>HashMap Server to Filter]
    end
    
    subgraph "MCP Server 1"
        S1_T1[Tool: read_file]
        S1_T2[Tool: write_file]
    end
    
    subgraph "MCP Server 2"
        S2_T1[Tool: search_code]
        S2_T2[Tool: analyze]
    end
    
    subgraph "Qualified Tool Names"
        Q1["mcp__server1__read_file"]
        Q2["mcp__server1__write_file"]
        Q3["mcp__server2__search_code"]
        Q4["mcp__server2__analyze"]
    end
    
    CLIENTS --> S1_T1
    CLIENTS --> S1_T2
    CLIENTS --> S2_T1
    CLIENTS --> S2_T2
    
    S1_T1 --> Q1
    S1_T2 --> Q2
    S2_T1 --> Q3
    S2_T2 --> Q4
    
    Q1 --> TOOLS
    Q2 --> TOOLS
    Q3 --> TOOLS
    Q4 --> TOOLS
```

## 6. 会话状态管理

```mermaid
flowchart TB
    subgraph "ConversationManager"
        NEW[new_conversation]
        RESUME[resume_conversation]
        GET[get_conversation]
    end
    
    subgraph "CodexConversation"
        CODEX_INST[Codex Instance]
        ROLLOUT[RolloutRecorder]
    end
    
    subgraph "Session"
        STATE[SessionState<br/>Mutex Protected]
        ACTIVE[ActiveTurn<br/>Mutex Protected]
        SERVICES[SessionServices<br/>Shared]
    end
    
    subgraph "SessionState"
        CONFIG[SessionConfiguration]
        HISTORY[ConversationHistory]
        RATE[RateLimitSnapshot]
    end
    
    subgraph "ConversationHistory"
        ITEMS[ResponseItems]
        TOKENS[TokenUsageInfo]
    end
    
    NEW --> CODEX_INST
    RESUME --> CODEX_INST
    GET --> CODEX_INST
    
    CODEX_INST --> STATE
    CODEX_INST --> ACTIVE
    CODEX_INST --> SERVICES
    
    STATE --> CONFIG
    STATE --> HISTORY
    STATE --> RATE
    
    HISTORY --> ITEMS
    HISTORY --> TOKENS
```

## 7. 多层沙箱策略

```mermaid
flowchart TD
    START([命令执行请求]) --> POLICY{SandboxPolicy}
    
    POLICY -->|DangerFullAccess| NO_SANDBOX[无沙箱限制]
    POLICY -->|ReadOnly| SELECT_PLATFORM1{操作系统}
    POLICY -->|WorkspaceWrite| SELECT_PLATFORM2{操作系统}
    
    SELECT_PLATFORM1 -->|macOS| SEATBELT_RO[Seatbelt<br/>只读策略]
    SELECT_PLATFORM1 -->|Linux| SECCOMP_RO[Seccomp<br/>只读策略]
    SELECT_PLATFORM1 -->|Windows| TOKEN_RO[RestrictedToken<br/>只读策略]
    
    SELECT_PLATFORM2 -->|macOS| SEATBELT_RW[Seatbelt<br/>工作区写策略]
    SELECT_PLATFORM2 -->|Linux| SECCOMP_RW[Seccomp<br/>工作区写策略]
    SELECT_PLATFORM2 -->|Windows| TOKEN_RW[RestrictedToken<br/>工作区写策略]
    
    SEATBELT_RO --> EXEC[执行命令]
    SECCOMP_RO --> EXEC
    TOKEN_RO --> EXEC
    SEATBELT_RW --> EXEC
    SECCOMP_RW --> EXEC
    TOKEN_RW --> EXEC
    NO_SANDBOX --> EXEC
    
    EXEC --> RESULT{执行结果}
    RESULT -->|成功| SUCCESS([返回输出])
    RESULT -->|沙箱拒绝| DENIED([SandboxErr Denied])
    RESULT -->|其他错误| ERROR([其他错误])
```

## 8. 架构对比：Codex vs AutoDev

```mermaid
flowchart TB
    subgraph "Codex Strengths"
        C1[Queue Pair 通信<br/>异步解耦]
        C2[Tool Orchestrator<br/>统一流程]
        C3[RwLock 并发<br/>性能优化]
        C4[MCP 生态<br/>可扩展性]
        C5[Rollout 持久化<br/>可恢复性]
    end
    
    subgraph "AutoDev Current"
        A1[同步回调<br/>紧耦合]
        A2[分散实现<br/>不一致]
        A3[串行执行<br/>低效]
        A4[无标准协议<br/>难扩展]
        A5[内存状态<br/>易丢失]
    end
    
    subgraph "Target Architecture"
        T1[Kotlin Channel<br/>协程通信]
        T2[Standard Pipeline<br/>一致执行]
        T3[ReentrantLock<br/>并行读写]
        T4[MCP Client<br/>统一接入]
        T5[Database/File<br/>持久化]
    end
    
    C1 -.启发.-> T1
    C2 -.启发.-> T2
    C3 -.启发.-> T3
    C4 -.启发.-> T4
    C5 -.启发.-> T5
    
    A1 -->|重构| T1
    A2 -->|重构| T2
    A3 -->|优化| T3
    A4 -->|集成| T4
    A5 -->|增强| T5
    
    style C1 fill:#90EE90
    style C2 fill:#90EE90
    style C3 fill:#87CEEB
    style C4 fill:#87CEEB
    style C5 fill:#FFB6C1
    
    style A1 fill:#FFB6C1
    style A2 fill:#FFB6C1
    style A3 fill:#FFA07A
    style A4 fill:#FFA07A
    style A5 fill:#DDA0DD
    
    style T1 fill:#FFD700
    style T2 fill:#FFD700
    style T3 fill:#FFA500
    style T4 fill:#FFA500
    style T5 fill:#DDA0DD
```

---

*这些图表对应 [codex-architecture-analysis.md](codex-architecture-analysis.md) 的详细分析内容*
