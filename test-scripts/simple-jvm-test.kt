#!/usr/bin/env kotlin

import cc.unitmesh.agent.CodingAgentContext
import cc.unitmesh.agent.tool.impl.*

/**
 * Simple JVM test to verify tool template generation with JSON Schema format
 */
fun main() {
    println("🔧 Testing JVM Tool Template Generation with JSON Schema")
    println("=".repeat(60))
    
    try {
        // Step 1: Create mock tools (similar to what ToolRegistry would provide)
        val mockTools = listOf(
            ReadFileTool(),
            WriteFileTool(),
            ShellTool(),
            GrepTool(),
            GlobTool()
        )
        
        println("✅ Created ${mockTools.size} mock tools:")
        mockTools.forEach { tool ->
            println("   - ${tool.name}: ${tool.description.take(50)}...")
        }
        
        // Step 2: Generate tool list with new JSON Schema format
        println("\n2. Generating Tool List with JSON Schema Format...")
        val toolList = CodingAgentContext.formatToolListForAI(mockTools)
        
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
        
        // Step 5: Verify specific tools are present
        println("\n5. Verifying Tool Presence...")
        val toolChecks = mapOf(
            "read-file tool" to toolList.contains("## read-file"),
            "write-file tool" to toolList.contains("## write-file"),
            "shell tool" to toolList.contains("## shell"),
            "grep tool" to toolList.contains("## grep"),
            "glob tool" to toolList.contains("## glob")
        )
        
        toolChecks.forEach { (check, passed) ->
            println("  ${if (passed) "✅" else "❌"} $check")
        }
        
        // Step 6: Verify JSON Schema structure for read-file
        println("\n6. Verifying JSON Schema Structure...")
        val readFileStart = toolList.indexOf("## read-file")
        val readFileEnd = toolList.indexOf("## ", readFileStart + 1)
        val readFileSection = if (readFileEnd > 0) {
            toolList.substring(readFileStart, readFileEnd)
        } else {
            toolList.substring(readFileStart)
        }
        
        val schemaChecks = mapOf(
            "path parameter" to readFileSection.contains("\"path\""),
            "string type" to readFileSection.contains("\"type\": \"string\""),
            "startLine parameter" to readFileSection.contains("\"startLine\""),
            "integer type" to readFileSection.contains("\"type\": \"integer\""),
            "minimum constraints" to readFileSection.contains("\"minimum\""),
            "default values" to readFileSection.contains("\"default\"")
        )
        
        schemaChecks.forEach { (check, passed) ->
            println("  ${if (passed) "✅" else "❌"} $check")
        }
        
        // Step 7: Export results
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
            appendLine("## Tool Checks")
            toolChecks.forEach { (check, passed) ->
                appendLine("- ${if (passed) "✅" else "❌"} $check")
            }
            appendLine()
            appendLine("## Schema Checks")
            schemaChecks.forEach { (check, passed) ->
                appendLine("- ${if (passed) "✅" else "❌"} $check")
            }
            appendLine()
            appendLine("## Metrics")
            appendLine("- Tool count: ${mockTools.size}")
            appendLine("- Tool list length: ${toolList.length} characters")
            appendLine()
            appendLine("## Tool List Sample")
            appendLine("```")
            appendLine(toolList.substring(0, minOf(1000, toolList.length)))
            appendLine("```")
        }
        
        java.io.File("/tmp/simple-jvm-test-results.md").writeText(results)
        println("✅ Results exported to /tmp/simple-jvm-test-results.md")
        
        // Step 8: Summary
        println("\n8. Summary:")
        val allFormatChecksPass = formatChecks.values.all { it }
        val allToolChecksPass = toolChecks.values.all { it }
        val allSchemaChecksPass = schemaChecks.values.all { it }
        
        if (allFormatChecksPass && allToolChecksPass && allSchemaChecksPass) {
            println("🎉 SUCCESS: JVM version works perfectly with JSON Schema format!")
            println("   - All format checks passed")
            println("   - All tool checks passed")
            println("   - All schema checks passed")
            println("   - Tool list length: ${toolList.length} characters")
        } else {
            println("⚠️  PARTIAL: Some checks failed.")
            println("   Format checks: ${formatChecks.values.count { it }}/${formatChecks.size}")
            println("   Tool checks: ${toolChecks.values.count { it }}/${toolChecks.size}")
            println("   Schema checks: ${schemaChecks.values.count { it }}/${schemaChecks.size}")
        }
        
    } catch (e: Exception) {
        println("❌ Error during JVM testing: ${e.message}")
        e.printStackTrace()
    }
}
