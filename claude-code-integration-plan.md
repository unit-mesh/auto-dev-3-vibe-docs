# Claude Code 集成方案

基于 IDEA 的 Claude Code 实现分析，制定集成方案。

## 核心发现

### 1. Claude Code 不使用 ACP 协议

**重要**：Claude Code 使用自定义的 JSON 流式协议，**不是 ACP**！

- 通信方式：stdio + JSON 流式消息
- 消息格式：自定义 `ClaudeMessage` 多态系统
- 进程管理：外部二进制文件（需下载）

### 2. IDEA 实现架构

```
ClaudeCodeChatAgent (ChatAgent)
    ↓
ClaudeCodeService (Project Service)
    ↓
ClaudeCodeLongRunningSession (ClaudeCodeSession)
    ↓
ClaudeCodeProcessHandler (KillableProcessHandler)
    ↓
claude-code binary (外部进程)
```

### 3. 核心组件

#### A. ClaudeCodeProcessHandler
- 继承 `KillableProcessHandler`
- 启动和管理 Claude Code 进程
- 解析 stdio 的 JSON 消息流
- 发送到 Kotlin Channel

#### B. 消息协议
- `SystemInitMessage` - 系统初始化
- `UserMessage` - 用户消息
- `AssistantMessage` - 助手回复
  - 包含 `Content`（文本、工具调用、工具结果等）
- `ControlRequestMessage` - 控制请求（中断、设置模型等）

#### C. 进程启动参数
```java
GeneralCommandLine cli = new GeneralCommandLine()
    .withExePath(binaryPath.toString())
    .withWorkingDirectory(workingDirectory)
    .withParameters("-p", "--verbose", "--output-format", "stream-json")
    
// 可选参数
.withParameters("-r", sessionId)                    // 恢复会话
.withParameters("--permission-mode", permissionMode)
.withParameters("--model", model)
.withParameters("--mcp-config", mcpConfig)          // MCP配置
.withParameters("--disallowedTools", tools)         // 禁用工具
.withParameters("--input-format", "stream-json")    // 输入格式
```

#### D. 环境变量
```
ANTHROPIC_BASE_URL
ANTHROPIC_AUTH_TOKEN
ANTHROPIC_API_KEY
ANTHROPIC_CUSTOM_HEADERS
CLAUDE_CODE_GIT_BASH_PATH (Windows)
CLAUDE_CODE_ENTRYPOINT
IJ_MCP_AIA_CHAT_ID
```

## 集成方案对比

### 方案 1：直接集成 Claude Code 二进制 ⭐ 推荐

**优点：**
- ✅ 功能完整（工具调用、MCP、思考显示）
- ✅ 官方支持（IDEA 已验证）
- ✅ 自动更新
- ✅ 权限管理完善

**缺点：**
- ❌ 需要下载外部二进制
- ❌ 不是 ACP 协议（需要新的适配层）
- ❌ 依赖 Anthropic API Key

**工作量：** 中等（3-5天）

**实现步骤：**

1. **下载服务** (`ClaudeCodeDownloadService`)
   - 检测平台（macOS/Linux/Windows）
   - 下载 claude-code 二进制
   - 校验和版本管理

2. **进程管理器** (`ClaudeCodeProcessHandler`)
   - 启动 claude-code 进程
   - 解析 JSON 消息流
   - 管理进程生命周期

3. **会话管理** (`ClaudeCodeSession`)
   - 发送用户消息
   - 接收助手回复
   - 处理工具调用

4. **渲染适配器** (`ClaudeCodeRenderer`)
   - 将 Claude 消息转换为 `CodingAgentRenderer` 调用
   - 处理流式文本
   - 显示工具调用

5. **配置集成**
   - 添加到 `AcpAgentPresets`（虽然不是 ACP，但保持一致性）
   - API Key 配置
   - MCP 配置支持

### 方案 2：使用 Claude API 直接调用

**优点：**
- ✅ 无需外部二进制
- ✅ 实现简单
- ✅ 跨平台

**缺点：**
- ❌ 功能受限（需自行实现工具调用）
- ❌ 无 MCP 支持
- ❌ 缺少 Claude Code 特有功能

**工作量：** 较小（1-2天）

### 方案 3：等待 Claude Code 支持 ACP

**优点：**
- ✅ 完全兼容现有架构
- ✅ 无需新适配层

**缺点：**
- ❌ 不确定何时支持
- ❌ 目前无法使用

## 推荐方案详细设计

### 架构设计

