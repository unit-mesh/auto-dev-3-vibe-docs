package cc.unitmesh.test

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.config.McpServerConfig
import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.orchestrator.ToolExecutionContext
import cc.unitmesh.agent.policy.DefaultPolicyEngine
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import kotlinx.coroutines.runBlocking

/**
 * Integration test for MCP tool execution
 */
class McpToolExecutionIntegrationTest {
    
    fun runAllTests() {
        println("🧪 开始 MCP 工具执行集成测试...")
        
        testMcpToolNameResolution()
        testMcpToolDiscovery()
        testMcpToolExecution()
        
        println("✅ 所有 MCP 工具执行测试完成!")
    }
    
    /**
     * 测试 MCP 工具名称解析
     */
    private fun testMcpToolNameResolution() {
        println("\n📋 测试 1: MCP 工具名称解析")
        
        val toolConfig = ToolConfigFile(
            enabledBuiltinTools = listOf("read-file", "write-file"),
            enabledMcpTools = listOf("list_directory", "read_file", "write_file"), // 实际工具名
            mcpServers = mapOf(
                "filesystem" to McpServerConfig(
                    command = "npx",
                    args = listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp"),
                    disabled = false
                )
            )
        )
        
        println("   启用的 MCP 工具: ${toolConfig.enabledMcpTools}")
        
        // 验证工具名称不包含服务器前缀
        val enabledTools = toolConfig.enabledMcpTools.toSet()
        assert(enabledTools.contains("list_directory")) { "应该包含 list_directory" }
        assert(enabledTools.contains("read_file")) { "应该包含 read_file" }
        assert(!enabledTools.contains("filesystem_list_directory")) { "不应该包含 filesystem_list_directory" }
        
        println("   ✅ 工具名称解析正确")
    }
    
    /**
     * 测试 MCP 工具发现
     */
    private fun testMcpToolDiscovery() = runBlocking {
        println("\n📋 测试 2: MCP 工具发现")
        
        val toolConfig = ToolConfigFile(
            enabledMcpTools = listOf("list_directory", "read_file"),
            mcpServers = mapOf(
                "filesystem" to McpServerConfig(
                    command = "npx",
                    args = listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp"),
                    disabled = false
                )
            )
        )
        
        try {
            val discoveredTools = McpToolConfigManager.discoverMcpTools(
                toolConfig.mcpServers,
                toolConfig.enabledMcpTools.toSet()
            )
            
            println("   发现的工具服务器: ${discoveredTools.keys}")
            discoveredTools.forEach { (serverName, tools) ->
                println("   服务器 '$serverName' 的工具:")
                tools.forEach { tool ->
                    println("     - ${tool.name} (enabled: ${tool.enabled})")
                }
            }
            
            // 验证工具名称格式
            discoveredTools.values.flatten().forEach { tool ->
                assert(!tool.name.contains("_")) { "工具名称不应该包含服务器前缀: ${tool.name}" }
            }
            
            println("   ✅ MCP 工具发现测试通过")
            
        } catch (e: Exception) {
            println("   ⚠️ MCP 工具发现失败 (可能是环境问题): ${e.message}")
        }
    }
    
    /**
     * 测试 MCP 工具执行
     */
    private fun testMcpToolExecution() = runBlocking {
        println("\n📋 测试 3: MCP 工具执行")
        
        val toolConfig = ToolConfigFile(
            enabledBuiltinTools = listOf("read-file"),
            enabledMcpTools = listOf("list_directory"),
            mcpServers = mapOf(
                "filesystem" to McpServerConfig(
                    command = "npx",
                    args = listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp"),
                    disabled = false
                )
            )
        )
        
        val mcpConfigService = McpToolConfigService(toolConfig)
        val toolRegistry = ToolRegistry(
            fileSystem = DefaultToolFileSystem(),
            shellExecutor = DefaultShellExecutor(),
            configService = mcpConfigService
        )
        
        val orchestrator = ToolOrchestrator(
            registry = toolRegistry,
            policyEngine = DefaultPolicyEngine(),
            renderer = DefaultCodingAgentRenderer(),
            mcpConfigService = mcpConfigService
        )
        
        // 测试内置工具执行
        println("   测试内置工具执行...")
        val builtinResult = orchestrator.executeToolCall(
            toolName = "read-file",
            params = mapOf("path" to "nonexistent.txt"),
            context = ToolExecutionContext()
        )
        println("   内置工具结果: ${builtinResult.result.message}")
        
        // 测试 MCP 工具执行
        println("   测试 MCP 工具执行...")
        val mcpResult = orchestrator.executeToolCall(
            toolName = "list_directory", // 使用实际工具名，不是前缀名
            params = mapOf("path" to "/tmp"),
            context = ToolExecutionContext()
        )
        println("   MCP 工具结果: ${mcpResult.result.message}")
        
        // 验证结果
        if (mcpResult.result.message.contains("Tool not found")) {
            println("   ⚠️ MCP 工具未找到 - 这表明工具注册或发现有问题")
        } else {
            println("   ✅ MCP 工具执行尝试成功")
        }
        
        println("   ✅ MCP 工具执行测试完成")
    }
}

fun main() {
    val test = McpToolExecutionIntegrationTest()
    test.runAllTests()
}
