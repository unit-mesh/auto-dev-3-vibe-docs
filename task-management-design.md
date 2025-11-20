# Task Management Design

## 设计理念（参考Cursor）

### Cursor的优势
1. **工作优先**：AI专注于实际工作，而不是报告进度
2. **自然流程**：任务管理不打断正常工作流
3. **自动推断**：系统自动识别任务边界和状态
4. **简洁UI**：清晰的可视化，不过度

### 我们的实现策略

#### 1. 最小化工具调用
- ❌ 不要求LLM频繁调用 `/task-boundary`
- ✅ 仅在重要里程碑时使用（5+步骤的复杂任务）
- ✅ 提示词强调"专注工作，而非报告"

#### 2. 简化的工具设计
```kotlin
TaskBoundaryParams(
    taskName: String,      // 任务标题
    status: String,        // PLANNING/WORKING/COMPLETED/BLOCKED/CANCELLED
    summary: String = ""   // 简短描述
)
```

#### 3. 使用指南

**推荐使用场景：**
```
User: "Implement user authentication with OAuth2, JWT tokens, and role-based access control"

Agent思考：这是一个5+步骤的复杂任务
1. 分析现有结构
2. 设计OAuth2流程
3. 实现JWT生成和验证
4. 添加角色系统
5. 集成到现有代码
6. 测试

Agent行动：
/task-boundary
```json
{"taskName": "Implement Authentication System", "status": "PLANNING", "summary": "Analyzing current auth structure"}
```
```

[开始实际工作...]
```

**不推荐使用：**
```
User: "Fix the typo in README.md"

Agent思考：简单任务，直接做就好
Agent行动：直接使用 /edit-file
```

#### 4. 提示词设计原则

**英文版：**
- "**focus on doing the work rather than reporting progress**"
- "Only use this if..."（明确限制条件）
- "**Don't over-communicate**"

**中文版：**
- "**专注于完成工作而不是报告进度**"
- "仅在以下情况使用..."（清晰的边界）
- "**不要过度沟通**"

## 与Cursor的对比

| 方面 | Cursor | 我们的实现 |
|------|--------|------------|
| 任务识别 | 自动 | LLM决定（但有明确指导）|
| UI集成 | 深度集成 | 通过工具输出 |
| 使用频率 | 每个对话 | 仅复杂任务 |
| 实现复杂度 | 高（UI层面）| 低（工具层面）|
| 适用场景 | 所有任务 | 5+步骤的复杂任务 |

## 技术实现

### 1. 工具注册
```kotlin
// BuiltinToolsProvider.kt
tools.add(TaskBoundaryTool())
```

### 2. 工具定义
- 位置：`mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/TaskBoundaryTool.kt`
- 分类：`ToolCategory.Utility`
- 参数验证：TaskStatus enum确保状态有效

### 3. 提示词集成
- 位置：`CodingAgentTemplate.kt`
- 策略：可选功能，明确使用边界
- 语言：英文和中文双版本

## 未来改进方向

### 短期（如果需要）
1. **自动任务推断**：
   - 监听工具调用序列
   - 自动识别任务边界（例如：read→edit→shell→read-file = 一个任务）
   - 无需LLM显式调用

2. **任务层次**：
   - 支持子任务
   - 自动归类相关操作

### 长期（需要UI支持）
1. **UI集成**：
   - 在mpp-ui中添加任务可视化组件
   - 实时更新任务状态
   - 支持折叠/展开

2. **智能建议**：
   - 基于任务复杂度自动建议是否使用task-boundary
   - 学习用户偏好

## 结论

当前实现采用了**实用主义**的方法：
- ✅ 提供了任务管理能力
- ✅ 不强制使用，避免过度报告
- ✅ 适合当前架构，易于集成
- ✅ 为未来改进留有空间

这比完全自动化的方案更适合当前阶段，因为：
1. 实现简单，维护成本低
2. LLM有足够智能决定何时使用
3. 不会因为误判而产生噪音
4. 可以快速迭代和改进

