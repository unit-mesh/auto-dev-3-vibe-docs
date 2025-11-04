# MCP Preloading Test Script

This script tests the MCP server preloading functionality added to `McpToolConfigManager` and `CodingAgentViewModel`.

## Test Overview

The implementation adds the following features:

1. **Background MCP Server Preloading**: `McpToolConfigManager.init()` now starts background preloading of all configured MCP servers
2. **Preloading Status Tracking**: Added methods to check preloading status and get preloaded server information
3. **UI Integration**: `CodingAgentViewModel` automatically starts MCP preloading when initialized and provides status updates

## Key Changes Made

### McpToolConfigManager.kt
- Added preloading state management with `isPreloading`, `preloadingJob`, `preloadedServers`
- Enhanced `init()` method to start background preloading of all configured MCP servers
- Added status query methods: `isPreloading()`, `getPreloadedServers()`, `isServerPreloaded()`, `waitForPreloading()`, `getPreloadingStatus()`
- Added `PreloadingStatus` data class to track preloading state

### CodingAgentViewModel.kt
- Added MCP preloading state properties: `mcpPreloadingStatus`, `mcpPreloadingMessage`
- Added `init` block that automatically starts MCP preloading when ViewModel is created
- Added `startMcpPreloading()` method that monitors preloading progress and updates UI state
- Added helper methods: `getMcpPreloadingStatus()`, `getMcpPreloadingMessage()`, `areMcpServersReady()`

## Test Steps

### 1. Basic Functionality Test

```kotlin
// Test that preloading starts automatically
val viewModel = CodingAgentViewModel(llmService, projectPath)

// Check initial state
println("Initial preloading status: ${viewModel.getMcpPreloadingMessage()}")

// Wait for preloading to complete
while (!viewModel.areMcpServersReady()) {
    delay(100)
    println("Status: ${viewModel.getMcpPreloadingMessage()}")
}

println("Final status: ${viewModel.getMcpPreloadingMessage()}")
```

### 2. Status Tracking Test

```kotlin
// Test McpToolConfigManager status methods
val toolConfig = ToolConfigFile(
    mcpServers = mapOf(
        "filesystem" to McpServerConfig(
            command = "npx",
            args = listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp")
        )
    )
)

McpToolConfigManager.init(toolConfig)

// Check preloading status
println("Is preloading: ${McpToolConfigManager.isPreloading()}")
println("Preloaded servers: ${McpToolConfigManager.getPreloadedServers()}")

// Wait for completion
McpToolConfigManager.waitForPreloading()

println("Final preloaded servers: ${McpToolConfigManager.getPreloadedServers()}")
```

### 3. Error Handling Test

Test with invalid MCP server configuration to ensure graceful error handling:

```kotlin
val invalidConfig = ToolConfigFile(
    mcpServers = mapOf(
        "invalid" to McpServerConfig(
            command = "nonexistent-command",
            args = listOf("invalid", "args")
        )
    )
)

McpToolConfigManager.init(invalidConfig)
// Should handle errors gracefully and continue with other servers
```

## Expected Behavior

1. **Automatic Preloading**: When `CodingAgentViewModel` is created, it should automatically start preloading MCP servers in the background
2. **Status Updates**: The UI should show loading messages like "Loading MCP servers..." and "MCP servers loaded successfully (X/Y servers)"
3. **Non-blocking**: The preloading should not block the UI or other operations
4. **Caching**: Successfully preloaded tools should be cached for faster subsequent access
5. **Error Resilience**: Failed servers should not prevent other servers from loading

## Benefits

- **Faster Tool Access**: MCP tools are pre-loaded and cached, avoiding delays during actual usage
- **Better UX**: Users see loading status and know when MCP servers are ready
- **Improved Performance**: Subsequent tool discovery operations use cached results
- **Reliability**: Background loading with proper error handling ensures robust operation
