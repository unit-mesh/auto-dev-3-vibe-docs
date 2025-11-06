# WebFetchTool Test Script

## 概述

测试新实现的 `WebFetchTool`，它使用 Ktor HTTP Client 和 expect/actual 模式实现跨平台 Web 内容抓取。

## 架构设计

### Expect/Actual 模式

使用 Kotlin Multiplatform 的 expect/actual 模式为不同平台提供最优的 HTTP 引擎：

```kotlin
// commonMain: expect 声明
expect object HttpClientFactory {
    fun create(): HttpClient
}

// jvmMain: actual 实现（CIO engine）
actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(CIO) { /* config */ }
    }
}

// jsMain: actual 实现（Js engine - fetch API）
actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(Js) { /* config */ }
    }
}
```

### 依赖版本

- **Ktor**: 3.2.2 (与 Koog AI Framework 保持一致)
- **引擎**:
  - JVM: `ktor-client-cio` (异步协程引擎)
  - JS: `ktor-client-js` (使用 fetch API)

### 工具特性

1. **URL 解析和验证**：自动识别和验证 URL
2. **GitHub URL 转换**：自动将 GitHub blob URL 转换为 raw URL
3. **HTML 转文本**：简单的 HTML 标签剥离
4. **AI 处理**：使用 KoogLLMService 根据用户指令处理内容
5. **错误处理**：完善的超时和错误处理机制

## 测试前准备

### 1. 启用 WebFetchTool

更新配置文件 `~/.autodev/mcp.json`：

```json
{
    "enabledBuiltinTools": [
        "read-file",
        "write-file",
        "edit-file",
        "grep",
        "glob",
        "shell",
        "code-agent",
        "ask-agent",
        "web-fetch"
    ],
    "enabledMcpTools": [],
    "mcpServers": {}
}
```

### 2. 构建项目

```bash
cd /Volumes/source/ai/autocrud
./gradlew :mpp-core:assembleJsPackage
cd mpp-ui && npm run build:ts
```

## 测试用例

### 测试 1: 抓取并总结网页内容

```bash
cd /Volumes/source/ai/autocrud/mpp-ui
node dist/jsMain/typescript/index.js code \
  --task "Summarize the content from https://kotlinlang.org/docs/multiplatform.html" \
  -p /tmp
```

**预期结果**：
- ✅ 工具成功抓取 Kotlin Multiplatform 文档
- ✅ AI 生成内容摘要
- ✅ 显示来源 URL

### 测试 2: GitHub 文件抓取

```bash
cd /Volumes/source/ai/autocrud/mpp-ui
node dist/jsMain/typescript/index.js code \
  --task "Fetch and explain the content from https://github.com/ktorio/ktor/blob/main/README.md" \
  -p /tmp
```

**预期结果**：
- ✅ 自动转换为 raw URL: `https://raw.githubusercontent.com/ktorio/ktor/main/README.md`
- ✅ 成功获取 README 内容
- ✅ AI 解释内容

### 测试 3: 提取特定信息

```bash
cd /Volumes/source/ai/autocrud/mpp-ui
node dist/jsMain/typescript/index.js code \
  --task "Extract the key features from https://ktor.io/docs/client-engines.html" \
  -p /tmp
```

**预期结果**：
- ✅ 抓取 Ktor 客户端引擎文档
- ✅ AI 提取并列出关键特性

### 测试 4: 错误处理 - 无效 URL

```bash
cd /Volumes/source/ai/autocrud/mpp-ui
node dist/jsMain/typescript/index.js code \
  --task "Fetch content from invalid-url-without-protocol" \
  -p /tmp
```

**预期结果**：
- ✅ 报错：URL 必须以 http:// 或 https:// 开头
- ✅ 提供清晰的错误信息

### 测试 5: 错误处理 - 404 页面

```bash
cd /Volumes/source/ai/autocrud/mpp-ui
node dist/jsMain/typescript/index.js code \
  --task "Fetch https://example.com/this-page-does-not-exist-404" \
  -p /tmp
```

