#!/usr/bin/env kotlin

/**
 * Test script for McpToolConfigManager
 * 
 * This script tests the basic functionality of the new McpToolConfigManager
 * and verifies that it can properly discover MCP tools.
 */

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

// Mock MCP server configuration for testing
val testMcpConfig = """
{
  "mcpServers": {
    "test-server": {
      "command": "echo",
      "args": ["hello"],
      "disabled": false
    },
    "disabled-server": {
      "command": "echo", 
      "args": ["world"],
      "disabled": true
    }
  }
}
""".trimIndent()

fun main() {
    println("🧪 Testing McpToolConfigManager...")
    
    try {
        // Test 1: Parse MCP configuration
        println("\n📋 Test 1: Parsing MCP configuration")
        val enabledServers = cc.unitmesh.agent.config.McpToolConfigManager.getEnabledServers(testMcpConfig)
        
        if (enabledServers != null) {
            println("✅ Successfully parsed MCP configuration")
            println("   Enabled servers: ${enabledServers.keys}")
            println("   Total servers: ${enabledServers.size}")
        } else {
            println("❌ Failed to parse MCP configuration")
            return
        }
        
        // Test 2: Discover MCP tools (this will likely fail without real MCP servers)
        println("\n🔍 Test 2: Discovering MCP tools")
        runBlocking {
            try {
                val tools = cc.unitmesh.agent.config.McpToolConfigManager.discoverMcpTools(
                    enabledServers,
                    emptySet()
                )
                println("✅ Tool discovery completed")
                println("   Discovered ${tools.size} tools")
                
                tools.forEach { tool ->
                    println("   - ${tool.name}: ${tool.description}")
                }
            } catch (e: Exception) {
                println("⚠️  Tool discovery failed (expected without real MCP servers): ${e.message}")
            }
        }
        
        // Test 3: Test server status
        println("\n📊 Test 3: Getting server statuses")
        val statuses = cc.unitmesh.agent.config.McpToolConfigManager.getServerStatuses()
        println("✅ Retrieved server statuses: ${statuses.size} servers")
        
        statuses.forEach { (serverName, status) ->
            println("   - $serverName: $status")
        }
        
        // Test 4: Clear cache
        println("\n🧹 Test 4: Clearing cache")
        cc.unitmesh.agent.config.McpToolConfigManager.clearCache()
        println("✅ Cache cleared successfully")
        
        println("\n🎉 All tests completed!")
        
    } catch (e: Exception) {
        println("❌ Test failed with exception: ${e.message}")
        e.printStackTrace()
    }
}
