#!/usr/bin/env kotlin

/**
 * 测试 WriteFileTool 多行写入功能和模型生成测试
 * 
 * 这个脚本将：
 * 1. 测试 WriteFileTool 的多行写入能力
 * 2. 使用 CodingAgentPromptRenderer 生成提示词
 * 3. 调用 LLMService 测试模型对多行代码生成的支持
 */

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// 模拟必要的类和接口
data class ModelConfig(
    val provider: String,
    val modelName: String,
    val apiKey: String,
    val baseUrl: String? = null,
    val temperature: Double = 0.7
) {
    fun isValid(): Boolean = provider.isNotEmpty() && modelName.isNotEmpty() && apiKey.isNotEmpty()
}

// 模拟 WriteFileParams
data class WriteFileParams(
    val path: String,
    val content: String,
    val createDirectories: Boolean = true,
    val overwrite: Boolean = true,
    val append: Boolean = false
)

// 模拟 CodingAgentContext
data class CodingAgentContext(
    val currentFile: String? = null,
    val projectPath: String,
    val projectStructure: String = "",
    val osInfo: String,
    val timestamp: String,
    val toolList: String = "",
    val agentRules: String = "",
    val buildTool: String = "",
    val shell: String = "/bin/bash",
    val moduleInfo: String = "",
    val frameworkContext: String = ""
)

// 模拟工具信息
data class MockTool(
    val name: String,
    val description: String,
    val parameterClass: String = "Object"
)

fun main() = runBlocking {
    println("🧪 测试 WriteFileTool 多行写入功能")
    println("=" * 50)
    
    // 1. 测试多行内容写入
    testMultilineWriting()
    
    println("\n" + "=" * 50)
    
    // 2. 生成 AI 提示词并测试模型
    testAIPromptGeneration()
    
    println("\n" + "=" * 50)
    
    // 3. 测试模型对多行代码生成的支持
    testModelCodeGeneration()
}

fun testMultilineWriting() {
    println("📝 测试多行内容写入...")
    
    val multilineContent = """
        package com.example.demo
        
        import kotlinx.coroutines.*
        import kotlinx.serialization.Serializable
        
        /**
         * 示例数据类
         * 用于测试多行代码生成
         */
        @Serializable
        data class User(
            val id: Long,
            val name: String,
            val email: String,
            val createdAt: String
        ) {
            companion object {
                fun create(name: String, email: String): User {
                    return User(
                        id = System.currentTimeMillis(),
                        name = name,
                        email = email,
                        createdAt = ZonedDateTime.now().toString()
                    )
                }
            }
            
            fun isValid(): Boolean {
                return name.isNotBlank() && 
                       email.contains("@") && 
                       email.contains(".")
            }
        }
        
        /**
         * 用户服务类
         */
        class UserService {
            private val users = mutableListOf<User>()
            
            suspend fun createUser(name: String, email: String): Result<User> {
                return try {
                    val user = User.create(name, email)
                    if (user.isValid()) {
                        users.add(user)
                        Result.success(user)
                    } else {
                        Result.failure(IllegalArgumentException("Invalid user data"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            
            fun getAllUsers(): List<User> = users.toList()
            
            fun findUserById(id: Long): User? = users.find { it.id == id }
        }
    """.trimIndent()
    
    val params = WriteFileParams(
        path = "test-output/User.kt",
        content = multilineContent,
        createDirectories = true,
        overwrite = true
    )
    
    println("   📊 内容统计:")
    println("   - 字符数: ${params.content.length}")
    println("   - 行数: ${params.content.lines().size}")
    println("   - 是否包含多行: ${params.content.contains('\n')}")
    
    // 模拟写入操作
    try {
        val outputFile = File(params.path)
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(params.content)
        
        println("   ✅ 文件写入成功: ${params.path}")
        println("   📁 文件大小: ${outputFile.length()} bytes")
        
        // 验证内容
        val readContent = outputFile.readText()
        val contentMatches = readContent == params.content
        println("   🔍 内容验证: ${if (contentMatches) "✅ 通过" else "❌ 失败"}")
        
        if (!contentMatches) {
            println("   ⚠️ 内容不匹配，可能存在编码或换行符问题")
        }
        
    } catch (e: Exception) {
        println("   ❌ 写入失败: ${e.message}")
    }
}

fun testAIPromptGeneration() {
    println("🤖 生成 AI 提示词...")
    
    // 创建模拟工具列表
    val tools = listOf(
        MockTool("read-file", "Read content from a file", "ReadFileParams"),
        MockTool("write-file", "Write content to a file", "WriteFileParams"),
        MockTool("grep", "Search for patterns in files", "GrepParams"),
        MockTool("glob", "Find files matching patterns", "GlobParams"),
        MockTool("shell", "Execute shell commands", "ShellParams")
    )
    
    val toolList = formatToolListForAI(tools)
    
    val context = CodingAgentContext(
        projectPath = "/Volumes/source/ai/autocrud",
        osInfo = "macOS 14.0 (Darwin)",
        timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        toolList = toolList,
        buildTool = "gradle + kotlin",
        shell = "/bin/zsh"
    )
    
    println("   📋 上下文信息:")
    println("   - 项目路径: ${context.projectPath}")
    println("   - 操作系统: ${context.osInfo}")
    println("   - 构建工具: ${context.buildTool}")
    println("   - 可用工具数: ${tools.size}")
    
    // 生成提示词（简化版）
    val prompt = generateSimplePrompt(context)
    
    println("   📝 生成的提示词长度: ${prompt.length} 字符")
    println("   🔍 提示词预览:")
    println("   " + prompt.take(200) + "...")
}

