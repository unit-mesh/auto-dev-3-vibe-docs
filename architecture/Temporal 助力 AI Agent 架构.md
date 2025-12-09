# **基于 Temporal 机制构建高韧性分布式系统与 Agentic AI 架构的深度研究报告**

## **1\. 核心机制与范式转换：持久化执行的架构哲学**

在分布式系统的演进历程中，可靠性始终是核心挑战。传统的微服务架构依赖于将状态从应用进程中剥离至数据库，以应对进程的易变性（Volatility）。然而，这种模式导致了业务逻辑与基础设施代码的深度耦合——开发者不得不编写大量的重试逻辑、状态机管理代码以及复杂的队列消费者来处理不可避免的网络抖动和硬件故障。本次深度研究的核心对象 Temporal，代表了一种根本性的范式转换，即“持久化执行”（Durable Execution）。

### **1.1 瞬态执行与持久化执行的二元对立**

在传统的计算模型中，执行是瞬态的。一旦承载进程的容器崩溃或物理节点重启，内存中的调用栈、局部变量和执行进度将瞬间丢失。为了缓解这一问题，架构师们通常引入“数据库即队列”或外部消息中间件（如 Kafka、RabbitMQ）来通过检查点（Checkpointing）机制保存状态。然而，这种修补方案并未触及问题的本质：应用层逻辑依然是脆弱的，必须时刻防备底层的失效。

Temporal 的机制创新在于将“执行”本身视为一种一级资源（First-class Citizen）。它通过事件溯源（Event Sourcing）模式，将应用程序的每一步执行状态——无论是函数调用、变量变更还是外部 API 请求——都记录为不可变的历史事件流（Event History）。当故障发生时，Temporal 并非简单地重启服务，而是通过重放（Replay）历史事件，将工作流（Workflow）恢复到故障前的确切状态 1。这种机制使得开发者能够编写看似同步、单线程的代码，却能在分布式环境中实现跨越数秒至数年的长周期运行，且对底层故障免疫 3。

### **1.2 确定性重放：时间旅行的工程实现**

理解 Temporal 机制的关键在于理解其“重放”逻辑。当一个 Worker 节点崩溃后，任务被重新调度到另一个节点。新的 Worker 并不是从零开始执行，而是从头开始运行代码，但在遇到诸如“执行活动（ExecuteActivity）”或“等待计时器（Sleep）”等指令时，它会首先检查事件历史。

如果历史记录显示该步骤已经完成并有结果，Worker 会直接使用历史中的结果（例如 API 调用的返回值），而跳过实际的执行。这种机制要求工作流代码必须是严格确定性的（Deterministic）。给定相同的输入和历史事件序列，代码必须产生完全相同的命令序列 1。

这一约束带来了深远的架构影响。在设计我们的方案时，任何涉及随机性（如 UUID.randomUUID()）、系统时间（System.currentTimeMillis()）、多线程并发或外部 I/O 的操作，都必须通过 Temporal SDK 提供的封装方法（如 Workflow.sideEffect 或 Workflow.now）进行，或者被隔离在 Activity 中执行。这种强制性的逻辑分离，虽然增加了学习曲线，但也迫使架构师清晰地界定“纯业务逻辑”（Workflow）与“副作用操作”（Activity）的边界，从而显著提升了系统的可测试性和可维护性 5。

### **1.3 服务端架构：分片与一致性模型**

Temporal 服务端（Temporal Server）采用了高度可扩展的架构，其核心组件包括 Frontend、History、Matching 和 Worker Service。其中，History Service 是系统的心脏，负责维护工作流的状态转换和事件存储。为了实现海量并发下的高吞吐量，History Service 采用了基于分片（Sharding）的设计 4。

