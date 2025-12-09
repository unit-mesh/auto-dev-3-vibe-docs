# [Feature] 企业级 Text2SQL 分析与可视化智能体（Agent）

## 📋 概述

实现一个跨平台的 **Text2SQL Agent**，能够将自然语言查询转换为 SQL 语句，并自动生成智能化的数据可视化展示。该解决方案采用 **Agentic BI（智能体商业智能）** 架构，具备推理、规划、工具使用和自我修正能力，能在企业环境中实现高准确率和鲁棒性的 SQL 生成与执行。

### 核心特性
- ✅ **高准确率**：通过 Schema Linking 和推理增强，解决 LLM 幻觉和上下文缺失问题
- ✅ **自我修正**：自动修复 SQL 语法错误和执行错误，多轮迭代提升成功率
- ✅ **智能可视化**：根据数据特征自动生成最适合的图表规范，而非仅输出表格
- ✅ **企业级安全**：最小权限原则、AST 防御、沙盒隔离、Prompt 防注入

---

## 🏗️ 分阶段实现方案

### 📌 阶段一：跨平台数据库 Provider 基础设施（第一阶段）

**目标**：建立统一的跨平台数据库访问层，为后续 Text2SQL 核心功能奠定基础。

#### 1.1 跨平台 DB Provider 接口设计

**位置**：`mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/database/`

创建统一的数据库连接接口，支持所有平台（JVM、IDEA、JS、Android、iOS、WASM）：

```kotlin
// DatabaseConnection.kt - 核心接口
interface DatabaseConnection {
    suspend fun executeQuery(sql: String): QueryResult
    suspend fun getSchema(): DatabaseSchema
    suspend fun close()
}

// 数据模型
data class QueryResult(
    val columns: List<String>,
    val rows: List<List<Any?>>,
    val rowCount: Int
)

data class DatabaseSchema(
    val databaseName: String,
    val tables: List<TableSchema>
)

data class TableSchema(
    val name: String,
    val columns: List<ColumnSchema>,
    val comment: String? = null
)

data class ColumnSchema(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val comment: String? = null,
    val isPrimaryKey: Boolean,
    val isForeignKey: Boolean
)

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val databaseName: String,
    val username: String,
    val password: String?,
    val dialect: String // MySQL, MariaDB, PostgreSQL...
)
```

**核心能力**：
- 统一的 SQL 执行接口
- Schema 元数据完整检索
- 结果自动格式化（CSV、表格、JSON）
- Schema 描述自动生成（用于 LLM Prompt）
- 跨平台工厂方法（expect/actual）

#### 1.2 JVM 平台实现 - 使用 JetBrains Exposed

**位置**：`mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/database/`

- **库**：JetBrains Exposed (类型安全的 Kotlin SQL DSL)
- **驱动**：MySQL Connector/J
- **连接池**：HikariCP
- **初期支持**：MySQL/MariaDB（本地和云端）

```kotlin
class ExposedDatabaseConnection(database: Database) : DatabaseConnection {
    override suspend fun executeQuery(sql: String): QueryResult { ... }
    override suspend fun getSchema(): DatabaseSchema { ... }
    override suspend fun close() { ... }
}

// 工厂方法
actual fun createDatabaseConnection(config: DatabaseConfig): DatabaseConnection
```

**实现要点**：
- 使用 Exposed 的 DSL 进行 Schema 检索（元数据）
- 原生 SQL 执行用于用户查询
- HikariCP 连接池管理
- 事务隔离级别配置（Read Committed）
- 结果集处理和类型映射

**Gradle 依赖**：
```gradle
// Exposed SQL Framework
implementation("org.jetbrains.exposed:exposed-core:0.47.0")
implementation("org.jetbrains.exposed:exposed-dao:0.47.0")
implementation("org.jetbrains.exposed:exposed-jdbc:0.47.0")

// MySQL/MariaDB Driver
implementation("com.mysql:mysql-connector-j:9.0.0")

// Connection Pooling
implementation("com.zaxxer:HikariCP:6.0.0")
```

#### 1.3 IDEA 平台实现 - 使用 IDEA 内置 Database API

