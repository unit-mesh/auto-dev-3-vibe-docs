# MCP 工具执行修复总结

## 🎯 问题描述

用户报告在调用 MCP 工具时出现错误：
```
Error: Tool not found: filesystem_list_directory
```

**根本原因分析**：
1. **工具名称问题**: MCP 工具被错误地注册为 `${serverName}_${toolName}` 格式（如 `filesystem_list_directory`）
2. **架构缺陷**: `ToolOrchestrator` 只处理内置工具，没有处理 MCP 工具
3. **配置不一致**: 配置文件中使用了服务器前缀的工具名称

## ✅ 修复方案

### 1. 修复工具名称格式

**问题**: MCP 工具名称包含服务器前缀
```kotlin
// 修复前 (错误)
name = "${serverName}_${toolInfo.name}" // filesystem_list_directory

// 修复后 (正确)  
name = toolInfo.name // list_directory
```

**修复文件**:
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/config/McpToolConfigManager.kt`

### 2. 扩展 ToolOrchestrator 支持 MCP 工具

**问题**: `ToolOrchestrator.executeToolInternal()` 只处理内置工具

**修复**: 添加 MCP 工具支持
```kotlin
private suspend fun executeToolInternal(toolName: String, params: Map<String, Any>, context: ToolExecutionContext): ToolResult {
    // 首先尝试内置工具
    val tool = registry.getTool(toolName)
    if (tool != null) {
        // 执行内置工具...
    }
    
    // 如果不是内置工具，尝试 MCP 工具
    return executeMcpTool(toolName, params, context)
}
```

**新增方法**:
- `executeMcpTool()`: 执行 MCP 工具
- `findMcpServerForTool()`: 查找提供指定工具的服务器
- `convertParamsToJson()`: 转换参数为 JSON 格式

### 3. 修复配置文件

**修复前**:
```json
{
  "enabledMcpTools": [
    "filesystem_read_file",
    "filesystem_list_directory",
    "filesystem_write_file"
  ]
}
```

**修复后**:
```json
{
  "enabledMcpTools": [
    "read_file",
    "list_directory", 
    "write_file"
  ]
}
```

### 4. 传递 McpToolConfigService

**问题**: `CodingAgent` 创建 `ToolOrchestrator` 时没有传递 MCP 配置服务

**修复**:
```kotlin
private val toolOrchestrator = ToolOrchestrator(
    toolRegistry, 
    policyEngine, 
    renderer,
    mcpConfigService = mcpToolConfigService // 添加 MCP 配置服务
)
```

## 🧪 测试验证

### 单元测试
创建了 `McpToolExecutionTest.kt` 验证：
- MCP 工具名称不包含服务器前缀
- `ToolOrchestrator` 正确处理 MCP 工具
- 参数转换正确

### 集成测试
创建了 `test-mcp-tool-fix.kt` 验证：
- 工具名称解析正确
- MCP 工具执行路由正确
- 拒绝错误的前缀工具名

### 配置迁移
创建了 `migrate-mcp-config.kt` 自动迁移现有配置文件

## 📊 修复效果

### 修复前
```
❌ Error: Tool not found: filesystem_list_directory
❌ 工具名称: filesystem_list_directory (包含服务器前缀)
❌ ToolOrchestrator 不支持 MCP 工具
```

### 修复后
```
✅ 工具名称: list_directory (实际工具名)
✅ ToolOrchestrator 支持 MCP 工具执行
✅ 正确路由到 McpToolConfigManager.executeTool()
```

### 状态栏显示
```
🔵 Built-in (1/5)     - 1个启用/5个可用
🟣 SubAgents (3/3)    - 3个 AI 子代理全部启用  
🟢 MCP Tools (14/14)  - 14个 MCP 工具正确显示
✓ All tools ready     - 所有工具加载完成
```

## 🚀 技术价值

1. **架构完整性**: MCP 工具现在完全集成到工具执行流程中
2. **名称一致性**: 工具名称使用实际名称，不包含服务器前缀
3. **错误处理**: 提供清晰的错误信息和回退机制
4. **可测试性**: 完整的单元测试和集成测试覆盖
5. **向后兼容**: 提供配置迁移工具

## 🎯 关键修复点

1. **工具注册**: MCP 工具使用 `toolInfo.name` 而不是 `${serverName}_${toolInfo.name}`
2. **工具执行**: `ToolOrchestrator` 支持 MCP 工具执行路径
3. **服务器查找**: 动态查找提供指定工具的 MCP 服务器
4. **参数转换**: 正确转换参数为 MCP 所需的 JSON 格式
5. **配置一致**: 配置文件使用实际工具名称

这个修复确保了 MCP 工具能够正确执行，解决了 "Tool not found" 错误，并提供了完整的测试覆盖！🎉
