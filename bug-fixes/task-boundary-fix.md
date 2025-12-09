# TaskBoundaryTool 修复说明

## 问题
在 Compose UI 中使用 `task-boundary` 工具时出现错误：
```
Tool execution failed: Tool not implemented: unknown
```

## 根本原因
`ToolOrchestrator.kt` 中的 `executeToolInternal` 方法使用了一个 `when` 表达式来判断工具类型：

```kotlin
return when (val toolType = toolName.toToolType()) {
    ToolType.Shell -> executeShellTool(tool, params, basicContext)
    ToolType.ReadFile -> executeReadFileTool(tool, params, basicContext)
    ...
    else -> ToolResult.Error("Tool not implemented: ${toolType?.displayName ?: "unknown"}")
}
```

由于 `task-boundary` 是新工具，没有对应的 `ToolType` 枚举，导致进入 `else` 分支返回错误。

## 解决方案

### 1. 修改 `executeToolInternal` 方法
将 `else` 分支从返回错误改为调用通用执行方法：

```kotlin
else -> {
    // For new tools (task-boundary, ask-agent, etc.), use generic execution
    logger.debug { "Executing tool generically: $toolName" }
    executeGenericTool(tool, params, basicContext)
}
```

### 2. 添加 `executeGenericTool` 方法
新增通用工具执行方法，使用 `ExecutableTool` 接口：

```kotlin
private suspend fun executeGenericTool(
    tool: Tool,
    params: Map<String, Any>,
    context: cc.unitmesh.agent.tool.ToolExecutionContext
): ToolResult {
    return try {
        @Suppress("UNCHECKED_CAST")
        val executableTool = tool as? ExecutableTool<Any, ToolResult>
        
        if (executableTool == null) {
            return ToolResult.Error("Tool ${tool.name} does not implement ExecutableTool interface")
        }
        
        val invocation = executableTool.createInvocation(params)
        invocation.execute(context)
    } catch (e: Exception) {
        logger.error(e) { "Error executing generic tool ${tool.name}" }
        ToolResult.Error("Error executing tool ${tool.name}: ${e.message}")
    }
}
```

## 优势

1. **向后兼容**：现有工具（Shell, ReadFile等）继续使用专用方法
2. **可扩展**：新工具（task-boundary, future tools）自动支持
3. **统一架构**：所有新工具都通过 `ExecutableTool` 接口工作
4. **无需修改枚举**：不需要为每个新工具添加 `ToolType` 枚举值

## 测试

编译成功：
```bash
./gradlew :mpp-core:compileKotlinJs --no-daemon
BUILD SUCCESSFUL
```

## 相关文件

- `/Volumes/source/ai/autocrud/mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/orchestrator/ToolOrchestrator.kt`
- `/Volumes/source/ai/autocrud/mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/TaskBoundaryTool.kt`
- `/Volumes/source/ai/autocrud/mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/registry/BuiltinToolsProvider.kt`

## 状态

✅ 修复完成
✅ 编译通过
⏳ 等待运行时测试