**位置**：`mpp-idea/mpp-idea-core/src/main/kotlin/cc/unitmesh/devti/database/`

- **库**：IDEA Database Plugin API
- **特性**：
  - 复用 IDEA Database 工具窗口中已配置的数据源
  - 支持 IDEA 管理的所有数据库类型
  - 无需额外配置，直接从 IDEA 中获取连接信息
  - 支持 SSH tunnel 和代理配置

```kotlin
class IdeaDatabaseConnection(
    ideaConnection: Connection,
    dataSourceName: String
) : DatabaseConnection { 
    override suspend fun executeQuery(sql: String): QueryResult { ... }
    override suspend fun getSchema(): DatabaseSchema { ... }
}

object IdeaDatabaseHelper {
    fun getAvailableDataSources(project: Project): List<String>
    fun createFromIdea(project: Project, dataSourceName: String): IdeaDatabaseConnection
}
```

**实现要点**：
- 从 LocalDataSourceManager 获取已配置数据源
- 复用 IDEA 的连接管理（SSH tunnel、代理等）
- 协程化包装（withContext Dispatchers.IO）
- 数据源列表在 IDEA UI 中展示

#### 1.4 其他平台占位符实现

**位置**：`mpp-core/src/{jsMain,androidMain,iosMain,wasmJsMain}/kotlin/cc/unitmesh/agent/database/`

- **JS/WASM**：抛出 `UnsupportedOperationException`，文档说明应通过 HTTP API 访问
- **Android**：抛出异常，推荐使用 Room 或通过服务器 API
- **iOS**：抛出异常，推荐使用 Core Data 或通过服务器 API

```kotlin
// For each platform, e.g., JS:
actual fun createDatabaseConnection(config: DatabaseConfig): DatabaseConnection {
    throw UnsupportedOperationException(
        "Database connections not supported on JS. Use server API instead."
    )
}
```

#### 1.5 辅助功能模块

**结果格式化**：
- CSV 格式：用于发送给 LLM
- ASCII 表格：用于在 CLI/TUI 中展示
- JSON 格式：用于序列化和网络传输

**Schema 描述生成**：
- 表级别描述：表名、注释、列列表
- 列级别描述：列名、类型、约束、注释
- 自动关键信息提取：主键、外键、非空约束

**错误处理**：
```kotlin
class DatabaseException(message: String, cause: Throwable? = null) : Exception()

companion object {
    fun connectionFailed(reason: String): DatabaseException
    fun queryFailed(sql: String, reason: String): DatabaseException
    fun invalidSQL(sql: String, reason: String): DatabaseException
}
```

#### 1.6 单元测试

**位置**：`mpp-core/src/jvmTest/kotlin/cc/unitmesh/agent/database/`

- 使用 H2 嵌入式数据库进行集成测试
- 测试 Schema 获取、查询执行、结果格式化等功能
- 测试异常处理和边界情况

**Gradle 依赖**：
```gradle
// H2 for testing
implementation("com.h2database:h2:2.2.224")
```

**测试覆盖**：
- ✅ 数据库连接和查询执行
- ✅ Schema 元数据检索（表、列、约束）
- ✅ 结果集处理和格式化
- ✅ 异常处理和重连机制
- ✅ 性能测试（连接池、查询效率）

---

### 📌 阶段二：Text2SQL Agent 核心模块（后续）

#### 2.1 Schema Linking（模式链接）

**目标**：解决上下文窗口溢出问题，通过向量相似度检索，仅将最相关的表和列注入 Prompt。

**核心组件**：
- 表元数据向量化（使用 LLM 生成自然语言描述）
- 向量数据库存储（Milvus 或 PgVector）
- 列级别的自然语言映射（"销售额" → sales_amount）
- 关键词识别和值检索（"深圳" → WHERE city = "Shenzhen"）

#### 2.2 推理增强（Reasoning Enhancement）

**目标**：解决复杂业务逻辑（多表关联、指标计算、时间聚合）。

**策略**：
1. **动态 Few-Shot**：检索与问题最相似的 3-5 个历史优选 SQL，隐式传递业务逻辑
2. **Chain-of-Thought (CoT)**：强制 LLM 在生成 SQL 前输出思考过程
3. **多候选生成**：使用较高 Temperature 生成多条 SQL，通过 Self-Consistency 投票选择

