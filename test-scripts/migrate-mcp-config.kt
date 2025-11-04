package cc.unitmesh.test

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * 迁移 MCP 配置文件，移除工具名称中的服务器前缀
 */
class McpConfigMigration {
    
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    fun migrateConfig(configPath: String = "${System.getProperty("user.home")}/.autodev/mcp.json") {
        println("🔧 开始迁移 MCP 配置文件: $configPath")
        
        val configFile = File(configPath)
        if (!configFile.exists()) {
            println("❌ 配置文件不存在: $configPath")
            return
        }
        
        try {
            // 备份原文件
            val backupFile = File("$configPath.backup.${System.currentTimeMillis()}")
            configFile.copyTo(backupFile)
            println("📋 已备份原配置文件到: ${backupFile.absolutePath}")
            
            // 读取配置
            val configContent = configFile.readText()
            val configJson = json.parseToJsonElement(configContent).jsonObject
            
            // 迁移 enabledMcpTools
            val enabledMcpTools = configJson["enabledMcpTools"]?.jsonArray
            if (enabledMcpTools != null) {
                val migratedTools = migrateToolNames(enabledMcpTools)
                
                println("🔄 迁移工具名称:")
                enabledMcpTools.forEachIndexed { index, oldTool ->
                    val oldName = oldTool.jsonPrimitive.content
                    val newName = migratedTools[index].jsonPrimitive.content
                    if (oldName != newName) {
                        println("   $oldName -> $newName")
                    }
                }
                
                // 构建新的配置
                val newConfig = JsonObject(
                    configJson.toMutableMap().apply {
                        put("enabledMcpTools", JsonArray(migratedTools))
                    }
                )
                
                // 写入新配置
                val newConfigContent = json.encodeToString(JsonObject.serializer(), newConfig)
                configFile.writeText(newConfigContent)
                
                println("✅ 配置文件迁移完成")
            } else {
                println("ℹ️ 没有找到 enabledMcpTools，无需迁移")
            }
            
        } catch (e: Exception) {
            println("❌ 迁移失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 迁移工具名称，移除服务器前缀
     */
    private fun migrateToolNames(tools: JsonArray): List<JsonPrimitive> {
        return tools.map { toolElement ->
            val toolName = toolElement.jsonPrimitive.content
            val migratedName = removeServerPrefix(toolName)
            JsonPrimitive(migratedName)
        }
    }
    
    /**
     * 移除服务器前缀
     */
    private fun removeServerPrefix(toolName: String): String {
        // 已知的服务器前缀
        val serverPrefixes = listOf("filesystem_", "context7_")
        
        for (prefix in serverPrefixes) {
            if (toolName.startsWith(prefix)) {
                return toolName.removePrefix(prefix)
            }
        }
        
        return toolName
    }
    
    /**
     * 验证迁移结果
     */
    fun validateMigration(configPath: String = "${System.getProperty("user.home")}/.autodev/mcp.json") {
        println("\n🔍 验证迁移结果...")
        
        val configFile = File(configPath)
        if (!configFile.exists()) {
            println("❌ 配置文件不存在")
            return
        }
        
        try {
            val configContent = configFile.readText()
            val configJson = json.parseToJsonElement(configContent).jsonObject
            
            val enabledMcpTools = configJson["enabledMcpTools"]?.jsonArray
            if (enabledMcpTools != null) {
                println("📋 当前启用的 MCP 工具:")
                enabledMcpTools.forEach { tool ->
                    val toolName = tool.jsonPrimitive.content
                    val hasPrefix = toolName.contains("_") && 
                        (toolName.startsWith("filesystem_") || toolName.startsWith("context7_"))
                    
                    val status = if (hasPrefix) "❌ 仍有前缀" else "✅ 已移除前缀"
                    println("   $toolName - $status")
                }
                
                val toolsWithPrefix = enabledMcpTools.count { tool ->
                    val toolName = tool.jsonPrimitive.content
                    toolName.startsWith("filesystem_") || toolName.startsWith("context7_")
                }
                
                if (toolsWithPrefix == 0) {
                    println("✅ 所有工具名称都已正确迁移")
                } else {
                    println("⚠️ 还有 $toolsWithPrefix 个工具名称包含服务器前缀")
                }
            }
            
        } catch (e: Exception) {
            println("❌ 验证失败: ${e.message}")
        }
    }
}

fun main() {
    val migration = McpConfigMigration()
    
    // 执行迁移
    migration.migrateConfig()
    
    // 验证结果
    migration.validateMigration()
    
    println("\n🎯 迁移完成！现在 MCP 工具名称使用实际的工具名称，不再包含服务器前缀。")
    println("   例如: filesystem_list_directory -> list_directory")
    println("   这样可以避免 'Tool not found' 错误。")
}
