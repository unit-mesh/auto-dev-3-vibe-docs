// Simple test to verify MCP tools are included in CodingAgent context

import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.runBlocking

class TestRenderer : CodingAgentRenderer {
    override fun renderStep(step: String) = println("Step: $step")
    override fun renderError(error: String) = println("Error: $error")
    override fun renderResult(result: String) = println("Result: $result")
    override fun clearError() {}
    override fun renderIterationHeader(iteration: Int, maxIterations: Int) = 
        println("=== Iteration $iteration/$maxIterations ===")
    override fun renderLLMResponseStart() = println("🤖 LLM Response:")
    override fun renderLLMResponseChunk(chunk: String) = print(chunk)
    override fun renderLLMResponseEnd() = println("\n")
    override fun renderToolCall(toolName: String, params: String) = 
        println("🔧 Tool Call: $toolName($params)")
    override fun renderTaskComplete() = println("✅ Task Complete")
    override fun renderRepeatWarning(toolName: String, count: Int) = 
        println("⚠️ Repeat Warning: $toolName called $count times")
}

class MockLLMService : KoogLLMService {
    override suspend fun completion(prompt: String): String {
        println("🤖 [MockLLM] Received prompt with ${prompt.length} characters")
        // Look for tool list in the prompt
        if (prompt.contains("filesystem_")) {
            println("✅ [MockLLM] Found MCP tools in prompt!")
        } else {
            println("❌ [MockLLM] No MCP tools found in prompt")
        }
        return "Task completed successfully."
    }
    
    override suspend fun streamCompletion(prompt: String, onChunk: (String) -> Unit): String {
        val response = completion(prompt)
        onChunk(response)
        return response
    }
}

fun main() = runBlocking {
    println("🧪 Testing MCP tools in CodingAgent context...")
    
    try {
        // Load configuration
        val toolConfig = ConfigManager.loadToolConfig()
        val mcpToolConfigService = McpToolConfigService(toolConfig)
        
        // Create CodingAgent
        val codingAgent = CodingAgent(
            projectPath = "/Volumes/source/ai/autocrud",
            llmService = MockLLMService(),
            maxIterations = 1,
            renderer = TestRenderer(),
            mcpToolConfigService = mcpToolConfigService
        )
        
        // Create a simple test task
        val testTask = AgentTask(
            instruction = "List the files in the current directory",
            projectPath = "/Volumes/source/ai/autocrud"
        )
        
        println("🧪 Executing test task to trigger context building...")
        
        // This will trigger buildContext which should include MCP tools
        val result = codingAgent.executeTask(testTask)
        
        println("🧪 Task execution result: ${result.success}")
        println("🧪 Test completed. Check the logs above for MCP tools in context.")
        
    } catch (e: Exception) {
        println("⚠️ Test failed: ${e.message}")
        e.printStackTrace()
    }
}