#### 2.3 Revise Agent（自我修正闭环）

**目标**：模拟人类开发者的调试过程，确保 SQL 可执行。

**工作流**：
1. 静态检查：使用 JSqlParser 检查语法，拦截非 SELECT
2. 试错执行：沙盒环境中执行
3. 错误回环：将执行错误反馈给 LLM，提示修正
4. 多轮迭代：允许 3-5 轮修正，实验表明可将准确率从 50-60% 提升至 90%

#### 2.4 智能化数据可视化

**目标**：自动决定最佳图表类型，生成安全的图表规范。

**流程**：
1. 数据画像分析：是否有时间列、数据量级、分类数量
2. 生成 JSON 规范：输出 Vega-Lite 或 Lets-Plot JSON，而非 Python 代码
3. 前端渲染：前端解析 JSON 进行渲染

---

### 📌 阶段三：UI 集成（后续）

**位置**：`mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/AgentInterfaceRouter.kt`

在现有 AgentInterfaceRouter 中添加：
- Text2SQL Agent 类型入口（AgentType.TEXT2SQL）
- 数据库连接配置界面
- SQL 编辑器与可视化查看器
- 结果表格与图表展示

---

## 🔐 企业级安全措施

| 层级 | 措施 | 实现细节 |
|-----|------|---------|
| **数据访问** | 最小权限原则 | Agent 连接账号为 Read-Only，通过 Row-Level Security 限制数据范围 |
| **SQL 防御** | AST 解析校验 | 使用 JSqlParser 解析 SQL，白名单表控制，拦截 DROP/DELETE/UPDATE |
| **执行环境** | 沙盒隔离 | GraalVM Polyglot 或容器运行，限制 CPU、内存、IO、网络 |
| **输入防护** | Prompt 防注入 | 验证用户输入，防止覆盖系统指令 |
| **审计日志** | 完整记录 | 记录所有执行的 SQL、用户、时间、结果统计 |

---

## 📚 技术栈选择

### JVM 生态（推荐）

| 组件 | 技术选型 | 版本 | 说明 |
|-----|--------|------|------|
| SQL 框架 | JetBrains Exposed | 0.47.0 | 类型安全、Kotlin 原生、支持多数据库方言 |
| 数据库驱动 | MySQL Connector/J | 9.0.0 | 官方驱动，支持 MySQL/MariaDB |
| 连接池 | HikariCP | 6.0.0 | 高性能连接池，业界标准 |
| SQL 解析 | JSqlParser | 4.6+ | AST 级别的 SQL 校验和分析 |
| 向量数据库 | Milvus / PgVector | - | Schema 和 Few-Shot 案例存储 |
| LLM 编排 | LangChain4j | - | 统一的 LLM 接口和 ReAct Agent |
| 数据处理 | Kotlin DataFrame | - | 类型安全的数据操作 |
| 可视化 | Kandy / Lets-Plot | - | 生成跨平台的 JSON 图表规范 |

### IDEA 集成

| 组件 | 技术 | 说明 |
|-----|------|------|
| Database API | IDEA Database Plugin API | 复用 IDEA 内置数据库工具 |
| Project 访问 | IDEA Project API | 获取项目上下文 |
| UI 集成 | IDEA Editor Tabs | 在编辑器中展示 SQL 编辑和结果 |

---

## 📊 预期成果

### 功能完整性
- ✅ 支持 MySQL/MariaDB 本地和云端连接
- ✅ IDEA 数据源无缝集成
- ✅ 自然语言到 SQL 的高准确率转换（目标 > 70%）
- ✅ 自动错误修复能力
- ✅ 智能图表推荐和生成

### 性能指标
- ✅ Schema 检索 < 200ms（向量相似度）
- ✅ SQL 生成 < 3s（包括 LLM 调用）
- ✅ 多轮修正 3-5 轮内完成
- ✅ 可视化生成 < 1s