**预期结果**：
- ✅ 报错：HTTP 404 Not Found
- ✅ 工具正确处理非 2xx 响应

## 工具注册验证

运行任意命令，查看工具是否正确注册：

```bash
cd /Volumes/source/ai/autocrud/mpp-ui
node dist/jsMain/typescript/index.js code --task "list files" -p /tmp 2>&1 | grep -A 10 "Registered.*tools"
```

**预期输出应包含**：
```
🔧 Registered 8/8 built-in tools
   Built-in tool: read-file
   Built-in tool: write-file
   Built-in tool: edit-file
   Built-in tool: grep
   Built-in tool: glob
   Built-in tool: shell
   Built-in tool: ask-agent
   Built-in tool: web-fetch  ← 新增的工具
```

## 架构亮点

### 1. **跨平台统一抽象**

```kotlin
interface HttpFetcher {
    suspend fun fetch(url: String, timeout: Long = 10000): FetchResult
}
```

### 2. **平台特定优化**

- **JVM**: CIO 引擎 - 完全异步，协程驱动
- **JS**: Js 引擎 - 自动使用浏览器 fetch 或 node-fetch
- **Native** (未来): Darwin/Curl/WinHttp 引擎

### 3. **依赖注入**

通过 `ToolDependencies` 统一管理：
```kotlin
data class ToolDependencies(
    val fileSystem: ToolFileSystem,
    val shellExecutor: ShellExecutor,
    val subAgentManager: SubAgentManager? = null,
    val llmService: KoogLLMService? = null,  // WebFetchTool 需要
    val httpFetcher: HttpFetcher? = null      // WebFetchTool 需要
)
```

### 4. **AI 驱动的内容处理**

不只是简单抓取，而是：
1. 抓取网页内容
2. 转换 HTML 为纯文本
3. 使用 AI (KoogLLMService) 根据用户指令处理
4. 返回智能化结果

## 性能考虑

- **超时控制**: 默认 10 秒
- **内容大小限制**: 最大 100KB
- **HTML 简化**: 移除 script/style 标签
- **连接池**: JVM CIO 引擎支持连接复用

## 已知限制

1. **HTML 转文本**：使用简单的正则表达式，不如专门的 HTML parser 精确
2. **单 URL 支持**：当前实现仅处理第一个 URL
3. **无缓存**：每次请求都重新获取内容

## 故障排除

### 问题：工具未注册

**检查**：
1. 确认 `mcp.json` 中启用了 `web-fetch`
2. 确认 `llmService` 和 `httpFetcher` 都已提供给 `ToolDependencies`

### 问题：编译错误

**检查**：
1. Ktor 版本是否为 3.2.2
2. 是否正确添加了平台特定的引擎依赖

### 问题：运行时错误

**检查**：
1. 网络连接是否正常
2. URL 是否有效且可访问
3. 查看详细错误信息

## 相关文件

- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/WebFetchTool.kt` - 主要实现
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/KtorHttpFetcher.kt` - Ktor HTTP 抓取器
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/HttpClientFactory.kt` - Expect 声明
- `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/tool/impl/HttpClientFactory.jvm.kt` - JVM Actual
- `mpp-core/src/jsMain/kotlin/cc/unitmesh/agent/tool/impl/HttpClientFactory.js.kt` - JS Actual
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/provider/BuiltinToolsProvider.kt` - 工具注册

## 参考资料

- [Ktor Client Documentation](https://ktor.io/docs/client-engines.html)
- [Kotlin Multiplatform expect/actual](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html)
- [Gemini CLI WebFetch Tool](https://github.com/google/generative-ai-cli) - 原始参考实现

## 总结

WebFetchTool 成功集成了：
✅ Ktor 3.2.2 HTTP Client
✅ Expect/Actual 模式实现跨平台
✅ AI 驱动的内容处理
✅ 完整的错误处理
✅ 插件化架构（ToolProvider）

这是一个完整的、生产就绪的工具实现，展示了 Kotlin Multiplatform 的强大能力。



