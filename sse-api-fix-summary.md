# SSE API 修复总结

## 问题描述

用户尝试在 mpp-server 中实现 SSE (Server-Sent Events) API 来提供流式 AI Agent 服务，但遇到以下问题：

1. **curl 测试没有任何输出或响应**
2. **服务器返回 406 Not Acceptable 错误**
3. **Agent 不是以流式方式运行**

## 根本原因

### 1. 路由配置问题
原代码使用 `post("/stream")` + `respondTextWriter`，但这种组合在 Ktor 中不兼容。
Ktor 的 SSE 插件期望使用专门的 `sse()` DSL。

### 2. 协程作用域问题
在 Flow builder 中使用了独立的 `CoroutineScope`，导致：
- Flow 收集可能在 Agent 产生任何事件之前就结束
- 事件流的时序不正确

### 3. LLM 流式输出被禁用
`enableLLMStreaming = false` 导致没有实时的流式输出，无法看到 Agent 的执行过程。

## 解决方案

### 1. 使用 Ktor SSE DSL（Routing.kt）

**之前：**
```kotlin
post("/stream") {
    val request = call.receive<AgentRequest>()
    // ...
    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
        // SSE streaming
    }
}
```

**之后：**
```kotlin
sse("/stream") {
    val projectId = call.parameters["projectId"] ?: /* error */
    val task = call.parameters["task"] ?: /* error */
    
    agentService.executeAgentStream(project.path, request).collect { event ->
        send(ServerSentEvent(data = data, event = eventType))
    }
}
```

**改进：**
- 使用 `sse()` DSL 替代 `post()`
- 使用 GET 请求 + 查询参数（SSE 标准方式）
- 使用 `ServerSentEvent` 对象发送事件
- 添加 `io.ktor.sse.*` 导入

### 2. 修复协程作用域（AgentService.kt）

**之前：**
```kotlin
suspend fun executeAgentStream(...): Flow<AgentEvent> = flow {
    // Launch agent in independent scope (WRONG)
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        val result = agent.executeTask(task)
        renderer.sendComplete(...)
    }
    
    // Start collecting immediately (races with agent execution)
    renderer.events.collect { event ->
        emit(event)
    }
}
```

**之后：**
```kotlin
suspend fun executeAgentStream(...): Flow<AgentEvent> = flow {
    coroutineScope {  // Use proper coroutineScope
        launch {
            val result = agent.executeTask(task)
            renderer.sendComplete(...)
        }
        
        // Collect events - will wait until channel is closed
        renderer.events.collect { event ->
            emit(event)
        }
    }
}
```

**改进：**
- 使用 `coroutineScope` 替代独立的 `CoroutineScope`
- 导入 `kotlinx.coroutines.*`（包含 `coroutineScope`）
- 确保事件收集正确等待 Agent 执行

### 3. 启用 LLM 流式输出（AgentService.kt）

**之前：**
```kotlin
CodingAgent(
    // ...
    enableLLMStreaming = false  // 禁用流式
)
```

**之后：**
```kotlin
CodingAgent(
    // ...
    enableLLMStreaming = true  // 启用流式以支持 SSE
)
```

## 文件修改清单

### 1. `mpp-server/src/main/kotlin/cc/unitmesh/server/plugins/Routing.kt`
- ✅ 添加 `io.ktor.sse.*` 导入
- ✅ 将 `post("/stream")` 改为 `sse("/stream")`
- ✅ 使用查询参数 `projectId` 和 `task` 替代 POST body
- ✅ 使用 `ServerSentEvent` 对象发送事件

### 2. `mpp-server/src/main/kotlin/cc/unitmesh/server/service/AgentService.kt`
- ✅ 添加 `import kotlinx.coroutines.*`（包含 `coroutineScope`）
- ✅ 在 `executeAgentStream` 中使用 `coroutineScope` 替代独立的 `CoroutineScope`
- ✅ 将 `enableLLMStreaming` 改为 `true`
- ✅ 添加 `e.printStackTrace()` 以便调试

### 3. `docs/test-scripts/test-sse-api.sh`
- ✅ 创建测试脚本

### 4. `docs/sse-api-guide.md`
- ✅ 创建完整的 API 使用文档

## API 使用方式变更

**之前（不工作）：**
```bash
curl -X POST http://localhost:8080/api/agent/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"projectId": "autocrud", "task": "test"}'
```

**之后（工作正常）：**
```bash
curl -N "http://localhost:8080/api/agent/stream?projectId=my-project&task=list%20files" \
  -H "Accept: text/event-stream"
```

## 测试结果

✅ **成功** - SSE 流式输出正常工作：
```
event: iteration
data: {"current":1,"max":20}

event: llm_chunk
data: {"chunk":"I'll"}

event: llm_chunk
data: {"chunk":" start"}

event: llm_chunk
data: {"chunk":" by"}
...
```

## 技术要点

1. **SSE 与 HTTP 方法**: SSE 通常使用 GET 请求，因为它是单向的长连接
2. **Ktor SSE 插件**: 使用 `sse()` DSL 是处理 SSE 的标准方式
3. **协程作用域**: 在 Flow builder 中使用 `coroutineScope` 确保正确的结构化并发
4. **事件流时序**: 使用 Channel + Flow 确保事件按正确顺序发送
5. **LLM 流式输出**: 必须启用才能看到实时的 AI 输出

## 相关资源

- [Ktor SSE 文档](https://ktor.io/docs/server-server-sent-events.html)
- [Kotlin Coroutines Flow](https://kotlinlang.org/docs/flow.html)
- [Server-Sent Events 规范](https://html.spec.whatwg.org/multipage/server-sent-events.html)

## 未来改进建议

1. 支持 POST + SSE 的组合（如果需要发送大量参数）
2. 添加连接重试机制
3. 实现心跳 (heartbeat) 保持连接活跃
4. 添加请求认证和授权
5. 支持任务取消功能

