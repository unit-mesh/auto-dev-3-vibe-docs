package cc.unitmesh.test

import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.config.PreloadingStatus
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.ToolCategory
import cc.unitmesh.devins.ui.compose.agent.CodingAgentViewModel
import cc.unitmesh.devins.ui.compose.agent.ToolLoadingStatus
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.LLMService
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.LLMProviderType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

/**
 * 自动化测试工具状态栏功能
 */
class ToolStatusBarTest {
    
    fun runAllTests() {
        println("🧪 开始工具状态栏自动化测试...")
        
        testToolTypeIntegration()
        testMcpStatusUpdates()
        testConfigurationResponse()
        
        println("✅ 所有测试完成!")
    }
    
    /**
     * 测试 ToolType 集成
     */
    private fun testToolTypeIntegration() {
        println("\n📋 测试 1: ToolType 集成")
        
        // 验证内置工具数量
        val allBuiltinTools = ToolType.ALL_TOOLS
        println("   内置工具总数: ${allBuiltinTools.size}")
        assert(allBuiltinTools.size >= 5) { "内置工具数量应该至少有 5 个" }
        
        // 验证 SubAgent 数量
        val subAgentTools = ToolType.byCategory(ToolCategory.SubAgent)
        println("   SubAgent 数量: ${subAgentTools.size}")
        assert(subAgentTools.size == 3) { "SubAgent 应该有 3 个" }
        
        // 验证工具名称
        val expectedBuiltinTools = setOf("read-file", "write-file", "grep", "glob", "shell")
        val actualBuiltinTools = allBuiltinTools.map { it.name }.toSet()
        val missingTools = expectedBuiltinTools - actualBuiltinTools
        assert(missingTools.isEmpty()) { "缺少内置工具: $missingTools" }
        
        println("   ✅ ToolType 集成测试通过")
    }
    
    /**
     * 测试 MCP 状态更新
     */
    private fun testMcpStatusUpdates() = runBlocking {
        println("\n📋 测试 2: MCP 状态更新")
        
        // 创建模拟的 ViewModel
        val mockLLMService = LLMService(ModelConfig(
            provider = LLMProviderType.DEEPSEEK,
            modelName = "deepseek-chat",
            apiKey = "test-key"
        ))
        
        val viewModel = CodingAgentViewModel(
            llmService = mockLLMService,
            projectPath = "/test/path",
            maxIterations = 1
        )
        
        // 初始状态检查
        var toolStatus = viewModel.getToolLoadingStatus()
        println("   初始状态 - MCP 工具: ${toolStatus.mcpToolsEnabled}")
        println("   初始状态 - 加载中: ${toolStatus.isLoading}")
        
        // 等待 MCP 预加载完成
        var attempts = 0
        while (toolStatus.isLoading && attempts < 30) {
            delay(1000)
            toolStatus = viewModel.getToolLoadingStatus()
            println("   等待预加载... 尝试 ${attempts + 1}/30")
            attempts++
        }
        
        // 验证最终状态
        println("   最终状态 - MCP 工具: ${toolStatus.mcpToolsEnabled}")
        println("   最终状态 - 加载中: ${toolStatus.isLoading}")
        println("   最终状态 - 服务器: ${toolStatus.mcpServersLoaded}/${toolStatus.mcpServersTotal}")
        
        // 断言
        assert(!toolStatus.isLoading) { "MCP 预加载应该已完成" }
        if (toolStatus.mcpServersTotal > 0) {
            assert(toolStatus.mcpToolsEnabled > 0) { "如果有 MCP 服务器，应该有 MCP 工具" }
        }
        
        viewModel.dispose()
        println("   ✅ MCP 状态更新测试通过")
    }
    
    /**
     * 测试配置响应
     */
    private fun testConfigurationResponse() = runBlocking {
        println("\n📋 测试 3: 配置响应")
        
        try {
            // 加载当前配置
            val toolConfig = ConfigManager.loadToolConfig()
            println("   当前启用的内置工具: ${toolConfig.enabledBuiltinTools.size}")
            println("   当前启用的 MCP 工具: ${toolConfig.enabledMcpTools.size}")
            println("   当前 MCP 服务器: ${toolConfig.mcpServers.size}")
            
            // 验证配置结构
            assert(toolConfig.enabledBuiltinTools.isNotEmpty()) { "应该有启用的内置工具" }
            
            println("   ✅ 配置响应测试通过")
        } catch (e: Exception) {
            println("   ⚠️ 配置加载失败: ${e.message}")
        }
    }
}

fun main() {
    val test = ToolStatusBarTest()
    test.runAllTests()
}