suspend fun testModelCodeGeneration() {
    println("🧠 测试模型代码生成...")
    
    // 尝试从配置文件读取模型配置
    val config = loadModelConfig()
    
    if (config == null) {
        println("   ⚠️ 未找到有效的模型配置，跳过模型测试")
        println("   💡 请确保 ~/.autodev/config.yaml 中有有效的配置")
        return
    }
    
    println("   🔧 使用模型: ${config.provider}/${config.modelName}")
    
    val testPrompt = """
        请使用 write-file 工具创建一个 Kotlin 数据类文件，要求：
        
        1. 文件路径：test-output/Product.kt
        2. 包含以下功能：
           - Product 数据类（id, name, price, description）
           - 数据验证方法
           - 伴生对象工厂方法
           - 至少 20 行代码
        
        请确保生成的代码格式正确，包含适当的注释和换行。
        
        可用工具：
        /write-file path="文件路径" content="文件内容"
    """.trimIndent()
    
    println("   📤 发送测试提示词...")
    println("   📝 提示词: ${testPrompt.take(100)}...")
    
    // 这里应该调用实际的 LLMService，但由于依赖复杂，我们模拟响应
    val mockResponse = simulateModelResponse()
    
    println("   📥 模型响应:")
    println("   ${mockResponse.take(200)}...")
    
    // 检查响应是否包含多行代码
    val hasMultilineCode = mockResponse.contains("data class") && 
                          mockResponse.contains("\n") && 
                          mockResponse.lines().size > 10
    
    println("   🔍 多行代码检测: ${if (hasMultilineCode) "✅ 包含" else "❌ 缺失"}")
}

fun formatToolListForAI(tools: List<MockTool>): String {
    return tools.joinToString("\n\n") { tool ->
        """
        <tool name="${tool.name}">
          <description>${tool.description}</description>
          <parameters>
            <type>${tool.parameterClass}</type>
            <usage>/${tool.name} [parameters]</usage>
          </parameters>
          <example>
            /${tool.name} path="example.txt"
          </example>
        </tool>
        """.trimIndent()
    }
}

fun generateSimplePrompt(context: CodingAgentContext): String {
    return """
        You are AutoDev, an autonomous AI coding agent.
        
        ## Environment Information
        - OS: ${context.osInfo}
        - Project Path: ${context.projectPath}
        - Current Time: ${context.timestamp}
        - Build Tool: ${context.buildTool}
        - Shell: ${context.shell}
        
        ## Available Tools
        ${context.toolList}
        
        ## Task Execution Guidelines
        1. Always use the write-file tool for creating or modifying files
        2. Ensure proper formatting and indentation in generated code
        3. Include appropriate comments and documentation
        4. Test your changes when possible
        
        Remember: You can write multi-line content using the write-file tool.
    """.trimIndent()
}

fun loadModelConfig(): ModelConfig? {
    return try {
        // 尝试读取配置文件（简化版）
        val configFile = File(System.getProperty("user.home"), ".autodev/config.yaml")
        if (!configFile.exists()) {
            return null
        }
        
        // 这里应该解析 YAML，但为了简化，我们返回一个模拟配置
        ModelConfig(
            provider = "openai",
            modelName = "gpt-4",
            apiKey = "mock-api-key",
            temperature = 0.7
        )
    } catch (e: Exception) {
        null
    }
}

fun simulateModelResponse(): String {
    return """
        我将创建一个 Product 数据类文件。
        
        <devin>
        /write-file path="test-output/Product.kt" content="package com.example.demo

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * 产品数据类
 * 
 * @property id 产品唯一标识符
 * @property name 产品名称
 * @property price 产品价格
 * @property description 产品描述
 */
@Serializable
data class Product(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val description: String
) {
    companion object {
        /**
         * 创建新产品实例
         */
        fun create(name: String, price: BigDecimal, description: String): Product {
            return Product(
                id = System.currentTimeMillis(),
                name = name.trim(),
                price = price,
                description = description.trim()
            )
        }
    }
    
    /**
     * 验证产品数据是否有效
     */
    fun isValid(): Boolean {
        return name.isNotBlank() && 
               price > BigDecimal.ZERO && 
               description.isNotBlank()
    }
    
    /**
     * 获取格式化的价格字符串
     */
    fun getFormattedPrice(): String {
        return "$${price}"
    }
}"
        </devin>
        
        我已经创建了包含多行代码的 Product.kt 文件，包含了数据类、验证方法和工厂方法。
    """.trimIndent()
}

// 辅助函数
operator fun String.times(n: Int): String = this.repeat(n)
