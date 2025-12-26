#!/usr/bin/env kotlin

/**
 * 上下文压缩功能演示脚本
 * 
 * 本脚本演示如何使用 mpp-core 的上下文压缩功能，包括：
 * 1. 配置压缩参数
 * 2. 模拟长对话场景
 * 3. 触发自动压缩
 * 4. 监控压缩过程
 */

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import cc.unitmesh.llm.LLMService
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.compression.CompressionConfig
import cc.unitmesh.llm.compression.CompressionStatus
import cc.unitmesh.agent.conversation.ConversationManager
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("🚀 上下文压缩功能演示")
    println("=" * 50)
    
    // 1. 配置模型和压缩参数
    println("\n📋 1. 配置模型和压缩参数")
    
    val modelConfig = ModelConfig(
        provider = LLMProviderType.OPENAI,
        modelName = "gpt-3.5-turbo",
        apiKey = System.getenv("OPENAI_API_KEY") ?: "demo-key",
        baseUrl = "https://api.openai.com/v1",
        maxTokens = 2000  // 设置较小的限制以便演示压缩
    )
    
    val compressionConfig = CompressionConfig(
        contextPercentageThreshold = 0.6,  // 60% 时触发压缩
        preserveRecentRatio = 0.4,         // 保留 40% 的最近对话
        autoCompressionEnabled = true,
        retryAfterMessages = 3
    )
    
    println("   模型: ${modelConfig.modelName}")
    println("   最大 tokens: ${modelConfig.maxTokens}")
    println("   压缩阈值: ${(compressionConfig.contextPercentageThreshold * 100).toInt()}%")
    println("   保留比例: ${(compressionConfig.preserveRecentRatio * 100).toInt()}%")
    
    // 2. 创建 LLM 服务和对话管理器
    println("\n🔧 2. 初始化服务")
    
    val llmService = LLMService.create(modelConfig, compressionConfig)
    val conversationManager = ConversationManager(
        llmService = llmService,
        systemPrompt = "你是一个专业的编程助手，帮助用户解决技术问题。",
        autoCompress = true
    )
    
    // 3. 设置监控回调
    println("\n📊 3. 设置监控回调")
    
    conversationManager.onTokenUpdate = { tokenInfo ->
        val usage = tokenInfo.getUsagePercentage(llmService.getMaxTokens())
        println("   📈 Token 使用率: ${String.format("%.1f", usage)}%")
        println("      输入: ${tokenInfo.inputTokens}, 输出: ${tokenInfo.outputTokens}")
    }
    
    conversationManager.onCompressionNeeded = { currentTokens, maxTokens ->
        val percentage = (currentTokens.toDouble() / maxTokens.toDouble() * 100).toInt()
        println("   ⚠️  需要压缩: ${currentTokens}/${maxTokens} (${percentage}%)")
    }
    
    conversationManager.onCompressionCompleted = { result ->
        println("   ✅ 压缩完成:")
        println("      状态: ${result.info.compressionStatus}")
        println("      原始 tokens: ${result.info.originalTokenCount}")
        println("      压缩后 tokens: ${result.info.newTokenCount}")
        println("      压缩比例: ${String.format("%.1f", result.info.compressionRatio * 100)}%")
        println("      节省 tokens: ${result.info.tokensSaved}")
    }
    
    println("   回调设置完成")
    
    // 4. 模拟长对话场景
    println("\n💬 4. 模拟长对话场景")
    
    val longConversationTopics = listOf(
        "如何在 Kotlin 中实现单例模式？",
        "解释一下协程的工作原理",
        "什么是依赖注入？有什么好处？",
        "如何优化数据库查询性能？",
        "解释 SOLID 原则",
        "什么是微服务架构？",
        "如何处理并发编程中的竞态条件？",
        "解释 RESTful API 的设计原则",
        "什么是设计模式？举几个例子",
        "如何进行单元测试？"
    )
    
    println("   开始模拟对话...")
    
    longConversationTopics.forEachIndexed { index, topic ->
        println("\n   💭 对话 ${index + 1}: $topic")
        
        // 添加用户消息
        conversationManager.addUserMessage(topic)
        
        // 模拟助手回复（实际场景中这会是 LLM 的回复）
        val mockResponse = generateMockResponse(topic, index)
        conversationManager.addAssistantMessage(mockResponse)
        
        // 显示当前统计
        val stats = conversationManager.getConversationStats()
        println("      消息数: ${stats.messageCount}")
        println("      使用率: ${String.format("%.1f", stats.utilizationRatio * 100)}%")
        
        // 检查是否需要压缩
        if (conversationManager.needsCompression()) {
            println("      🔄 触发自动压缩...")
            val result = conversationManager.compressHistory()
            
            if (result.info.compressionStatus == CompressionStatus.COMPRESSED) {
                println("      ✅ 压缩成功")
            } else {
                println("      ❌ 压缩失败: ${result.info.errorMessage ?: "未知错误"}")
            }
        }
        
        Thread.sleep(100) // 模拟对话间隔
    }
    
    // 5. 最终统计
    println("\n📈 5. 最终统计")
    val finalStats = conversationManager.getConversationStats()
    println("   总消息数: ${finalStats.messageCount}")
    println("   最终使用率: ${String.format("%.1f", finalStats.utilizationRatio * 100)}%")
    println("   Token 信息: ${finalStats.tokenInfo}")
    
    println("\n🎉 演示完成！")
}

fun generateMockResponse(topic: String, index: Int): String {
    return """
    这是对"$topic"的详细回答 (第${index + 1}轮对话)。
    
    在实际应用中，这里会包含：
    - 详细的技术解释
    - 代码示例
    - 最佳实践建议
    - 相关资源链接
    
    这个回答包含了足够的内容来模拟真实的 LLM 响应，
    用于测试上下文压缩功能的触发和执行。
    """.trimIndent()
}

// 扩展函数：字符串重复
operator fun String.times(n: Int): String = this.repeat(n)
