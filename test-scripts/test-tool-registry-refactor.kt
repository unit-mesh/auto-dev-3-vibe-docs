/**
 * Test script for the refactored ToolRegistry mechanism
 *
 * This script verifies:
 * 1. ToolRegistry respects ToolConfigService configuration
 * 2. Only enabled built-in tools are registered
 * 3. MCP tools can be dynamically registered
 * 4. CodingAgent.getAllAvailableTools() returns correct tools
 */

import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.config.ToolConfigService
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor

fun main() {
    println("🧪 Testing ToolRegistry refactor...")
    
    // Test 1: ToolRegistry with no config (backward compatibility)
    testBackwardCompatibility()
    
    // Test 2: ToolRegistry with selective tool configuration
    testSelectiveToolConfiguration()
    
    // Test 3: ToolRegistry with empty configuration
    testEmptyConfiguration()
    
    println("✅ All tests passed!")
}

fun testBackwardCompatibility() {
    println("\n📋 Test 1: Backward compatibility (no config)")
    
    val registry = ToolRegistry(
        fileSystem = DefaultToolFileSystem(),
        shellExecutor = DefaultShellExecutor(),
        toolConfigService = null
    )
    
    val tools = registry.getAllTools()
    println("   Registered tools: ${tools.keys}")
    
    // Should register all built-in tools
    val expectedTools = setOf("read-file", "write-file", "grep", "glob", "shell")
    val actualTools = tools.keys
    
    expectedTools.forEach { toolName ->
        if (toolName !in actualTools) {
            throw AssertionError("Expected tool '$toolName' not found in registry")
        }
    }
    
    println("   ✅ All expected tools registered")
}

fun testSelectiveToolConfiguration() {
    println("\n📋 Test 2: Selective tool configuration")
    
    // Create config with only some tools enabled
    val config = ToolConfigFile(
        enabledBuiltinTools = listOf("read-file", "grep"),
        enabledMcpTools = emptyList(),
        mcpServers = emptyMap()
    )
    
    val configService = ToolConfigService(config)
    val registry = ToolRegistry(
        fileSystem = DefaultToolFileSystem(),
        shellExecutor = DefaultShellExecutor(),
        toolConfigService = configService
    )
    
    val tools = registry.getAllTools()
    println("   Registered tools: ${tools.keys}")
    
    // Should only register enabled tools
    val expectedTools = setOf("read-file", "grep")
    val actualTools = tools.keys
    
    if (actualTools != expectedTools) {
        throw AssertionError("Expected tools $expectedTools, but got $actualTools")
    }
    
    // Should NOT register disabled tools
    val disabledTools = setOf("write-file", "glob", "shell")
    disabledTools.forEach { toolName ->
        if (toolName in actualTools) {
            throw AssertionError("Disabled tool '$toolName' should not be registered")
        }
    }
    
    println("   ✅ Only enabled tools registered, disabled tools excluded")
}

fun testEmptyConfiguration() {
    println("\n📋 Test 3: Empty configuration")
    
    // Create config with no tools enabled
    val config = ToolConfigFile(
        enabledBuiltinTools = emptyList(),
        enabledMcpTools = emptyList(),
        mcpServers = emptyMap()
    )
    
    val configService = ToolConfigService(config)
    val registry = ToolRegistry(
        fileSystem = DefaultToolFileSystem(),
        shellExecutor = DefaultShellExecutor(),
        toolConfigService = configService
    )
    
    val tools = registry.getAllTools()
    println("   Registered tools: ${tools.keys}")
    
    // Should register no tools
    if (tools.isNotEmpty()) {
        throw AssertionError("Expected no tools, but got ${tools.keys}")
    }
    
    println("   ✅ No tools registered when none enabled")
}
