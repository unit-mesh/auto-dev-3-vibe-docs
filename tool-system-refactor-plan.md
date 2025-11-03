# Tool System 重构方案

## 🎯 现状问题分析

### CodingAgent.kt 的问题
1. **职责过重**：包含工具解析、执行、状态管理等多种职责
2. **硬编码逻辑**：工具调用逻辑散落在 parseAction、executeAction 等方法中
3. **缺乏扩展性**：添加新工具需要修改核心 Agent 代码
4. **无状态管理**：工具执行状态无法追踪和调试
5. **无权限控制**：直接执行所有工具，存在安全风险

## 🏗️ 参考架构分析

### Gemini CLI 的优秀设计
1. **CoreToolScheduler**：状态机驱动的工具编排
2. **ToolRegistry**：统一的工具注册和发现
3. **PolicyEngine**：声明式权限控制
4. **AgentExecutor**：隔离的工具执行环境

### 现有 ToolRegistry 的基础
- 已有基本的工具注册机制
- 支持内置工具自动注册
- 提供工具发现和获取接口

## 🎨 重构设计方案

### 1. 工具编排器 (ToolOrchestrator)
```kotlin
class ToolOrchestrator(
    private val registry: ToolRegistry,
    private val policyEngine: PolicyEngine,
    private val renderer: AgentRenderer
) {
    suspend fun executeToolCall(
        toolName: String,
        params: Map<String, Any>,
        context: ToolExecutionContext
    ): ToolExecutionResult
    
    suspend fun executeToolChain(
        calls: List<ToolCall>,
        context: ToolExecutionContext
    ): List<ToolExecutionResult>
}
```

### 2. 工具调用解析器 (ToolCallParser)
```kotlin
class ToolCallParser {
    fun parseToolCalls(llmResponse: String): List<ToolCall>
    fun parseDevinBlocks(content: String): List<DevinBlock>
    private fun processEscapeSequences(content: String): String
}
```

### 3. 工具执行状态管理
```kotlin
sealed class ToolExecutionState {
    data class Pending(val callId: String, val toolCall: ToolCall) : ToolExecutionState()
    data class Executing(val callId: String, val startTime: Long) : ToolExecutionState()
    data class Success(val callId: String, val result: ToolResult, val duration: Long) : ToolExecutionState()
    data class Failed(val callId: String, val error: String, val duration: Long) : ToolExecutionState()
}
```

### 4. 权限控制引擎 (PolicyEngine)
```kotlin
interface PolicyEngine {
    fun checkPermission(toolCall: ToolCall, context: ToolExecutionContext): PolicyDecision
}

enum class PolicyDecision {
    ALLOW,      // 直接允许
    DENY,       // 直接拒绝
    ASK_USER    // 需要用户确认
}
```

## 📁 文件结构设计

```
mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/
├── orchestrator/
│   ├── ToolOrchestrator.kt           # 工具编排器
│   ├── ToolExecutionContext.kt       # 执行上下文
│   └── ToolExecutionResult.kt        # 执行结果
├── parser/
│   ├── ToolCallParser.kt             # 工具调用解析
│   ├── DevinBlockParser.kt           # DevIn 块解析
│   └── EscapeSequenceProcessor.kt    # 转义序列处理
├── policy/
│   ├── PolicyEngine.kt               # 权限控制接口
│   ├── DefaultPolicyEngine.kt        # 默认权限实现
│   └── PolicyDecision.kt             # 权限决策枚举
├── state/
│   ├── ToolExecutionState.kt         # 执行状态定义
│   └── ToolStateManager.kt           # 状态管理器
└── CodingAgent.kt                    # 简化后的主 Agent
```
``
## 🔄 重构步骤

### 阶段 1: 提取工具解析逻辑
1. 创建 `ToolCallParser` 类
2. 将 `parseAction`、`parseAllActions` 移动到解析器
3. 提取 `processEscapeSequences` 为独立工具类

### 阶段 2: 创建工具编排器
1. 创建 `ToolOrchestrator` 类
2. 将 `executeAction` 逻辑移动到编排器
3. 添加工具执行状态管理

### 阶段 3: 添加权限控制
1. 创建 `PolicyEngine` 接口和默认实现
2. 在工具执行前添加权限检查
3. 支持用户确认机制

### 阶段 4: 简化 CodingAgent
1. 移除工具相关的直接逻辑
2. 通过编排器执行工具调用
3. 专注于 Agent 的核心逻辑：对话管理、迭代控制

## 🎯 预期收益

### 代码质量
- **单一职责**：每个类职责明确
- **可测试性**：各组件可独立测试
- **可维护性**：工具逻辑集中管理

### 功能扩展
- **新工具添加**：只需注册到 ToolRegistry
- **权限控制**：灵活的权限策略
- **状态追踪**：完整的执行状态管理

### 安全性
- **权限控制**：防止危险工具直接执行
- **执行隔离**：工具执行环境隔离
- **审计日志**：完整的工具调用记录
