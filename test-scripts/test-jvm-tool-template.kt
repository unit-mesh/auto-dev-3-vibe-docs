#!/usr/bin/env kotlin

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

import cc.unitmesh.agent.CodingAgentContext
import cc.unitmesh.agent.CodingAgentTemplate
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.devins.compiler.template.TemplateCompiler
import kotlinx.serialization.json.*

/**
 * Test JVM version of tool template generation with JSON Schema format
 */
fun main() {
    println("🔧 Testing JVM Tool Template Generation with JSON Schema")
    println("=".repeat(60))
    
    try {
        // Step 1: Create tool registry
        println("\n1. Creating Tool Registry...")
        val projectPath = "/test/kotlin-project"
        val fileSystem = DefaultToolFileSystem(projectPath)
        val toolRegistry = ToolRegistry(fileSystem)
        
        val availableTools = toolRegistry.getAllTools().values.toList()
        println("✅ Tool registry created with ${availableTools.size} tools:")
        availableTools.forEach { tool ->
            println("   - ${tool.name}: ${tool.description.take(50)}...")
        }
        
        // Step 2: Generate tool list with new JSON Schema format
        println("\n2. Generating Tool List with JSON Schema Format...")
        val toolList = CodingAgentContext.formatToolListForAI(availableTools)
        
        println("✅ Tool list generated successfully!")
        println("Tool list length: ${toolList.length} characters")
        
        // Step 3: Analyze the format
        println("\n3. Analyzing JSON Schema Format...")
        val formatChecks = mapOf(
            "Uses Markdown headers (##)" to toolList.contains("## "),
            "Has JSON Schema blocks" to toolList.contains("```json"),
            "Contains \$schema field" to toolList.contains("\"${'$'}schema\""),
            "Has draft-07 schema" to toolList.contains("draft-07/schema#"),
            "Contains type object" to toolList.contains("\"type\": \"object\""),
            "Has properties field" to toolList.contains("\"properties\""),
            "Has required field" to toolList.contains("\"required\""),
            "Has additionalProperties" to toolList.contains("\"additionalProperties\""),
            "No XML tags" to !toolList.contains("<tool name="),
            "No XML parameters" to !toolList.contains("<parameters>"),
            "Has example blocks" to toolList.contains("**Example:**")
        )
        
        formatChecks.forEach { (check, passed) ->
            println("  ${if (passed) "✅" else "❌"} $check")
        }
        
        // Step 4: Show sample of new format
        println("\n4. Sample of New Format:")
        println("-".repeat(50))
        println(toolList.substring(0, minOf(1200, toolList.length)))
        println("-".repeat(50))
        
        // Step 5: Create complete context and template
        println("\n5. Creating Complete Context and Template...")
        val context = CodingAgentContext(
            projectPath = projectPath,
            osInfo = "Linux Ubuntu 22.04",
            timestamp = "2024-01-01T00:00:00Z",
            toolList = toolList,
            buildTool = "gradle",
            shell = "/bin/bash"
        )
        
        val variableTable = context.toVariableTable()
        val compiler = TemplateCompiler(variableTable)
        val template = compiler.compile(CodingAgentTemplate.EN)
        
        println("✅ Template compiled successfully!")
        println("Template length: ${template.length} characters")
        
        // Step 6: Analyze template quality
        println("\n6. Template Quality Analysis...")
        val templateChecks = mapOf(
            "Available Tools section" to template.contains("Available Tools"),
            "JSON Schema format" to template.contains("```json"),
            "Standard schema format" to template.contains("draft-07/schema#"),
            "Proper tool structure" to template.contains("## read-file"),
            "Parameter descriptions" to template.contains("\"description\""),
            "Type information" to template.contains("\"type\""),
            "Required fields" to template.contains("\"required\""),
            "Example usage" to template.contains("**Example:**")
        )
        
        templateChecks.forEach { (check, passed) ->
            println("  ${if (passed) "✅" else "❌"} $check")
        }
        
        // Step 7: Export results for comparison
        println("\n7. Exporting Results...")
        val results = buildString {
            appendLine("# JVM Tool Template Test Results")
            appendLine("Generated at: ${java.time.LocalDateTime.now()}")
            appendLine()
            appendLine("## Format Checks")
            formatChecks.forEach { (check, passed) ->
                appendLine("- ${if (passed) "✅" else "❌"} $check")
            }
            appendLine()
            appendLine("## Template Checks")
            templateChecks.forEach { (check, passed) ->
                appendLine("- ${if (passed) "✅" else "❌"} $check")
            }
            appendLine()
            appendLine("## Metrics")
            appendLine("- Tool count: ${availableTools.size}")
            appendLine("- Tool list length: ${toolList.length} characters")
            appendLine("- Template length: ${template.length} characters")
            appendLine()
            appendLine("## Tool List Sample")
            appendLine("```")
            appendLine(toolList.substring(0, minOf(1000, toolList.length)))
            appendLine("```")
        }
        
        java.io.File("/tmp/jvm-tool-template-test.md").writeText(results)
        println("✅ Results exported to /tmp/jvm-tool-template-test.md")
        
        // Step 8: Summary
        println("\n8. Summary:")
        val allFormatChecksPass = formatChecks.values.all { it }
        val allTemplateChecksPass = templateChecks.values.all { it }
        
        if (allFormatChecksPass && allTemplateChecksPass) {
            println("🎉 SUCCESS: JVM version works perfectly with JSON Schema format!")
            println("   - All format checks passed")
            println("   - All template checks passed")
            println("   - Tool list length: ${toolList.length} characters")
            println("   - Template length: ${template.length} characters")
        } else {
            println("⚠️  PARTIAL: Some checks failed.")
            println("   Format checks: ${formatChecks.values.count { it }}/${formatChecks.size}")
            println("   Template checks: ${templateChecks.values.count { it }}/${templateChecks.size}")
        }
        
    } catch (e: Exception) {
        println("❌ Error during JVM testing: ${e.message}")
        e.printStackTrace()
    }
}
