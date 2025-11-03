# CodingAgent 架构重构总结

## 背景

原本 TypeScript 的 `CodingAgentService.ts` 直接实现了所有 Coding Agent 逻辑。这导致：
- TypeScript 和 Kotlin 逻辑重复
- SubAgents 无法有效管理
- 难以实现"Agent as Tool"的设计理念

## 新架构

### 核心设计

```
ExecutableTool (接口)
    ↓
Agent<TInput, TOutput> (抽象基类)
    ├─→ SubAgent (单一任务 Agent)
    │    ├─→ ErrorRecoveryAgent
    │    └─→ LogSummaryAgent
    └─→ MainAgent (主任务 Agent)
         └─→ CodingAgent (实现 CodingAgentService)
```

### Kotlin 实现

#### 1. CodingAgent (mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodingAgent.kt)

```kotlin
class CodingAgent(
    private val projectPath: String,
    private val llmService: KoogLLMService,
    override val maxIterations: Int = 100
) : MainAgent<AgentTask, ToolResult.AgentResult>(...)
  , CodingAgentService {
    
    init {
        // 注册 SubAgents 作为 Tools
        registerTool(ErrorRecoveryAgent(projectPath, llmService))
        registerTool(LogSummaryAgent(llmService, threshold = 2000))
    }
    
    override suspend fun executeTask(task: AgentTask): AgentResult {
        // 主循环：context → prompt → LLM → action → execute
        while (shouldContinue()) {
            // 1. 构建上下文
            // 2. 生成提示
            // 3. 调用 LLM
            // 4. 解析和执行行动
            // 5. 检查是否完成
        }
    }
}
```

**特点：**
- 继承自 `MainAgent`，拥有工具管理能力
- 实现 `CodingAgentService` 接口，符合现有规范
- SubAgents 自动注册，按优先级排序
- 主循环逻辑清晰：context → prompt → LLM → action → execute

#### 2. JS 导出 (mpp-core/src/jsMain/kotlin/cc/unitmesh/agent/CodingAgentExports.kt)

```kotlin
@JsExport
class JsCodingAgent(
    private val projectPath: String,
    private val llmService: JsKoogLLMService,
    private val maxIterations: Int = 100
) {
    private val agent: CodingAgent = CodingAgent(
        projectPath, llmService.service, maxIterations
    )
    
    fun executeTask(task: JsAgentTask): Promise<JsAgentResult> {
        return GlobalScope.promise {
            agent.executeTask(task.toCommon())
                .let { JsAgentResult.fromCommon(it) }
        }
    }
}
```

### TypeScript 使用

#### 替换前 (不推荐)
```typescript
import { CodingAgentService } from './agents/CodingAgentService.js';
const service = new CodingAgentService(projectPath, config);
const result = await service.executeTask(task);
```

#### 替换后 (推荐)
```typescript
import MppCore from '@autodev/mpp-core';
const { JsCodingAgent, JsAgentTask } = MppCore.cc.unitmesh.agent;
const { JsKoogLLMService, JsModelConfig } = MppCore.cc.unitmesh.llm;

// 创建 LLM Service
const modelConfig = new JsModelConfig(
    "DEEPSEEK", "deepseek-chat", apiKey, 0.7, 4096, baseUrl
);
const llmService = new JsKoogLLMService(modelConfig);

// 创建 CodingAgent
const agent = new JsCodingAgent(projectPath, llmService, 100);

// 执行任务
const task = new JsAgentTask(requirement, projectPath);
const result = await agent.executeTask(task);

console.log(`Success: ${result.success}`);
console.log(`Message: ${result.message}`);
console.log(`Steps: ${result.steps.length}`);
```

## 优势

### 1. 统一架构
- **单一实现**：CodingAgent 的核心逻辑在 Kotlin 中实现一次
- **跨平台复用**：JVM、Android、JS、iOS 都可以使用同一套代码
- **类型安全**：Kotlin 的强类型系统保证正确性

### 2. Agent as Tool
- **SubAgents 是 Tools**：ErrorRecoveryAgent、LogSummaryAgent 都是 ExecutableTool
- **可组合**：MainAgent 可以注册任意 Tool（包括其他 Agents）
- **统一接口**：所有 Tools 都有相同的 execute() 接口

### 3. 更好的管理
- **优先级排序**：SubAgents 按优先级自动排序
- **生命周期管理**：Agent 基类提供统一的生命周期钩子
- **错误处理**：统一的 ToolResult 处理成功/失败

### 4. 易于扩展
- 添加新 SubAgent？只需实现 `SubAgent<TInput, ToolResult>` 并注册
- 添加新 Tool？实现 `ExecutableTool<TInput, TOutput>` 即可
- 修改主循环？只需修改 `CodingAgent.executeTask()`

## 当前状态

### ✅ 已完成
1. **Kotlin CodingAgent** - 基于 MainAgent 实现，集成 SubAgents
2. **JS 导出** - JsCodingAgent 可在 TypeScript 中使用
3. **SubAgents 触发** - ErrorRecoveryAgent 和 LogSummaryAgent 正常触发
4. **编译通过** - JVM 和 JS 平台都编译成功

### ⚠️ 待完善
1. **LLM 调用** - AIAgent.run() 在 JS 环境中有问题，需要直接使用 PromptExecutor
2. **工具执行** - parseAction() 和 executeAction() 需要实现具体逻辑
3. **上下文构建** - buildContext() 需要扫描项目结构
4. **完成检测** - isTaskComplete() 需要更智能的判断逻辑

### 🔄 下一步
1. 修复 `KoogLLMService.sendPrompt()` 在 JS 环境的问题
2. 实现 CodingAgent 的工具调用解析和执行
3. 完善项目结构扫描和上下文构建
4. 删除 TypeScript 的 `CodingAgentService.ts`（完全使用 Kotlin 版本）
5. 更新 mpp-ui CLI 使用 `JsCodingAgent`

## 迁移指南

### 对于 mpp-ui CLI

**步骤：**
1. 更新 `index.ts` 或入口文件，导入 `JsCodingAgent`
2. 替换 `CodingAgentService` 实例化为 `JsCodingAgent`
3. 删除 `src/jsMain/typescript/agents/CodingAgentService.ts`
4. 删除 `src/jsMain/typescript/agents/ErrorRecoveryAgent.ts`
5. 删除 `src/jsMain/typescript/agents/LogSummaryAgent.ts`
6. 测试完整流程

### 对于其他平台

**JVM/Android:**
```kotlin
val agent = CodingAgent(projectPath, llmService)
val result = agent.executeTask(AgentTask(requirement, projectPath))
```

**iOS (via Kotlin/Native):**
```swift
// 待实现，需要 Kotlin/Native bindings
```

## 总结

这次重构实现了"Agent as Tool"的核心设计理念，将 TypeScript 的 `CodingAgentService` 替换为基于 Kotlin `MainAgent` 的实现。这不仅统一了架构，还为未来扩展打下了坚实基础。

**关键成果：**
- ✅ Agent 就是 Tool
- ✅ SubAgents 自动管理
- ✅ 跨平台复用
- ✅ 类型安全
- ✅ 易于扩展
