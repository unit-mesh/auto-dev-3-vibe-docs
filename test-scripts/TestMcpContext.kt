package cc.unitmesh.test

import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.LLMProviderType
import kotlinx.coroutines.runBlocking

class TestRenderer : CodingAgentRenderer {
    override fun renderIterationHeader(current: Int, max: Int) = println("=== Iteration $current/$max ===")
    override fun renderLLMResponseStart() = println("🤖 LLM Response:")
    override fun renderLLMResponseChunk(chunk: String) {
        // Check if the chunk contains MCP tools
        if (chunk.contains("filesystem_") || chunk.contains("context7_")) {
            println("✅ Found MCP tools in LLM response!")
        }
        print(chunk)
    }
    override fun renderLLMResponseEnd() = println("\n")
    override fun renderToolCall(toolName: String, paramsStr: String) = println("🔧 Tool Call: $toolName($paramsStr)")
    override fun renderToolResult(toolName: String, success: Boolean, output: String?, fullOutput: String?) = 
        println("🔧 Tool Result: $toolName -> ${if (success) "✓" else "✗"}")
    override fun renderTaskComplete() = println("✅ Task Complete")
    override fun renderFinalResult(success: Boolean, message: String, iterations: Int) = 
        println("🏁 Final: ${if (success) "✅" else "❌"} $message")
    override fun renderError(message: String) = println("❌ Error: $message")
    override fun renderRepeatWarning(toolName: String, count: Int) = 
        println("⚠️ Repeat Warning: $toolName called $count times")
    override fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>) = 
        println("🔐 Confirmation needed for $toolName")
}

fun main() = runBlocking {
    println("🧪 Testing MCP tools in CodingAgent context...")
    
    try {
        // Load configuration
        val toolConfig = ConfigManager.loadToolConfig()
        val mcpToolConfigService = McpToolConfigService(toolConfig)
        
        // Create a simple LLM service that just returns a simple response
        val modelConfig = ModelConfig(
            provider = LLMProviderType.DEEPSEEK,
            modelName = "deepseek-chat",
            apiKey = "test-key",
            temperature = 0.7,
            maxTokens = 1000,
            baseUrl = "https://api.deepseek.com"
        )
        val llmService = KoogLLMService(modelConfig)
        
        // Create CodingAgent
        val codingAgent = CodingAgent(
            projectPath = "/Volumes/source/ai/autocrud",
            llmService = llmService,
            maxIterations = 1,
            renderer = TestRenderer(),
            mcpToolConfigService = mcpToolConfigService
        )
        
        // Create a simple test task
        val testTask = AgentTask(
            requirement = "List the files in the current directory using available tools",
            projectPath = "/Volumes/source/ai/autocrud"
        )
        
        println("🧪 Executing test task to trigger context building...")
        
        // This will trigger buildContext which should include MCP tools
        // The CodingAgentPromptRenderer will print debug info about the tools
        val result = codingAgent.executeTask(testTask)
        
        println("🧪 Task execution result: ${result.success}")
        println("🧪 Test completed. Check the logs above for MCP tools in context.")
        
    } catch (e: Exception) {
        println("⚠️ Test failed: ${e.message}")
        e.printStackTrace()
    }
}