每个工作流执行（Workflow Execution）通过其 ID 的哈希值被映射到一个特定的分片上。该分片负责处理该工作流的所有事件写入和状态更新。这种设计确保了单个工作流的一致性，同时也实现了水平扩展能力。然而，在设计我们的方案时，必须意识到这种分片机制带来的限制：单个工作流实例的吞吐量是有限的。对于每秒数千次信号（Signal）的高频交互场景，必须采用“实体工作流”（Entity Workflow）模式或分层架构，避免产生“热分片”（Hot Shard）导致性能瓶颈 6。

## ---

**2\. 核心原语与机制化应用设计**

为了基于 Temporal 机制构建我们的方案，必须深入剖析其提供的核心原语（Primitives），并将其映射到具体的业务场景中。这些原语不仅仅是 API，更是构建分布式系统的新型乐高积木。

### **2.1 Workflow：编排与状态的容器**

Workflow 是 Temporal 中的核心抽象，代表了一个端到端的业务流程。在我们的方案中，Workflow 不应仅仅被视为一段脚本，而应被设计为一个有状态的对象或“实体”。

* **长周期运行能力**：Workflow 可以休眠任意时长，不消耗 CPU 资源，仅占用存储。这使得我们可以用一个 Workflow 实例来代表一个用户的完整生命周期（例如：订阅管理、忠诚度计划）。这种模式消除了传统的“定时任务轮询数据库”架构，将被动的轮询转变为主动的事件驱动模型 7。  
* **状态隔离**：每个 Workflow 拥有独立的内存空间和事件历史，天然实现了多租户的数据隔离。这对于处理敏感数据或需要高并发隔离的 Agentic AI 场景至关重要。

### **2.2 Activity：不可靠世界的防腐层**

Activity 是 Workflow 与外部世界交互的唯一桥梁。所有非确定性的操作（调用 LLM、读写数据库、发送邮件）都必须封装在 Activity 中。

* **自动重试与退避**：Temporal 允许为 Activity 配置复杂的重试策略（Retry Policy）。在我们的方案中，对于调用 LLM 等不稳定的外部服务，可以设置指数退避（Exponential Backoff）策略，确保在服务限流或临时中断时系统能够自愈，而无需在业务逻辑中硬编码重试循环 2。  
* **心跳机制（Heartbeating）**：对于耗时较长的 Activity（如视频转码、大数据处理），必须实现心跳机制。Activity 定期向 Server 汇报进度。如果 Worker 崩溃，Server 会立即检测到心跳丢失并重新调度任务，而不是等到超时结束。这对于提升系统的响应速度和资源利用率至关重要 9。

### **2.3 Signal：异步数据注入**

Signal 机制允许外部系统向运行中的 Workflow 发送消息，从而改变其内部状态或触发新的行为。这使得 Workflow 可以表现得像一个长期运行的服务器进程。

* **人机交互（Human-in-the-Loop）**：在 AI Agent 场景中，Agent 可能需要暂停执行，等待人类的审批或输入。通过 Signal，前端 UI 可以将用户的决策异步推送给 Workflow，唤醒沉睡的 Agent 继续执行后续步骤 10。  
* **并发控制**：虽然 Signal 是异步发送的，但 Temporal 保证它们会在 Workflow 的事件循环中按顺序处理。这意味着开发者无需使用锁（Mutex）来处理并发修改状态的问题，大大简化了并发编程的复杂度 12。

### **2.4 Query：同步状态查询**

Query 提供了一种只读的、同步的方式来获取 Workflow 的当前状态。与直接查询数据库不同，Query 请求会被路由到持有该 Workflow 状态的 Worker，Worker 通过重放历史来重建内存状态并返回查询结果 7。

* **实时性与一致性**：由于 Query 是基于 Worker 内存状态的，它能提供强一致性的即时数据，这在需要实时展示任务进度的 UI 场景中非常有用。然而，如果 Worker 负载过高或历史记录过长，Query 的延迟可能会增加。

### **2.5 Update：同步状态变更与反馈**

Update 是 Temporal 最新引入的强大原语，它结合了 Signal 的写能力和 Query 的读能力。客户端发送 Update 请求后，会阻塞等待 Workflow 处理该请求并返回结果 14。

