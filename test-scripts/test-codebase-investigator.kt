#!/usr/bin/env kotlin

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import kotlinx.coroutines.runBlocking

/**
 * Test script for CodebaseInvestigatorAgent
 * 
 * This script tests the new CodebaseInvestigatorAgent functionality
 * to ensure it properly analyzes codebase structure and provides insights.
 */

fun main() = runBlocking {
    println("🧪 Testing CodebaseInvestigatorAgent")
    println("=" * 50)
    
    // Test 1: Basic investigation query
    testBasicInvestigation()
    
    // Test 2: Class-specific investigation
    testClassInvestigation()
    
    // Test 3: Method-specific investigation
    testMethodInvestigation()
    
    // Test 4: Dependency analysis
    testDependencyAnalysis()
    
    // Test 5: Error handling
    testErrorHandling()
    
    println("\n✅ All CodebaseInvestigatorAgent tests completed!")
}

suspend fun testBasicInvestigation() {
    println("\n📋 Test 1: Basic Investigation Query")
    
    try {
        // Mock LLM service for testing
        val mockLLMService = MockKoogLLMService()
        val agent = CodebaseInvestigatorAgent("/test/project", mockLLMService)
        
        val input = mapOf(
            "query" to "Find authentication related code",
            "scope" to "all"
        )
        
        val result = agent.run(input) { progress ->
            println("  📊 $progress")
        }
        
        println("  ✅ Result: ${result.take(100)}...")
        assert(result.contains("Investigation Summary")) { "Should contain investigation summary" }
        
    } catch (e: Exception) {
        println("  ❌ Test failed: ${e.message}")
        throw e
    }
}

suspend fun testClassInvestigation() {
    println("\n📋 Test 2: Class-Specific Investigation")
    
    try {
        val mockLLMService = MockKoogLLMService()
        val agent = CodebaseInvestigatorAgent("/test/project", mockLLMService)
        
        val input = mapOf(
            "query" to "Find user management classes",
            "scope" to "classes"
        )
        
        val result = agent.run(input) { progress ->
            println("  📊 $progress")
        }
        
        println("  ✅ Result: ${result.take(100)}...")
        assert(result.contains("class")) { "Should contain class-related findings" }
        
    } catch (e: Exception) {
        println("  ❌ Test failed: ${e.message}")
        throw e
    }
}

suspend fun testMethodInvestigation() {
    println("\n📋 Test 3: Method-Specific Investigation")
    
    try {
        val mockLLMService = MockKoogLLMService()
        val agent = CodebaseInvestigatorAgent("/test/project", mockLLMService)
        
        val input = mapOf(
            "query" to "Find validation methods",
            "scope" to "methods"
        )
        
        val result = agent.run(input) { progress ->
            println("  📊 $progress")
        }
        
        println("  ✅ Result: ${result.take(100)}...")
        assert(result.contains("method")) { "Should contain method-related findings" }
        
    } catch (e: Exception) {
        println("  ❌ Test failed: ${e.message}")
        throw e
    }
}

suspend fun testDependencyAnalysis() {
    println("\n📋 Test 4: Dependency Analysis")
    
    try {
        val mockLLMService = MockKoogLLMService()
        val agent = CodebaseInvestigatorAgent("/test/project", mockLLMService)
        
        val input = mapOf(
            "query" to "Analyze database dependencies",
            "scope" to "dependencies"
        )
        
        val result = agent.run(input) { progress ->
            println("  📊 $progress")
        }
        
        println("  ✅ Result: ${result.take(100)}...")
        assert(result.contains("dependencies")) { "Should contain dependency analysis" }
        
    } catch (e: Exception) {
        println("  ❌ Test failed: ${e.message}")
        throw e
    }
}

suspend fun testErrorHandling() {
    println("\n📋 Test 5: Error Handling")
    
    try {
        val mockLLMService = MockKoogLLMService()
        val agent = CodebaseInvestigatorAgent("/test/project", mockLLMService)
        
        // Test with missing query parameter
        val input = mapOf<String, Any>()
        
        try {
            agent.run(input) { progress ->
                println("  📊 $progress")
            }
            throw AssertionError("Should have thrown an exception for missing query")
        } catch (e: IllegalArgumentException) {
            println("  ✅ Correctly handled missing query parameter")
        }
        
    } catch (e: Exception) {
        println("  ❌ Test failed: ${e.message}")
        throw e
    }
}

// Mock implementations for testing
class MockKoogLLMService : LLMService {
    // Simplified mock implementation
    // In real tests, you would use a proper mocking framework
}
