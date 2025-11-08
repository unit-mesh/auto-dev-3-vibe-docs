# 工具渲染问题修复总结

## 问题描述

多工具场景下，工具调用和结果显示顺序不对：
- **现象**：所有工具调用信息没有立即显示，直到最后一个工具完成才全部显示
- **期望**：工具1调用 → 工具1结果 → 工具2调用 → 工具2结果 → ...
- **实际**：(等待所有工具) → 工具1调用 → 工具2调用 → ... → 工具1结果 → 工具2结果 → ...

## 根本原因

`CodingAgentExecutor.kt` 中的执行流程分为三个阶段：

```
Phase 1: 循环渲染所有工具调用信息（同步）
Phase 2: 并行执行所有工具（无输出）
Phase 3: 等待完成后，顺序渲染所有结果（最后才输出）
```

这导致用户看到的是所有工具信息堆积显示，然后再显示结果。

## 解决方案

### 核心改进

将执行流程改为 **流式处理**：每个工具的调用、执行、结果渲染在一个异步任务中完成，多个任务并行执行。

```
Tool 1 Task:  [调用] → [执行] → [结果] ┐
Tool 2 Task:  [调用] → [执行] → [结果] ├─ 并行执行
Tool 3 Task:  [调用] → [执行] → [结果] ┘
```

### 代码改动

**文件**: `/Volumes/source/ai/autocrud/mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/executor/CodingAgentExecutor.kt`

#### 改动前（三阶段）
```kotlin
// Step 1: 先渲染所有工具调用
for (toolCall in toolsToExecute) {
    renderer.renderToolCall(toolName, paramsStr)
}

// Step 2: 并行执行所有工具
val executionJobs = toolsToExecute.map { toolCall ->
    async {
        toolOrchestrator.executeToolCall(toolName, params, context)
    }
}

// Step 3: 等待完成后才渲染结果
val executionResults = executionJobs.awaitAll()
for ((toolName, params, result) in executionResults) {
    renderer.renderToolResult(...)
}
```

#### 改动后（流式处理）
```kotlin
// 为每个工具创建完整的处理流程：调用 → 执行 → 结果
val executionJobs = toolsToExecute.map { toolCall ->
    async {
        // 1a: 渲染工具调用信息
        renderer.renderToolCall(toolName, paramsStr)
        
        // 1b: 执行工具
        val executionResult = toolOrchestrator.executeToolCall(toolName, params, context)
        
        // 1c: 立即渲染该工具的结果
        renderer.renderToolResult(toolName, success, output, fullOutput, metadata)
        
        Triple(toolName, params, executionResult)
    }
}

// Step 2: 等待所有工具完成（包括渲染）
val executionResults = executionJobs.awaitAll()

// Step 3: 后续处理（错误恢复等）
for ((toolName, params, result) in executionResults) {
    steps.add(stepResult)
    recordFileEdit(params)  // 只保留必要的后续处理
}
```

### 其他改动

1. **更新 RendererExports.kt**
   - `JsCodingAgentRenderer` 接口添加 `metadata` 参数
   - `JsRendererAdapter` 正确传递 metadata

2. **更新 TypeScript 渲染器**
   - `BaseRenderer.ts`: 更新 `renderToolResult` 签名添加 `metadata` 参数
   - `CliRenderer.ts`: 更新方法签名
   - `TuiRenderer.ts`: 更新方法签名

## 优势

| 方面 | 改动前 | 改动后 |
|------|--------|--------|
| 用户体验 | 工具信息堆积显示 | 流畅实时显示 |
| 响应时间 | 等待所有工具执行完成 | 即时反馈 |
| 调试效率 | 难以追踪工具执行 | 清晰的顺序输出 |
| 并行性 | 保留 | 保留 ✓ |
| 错误恢复 | 保留 | 保留 ✓ |

## 测试验证

- ✅ `./gradlew :mpp-core:compileKotlinJs` - 编译成功
- ✅ `./gradlew :mpp-core:assembleJsPackage` - JS 包生成成功
- ✅ `npm run build` - UI 构建成功
- ✅ 生成的 TypeScript 定义文件包含正确的 metadata 参数

## 实际效果

现在在多工具场景下，输出顺序应该如下：

```
💭 Task analysis...

● read-file - file reader
  ⎿ Reading file: src/main.java
  ⎿ Read 45 lines

● grep - content finder  
  ⎿ Searching for pattern: class
  ⎿ Found 3 matches

● write-file - file editor
  ⎿ Updating file: src/main.java
  ⎿ Edited with 5 additions and 2 deletions
```

而不是所有工具信息堆积在一起。