* **低延迟交互**：在聊天机器人或交互式应用中，用户发送消息后往往需要立即得到确认或回复。传统的 Signal+Query 模式需要两次网络往返，且存在竞态条件。Update 允许在一个请求中完成“发送消息 \-\> 处理逻辑 \-\> 返回回执”的全过程，极大地降低了端到端延迟 14。  
* **验证机制**：Update 支持验证器（Validator）函数，允许 Workflow 在持久化事件之前拒绝非法的请求（例如：库存不足时拒绝下单），从而避免了无效事件污染历史记录。

## ---

**3\. 基于 Temporal 的 Agentic AI 架构方案**

当前 AI 技术正从单纯的聊天机器人（Chatbot）向具备自主行动能力的智能体（Agent）演进。构建生产级 Agent 的核心难点在于：**如何管理多轮对话的上下文状态**、**如何确保工具调用的可靠性**以及**如何处理长时间运行的复杂任务**。Temporal 的机制天然契合这些需求，为 Agentic AI 提供了一个近乎完美的基础设施层。

### **3.1 确定性编排与非确定性推理的解耦**

LLM 本质上是概率性的、非确定性的系统。对于相同的输入，模型可能会生成不同的输出。这看似与 Temporal 的确定性要求冲突，实则可以通过架构设计完美化解。

在我们的方案中，我们将 Agent 的架构拆分为“大脑”（Brain）与“躯干”（Body）：

* **大脑（LLM）**：负责推理、规划和生成内容。这部分逻辑被封装在 **Activity** 中。  
* **躯干（Workflow）**：负责编排、记忆和执行。Workflow 调用“大脑”Activity 获取指令，然后根据指令调度其他 Activity（如搜索网络、查询数据库）执行具体操作 2。

机制详解：  
当 Workflow 调用 LLM Activity 时，Temporal 会记录 LLM 的返回结果（例如：“调用天气 API”）。如果在后续步骤中 Workflow 发生故障并重放，它会直接读取历史记录中的决策，而不会重新询问 LLM。这确保了即使 LLM 是随机的，一旦决策被做出并记录，整个执行流就变得确定且可复现 5。这种设计赋予了 Agent 极其宝贵的特性：执行的稳定性。无论基础设施如何波动，Agent 都会坚定地沿着既定路径执行，直到达成目标。

### **3.2 长期记忆与状态管理（Long-term Memory）**

传统的 Agent 框架（如 LangChain）通常依赖外部向量数据库或 Redis 来手动管理会话历史（Memory）。而在 Temporal 架构中，Workflow 本身就是状态的容器。

* **Context as Code**：我们可以将对话历史、用户偏好和当前任务状态直接保存在 Workflow 的局部变量中。Temporal 的持久化机制保证了这些变量在数月甚至数年后依然可用且一致 16。  
* **无限上下文窗口的工程化**：虽然 LLM 的 Context Window 是有限的，但 Workflow 的存储是近乎无限的。我们可以设计一个 Workflow，它在内存中维护最近的 N 轮对话，同时将更早的历史归档到 S3 或向量数据库中。当需要回顾历史时，Workflow 可以动态地触发一个检索 Activity，将相关记忆加载回上下文中。这种混合存储架构既利用了 Temporal 的便捷性，又规避了 Blob 大小的限制 17。

### **3.3 人机回环（Human-in-the-Loop）的高级模式**

在企业级应用中，全自动的 Agent 往往风险过高，需要人类介入关键决策。Temporal 的 Signal 和 Update 机制为实现 Human-in-the-Loop 提供了标准范式。

场景设计：  
一个采购 Agent 发现库存不足，生成了一份补货计划。

