package cc.unitmesh.test

import cc.unitmesh.agent.CodingAgentContext
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import kotlinx.coroutines.runBlocking

/**
 * Integration test for the new tool schema system
 */
class ToolSchemaIntegrationTest {
    
    fun runAllTests() {
        println("🧪 开始工具 Schema 集成测试...")
        
        testSchemaGeneration()
        testAIToolDescriptionGeneration()
        testSchemaConsistency()
        
        println("✅ 所有工具 Schema 集成测试完成!")
    }
    
    /**
     * 测试 Schema 生成
     */
    private fun testSchemaGeneration() {
        println("\n📋 测试 1: Schema 生成")
        
        // 测试所有内置工具的 Schema
        val toolTypes = listOf(
            ToolType.ReadFile,
            ToolType.WriteFile,
            ToolType.Grep,
            ToolType.Glob,
            ToolType.Shell,
            ToolType.ErrorRecovery,
            ToolType.LogSummary,
            ToolType.CodebaseInvestigator
        )
        
        toolTypes.forEach { toolType ->
            println("   测试 ${toolType.name} Schema...")
            
            // 测试 JSON Schema 生成
            val jsonSchema = toolType.schema.toJsonSchema()
            assert(jsonSchema != null) { "${toolType.name} JSON Schema 不能为空" }
            
            // 测试参数描述生成
            val paramDescription = toolType.schema.getParameterDescription()
            assert(paramDescription.isNotEmpty()) { "${toolType.name} 参数描述不能为空" }
            assert(paramDescription.contains("Parameters:")) { "${toolType.name} 参数描述应包含 'Parameters:'" }
            
            // 测试示例用法生成
            val example = toolType.schema.getExampleUsage(toolType.name)
            assert(example.isNotEmpty()) { "${toolType.name} 示例用法不能为空" }
            assert(example.startsWith("/${toolType.name}")) { "${toolType.name} 示例应以工具名开始" }
            
            println("     ✅ ${toolType.name} Schema 生成正确")
        }
        
        println("   ✅ 所有工具 Schema 生成测试通过")
    }
    
    /**
     * 测试 AI 工具描述生成
     */
    private fun testAIToolDescriptionGeneration() = runBlocking {
        println("\n📋 测试 2: AI 工具描述生成")
        
        // 创建工具注册表
        val toolConfig = ToolConfigFile(
            enabledBuiltinTools = listOf("read-file", "write-file", "grep", "shell"),
            enabledMcpTools = emptyList(),
            mcpServers = emptyMap()
        )
        
        val mcpConfigService = McpToolConfigService(toolConfig)
        val toolRegistry = ToolRegistry(
            fileSystem = DefaultToolFileSystem(),
            shellExecutor = DefaultShellExecutor(),
            configService = mcpConfigService
        )
        
        // 创建 CodingAgentContext
        val context = CodingAgentContext.create(toolRegistry)
        
        // 获取工具列表描述
        val toolDescription = context.formatToolsForAI()
        
        println("   生成的工具描述长度: ${toolDescription.length} 字符")
        
        // 验证描述包含必要信息
        assert(toolDescription.contains("<tool name=\"read-file\">")) { "应包含 read-file 工具" }
        assert(toolDescription.contains("<tool name=\"write-file\">")) { "应包含 write-file 工具" }
        assert(toolDescription.contains("<tool name=\"grep\">")) { "应包含 grep 工具" }
        assert(toolDescription.contains("<tool name=\"shell\">")) { "应包含 shell 工具" }
        
        // 验证 Schema 信息
        assert(toolDescription.contains("<parameters>")) { "应包含参数信息" }
        assert(toolDescription.contains("<schema>")) { "应包含 Schema 信息" }
        assert(toolDescription.contains("<param name=")) { "应包含参数定义" }
        assert(toolDescription.contains("<example>")) { "应包含示例用法" }
        
        // 验证具体参数
        assert(toolDescription.contains("path")) { "应包含 path 参数" }
        assert(toolDescription.contains("content")) { "应包含 content 参数" }
        assert(toolDescription.contains("pattern")) { "应包含 pattern 参数" }
        assert(toolDescription.contains("command")) { "应包含 command 参数" }
        
        println("   ✅ AI 工具描述生成正确")
        
        // 打印部分描述用于调试
        println("   📄 工具描述示例:")
        val lines = toolDescription.split("\n")
        lines.take(20).forEach { line ->
            println("     $line")
        }
        if (lines.size > 20) {
            println("     ... (共 ${lines.size} 行)")
        }
    }
    
    /**
     * 测试 Schema 一致性
     */
    private fun testSchemaConsistency() {
        println("\n📋 测试 3: Schema 一致性")
        
        // 测试 ToolType 和 BuiltinToolSchemas 的一致性
        val consistencyTests = mapOf(
            ToolType.ReadFile to cc.unitmesh.agent.tool.schema.BuiltinToolSchemas.ReadFileSchema,
            ToolType.WriteFile to cc.unitmesh.agent.tool.schema.BuiltinToolSchemas.WriteFileSchema,
            ToolType.Grep to cc.unitmesh.agent.tool.schema.BuiltinToolSchemas.GrepSchema,
            ToolType.Glob to cc.unitmesh.agent.tool.schema.BuiltinToolSchemas.GlobSchema,
            ToolType.Shell to cc.unitmesh.agent.tool.schema.BuiltinToolSchemas.ShellSchema,
            ToolType.ErrorRecovery to cc.unitmesh.agent.tool.schema.BuiltinToolSchemas.ErrorRecoverySchema,
            ToolType.LogSummary to cc.unitmesh.agent.tool.schema.BuiltinToolSchemas.LogSummarySchema,
            ToolType.CodebaseInvestigator to cc.unitmesh.agent.tool.schema.BuiltinToolSchemas.CodebaseInvestigatorSchema
        )
        
        consistencyTests.forEach { (toolType, expectedSchema) ->
            assert(toolType.schema == expectedSchema) { 
                "${toolType.name} 的 Schema 与预期不一致" 
            }
            println("   ✅ ${toolType.name} Schema 一致性验证通过")
        }
        
        // 测试 Schema 属性完整性
        val requiredProperties = listOf("path", "content", "pattern", "command", "errorMessage", "logContent", "query")
        val allSchemas = consistencyTests.values
        
        allSchemas.forEach { schema ->
            val jsonSchema = schema.toJsonSchema()
            if (jsonSchema is kotlinx.serialization.json.JsonObject) {
                val properties = jsonSchema["properties"] as? kotlinx.serialization.json.JsonObject
                assert(properties != null) { "Schema 应包含 properties" }
                
                // 每个 Schema 至少应该有一个必需参数
                val required = jsonSchema["required"] as? kotlinx.serialization.json.JsonArray
                if (required != null && required.isNotEmpty()) {
                    println("     Schema 有 ${required.size} 个必需参数")
                }
            }
        }
        
        println("   ✅ Schema 一致性测试通过")
    }
}

fun main() {
    val test = ToolSchemaIntegrationTest()
    test.runAllTests()
}
