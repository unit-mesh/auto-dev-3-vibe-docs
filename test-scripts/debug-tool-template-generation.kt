#!/usr/bin/env kotlin

/**
 * Debug script to test tool template generation and identify issues
 * with DeclarativeToolSchema and CodingAgentTemplate
 */

import cc.unitmesh.agent.CodingAgentContext
import cc.unitmesh.agent.CodingAgentTemplate
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.impl.*
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.devins.compiler.template.TemplateCompiler

// Mock implementations for testing
class MockExecutableTool(
    override val name: String,
    override val description: String,
    private val parameterClass: String = "Unit"
) : ExecutableTool<Unit, Any> {
    override fun getParameterClass(): String = parameterClass
    override suspend fun execute(params: Unit, context: Any): Any = "Mock result"
}

fun main() {
    println("🔍 Debug: Tool Template Generation")
    println("=" * 50)
    
    // Test 1: Check individual schema generation
    println("\n1. Testing Individual Schema Generation:")
    testIndividualSchemas()
    
    // Test 2: Check tool list formatting
    println("\n2. Testing Tool List Formatting:")
    testToolListFormatting()
    
    // Test 3: Check complete template generation
    println("\n3. Testing Complete Template Generation:")
    testCompleteTemplate()
    
    // Test 4: Check for potential issues
    println("\n4. Checking for Potential Issues:")
    checkPotentialIssues()
}

fun testIndividualSchemas() {
    val schemas = listOf(
        "ReadFile" to ReadFileSchema,
        "WriteFile" to WriteFileSchema,
        "Shell" to ShellSchema,
        "Grep" to GrepSchema,
        "Glob" to GlobSchema
    )
    
    schemas.forEach { (name, schema) ->
        println("\n--- $name Schema ---")
        try {
            val paramDesc = schema.getParameterDescription()
            val example = schema.getExampleUsage(name.lowercase())
            
            println("Parameter Description:")
            println(paramDesc)
            println("\nExample Usage:")
            println(example)
            
            // Check JSON schema
            val jsonSchema = schema.toJsonSchema()
            println("\nJSON Schema generated: ${jsonSchema.toString().length} chars")
            
        } catch (e: Exception) {
            println("❌ Error with $name schema: ${e.message}")
            e.printStackTrace()
        }
    }
}

fun testToolListFormatting() {
    val mockTools = listOf(
        MockExecutableTool("read-file", "Read file content", "ReadFileParams"),
        MockExecutableTool("write-file", "Write file content", "WriteFileParams"),
        MockExecutableTool("shell", "Execute shell commands", "ShellParams"),
        MockExecutableTool("grep", "Search in files", "GrepParams"),
        MockExecutableTool("glob", "Find files by pattern", "GlobParams")
    )
    
    println("Formatting ${mockTools.size} tools...")
    
    try {
        val context = CodingAgentContext(
            projectPath = "/test/project",
            osInfo = "Test OS",
            timestamp = "2024-01-01T00:00:00Z",
            toolList = CodingAgentContext.formatToolListForAI(mockTools)
        )
        
        println("Tool list generated successfully!")
        println("Length: ${context.toolList.length} characters")
        
        // Show first few tools
        val lines = context.toolList.split("\n")
        println("\nFirst 20 lines of tool list:")
        lines.take(20).forEachIndexed { index, line ->
            println("${index + 1:2}: $line")
        }
        
    } catch (e: Exception) {
        println("❌ Error formatting tool list: ${e.message}")
        e.printStackTrace()
    }
}

fun testCompleteTemplate() {
    val mockTools = listOf(
        MockExecutableTool("read-file", "Read file content", "ReadFileParams"),
        MockExecutableTool("shell", "Execute shell commands", "ShellParams")
    )
    
    try {
        val context = CodingAgentContext(
            projectPath = "/test/project",
            osInfo = "Test OS",
            timestamp = "2024-01-01T00:00:00Z",
            toolList = CodingAgentContext.formatToolListForAI(mockTools)
        )
        
        val variableTable = context.toVariableTable()
        val compiler = TemplateCompiler(variableTable)
        val compiledTemplate = compiler.compile(CodingAgentTemplate.EN)
        
        println("Template compiled successfully!")
        println("Length: ${compiledTemplate.length} characters")
        
        // Check if tools section is properly populated
        val toolsSection = extractToolsSection(compiledTemplate)
        if (toolsSection.isNotEmpty()) {
            println("\n✅ Tools section found in template")
            println("Tools section length: ${toolsSection.length} characters")
        } else {
            println("\n❌ Tools section not found or empty in template")
        }
        
    } catch (e: Exception) {
        println("❌ Error compiling template: ${e.message}")
        e.printStackTrace()
    }
}

fun checkPotentialIssues() {
    println("Checking for common issues...")
    
    // Issue 1: Check if ToolType.toToolType() works
    val toolNames = listOf("read-file", "write-file", "shell", "grep", "glob")
    toolNames.forEach { name ->
        val toolType = name.toToolType()
        if (toolType != null) {
            println("✅ $name -> ${toolType::class.simpleName}")
        } else {
            println("❌ $name -> null (not found)")
        }
    }
    
    // Issue 2: Check if schemas have proper getExampleUsage implementation
    println("\nChecking schema example usage:")
    ToolType.ALL_TOOLS.forEach { toolType ->
        try {
            val example = toolType.schema.getExampleUsage(toolType.name)
            if (example.isNotEmpty()) {
                println("✅ ${toolType.name}: $example")
            } else {
                println("❌ ${toolType.name}: empty example")
            }
        } catch (e: Exception) {
            println("❌ ${toolType.name}: error - ${e.message}")
        }
    }
}

fun extractToolsSection(template: String): String {
    val startMarker = "## Available Tools"
    val endMarker = "## Task Execution Guidelines"
    
    val startIndex = template.indexOf(startMarker)
    val endIndex = template.indexOf(endMarker)
    
    return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
        template.substring(startIndex, endIndex).trim()
    } else {
        ""
    }
}

// Helper extension
operator fun String.times(n: Int): String = this.repeat(n)