1. **暂停与通知**：Agent Workflow 执行到审批环节，调用 workflow.await 进入休眠状态，并触发一个 Activity 发送审批邮件给经理。  
2. **外部交互**：经理点击邮件中的“批准”按钮。  
3. **信号唤醒**：后端服务收到 HTTP 请求，向 Agent Workflow 发送一个 ApproveSignal。  
4. **恢复执行**：Workflow 被唤醒，检查到审批信号，继续执行后续的下单操作。

相比于传统的轮询数据库状态，这种模式几乎不消耗计算资源，且对审批时效没有限制，无论是几分钟还是几周，系统表现一致 10。

### **3.4 多智能体协同（Multi-Agent Swarm）**

对于复杂任务，单一 Agent 往往力不从心，需要多个专用 Agent 协作。基于 Temporal，我们可以构建一个层级化的 Swarm 架构 18。

* **父子工作流（Parent-Child Workflows）**：主控 Agent（Master Agent）作为父工作流，负责任务拆解。它根据需求动态启动多个子工作流（Worker Agents），分别负责调研、撰写代码、测试等任务。  
* **并行与同步**：这些子 Agent 可以并行运行，利用 Temporal 的异步特性最大化吞吐量。主控 Agent 使用 Promise.all 等待所有子任务完成，汇总结果。  
* **故障隔离**：如果某个子 Agent 陷入死循环或报错，父工作流可以捕获异常，并根据策略重启该子 Agent 或指派另一个 Agent 接手，而不会导致整个任务失败。

## ---

**4\. 高性能与低延迟场景的架构优化**

尽管 Temporal 以高吞吐和可靠性著称，但在默认配置下，其基于数据库轮询的机制可能引入数百毫秒的延迟。对于即时通讯或实时交互场景，我们需要利用特定的高级特性来优化架构。

### **4.1 Update-With-Start：消灭冷启动延迟**

在用户首次与 Agent 交互时，往往面临“先启动工作流，再发送信号”的双重网络开销。Temporal 的 **Update-With-Start** 特性完美解决了这一问题 19。

机制说明：  
客户端发送一个特殊的 Update 请求，语义为：“如果该 ID 的工作流已存在，则处理此更新；如果不存在，则先原子性地创建工作流，然后立即处理此更新。”  
这一机制将原本需要的 StartWorkflow \+ SignalWorkflow 两次 RPC 调用合并为一次。对于聊天应用，这意味着用户发送的第一条消息可以立即被处理并返回响应，将首字节延迟（TTFB）降低了 50% 以上，带来类似传统 HTTP API 的响应速度，同时保留了持久化能力。

### **4.2 实体工作流（Entity Workflow）与状态缓存**

为了进一步降低延迟，我们采用**实体工作流**模式。每个活跃的用户会话或设备都对应一个长期运行的 Workflow 实例。

* **热状态缓存**：由于 Workflow 驻留在 Worker 的内存中（在处理任务期间），读取状态（通过 Query）极其快速，无需频繁访问数据库。  
* **生命周期管理**：为了防止内存泄漏和历史记录过长，我们设计自动休眠与唤醒机制。Workflow 在空闲 N 小时后自动调用 Continue-As-New 清理历史，或者在无操作 M 天后正常结束，释放资源。当用户再次活跃时，利用 Signal-With-Start 或 Update-With-Start 瞬间重建状态 7。

### **4.3 侧信道流式传输（Side-Channel Streaming）**

LLM 应用的一个核心体验是“打字机效果”（Streaming Token）。然而，Temporal 的 Activity 结果必须是完整的、确定性的数据，不能直接通过 Activity 返回流式数据，否则会产生海量的历史事件，撑爆数据库 17。

解决方案：Redis Pub/Sub 侧信道  
我们设计一种双通道架构来解决此矛盾 22：

1. **控制通道（Temporal）**：Workflow 调度 GenerateResponse Activity，传递任务上下文和 Channel ID。这是持久化的、可靠的控制流。  
2. **数据通道（Redis）**：  
   * Activity 在执行过程中，连接到 Redis Pub/Sub，将 LLM 生成的每一个 Token 实时发布到指定的 Channel ID。  
   * 前端客户端通过 WebSocket 或 SSE（Server-Sent Events）订阅该 Channel，实时接收并渲染 Token。  
