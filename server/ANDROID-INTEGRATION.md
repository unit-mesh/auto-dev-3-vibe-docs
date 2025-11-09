# Android 集成指南

本文档说明如何在 Android 应用中集成 mpp-server 的远程 AI Coding Agent 能力。

## 📋 前提条件

1. mpp-server 已启动并可访问 (例如: `http://your-server:8080`)
2. Android 项目已添加 Ktor Client 依赖
3. 已配置网络权限

## 🔧 依赖配置

### build.gradle.kts

```kotlin
dependencies {
    // Ktor Client
    implementation("io.ktor:ktor-client-android:3.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
```

### AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## 📦 数据模型

### 请求模型

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class AgentRequest(
    val projectId: String,
    val task: String,
    val llmConfig: LLMConfig? = null
)

@Serializable
data class LLMConfig(
    val provider: String,
    val modelName: String,
    val apiKey: String,
    val baseUrl: String = ""
)
```

### 响应模型

```kotlin
@Serializable
data class AgentResponse(
    val success: Boolean,
    val message: String,
    val output: String? = null,
    val iterations: Int = 0,
    val steps: List<AgentStepInfo> = emptyList(),
    val edits: List<AgentEditInfo> = emptyList()
)

@Serializable
data class AgentStepInfo(
    val step: Int,
    val action: String,
    val tool: String,
    val success: Boolean
)

@Serializable
data class AgentEditInfo(
    val file: String,
    val operation: String,
    val content: String
)
```

### SSE 事件模型

```kotlin
@Serializable
sealed interface AgentEvent {
    @Serializable
    data class IterationStart(val current: Int, val max: Int) : AgentEvent
    
    @Serializable
    data class LLMResponseChunk(val chunk: String) : AgentEvent
    
    @Serializable
    data class ToolCall(val toolName: String, val params: String) : AgentEvent
    
    @Serializable
    data class ToolResult(
        val toolName: String,
        val success: Boolean,
        val output: String?
    ) : AgentEvent
    
    @Serializable
    data class Error(val message: String) : AgentEvent
    