### 安全合规
- ✅ 所有 SQL 均通过 AST 校验
- ✅ 无 DROP/DELETE/UPDATE 操作
- ✅ 完整的审计日志
- ✅ 数据访问控制清晰

---

## 📝 实现清单

### 阶段一：DB Provider 基础设施

- [ ] **设计跨平台接口** (mpp-core/src/commonMain/)
  - [ ] DatabaseConnection.kt - 核心接口定义
  - [ ] QueryResult.kt - 查询结果数据模型
  - [ ] DatabaseSchema.kt - 表和列的 Schema 定义
  - [ ] ColumnSchema.kt - 列元数据和约束信息
  - [ ] DatabaseConfig.kt - 数据库配置信息
  - [ ] DatabaseException.kt - 异常体系

- [ ] **JVM 平台实现** (mpp-core/src/jvmMain/)
  - [ ] 在 mpp-core/build.gradle.kts 添加依赖：
    - [ ] Exposed Core/DAO/JDBC (0.47.0)
    - [ ] MySQL Connector/J (9.0.0)
    - [ ] HikariCP (6.0.0)
  - [ ] ExposedDatabaseConnection.kt - 使用 Exposed 实现
  - [ ] Schema 元数据检索实现
  - [ ] 结果集处理和格式化
  - [ ] 连接池配置和管理

- [ ] **IDEA 平台实现** (mpp-idea/mpp-idea-core/src/main/)
  - [ ] IdeaDatabaseConnection.kt - 集成 IDEA Database API
  - [ ] 数据源列表获取
  - [ ] SSH tunnel 和代理支持
  - [ ] 协程化包装

- [ ] **其他平台占位符** 
  - [ ] JS 平台占位实现
  - [ ] Android 平台占位实现
  - [ ] iOS 平台占位实现
  - [ ] WASM 平台占位实现

- [ ] **辅助功能**
  - [ ] 结果格式化：CSV、ASCII 表格、JSON
  - [ ] Schema 描述自动生成
  - [ ] 连接校验（isConnected）
  - [ ] 便利方法（tableExists、getTableRowCount、queryScalar）

- [ ] **单元/集成测试** (mpp-core/src/jvmTest/)
  - [ ] 在 build.gradle.kts 添加 H2 依赖
  - [ ] 创建 DatabaseConnectionTest.kt
  - [ ] 测试 Schema 获取、查询执行、格式化
  - [ ] 测试异常处理和边界情况
  - [ ] 性能基准测试

- [ ] **示例和文档**
  - [ ] DatabaseConnectionExample.kt - 使用示例
  - [ ] README - API 使用指南
  - [ ] 配置示例（MariaDB、MySQL 等）

---

## 🔗 相关文档和资源

### 项目内相关文档
- `docs/temp-project-docs/agent-architecture-analysis.md` - Agent 架构分析
- `docs/agent/renderer-interface-spec.md` - Renderer 接口规范
- `AGENTS.md` - 项目 Agent 开发指南
- `mpp-core/README.md` - mpp-core 项目文档