3. **最终一致性**：Activity 执行完毕后，将完整的响应文本作为返回值传回 Workflow。Workflow 将其记录到历史中，作为后续逻辑的依据。

这种架构巧妙地将**即时性需求**（UI 渲染）与**一致性需求**（业务逻辑）剥离。Redis 负责由快变慢的缓冲，Temporal 负责兜底和状态一致性。即使 Redis 消息丢失（极其罕见），Activity 最终返回的完整结果也能确保 Workflow 逻辑的正确性。

## ---

**5\. 前端集成与可视化策略**

如何将 Temporal 的后端状态实时、优雅地展示给最终用户，是落地方案中的最后一公里。

### **5.1 进度可视化的双重策略**

用户需要看到进度条或状态变更。我们采用 **Pull** 和 **Push** 结合的策略。

* 基于心跳的查询（Pull）：  
  Activity 在执行过程中，应定期调用 RecordHeartbeat(details) 更新进度（例如：“处理中：45%”）。前端可以通过 DescribeWorkflowExecution API 查询 Pending Activity 的详细信息，直接获取最新的心跳数据 23。这种方式不需要 Workflow 代码介入，直接利用了 Temporal 的系统元数据，开销极低。  
* 主动状态推送（Push）：  
  对于关键的状态变更（如“任务完成”、“需要审批”），Workflow 可以调用一个特殊的 NotificationActivity，将事件推送到 Webhook 或消息队列，进而触发前端的通知。

### **5.2 历史记录可视化**

Temporal 提供了强大的 Web UI，但在我们的 SaaS 方案中，可能需要为用户提供定制化的任务视图。

* **Timeline View**：参考 Temporal 官方的 Timeline 视图实现，利用 SDK 获取 GetWorkflowExecutionHistory，在前端解析事件流，渲染出类似甘特图的任务执行图 25。  
* **DAG 可视化**：对于复杂的 Agent 流程，可以通过拦截器（Interceptor）记录 Activity 的依赖关系，生成 Mermaid 或 JSON 格式的 DAG 图，在前端动态展示 Agent 的思维链（Chain of Thought）26。

## ---

**6\. 运维治理与工程化实践**

将 Temporal 引入生产环境，不仅是代码层面的改变，更是运维体系的升级。

### **6.1 版本控制：从 Patch 到 Worker Versioning**

Workflow 代码的变更（如增加一步逻辑）会导致对旧历史重放时的非确定性错误（Non-Deterministic Error）。

* **旧方案：Patching**：使用 workflow.patched() API 在代码中插入版本标记。这会导致代码中充斥着 if (patched)... else... 的逻辑，长期维护极其痛苦。  
* **推荐方案：Worker Versioning**：利用 Temporal 新推出的 Worker Versioning 功能。我们为新代码分配一个新的 Build ID。Temporal Server 会自动将新的工作流路由到新版本的 Worker，而旧的工作流继续在旧版本 Worker 上运行直到结束。这种蓝绿部署（Blue-Green Deployment）模式彻底解决了版本兼容性地狱 25。

### **6.2 可观测性（Observability）**

Temporal 提供了丰富的 Metrics 和 Tracing 集成。

* **全链路追踪**：必须配置 OpenTelemetry。在我们的方案中，Trace ID 应当从 API Gateway 生成，透传给 Temporal Client，并在 Workflow 和 Activity 中延续。这样我们可以在 Jaeger 或 Datadog 中看到一个请求从 HTTP 入口到 Workflow 调度，再到 Activity 执行数据库操作的完整瀑布图 27。  
* **关键指标监控**：重点监控 ScheduleToStartLatency（任务堆积延迟）和 WorkflowTaskExecutionFailure（逻辑错误）。高延迟意味着 Worker 数量不足，需触发自动扩容。

