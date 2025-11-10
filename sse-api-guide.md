# SSE API Guide for mpp-server

## 概述

mpp-server 提供了基于 Server-Sent Events (SSE) 的流式 API，允许客户端实时接收 AI Agent 的执行进度和输出。

## API 端点

### GET /api/agent/stream

流式执行 Agent 任务，实时返回执行过程中的事件。

**请求参数（Query Parameters）：**

- `projectId` (必需): 项目 ID
- `task` (必需): 要执行的任务描述

**请求头：**

```
Accept: text/event-stream
```

**响应：**

返回 `text/event-stream` 格式的流式数据。

## 事件类型

### 1. iteration
迭代开始事件

```
event: iteration
data: {"current":1,"max":20}
```

**数据格式：**
```typescript
{
  current: number,  // 当前迭代次数
  max: number       // 最大迭代次数
}
```

### 2. llm_chunk
LLM 响应的流式输出（逐字符或逐词）

```
event: llm_chunk
data: {"chunk":"Hello"}
```

**数据格式：**
```typescript
{
  chunk: string  // LLM 输出的文本片段
}
```

### 3. tool_call
工具调用事件

```
event: tool_call
data: {"toolName":"read_file","params":"..."}
```

**数据格式：**
```typescript
{
  toolName: string,  // 工具名称
  params: string     // 工具参数（JSON字符串）
}
```

### 4. tool_result
工具执行结果

```
event: tool_result
data: {"toolName":"read_file","success":true,"output":"..."}
```

**数据格式：**
```typescript
{
  toolName: string,   // 工具名称
  success: boolean,   // 执行是否成功
  output: string      // 工具输出（可能为null）
}
```

### 5. error
错误事件

```
event: error
data: {"message":"Error description"}
```

**数据格式：**
```typescript
{
  message: string  // 错误信息
}
```

### 6. complete
任务完成事件

```
event: complete
data: {"success":true,"message":"Task completed","iterations":3,"steps":[...],"edits":[...]}
```

**数据格式：**
```typescript
{
  success: boolean,
  message: string,
  iterations: number,
  steps: Array<{
    step: number,
    action: string,
    tool: string,
    success: boolean
  }>,
  edits: Array<{
    file: string,
    operation: string,
    content: string
  }>
}
```

## 使用示例

### 使用 curl

```bash
curl -N "http://localhost:8080/api/agent/stream?projectId=my-project&task=list%20files" \
  -H "Accept: text/event-stream"
```

### 使用 JavaScript/TypeScript (EventSource)

```typescript
const projectId = 'my-project';
const task = 'list files';
const url = `http://localhost:8080/api/agent/stream?projectId=${encodeURIComponent(projectId)}&task=${encodeURIComponent(task)}`;

const eventSource = new EventSource(url);

eventSource.addEventListener('iteration', (e) => {
  const data = JSON.parse(e.data);
  console.log(`Iteration ${data.current}/${data.max}`);
});

eventSource.addEventListener('llm_chunk', (e) => {
  const data = JSON.parse(e.data);
  process.stdout.write(data.chunk);
});

eventSource.addEventListener('tool_call', (e) => {
  const data = JSON.parse(e.data);
  console.log(`\n🔧 Tool: ${data.toolName}`);
});

eventSource.addEventListener('tool_result', (e) => {
  const data = JSON.parse(e.data);
  console.log(`✓ ${data.toolName}: ${data.success ? 'Success' : 'Failed'}`);
});

eventSource.addEventListener('error', (e) => {
  const data = JSON.parse(e.data);
  console.error(`Error: ${data.message}`);
  eventSource.close();
});

eventSource.addEventListener('complete', (e) => {
  const data = JSON.parse(e.data);
  console.log(`\n✓ Task completed: ${data.message}`);
  eventSource.close();
});

eventSource.onerror = (error) => {
  console.error('EventSource error:', error);
  eventSource.close();
};
```

### 使用 Node.js (fetch with streaming)

```typescript
const response = await fetch(
  `http://localhost:8080/api/agent/stream?projectId=${projectId}&task=${encodeURIComponent(task)}`,
  {
    headers: {
      'Accept': 'text/event-stream',
    },
  }
);

const reader = response.body!.getReader();
const decoder = new TextDecoder();

let buffer = '';

while (true) {
  const { done, value } = await reader.read();
  
  if (done) break;
  
  buffer += decoder.decode(value, { stream: true });
  
  const lines = buffer.split('\n');
  buffer = lines.pop() || '';
  
  let event = '';
  for (const line of lines) {
    if (line.startsWith('event:')) {
      event = line.substring(6).trim();
    } else if (line.startsWith('data:')) {
      const data = JSON.parse(line.substring(5).trim());
      
      switch (event) {
        case 'llm_chunk':
          process.stdout.write(data.chunk);
          break;
        case 'complete':
          console.log('\n✓ Task completed');
          break;
        // Handle other events...
      }
    }
  }
}
```

## 启动服务器

```bash
cd /Volumes/source/ai/autocrud
./gradlew :mpp-server:run
```

服务器默认监听在 `http://localhost:8080`

## 测试

使用提供的测试脚本：

```bash
./docs/test-scripts/test-sse-api.sh
```

## 技术实现

- **服务器框架**: Ktor 3.x
- **SSE 支持**: `ktor-server-sse` plugin
- **流式输出**: 启用 `enableLLMStreaming = true`
- **事件传递**: 使用 Kotlin Coroutines Flow + Channel

## 注意事项

1. **连接超时**: SSE 连接可能会因为网络超时而断开，客户端应实现重连机制
2. **项目ID**: 请先通过 `GET /api/projects` 获取可用的项目列表
3. **任务描述**: `task` 参数应该是清晰的自然语言描述
4. **URL 编码**: 查询参数需要进行 URL 编码（特别是 task 参数）
5. **LLM 配置**: 服务器使用 `~/.autodev/config.yaml` 中的 LLM 配置

## 相关文件

- 路由配置: `mpp-server/src/main/kotlin/cc/unitmesh/server/plugins/Routing.kt`
- Agent 服务: `mpp-server/src/main/kotlin/cc/unitmesh/server/service/AgentService.kt`
- SSE Renderer: `mpp-server/src/main/kotlin/cc/unitmesh/server/render/ServerSideRenderer.kt`
- 事件模型: `mpp-server/src/main/kotlin/cc/unitmesh/server/model/AgentEvent.kt`