### 现有代码参考
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/core/Agent.kt` - Agent 基类
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/Tool.kt` - Tool 接口
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/AgentInterfaceRouter.kt` - Agent 路由
- `mpp-idea/mpp-idea-core/src/main/kotlin/cc/unitmesh/devti/observer/agent/AgentProcessor.kt` - IDEA Agent 处理

### 外部资源
- [JetBrains Exposed 文档](https://github.com/JetBrains/Exposed)
- [IDEA Database Plugin API 文档](https://plugins.jetbrains.com/docs/intellij/database.html)
- [JSqlParser 项目](https://github.com/JSQLParser/JSqlParser)
- [论文：Towards Foundational Models for Data Interaction](https://arxiv.org/abs/2301.08134)
- [MySQL Connector/J 文档](https://dev.mysql.com/doc/connector-j/8.0/en/)

---

## 💡 设计考虑

### 为什么选择 Exposed？

1. **类型安全**：Kotlin DSL，编译时检查，减少 SQL 注入风险
2. **多方言支持**：MySQL、PostgreSQL、H2 等，便于扩展
3. **Kotlin 原生**：与项目技术栈一致，代码简洁
4. **社区支持**：JetBrains 官方维护，持续更新
5. **学习成本**：DSL 风格对 Kotlin 开发者友好

### 为什么集成 IDEA Database？

1. **零配置**：复用 IDEA 中已配置的数据源，无需重复配置
2. **功能完整**：IDEA 已有 SSH tunnel、代理、连接测试等完整功能
3. **用户体验**：减少配置步骤，提升开发效率
4. **企业兼容**：适应企业环境的复杂网络配置（VPN、堡垒机等）
5. **一致性**：与 IDEA 内置的 Database 工具窗口体验一致

### 阶段化实现的好处

1. **快速反馈**：阶段一快速完成（1-2 周），可即时测试和验证
2. **解耦建设**：DB Provider 和 Agent 逻辑完全分离，独立演进
3. **风险可控**：逐步引入复杂功能（向量、Revise 等），减少技术风险
4. **社区反馈**：阶段一后可收集用户反馈，指导后续迭代
5. **技术验证**：在部署 LLM Agent 前，先验证数据库集成方案

### 跨平台设计的考虑

1. **expect/actual 机制**：Kotlin Multiplatform 标准做法，保证编译时检查
2. **优雅降级**：非 JVM 平台明确抛出异常，引导用户使用服务器 API
3. **未来扩展**：如需支持其他平台，只需实现 actual 方法即可

---

## 🎯 相关 Issue 和 PR

### 同期需要完成的工作
- Agent 架构核心模块（已完成：AgentDefinition、AgentExecutor、SubAgent）
- Tool 系统框架（已完成：Tool 接口、ExecutableTool、ToolRegistry）
- Renderer 系统（已完成：BaseRenderer、多平台实现）

### 后续相关 Issue
- Text2SQL 核心 Agent 实现（Schema Linking、推理增强、Revise）
- 可视化 Agent 实现（数据画像、图表规范生成）
- UI 集成和用户界面
- 企业安全审计日志系统

---

## 💬 讨论点和开放问题

本 Issue 涉及以下技术决策，欢迎讨论：

1. **数据库方言支持范围**
   - 初期支持 MySQL/MariaDB？还是同时支持 PostgreSQL？
   - 是否需要支持 Oracle、SQL Server 等企业数据库？

2. **Schema 向量化的向量库选择**
   - Milvus（开源、部署灵活）
   - PgVector（基于 PostgreSQL，轻量）
   - LLaMA Index（与 LLM 框架集成）

3. **可视化框架最终选型**
   - Vega-Lite（功能完整、社区活跃）
   - Lets-Plot（Kotlin 友好）
   - ECharts（国内流行）

4. **企业安全需求的优先级**
   - Row-Level Security 是否为必需？
   - 审计日志的详细程度如何定义？
   - 数据脱敏需求？

5. **性能基准的设定**
   - Schema 检索目标 < 200ms 是否合理？
   - SQL 生成目标 < 3s 是否包括 LLM 调用？

---

## ✅ 完成标准

本 Feature 在满足以下条件时可视为完成：

1. **代码完整性**
   - [ ] 所有接口定义完成
   - [ ] JVM、IDEA 平台实现完成
   - [ ] 其他平台占位实现完成
   - [ ] 单元测试覆盖率 > 80%

2. **文档完整性**
   - [ ] API 文档（KDoc）
   - [ ] 使用指南
   - [ ] 配置示例
   - [ ] 故障排查文档

3. **功能验证**
   - [ ] 本地 MySQL/MariaDB 连接测试通过
   - [ ] IDEA 数据源集成测试通过
   - [ ] Schema 检索性能满足要求
   - [ ] 错误处理完整

4. **代码质量**
   - [ ] 无 lint 错误
   - [ ] 无编译警告
   - [ ] 代码审查通过
   - [ ] 与项目代码风格一致

---

## 📞 讨论和反馈

本 Issue 为 Text2SQL Agent 的第一阶段规划。我们期望通过这个设计文档：
- 📋 明确阶段目标和实现范围
- 🏗️ 给出详细的技术架构
- 📝 提供实施路线图
- 💡 讨论关键技术决策

欢迎在 Issue 中进行讨论和提问！