### **6.3 成本控制模型**

Temporal Cloud 或自建集群的成本主要由**Action**（状态转换次数）和**Storage**（历史记录大小）决定。

* **优化策略**：  
  * **减少 Payload**：严禁在 Workflow 输入/输出中传递大文件（如 10MB 的 PDF）。应传递 S3 的 URL 引用。大对象不仅增加存储成本，还会导致网络传输瓶颈 17。  
  * **Local Activity**：对于执行极快（毫秒级）、无需全局重试的短任务（如数据解析），应使用 LocalActivity。它们不记录详细的历史事件，仅记录最终结果，能显著降低 Action 数量和延迟 19。

## ---

**7\. 架构对比与决策建议**

为了验证本方案的优越性，我们将其与主流替代方案进行对比。

### **7.1 Temporal vs. Kafka \+ 数据库状态机**

* **Kafka 方案**：开发者需要手动维护 OrderTable 的状态列，编写消费者处理消息，处理幂等性，处理死信队列，并在服务重启时手动恢复内存状态。这是一套复杂的“管道胶水代码”。  
* **Temporal 方案**：状态保存在 Workflow 对象中，重试和队列由平台接管。代码量通常减少 50% 以上，且可读性大幅提升——业务逻辑看起来就是一段普通的顺序代码。  
* **结论**：对于逻辑复杂、涉及多步流转的业务，Temporal 完胜。仅在超高吞吐（百万级 TPS）的纯数据管道场景下，Kafka 更具优势。

### **7.2 Temporal vs. Airflow**

* **Airflow**：基于 DAG 的调度器，侧重于每天运行一次的批处理任务。延迟高，无法处理实时交互。  
* **Temporal**：事件驱动的编排器，支持毫秒级响应。  
* **结论**：我们的方案涉及 Agent 实时交互和复杂业务流，Airflow 无法满足低延迟和动态性需求。

## ---

**8\. 结论与实施路线图**

经过深度研发与分析，基于 Temporal 机制构建我们的分布式系统与 Agentic AI 方案，不仅在技术上可行，而且在工程效率和系统韧性上具有压倒性优势。Temporal 提供的“故障隔离”和“持久化执行”能力，使得我们能够将精力集中在业务价值的创造上，而非无休止的基础设施维护中。

**建议实施路线图**：

1. **阶段一（核心重构）**：引入 Temporal Server，利用 **Entity Workflow** 模式重构核心业务实体的生命周期管理，消除数据库轮询。  
2. **阶段二（Agent 接入）**：基于 **Update-With-Start** 和 **Activity** 封装，构建低延迟的 AI Agent 服务，实现确定性编排。  
3. **阶段三（体验优化）**：部署 **Redis Side-Channel**，打通前后端流式数据传输，实现极致的用户交互体验。  
4. **阶段四（规模化）**：实施 **Worker Versioning** 和全链路监控，建立自动扩缩容机制，应对生产级流量。

本报告所提出的架构方案，将使我们的系统具备“反脆弱”特性，在面对混乱的分布式环境时，不仅能生存，更能稳健运行。

1

#### **Works cited**