```
xiuper/
├── mpp-core/
│   └── src/
│       └── jvmMain/kotlin/cc/unitmesh/agent/claude/
│           ├── ClaudeCodeClient.kt              # Claude Code 客户端（类似 AcpClient）
│           ├── ClaudeCodeSession.kt             # 会话管理
│           ├── ClaudeCodeProcessHandler.kt      # 进程处理
│           ├── ClaudeCodeDownloadService.kt     # 二进制下载
│           └── protocol/
│               ├── ClaudeMessage.kt             # 消息基类
│               ├── UserMessage.kt
│               ├── AssistantMessage.kt
│               └── Content.kt
│
└── mpp-ui/
    └── src/
        ├── commonMain/kotlin/cc/unitmesh/devins/ui/compose/
        │   ├── config/
        │   │   └── AcpAgentPresets.kt           # [修改] 添加 Claude Code
        │   └── agent/claude/
        │       ├── ClaudeCodeRenderer.kt        # 渲染适配器
        │       └── ClaudeCodeConnectionProvider.kt
        │
        └── jvmMain/kotlin/cc/unitmesh/devins/ui/compose/agent/claude/
            └── ClaudeCodeConnectionProvider.jvm.kt
```

### 核心类设计

#### 1. ClaudeCodeClient.kt

```kotlin
class ClaudeCodeClient(
    private val coroutineScope: CoroutineScope,
    private val binaryPath: Path,
    private val workingDirectory: Path,
    private val apiKey: String,
    private val enableLogging: Boolean = true
) {
    private var process: Process? = null
    private val messagesChannel = Channel<ClaudeMessage>(Channel.UNLIMITED)
    
    suspend fun connect() {
        // 启动 claude-code 进程
        // 设置环境变量
        // 开始解析 JSON 流
    }
    
    suspend fun prompt(text: String, renderer: CodingAgentRenderer) {
        // 发送用户消息
        // 接收和处理响应流
    }
    
    suspend fun disconnect() {
        // 关闭进程
    }
    
    private fun parseJsonStream() {
        // 解析 JSON 消息
        // 发送到 messagesChannel
    }
}
```

#### 2. ClaudeCodeRenderer.kt

```kotlin
class ClaudeCodeRenderer(
    private val renderer: CodingAgentRenderer
) {
    suspend fun renderClaudeMessage(message: ClaudeMessage) {
        when (message) {
            is AssistantMessage -> renderAssistantMessage(message)
            is ToolCallMessage -> renderToolCall(message)
            // ...
        }
    }
    
    private fun renderAssistantMessage(message: AssistantMessage) {
        message.content.forEach { content ->
            when (content.type) {
                "text" -> renderer.renderLLMResponseChunk(content.text)
                "thinking" -> renderer.renderThinkingChunk(content.thinking)
                "tool_use" -> renderToolUse(content)
                // ...
            }
        }
    }
}
```

#### 3. ClaudeCodeConnectionProvider.kt

```kotlin
expect fun createClaudeCodeConnection(): ClaudeCodeConnection?

interface ClaudeCodeConnection {
    val isConnected: Boolean
    
    suspend fun connect(
        config: ClaudeCodeConfig,
        cwd: String
    )
    
    suspend fun prompt(text: String, renderer: CodingAgentRenderer): String
    
    suspend fun cancel()
    
    suspend fun disconnect()
}

// JVM 实现
actual fun createClaudeCodeConnection(): ClaudeCodeConnection? = 
    JvmClaudeCodeConnection()

class JvmClaudeCodeConnection : ClaudeCodeConnection {
    private var client: ClaudeCodeClient? = null
    
    override suspend fun connect(config: ClaudeCodeConfig, cwd: String) {
        val binaryPath = ClaudeCodeDownloadService.getBinaryPath()
        client = ClaudeCodeClient(
            coroutineScope = scope,
            binaryPath = binaryPath,
            workingDirectory = Paths.get(cwd),
            apiKey = config.apiKey
        )
        client?.connect()
    }
    
    override suspend fun prompt(text: String, renderer: CodingAgentRenderer): String {
        val claudeRenderer = ClaudeCodeRenderer(renderer)
        client?.prompt(text, claudeRenderer)
        return "completed"
    }
}
```

### 配置设计

#### AcpAgentPresets.kt（虽然不是 ACP，但保持一致性）

```kotlin
AcpAgentPreset(
    id = "claude-code",
    name = "Claude Code",
    command = "claude-code",  // 会被忽略，使用内部下载的二进制
    args = "",
    env = "ANTHROPIC_API_KEY=",  // 用户需要填写
    description = "Anthropic Claude Code with tool use support"
)
```

#### 配置文件 (~/.autodev/config.yaml)

