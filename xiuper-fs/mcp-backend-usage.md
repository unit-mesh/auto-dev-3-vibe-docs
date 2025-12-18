# MCP Backend 使用说明

MCP (Model Context Protocol) Backend 已重构为使用官方的 Kotlin MCP SDK (`io.modelcontextprotocol:kotlin-sdk`)，实现了真正的跨平台支持。

## 跨平台支持

MCP Backend 现在支持以下平台：
- ✅ JVM (Java/Kotlin)
- ✅ Android
- ✅ iOS
- ✅ JavaScript (Node.js)
- ✅ WebAssembly

所有平台使用统一的实现，无需平台特定代码。

## 核心架构

```
xiuper-fs (MCP Backend)
    ↓ 依赖
io.modelcontextprotocol:kotlin-sdk (官方 Kotlin MCP SDK)
    ↓ 提供
- Client: MCP 客户端
- Transport: 传输层抽象 (StdioClientTransport, SseClientTransport)
- Protocol: 协议定义 (resources, tools, prompts)
```

## 使用示例

### 1. 创建 MCP 客户端并连接服务器

```kotlin
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import cc.unitmesh.xiuper.fs.mcp.DefaultMcpBackend

// 1. 创建 MCP 客户端
val client = Client(clientInfo = Implementation(name = "MyApp", version = "1.0.0"))

// 2. 创建传输层 (Stdio 或 SSE)
// 对于 stdio (例如 npx MCP 服务器):
val transport = processLauncher.launchStdioProcess(
    McpProcessConfig(
        command = "npx",
        args = listOf("@modelcontextprotocol/server-filesystem", "/path/to/data"),
        inheritLoginEnv = true
    )
)

// 3. 连接客户端
client.connect(transport)

// 4. 创建 MCP Backend
val mcpBackend = DefaultMcpBackend(client)
```

### 2. 通过文件系统接口访问 MCP 资源

```kotlin
// 列出所有 MCP 资源
val resources = mcpBackend.list(FsPath("/resources"))
// 返回: [FsEntry.File("file___path_to_file1"), FsEntry.File("file___path_to_file2"), ...]

// 读取特定资源 (URI 中的 / 会被编码为 _)
val content = mcpBackend.read(FsPath("/resources/file___path_to_myfile.txt"), ReadOptions())
println(content.bytes.decodeToString())
```

### 3. 使用 MCP 工具

```kotlin
// 列出所有可用工具
val tools = mcpBackend.list(FsPath("/tools"))
// 返回: [FsEntry.Directory("read_file"), FsEntry.Directory("write_file"), ...]

// 设置工具参数
val args = """{"path": "/path/to/file.txt"}"""
mcpBackend.write(FsPath("/tools/read_file/args"), args.encodeToByteArray(), WriteOptions())

// 执行工具
val result = mcpBackend.write(FsPath("/tools/read_file/run"), ByteArray(0), WriteOptions())
if (result.ok) {
    println("Tool executed: ${result.message}")
}
```

### 4. 完整示例 (JVM)

```kotlin
import cc.unitmesh.agent.mcp.*
import cc.unitmesh.xiuper.fs.mcp.DefaultMcpBackend
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client

suspend fun main() {
    // 使用 mpp-core 的 McpProcessLauncher (跨平台)
    val processLauncher = DefaultMcpProcessLauncher()
    
    // 配置 MCP 服务器
    val config = McpProcessConfig(
        command = "npx",
        args = listOf("@modelcontextprotocol/server-filesystem", "/tmp"),
        inheritLoginEnv = true
    )
    
    // 启动 MCP 服务器并获取传输层
    val transport = processLauncher.launchStdioProcess(config)
    
    // 创建并连接客户端
    val client = Client(clientInfo = Implementation(name = "Xiuper", version = "1.0.0"))
    client.connect(transport)
    
    // 创建 MCP 文件系统后端
    val backend = DefaultMcpBackend(client)
    
    // 现在可以像使用普通文件系统一样使用 MCP 资源
    val files = backend.list(FsPath("/resources"))
    println("Found ${files.size} resources")
    
    // 清理
    client.close()
}
```

## 目录结构

MCP Backend 将 MCP 资源和工具映射为文件系统结构：

```
/                           # 根目录
├── resources/              # MCP 资源根目录
│   ├── file___tmp_test     # 资源 (URI 中的 / 编码为 _)
│   └── http___example.com  # HTTP 资源
└── tools/                  # MCP 工具根目录
    ├── read_file/          # 工具目录
    │   ├── args            # 工具参数 (JSON)
    │   └── run             # 执行工具 (写入触发执行)
    └── write_file/
        ├── args
        └── run
```

## 与 mpp-core 的集成

mpp-core 已经包含了完整的 MCP 管理功能：

```kotlin
// 使用 mpp-core 的 McpClientManager
import cc.unitmesh.agent.mcp.McpClientManager
import cc.unitmesh.agent.mcp.McpConfig

val manager = McpClientManager()

// 初始化配置
manager.initialize(McpConfig(
    mcpServers = mapOf(
        "filesystem" to McpServerConfig(
            command = "npx",
            args = listOf("@modelcontextprotocol/server-filesystem", "/tmp")
        )
    )
))

// 发现所有工具
val allTools = manager.discoverAllTools()

// 获取特定服务器的客户端
val client = manager.clients["filesystem"]
if (client != null) {
    // 使用客户端创建 MCP Backend
    val backend = DefaultMcpBackend(client)
    // ...
}
```

## 参考

- **MCP SDK**: `io.modelcontextprotocol:kotlin-sdk:0.7.2`
- **mpp-core MCP 实现**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/mcp/`
- **Process Launcher**: 各平台的 `McpProcessLauncher` 实现
  - JVM: `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/mcp/McpProcessLauncher.jvm.kt`
  - JS: `mpp-core/src/jsMain/kotlin/cc/unitmesh/agent/mcp/McpProcessLauncher.js.kt`
  - iOS: `mpp-core/src/iosMain/kotlin/cc/unitmesh/agent/mcp/McpProcessLauncher.ios.kt`

## 注意事项

1. **平台限制**: 
   - JS/WASM 平台目前不支持 stdio process 启动
   - 这些平台可以使用 SSE (Server-Sent Events) 传输层连接到 HTTP MCP 服务器

2. **URI 编码**: 
   - MCP 资源的 URI 中的 `/` 会被编码为 `_` 作为文件名
   - 例如: `file:///tmp/test.txt` → `/resources/file___tmp_test.txt`

3. **工具执行**: 
   - 工具参数必须是有效的 JSON 对象
   - 写入 `/tools/{name}/run` 会触发工具执行
   - 执行结果通过 WriteResult 返回