    @Serializable
    data class Complete(
        val success: Boolean,
        val message: String,
        val iterations: Int,
        val steps: List<AgentStepInfo>,
        val edits: List<AgentEditInfo>
    ) : AgentEvent
}
```

## 🔌 API 客户端实现

### 1. 同步执行 (推荐用于简单任务)

```kotlin
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class AgentApiClient(private val baseUrl: String) {
    
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    suspend fun executeAgent(
        projectId: String,
        task: String,
        llmConfig: LLMConfig
    ): AgentResponse {
        return client.post("$baseUrl/api/agent/run") {
            contentType(ContentType.Application.Json)
            setBody(AgentRequest(
                projectId = projectId,
                task = task,
                llmConfig = llmConfig
            ))
        }.body()
    }
    
    fun close() {
        client.close()
    }
}
```

### 2. SSE 流式执行 (推荐用于长时间任务)

```kotlin
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AgentStreamClient(private val baseUrl: String) {
    
    private val client = HttpClient(Android)
    
    fun executeAgentStream(
        projectId: String,
        task: String,
        llmConfig: LLMConfig
    ): Flow<AgentEvent> = flow {
        val response = client.post("$baseUrl/api/agent/stream") {
            contentType(ContentType.Application.Json)
            setBody(AgentRequest(
                projectId = projectId,
                task = task,
                llmConfig = llmConfig
            ))
        }
        
        val channel = response.bodyAsChannel()
        var currentEvent: String? = null
        
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: continue
            
            when {
                line.startsWith("event: ") -> {
                    currentEvent = line.removePrefix("event: ").trim()
                }
                line.startsWith("data: ") -> {
                    val data = line.removePrefix("data: ").trim()
                    val event = parseEvent(currentEvent, data)
                    if (event != null) {
                        emit(event)
                    }
                }
            }
        }
    }
    
    private fun parseEvent(eventType: String?, data: String): AgentEvent? {
        if (eventType == null || data.isEmpty()) return null
        
        return try {
            when (eventType) {
                "iteration" -> Json.decodeFromString<AgentEvent.IterationStart>(data)
                "llm_chunk" -> Json.decodeFromString<AgentEvent.LLMResponseChunk>(data)
                "tool_call" -> Json.decodeFromString<AgentEvent.ToolCall>(data)
                "tool_result" -> Json.decodeFromString<AgentEvent.ToolResult>(data)
                "error" -> Json.decodeFromString<AgentEvent.Error>(data)
                "complete" -> Json.decodeFromString<AgentEvent.Complete>(data)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun close() {
        client.close()
    }
}
```

## 📱 使用示例

### ViewModel 示例

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AgentViewModel : ViewModel() {
    
    private val apiClient = AgentApiClient("http://your-server:8080")
    private val streamClient = AgentStreamClient("http://your-server:8080")
    
    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState
    
    private val _streamEvents = MutableStateFlow<List<AgentEvent>>(emptyList())
    val streamEvents: StateFlow<List<AgentEvent>> = _streamEvents
    
    // 同步执行
    fun executeTask(projectId: String, task: String) {
        viewModelScope.launch {
            _agentState.value = AgentState.Loading
            
            try {
                val response = apiClient.executeAgent(
                    projectId = projectId,
                    task = task,
                    llmConfig = LLMConfig(
                        provider = "openai",
                        modelName = "gpt-4",
                        apiKey = "your-api-key"
                    )
                )
                
                _agentState.value = AgentState.Success(response)
            } catch (e: Exception) {
                _agentState.value = AgentState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    // 流式执行
    fun executeTaskStream(projectId: String, task: String) {
        viewModelScope.launch {
            _agentState.value = AgentState.Streaming
            _streamEvents.value = emptyList()
            
            try {
                streamClient.executeAgentStream(
                    projectId = projectId,
                    task = task,
                    llmConfig = LLMConfig(
                        provider = "openai",
                        modelName = "gpt-4",
                        apiKey = "your-api-key"
                    )
                ).collect { event ->
                    _streamEvents.value = _streamEvents.value + event
                    
                    // 处理完成事件
                    if (event is AgentEvent.Complete) {
                        _agentState.value = AgentState.StreamComplete(event)
                    }
                }
            } catch (e: Exception) {
                _agentState.value = AgentState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        apiClient.close()
        streamClient.close()
    }
}

sealed class AgentState {
    object Idle : AgentState()
    object Loading : AgentState()
    object Streaming : AgentState()
    data class Success(val response: AgentResponse) : AgentState()
    data class StreamComplete(val event: AgentEvent.Complete) : AgentState()
    data class Error(val message: String) : AgentState()
}
```

### Compose UI 示例

```kotlin
@Composable
fun AgentScreen(viewModel: AgentViewModel = viewModel()) {
    val agentState by viewModel.agentState.collectAsState()
    val streamEvents by viewModel.streamEvents.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 输入区域
        var task by remember { mutableStateOf("") }
        
        OutlinedTextField(
            value = task,
            onValueChange = { task = it },
            label = { Text("Task") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.executeTask("my-project", task) }) {
                Text("Execute (Sync)")
            }
            
            Button(onClick = { viewModel.executeTaskStream("my-project", task) }) {
                Text("Execute (Stream)")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 状态显示
        when (val state = agentState) {
            is AgentState.Idle -> Text("Ready")
            is AgentState.Loading -> CircularProgressIndicator()
            is AgentState.Streaming -> {
                Text("Streaming...", style = MaterialTheme.typography.titleMedium)
                LazyColumn {
                    items(streamEvents) { event ->
                        EventCard(event)
                    }
                }
            }
            is AgentState.Success -> {
                Text("Success!", color = Color.Green)
                Text(state.response.message)
            }
            is AgentState.StreamComplete -> {
                Text("Stream Complete!", color = Color.Green)
                Text(state.event.message)
            }
            is AgentState.Error -> {
                Text("Error: ${state.message}", color = Color.Red)
            }
        }
    }
}

@Composable
fun EventCard(event: AgentEvent) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            when (event) {
                is AgentEvent.IterationStart -> {
                    Text("Iteration ${event.current}/${event.max}", fontWeight = FontWeight.Bold)
                }
                is AgentEvent.LLMResponseChunk -> {
                    Text(event.chunk, style = MaterialTheme.typography.bodyMedium)
                }
                is AgentEvent.ToolCall -> {
                    Text("🔧 Tool: ${event.toolName}", fontWeight = FontWeight.Bold)
                    Text(event.params, style = MaterialTheme.typography.bodySmall)
                }
                is AgentEvent.ToolResult -> {
                    Text("✅ Result: ${event.toolName}", fontWeight = FontWeight.Bold)
                    Text(event.output ?: "No output", style = MaterialTheme.typography.bodySmall)
                }
                is AgentEvent.Error -> {
                    Text("❌ Error: ${event.message}", color = Color.Red)
                }
                is AgentEvent.Complete -> {
                    Text("✅ Complete", color = Color.Green, fontWeight = FontWeight.Bold)
                    Text("${event.iterations} iterations, ${event.steps.size} steps")
                }
            }
        }
    }
}
```

## 🔒 安全建议

1. **不要在客户端硬编码 API Key** - 使用服务器端配置或安全存储
2. **使用 HTTPS** - 生产环境必须使用 HTTPS
3. **添加认证** - 实现 JWT 或 OAuth 认证
4. **速率限制** - 防止滥用

## 📚 更多资源

- [mpp-server README](../../mpp-server/README.md)
- [Phase 5 & 6 完成报告](PHASE5-6-COMPLETE.md)
- [Ktor Client 文档](https://ktor.io/docs/client.html)