```yaml
claudeCode:
  enabled: true
  apiKey: "sk-ant-..."  # 或通过环境变量
  baseUrl: "https://api.anthropic.com"  # 可选
  model: "claude-sonnet-4-20250514"  # 默认模型
  permissionMode: "approve_all"  # approve_all, prompt, deny_all
  mcpConfig: ""  # MCP 配置路径（可选）
```

## 实现计划

### Phase 1: 基础架构（2天）

1. ✅ 创建 `ClaudeCodeClient.kt`
2. ✅ 实现进程启动和管理
3. ✅ 实现 JSON 消息解析
4. ✅ 添加单元测试

### Phase 2: 协议实现（1天）

1. ✅ 定义 `ClaudeMessage` 及子类
2. ✅ 实现消息序列化/反序列化
3. ✅ 处理不同类型的 Content

### Phase 3: 渲染集成（1天）

1. ✅ 创建 `ClaudeCodeRenderer`
2. ✅ 适配到 `CodingAgentRenderer`
3. ✅ 处理流式文本
4. ✅ 显示工具调用

### Phase 4: 下载服务（1天）

1. ✅ 实现 `ClaudeCodeDownloadService`
2. ✅ 平台检测
3. ✅ 版本管理
4. ✅ 校验和更新

### Phase 5: UI 集成（1天）

1. ✅ 添加配置界面
2. ✅ API Key 管理
3. ✅ 模型选择
4. ✅ 权限模式设置

### Phase 6: 测试和文档（1天）

1. ✅ 集成测试
2. ✅ 调试工具
3. ✅ 使用文档
4. ✅ 示例

## 与现有 ACP 架构的兼容性

### 相同点

1. **连接管理**
   - 连接/断开/重连
   - 会话生命周期管理

2. **渲染接口**
   - 使用相同的 `CodingAgentRenderer`
   - 流式响应处理

3. **配置方式**
   - 使用 `config.yaml`
   - 环境变量支持

### 不同点

1. **协议**
   - ACP agents: JSON-RPC over stdio (标准化)
   - Claude Code: 自定义 JSON 流式协议

2. **二进制管理**
   - ACP agents: 用户自行安装
   - Claude Code: 应用内下载管理

3. **认证**
   - ACP agents: 各自的认证方式
   - Claude Code: Anthropic API Key

## 挑战和风险

### 技术挑战

1. **JSON 流式解析**
   - 需要处理不完整的 JSON
   - 缓冲和拼接逻辑

2. **跨平台支持**
   - macOS/Linux/Windows 二进制
   - 不同的路径和权限

3. **MCP 集成**
   - 需要理解 Claude Code 的 MCP 配置格式
   - 与现有 MCP 系统集成

### 风险缓解

1. **参考 IDEA 实现**
   - 复用已验证的逻辑
   - 借鉴错误处理

2. **渐进式开发**
   - 先实现基本功能
   - 逐步添加高级特性

3. **充分测试**
   - 单元测试
   - 集成测试
   - 跨平台测试

## 对比：Claude Code vs Copilot

| 特性 | Claude Code | GitHub Copilot |
|------|-------------|----------------|
| 协议 | 自定义 JSON | ACP |
| 安装 | 应用内下载 | 用户安装 |
| 认证 | Anthropic API Key | GitHub 账号 |
| 工具调用 | 完整支持 | 完整支持 |
| MCP | 支持 | 内置 GitHub MCP |
| Thinking | 支持 | 支持 |
| 二进制来源 | Anthropic 官方 | GitHub 官方 |

## 建议

### 短期（现在）

1. **先完成 Copilot 集成的验证和文档**
2. **分析 Claude Code 二进制的可用性**
   - 是否需要许可证
   - 下载地址是否公开

### 中期（1-2周）

1. **实现 Claude Code 基础架构**
   - ClaudeCodeClient
   - 进程管理
   - 消息解析

2. **创建 MVP**
   - 基本对话功能
   - 文本流式显示

### 长期（1个月）

1. **完整功能实现**
   - 工具调用
   - MCP 支持
   - Thinking 显示

2. **与 ACP agents 统一管理**
   - 统一的配置界面
   - 统一的使用体验

## 参考资料

- [IDEA Claude Code 实现](file:///Users/phodal/Downloads/ml-llm/lib/ml-llm/sources/com/intellij/ml/llm/agents/claude/code/)
- [Anthropic Claude API](https://docs.anthropic.com/claude/reference/getting-started-with-the-api)
- [项目 ACP 实现](../mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/acp/)

## 下一步

1. ✅ 完成 Copilot 集成验证
2. 🔄 分析 Claude Code 二进制获取方式
3. ⏳ 实现 Phase 1: 基础架构
4. ⏳ 创建测试用例
5. ⏳ 编写使用文档
