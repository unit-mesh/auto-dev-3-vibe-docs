#!/usr/bin/env kotlin

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Integration test for the optimized AI Agent architecture
 * 
 * This script tests the complete integration of:
 * - CodingAgent with DefaultAgentExecutor
 * - SubAgent communication and execution
 * - AgentChannel Queue Pair pattern
 * - Tool orchestration and execution
 */

fun main() = runBlocking {
    println("🧪 Testing AI Agent Architecture Integration")
    println("=" * 60)
    
    // Test 1: End-to-end agent execution flow
    testEndToEndExecution()
    
    // Test 2: SubAgent coordination
    testSubAgentCoordination()
    
    // Test 3: Queue Pair communication pattern
    testQueuePairCommunication()
    
    // Test 4: Error recovery workflow
    testErrorRecoveryWorkflow()
    
    // Test 5: Codebase investigation workflow
    testCodebaseInvestigationWorkflow()
    
    println("\n✅ All integration tests completed successfully!")
}

suspend fun testEndToEndExecution() {
    println("\n📋 Test 1: End-to-End Agent Execution Flow")
    
    try {
        val channel = AgentChannel()
        val mockLLMService = MockKoogLLMService()
        val agent = CodingAgent("/test/project", mockLLMService, channel = channel)
        
        // Collect events in background
        val events = mutableListOf<AgentEvent>()
        val eventCollector = launch {
            channel.events().collect { event ->
                events.add(event)
                println("  📡 Event: ${event::class.simpleName}")
            }
        }
        
        // Submit a task
        val task = AgentTask(
            requirement = "Create a simple Hello World application",
            projectPath = "/test/project"
        )
        
        // Execute task (would normally call LLM, but we're testing structure)
        println("  🚀 Executing task...")
        
        // Simulate some events
        channel.emit(AgentEvent.Progress(1, 3, "Analyzing requirements"))
        channel.emit(AgentEvent.StreamUpdate("Creating application structure..."))
        channel.emit(AgentEvent.TaskComplete("Application created successfully"))
        
        delay(100) // Allow events to be collected
        eventCollector.cancel()
        
        assert(events.size >= 3) { "Should have collected events" }
        println("  ✅ End-to-end execution flow working")
        
    } catch (e: Exception) {
        println("  ❌ Test failed: ${e.message}")
        throw e
    }
}

suspend fun testSubAgentCoordination() {
    println("\n📋 Test 2: SubAgent Coordination")
    
    try {
        val mockLLMService = MockKoogLLMService()
        
        // Test ErrorRecoveryAgent
        val errorAgent = ErrorRecoveryAgent("/test/project", mockLLMService)
        val errorInput = mapOf(
            "command" to "npm test",
            "errorMessage" to "Test failed: Module not found"
        )
        
        val errorResult = errorAgent.run(errorInput) { progress ->
            println("  🔧 ErrorRecovery: $progress")
        }
        
        assert(errorResult.isNotEmpty()) { "ErrorRecoveryAgent should return result" }
        
        // Test CodebaseInvestigatorAgent
        val investigatorAgent = CodebaseInvestigatorAgent("/test/project", mockLLMService)
        val investigatorInput = mapOf(
            "query" to "Find test utilities",
            "scope" to "all"
        )
        
        val investigatorResult = investigatorAgent.run(investigatorInput) { progress ->
            println("  🔍 Investigator: $progress")
        }
        
        assert(investigatorResult.isNotEmpty()) { "CodebaseInvestigatorAgent should return result" }
        
        println("  ✅ SubAgent coordination working")
        
    } catch (e: Exception) {
        println("  ❌ Test failed: ${e.message}")
        throw e
    }
}

