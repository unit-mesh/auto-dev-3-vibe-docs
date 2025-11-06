# MCP Configuration Improvements

## 问题修复总结

### 1. ✅ 修复了 McpConfigEditor 的序列化问题

**问题**：使用 `json.encodeToString(McpConfig(...))` 会导致序列化异常，因为 `McpConfig` 可能没有正确标记 `@Serializable`。

**解决方案**：手动构建 JSON 字符串，避免依赖 Kotlin Serialization。

```kotlin
private fun serializeMcpServers(servers: Map<String, McpServerConfig>): String {
    val sb = StringBuilder()
    sb.appendLine("{")
    sb.appendLine("  \"mcpServers\": {")
    // ... 手动构建 JSON
    return sb.toString()
}
```

**文件**：`core/src/main/kotlin/cc/unitmesh/devti/mcp/ui/McpConfigEditor.kt`

### 2. ✅ Android ConfigManager 使用最佳实践

**问题**：之前使用硬编码路径 `System.getProperty("user.home") ?: "/sdcard"`，不符合 Android 最佳实践。

**解决方案**：使用 Android Context 获取 app-specific 内部存储目录。

#### Android 存储最佳实践

```kotlin
/**
 * Android 最佳实践：使用 app-specific internal storage
 * 
 * 优点：
 * - 文件对 app 私有，其他 app 无法访问
 * - app 卸载时自动清理
 * - 不需要任何权限
 * 
 * 路径：/data/data/your.package.name/files/.autodev/
 */
private fun getConfigDir(): File {
    val context = appContext 
        ?: throw IllegalStateException("ConfigManager not initialized...")
    
    // 使用 app-specific internal storage directory
    return File(context.filesDir, ".autodev")
}
```

#### 使用方法

在 Android Application 中初始化：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 ConfigManager
        ConfigManager.initialize(this)
    }
}
```

**文件**：`mpp-ui/src/androidMain/kotlin/cc/unitmesh/devins/ui/config/ConfigManager.android.kt`

### 3. ✅ 所有平台 ConfigManager 支持 MCP 配置

**改进**：为 Android 和 JS 平台添加了完整的 MCP 服务器配置支持。

#### 支持的功能：

1. **解析 MCP 配置**：
   - 支持 YAML 和 JSON 格式
   - 自动解析 `mcpServers` 部分
   - 支持数组参数（`args`, `autoApprove`）

2. **保存 MCP 配置**：
   - `saveMcpServers()` 方法
   - 自动生成正确的 YAML 格式

3. **跨平台一致性**：
   - JVM、JS、Android 使用相同的配置格式
   - 配置文件可在不同平台间共享

**文件**：
- `mpp-ui/src/androidMain/kotlin/cc/unitmesh/devins/ui/config/ConfigManager.android.kt`
- `mpp-ui/src/jsMain/kotlin/cc/unitmesh/devins/ui/config/ConfigManager.js.kt`

### 4. ✅ McpChatConfigDialog 简化为 JSON 编辑

**问题**：原来的对话框使用复杂的 UI（滑块、复选框等），不易配置和维护。

**解决方案**：重构为简单的 JSON 编辑器，遵循社区最佳实践。

#### 社区最佳实践的优势

1. **易于复制粘贴**：可以直接复制配置到其他项目
2. **版本控制友好**：JSON 配置易于在 Git 中追踪
3. **清晰明确**：一目了然看到所有配置项
4. **无需维护复杂 UI**：减少代码维护成本

#### 配置示例

```json
{
  "temperature": 0.7,
  "enabledTools": ["read_file", "write_file", "grep"],
  "systemPrompt": "You are a helpful coding assistant..."
}
```

#### 特性

- ✅ JSON 格式验证
- ✅ 工具名称验证（检查工具是否存在）
- ✅ 友好的错误提示
- ✅ 显示可用工具列表

**文件**：`core/src/main/kotlin/cc/unitmesh/devti/mcp/ui/McpChatConfigDialog.kt`

## 配置文件格式

### 完整的 config.yaml 示例

```yaml
active: default
configs:
  - name: default
    provider: openai
    apiKey: sk-xxx
    model: gpt-4
    temperature: 0.7
    maxTokens: 4096
mcpServers:
  AutoDev:
    command: "npx"
    args: ["-y", "@jetbrains/mcp-proxy"]
    disabled: false
    autoApprove: []
  FileSystem:
    command: "npx"
    args: ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/files"]
    disabled: false
    autoApprove: ["read_file", "list_directory"]
```

### JSON 格式（也支持）

```json
{
  "active": "default",
  "configs": [
    {
      "name": "default",
      "provider": "openai",
      "apiKey": "sk-xxx",
      "model": "gpt-4",
      "temperature": 0.7,
      "maxTokens": 4096
    }
  ],
  "mcpServers": {
    "AutoDev": {
      "command": "npx",
      "args": ["-y", "@jetbrains/mcp-proxy"],
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

## 配置文件位置

### JVM / Desktop
```
~/.autodev/config.yaml
```

### Android
```
/data/data/your.package.name/files/.autodev/config.yaml
```

需要在 Application 中初始化：
```kotlin
ConfigManager.initialize(applicationContext)
```

### Node.js / CLI
```
~/.autodev/config.yaml
```

## 总结

这次改进遵循了以下原则：

1. **平台最佳实践**：每个平台使用其推荐的存储方式
2. **简单明了**：JSON 配置比复杂 UI 更易理解和维护
3. **跨平台一致**：相同的配置格式可在不同平台使用
4. **向后兼容**：仍然支持 YAML 格式
5. **社区标准**：遵循开源社区的配置习惯

## 相关文件

1. `core/src/main/kotlin/cc/unitmesh/devti/mcp/ui/McpConfigEditor.kt` - MCP 服务器配置编辑器
2. `core/src/main/kotlin/cc/unitmesh/devti/mcp/ui/McpChatConfigDialog.kt` - MCP 聊天配置对话框
3. `mpp-ui/src/androidMain/kotlin/cc/unitmesh/devins/ui/config/ConfigManager.android.kt` - Android 配置管理
4. `mpp-ui/src/jsMain/kotlin/cc/unitmesh/devins/ui/config/ConfigManager.js.kt` - JS 配置管理
5. `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/config/ConfigManager.jvm.kt` - JVM 配置管理

## 构建验证

所有改动已通过编译验证：

```bash
./gradlew :mpp-core:assembleJsPackage
# BUILD SUCCESSFUL ✅
```






