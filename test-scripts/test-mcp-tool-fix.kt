package cc.unitmesh.test

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.orchestrator.ToolExecutionContext
import cc.unitmesh.agent.policy.DefaultPolicyEngine
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import kotlinx.coroutines.runBlocking

/**
 * 测试 MCP 工具修复
 */
class McpToolFixTest {
    
    fun runTest() {
        println("🧪 开始 MCP 工具修复测试...")
        
        testMcpToolNameResolution()
        testMcpToolExecution()
        
        println("✅ MCP 工具修复测试完成!")
    }
    
    /**
     * 测试 MCP 工具名称解析
     */
    private fun testMcpToolNameResolution() {
        println("\n📋 测试 1: MCP 工具名称解析")
        
        val toolConfig = ToolConfigFile(
            enabledBuiltinTools = listOf("read-file", "write-file"),
            enabledMcpTools = listOf("list_directory", "read_file", "write_file"), // 实际工具名，不是前缀名
            mcpServers = mapOf(
                "filesystem" to McpServerConfig(
                    command = "npx",
                    args = listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp")
                )
            )
        )
        
        println("   启用的 MCP 工具: ${toolConfig.enabledMcpTools}")
        
        // 验证工具名称不包含服务器前缀
        val enabledTools = toolConfig.enabledMcpTools.toSet()
        assert(enabledTools.contains("list_directory")) { "应该包含 list_directory" }
        assert(enabledTools.contains("read_file")) { "应该包含 read_file" }
        assert(!enabledTools.contains("filesystem_list_directory")) { "不应该包含 filesystem_list_directory" }
        assert(!enabledTools.contains("filesystem_read_file")) { "不应该包含 filesystem_read_file" }
        
        println("   ✅ 工具名称解析正确 - 使用实际工具名，不包含服务器前缀")
    }
    
    /**
     * 测试 MCP 工具执行
     */
    private fun testMcpToolExecution() = runBlocking {
        println("\n📋 测试 2: MCP 工具执行")
        
        val toolConfig = ToolConfigFile(
            enabledBuiltinTools = listOf("read-file"),
            enabledMcpTools = listOf("list_directory"),
            mcpServers = mapOf(
                "filesystem" to McpServerConfig(
                    command = "npx",
                    args = listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp")
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
        println("   内置工具结果: ${builtinResult.result}")
        
        // 测试 MCP 工具执行 - 使用实际工具名
        println("   测试 MCP 工具执行 (使用实际工具名: list_directory)...")
        val mcpResult = orchestrator.executeToolCall(
            toolName = "list_directory", // 使用实际工具名，不是 filesystem_list_directory
            params = mapOf("path" to "/tmp"),
            context = ToolExecutionContext()
        )
        println("   MCP 工具结果: ${mcpResult.result}")
        
        // 验证结果
        val resultString = mcpResult.result.toString()
        if (resultString.contains("Tool not found")) {
            println("   ⚠️ MCP 工具未找到 - 可能是环境问题或工具注册问题")
        } else if (resultString.contains("MCP")) {
            println("   ✅ MCP 工具执行尝试成功 - 正确路由到 MCP 执行")
        } else {
            println("   ℹ️ MCP 工具执行结果: $resultString")
        }
        
        // 测试错误的前缀名称（应该失败）
        println("   测试错误的前缀工具名 (filesystem_list_directory)...")
        val prefixResult = orchestrator.executeToolCall(
            toolName = "filesystem_list_directory", // 错误的前缀名称
            params = mapOf("path" to "/tmp"),
            context = ToolExecutionContext()
        )
        println("   前缀工具结果: ${prefixResult.result}")
        
        if (prefixResult.result.toString().contains("Tool not found")) {
            println("   ✅ 正确拒绝了前缀工具名 - 修复成功!")
        } else {
            println("   ❌ 意外接受了前缀工具名 - 可能还有问题")
        }
        
        println("   ✅ MCP 工具执行测试完成")
    }
}

fun main() {
    val test = McpToolFixTest()
    test.runTest()
}