1. Temporal \+ AI Agents: The Missing Piece for Production-Ready Agentic Systems \- DEV Community, accessed December 8, 2025, [https://dev.to/akki907/temporal-workflow-orchestration-building-reliable-agentic-ai-systems-3bpm](https://dev.to/akki907/temporal-workflow-orchestration-building-reliable-agentic-ai-systems-3bpm)  
2. Indestructible AI Agents: A Guide to Using Temporal \- ActiveWizards, accessed December 8, 2025, [https://activewizards.com/blog/indestructible-ai-agents-a-guide-to-using-temporal](https://activewizards.com/blog/indestructible-ai-agents-a-guide-to-using-temporal)  
3. Agentic AI Workflows: Why Orchestration with Temporal is Key | IntuitionLabs, accessed December 8, 2025, [https://intuitionlabs.ai/articles/agentic-ai-temporal-orchestration](https://intuitionlabs.ai/articles/agentic-ai-temporal-orchestration)  
4. System Design: A Breakdown of Temporal's Internal Architecture by Sanil Khurana | Data Science Collective \- Medium, accessed December 8, 2025, [https://medium.com/data-science-collective/system-design-series-a-step-by-step-breakdown-of-temporals-internal-architecture-52340cc36f30](https://medium.com/data-science-collective/system-design-series-a-step-by-step-breakdown-of-temporals-internal-architecture-52340cc36f30)  
5. Of course you can build dynamic AI agents with Temporal, accessed December 8, 2025, [https://temporal.io/blog/of-course-you-can-build-dynamic-ai-agents-with-temporal](https://temporal.io/blog/of-course-you-can-build-dynamic-ai-agents-with-temporal)  
6. Its good practice implement Temporal Workflows to push events to Websockets?, accessed December 8, 2025, [https://community.temporal.io/t/its-good-practice-implement-temporal-workflows-to-push-events-to-websockets/15276](https://community.temporal.io/t/its-good-practice-implement-temporal-workflows-to-push-events-to-websockets/15276)  
7. Managing very long-running Workflows with Temporal, accessed December 8, 2025, [https://temporal.io/blog/very-long-running-workflows](https://temporal.io/blog/very-long-running-workflows)  
8. Building a persistent conversational AI chatbot with Temporal, accessed December 8, 2025, [https://temporal.io/blog/building-a-persistent-conversational-ai-chatbot-with-temporal](https://temporal.io/blog/building-a-persistent-conversational-ai-chatbot-with-temporal)  
9. Understanding the 4 types of Activity timeouts in Temporal, accessed December 8, 2025, [https://temporal.io/blog/activity-timeouts](https://temporal.io/blog/activity-timeouts)  
10. Temporal Use Cases and Design Patterns, accessed December 8, 2025, [https://docs.temporal.io/evaluate/use-cases-design-patterns](https://docs.temporal.io/evaluate/use-cases-design-patterns)  
11. Demo: Build an AI Agent with Temporal, accessed December 8, 2025, [https://temporal.io/resources/on-demand/demo-ai-agent](https://temporal.io/resources/on-demand/demo-ai-agent)  
12. What are the orchestrating cases not suitable for Temporal? \- Community Support, accessed December 8, 2025, [https://community.temporal.io/t/what-are-the-orchestrating-cases-not-suitable-for-temporal/4764](https://community.temporal.io/t/what-are-the-orchestrating-cases-not-suitable-for-temporal/4764)  
13. Workflow.query can query Worker different from the Worker executing activity \- Community Support \- Temporal, accessed December 8, 2025, [https://community.temporal.io/t/workflow-query-can-query-worker-different-from-the-worker-executing-activity/10698](https://community.temporal.io/t/workflow-query-can-query-worker-different-from-the-worker-executing-activity/10698)  
14. Announcing a new operation: Workflow Update \- Temporal, accessed December 8, 2025, [https://temporal.io/blog/announcing-a-new-operation-workflow-update](https://temporal.io/blog/announcing-a-new-operation-workflow-update)  
15. Sending Signals, Queries, & Updates | Temporal Platform Documentation, accessed December 8, 2025, [https://docs.temporal.io/sending-messages](https://docs.temporal.io/sending-messages)  
16. Amazon Bedrock with Temporal: rock solid, accessed December 8, 2025, [https://temporal.io/blog/amazon-bedrock-with-temporal-rock-solid](https://temporal.io/blog/amazon-bedrock-with-temporal-rock-solid)  
17. Best way to streaming data between activities in Temporal \- Community Support, accessed December 8, 2025, [https://community.temporal.io/t/best-way-to-streaming-data-between-activities-in-temporal/13006](https://community.temporal.io/t/best-way-to-streaming-data-between-activities-in-temporal/13006)  
18. Durable multi-agentic AI architecture with Temporal, accessed December 8, 2025, [https://temporal.io/blog/using-multi-agent-architectures-with-temporal](https://temporal.io/blog/using-multi-agent-architectures-with-temporal)  
19. Reduce end-user latency and accelerate Temporal Workflows, accessed December 8, 2025, [https://temporal.io/blog/reduce-latency-and-speed-up-your-temporal-workflows](https://temporal.io/blog/reduce-latency-and-speed-up-your-temporal-workflows)  
20. Update-with-start now available in Pre-release in Go and Java. \- Temporal, accessed December 8, 2025, [https://temporal.io/change-log/update-with-start-now-available-in-pre-release-in-go-and-java](https://temporal.io/change-log/update-with-start-now-available-in-pre-release-in-go-and-java)  
21. What is LLM Streaming and How to Use It? \- Vellum AI, accessed December 8, 2025, [https://www.vellum.ai/llm-parameters/llm-streaming](https://www.vellum.ai/llm-parameters/llm-streaming)  
22. Streaming Messages from Temporal Workers to SSE Clients | Architecting Bytes, accessed December 8, 2025, [https://www.architectingbytes.com/posts/temporal-redis-sse](https://www.architectingbytes.com/posts/temporal-redis-sse)  
23. Detecting Activity failures | Temporal Platform Documentation, accessed December 8, 2025, [https://docs.temporal.io/encyclopedia/detecting-activity-failures](https://docs.temporal.io/encyclopedia/detecting-activity-failures)  
24. How to track and display the progress of my Python workflow with two activities on my UI to display a progress bar? \- Temporal Community, accessed December 8, 2025, [https://community.temporal.io/t/how-to-track-and-display-the-progress-of-my-python-workflow-with-two-activities-on-my-ui-to-display-a-progress-bar/15313](https://community.temporal.io/t/how-to-track-and-display-the-progress-of-my-python-workflow-with-two-activities-on-my-ui-to-display-a-progress-bar/15313)  
25. Workflow visualization with Temporal's Timeline View, accessed December 8, 2025, [https://temporal.io/blog/lets-visualize-a-workflow](https://temporal.io/blog/lets-visualize-a-workflow)  
26. DAG-like Workflow Visualization \- Show & Tell \- Temporal Community, accessed December 8, 2025, [https://community.temporal.io/t/dag-like-workflow-visualization/15080](https://community.temporal.io/t/dag-like-workflow-visualization/15080)  
27. LLM Observability & Application Tracing (open source) \- Langfuse, accessed December 8, 2025, [https://langfuse.com/docs/observability/overview](https://langfuse.com/docs/observability/overview)  
28. Serverless Temporal: The Next Chapter | by Luke Birdeau \- Medium, accessed December 8, 2025, [https://medium.com/@luke.birdeau/serverless-temporal-the-next-chapter-5ac565a7db68](https://medium.com/@luke.birdeau/serverless-temporal-the-next-chapter-5ac565a7db68)  
29. Durable Execution Platform \- Temporal, accessed December 8, 2025, [https://temporal.io/product](https://temporal.io/product)  
30. Basic Agentic Loop with Tool Calling \- Temporal Docs, accessed December 8, 2025, [https://docs.temporal.io/ai-cookbook/agentic-loop-tool-call-openai-python](https://docs.temporal.io/ai-cookbook/agentic-loop-tool-call-openai-python)  
31. What are Agentic AI Workflows? Scalable & Durable Workflows \- Temporal, accessed December 8, 2025, [https://temporal.io/blog/build-resilient-agentic-ai-with-temporal](https://temporal.io/blog/build-resilient-agentic-ai-with-temporal)  
32. temporalio/sdk-python: Temporal Python SDK \- GitHub, accessed December 8, 2025, [https://github.com/temporalio/sdk-python](https://github.com/temporalio/sdk-python)