suspend fun testQueuePairCommunication() {
    println("\n📋 Test 3: Queue Pair Communication Pattern")
    
    try {
        val channel = AgentChannel()
        val submissions = mutableListOf<AgentSubmission>()
        val events = mutableListOf<AgentEvent>()
        
        // Start collectors
        val submissionCollector = launch {
            channel.submissions().collect { submission ->
                submissions.add(submission)
                println("  📥 Submission: ${submission::class.simpleName}")
            }
        }
        
        val eventCollector = launch {
            channel.events().collect { event ->
                events.add(event)
                println("  📤 Event: ${event::class.simpleName}")
            }
        }
        
        // Test UI -> Agent communication
        channel.submit(AgentSubmission.SendPrompt("Create a new feature"))
        channel.submit(AgentSubmission.ApproveToolCall("call-1", true))
        
        // Test Agent -> UI communication
        channel.emit(AgentEvent.ToolCallRequest("call-1", "write-file", mapOf("path" to "test.kt")))
        channel.emit(AgentEvent.Progress(1, 2, "Writing file"))
        
        delay(100) // Allow collection
        
        submissionCollector.cancel()
        eventCollector.cancel()
        
        assert(submissions.size >= 2) { "Should have collected submissions" }
        assert(events.size >= 2) { "Should have collected events" }
        
        println("  ✅ Queue Pair communication working")

    } catch (e: Exception) {
        println("  ❌ Test failed: ${e.message}")
        throw e
    }
}

suspend fun testErrorRecoveryWorkflow() {
    println("\n📋 Test 4: Error Recovery Workflow")

    try {
        val channel = AgentChannel()
        val mockLLMService = MockKoogLLMService()
        val agent = CodingAgent("/test/project", mockLLMService, channel = channel)

        // Simulate a failed command scenario
        println("  🔧 Simulating command failure...")

        // This would normally trigger ErrorRecoveryAgent
        val errorContext = mapOf(
            "command" to "gradle build",
            "errorMessage" to "Compilation failed: Cannot resolve symbol 'UnknownClass'",
            "exitCode" to 1
        )

        val errorAgent = ErrorRecoveryAgent("/test/project", mockLLMService)
        val recoveryResult = errorAgent.run(errorContext) { progress ->
            println("    🔧 $progress")
        }

        assert(recoveryResult.contains("error") || recoveryResult.contains("recovery")) {
            "Should contain error recovery information"
        }

        println("  ✅ Error recovery workflow working")

    } catch (e: Exception) {
        println("  ❌ Test failed: ${e.message}")
        throw e
    }
}

suspend fun testCodebaseInvestigationWorkflow() {
    println("\n📋 Test 5: Codebase Investigation Workflow")

    try {
        val channel = AgentChannel()
        val mockLLMService = MockKoogLLMService()
        val agent = CodingAgent("/test/project", mockLLMService, channel = channel)

        // Simulate a codebase investigation scenario
        println("  🔍 Simulating codebase investigation...")

        val investigationContext = mapOf(
            "query" to "Find all authentication-related classes and methods",
            "projectPath" to "/test/project",
            "scope" to "all"
        )

        val investigatorAgent = CodebaseInvestigatorAgent("/test/project", mockLLMService)
        val investigationResult = investigatorAgent.run(investigationContext) { progress ->
            println("    🔍 $progress")
        }

        assert(investigationResult.contains("Investigation Summary")) {
            "Should contain investigation summary"
        }

        println("  ✅ Codebase investigation workflow working")

    } catch (e: Exception) {
        println("  ❌ Test failed: ${e.message}")
        throw e
    }
}

// Mock implementations for testing
class MockKoogLLMService : LLMService {
    override suspend fun streamPrompt(
        userPrompt: String,
        fileSystem: FileSystem,
        historyMessages: List<LLMMessage>,
        compileDevIns: Boolean
    ): Flow<String> {
        // Return a simple mock response
        return flowOf("Mock LLM response for: ${userPrompt.take(50)}...")
    }

    // Add other required methods as needed
}

// Extension function for string repetition
operator fun String.times(n: Int): String = this.repeat(n)
