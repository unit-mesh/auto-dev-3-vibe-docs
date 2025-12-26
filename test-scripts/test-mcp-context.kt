// Test script to verify MCP tools are included in CodingAgent context

import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.LLMService
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("🧪 Testing MCP tools in CodingAgent context...")
    
    // Load configuration
    val toolConfig = ConfigManager.loadToolConfig()
    val mcpToolConfigService = McpToolConfigService(toolConfig)
    
    // Create a mock LLM service
    val llmService = object : LLMService {
        override suspend fun completion(prompt: String): String = "Mock response"
        override suspend fun streamCompletion(prompt: String, onChunk: (String) -> Unit): String = "Mock response"
    }
    
    // Create CodingAgent
    val codingAgent = CodingAgent(
        projectPath = "/Volumes/source/ai/autocrud",
        llmService = llmService,
        maxIterations = 1,
        renderer = object : cc.unitmesh.agent.CodingAgentRenderer {
            override fun renderStep(step: String) = println("Step: $step")
            override fun renderError(error: String) = println("Error: $error")
            override fun renderResult(result: String) = println("Result: $result")
            override fun clearError() {}
        },
        mcpToolConfigService = mcpToolConfigService
    )
    
    // Create a simple test task
    val testTask = AgentTask(
        instruction = "List available tools",
        projectPath = "/Volumes/source/ai/autocrud"
    )
    
    println("🧪 Executing test task to trigger context building...")
    
    try {
        // This will trigger buildContext which should include MCP tools
        codingAgent.execute(testTask)
    } catch (e: Exception) {
        println("⚠️ Task execution failed (expected): ${e.message}")
    }
    
    println("🧪 Test completed. Check the logs above for MCP tools in context.")
}
