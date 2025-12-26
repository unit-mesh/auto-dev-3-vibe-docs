/**
 * 上下文压缩功能演示
 * 
 * 本示例展示如何使用 mpp-core 的上下文压缩功能
 */

import cc.unitmesh.llm.LLMService
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.compression.CompressionConfig
import cc.unitmesh.llm.compression.CompressionStatus
import cc.unitmesh.agent.conversation.ConversationManager
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("🚀 上下文压缩功能演示\n")
    
    // 1. 创建模型配置
    val modelConfig = ModelConfig(
        provider = LLMProviderType.DEEPSEEK,
        modelName = "deepseek-chat",
        apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "",
        temperature = 0.7,
        maxTokens = 8192  // 设置较小的值以便演示压缩
    )
    
    // 2. 创建压缩配置
    val compressionConfig = CompressionConfig(
        contextPercentageThreshold = 0.6,  // 60% 时触发压缩（方便演示）
        preserveRecentRatio = 0.3,         // 保留最近 30% 的对话
        autoCompressionEnabled = true,
        retryAfterMessages = 3
    )
    
    // 3. 创建 LLM 服务
    val llmService = LLMService.create(modelConfig, compressionConfig)
    
    println("✅ LLM 服务初始化完成")
    println("   模型: ${modelConfig.modelName}")
    println("   最大 tokens: ${llmService.getMaxTokens()}")
    println("   压缩阈值: ${(compressionConfig.contextPercentageThreshold * 100).toInt()}%\n")
    
    // 4. 创建对话管理器
    val conversationManager = ConversationManager(
        llmService = llmService,
        systemPrompt = "你是一个有用的 AI 助手。",
        autoCompress = true
    )
    
    // 设置回调监听
    conversationManager.onTokenUpdate = { tokenInfo ->
        val usage = tokenInfo.getUsagePercentage(llmService.getMaxTokens())
        println("📊 Token 使用率: ${String.format("%.1f", usage)}%")
        println("   输入: ${tokenInfo.inputTokens}, 输出: ${tokenInfo.outputTokens}")
    }
    
    conversationManager.onCompressionNeeded = { current, max ->
        println("\n⚠️ 建议压缩！")
        println("   当前: $current tokens")
        println("   最大: $max tokens")
        println("   使用率: ${String.format("%.1f", current.toDouble() / max.toDouble() * 100)}%\n")
    }
    
    conversationManager.onCompressionCompleted = { result ->
        println("\n✅ 压缩完成！")
        println("   原始 tokens: ${result.info.originalTokenCount}")
        println("   压缩后 tokens: ${result.info.newTokenCount}")
        println("   节省: ${result.info.tokensSaved} tokens (${String.format("%.1f", result.info.compressionRatio * 100)}%)\n")
    }
    
    // 5. 模拟长对话
    println("💬 开始模拟长对话...\n")
    
    val testMessages = listOf(
        "请介绍一下 Kotlin Multiplatform",
        "Compose Multiplatform 有什么优势？",
        "如何在 KMP 中处理网络请求？",
        "KMP 如何做依赖注入？",
        "请详细说明 expect/actual 机制",
        "KMP 中如何处理平台特定的 UI？",
        "解释一下 KMP 的编译流程",
        "KMP 和 Flutter 相比有什么区别？"
    )
    
    for ((index, userMessage) in testMessages.withIndex()) {
        println("👤 用户 [${index + 1}/${testMessages.size}]: $userMessage")
        
        // 发送消息并收集响应
        val responseBuilder = StringBuilder()
        conversationManager.sendMessage(userMessage).collect { chunk ->
            responseBuilder.append(chunk)
            print(chunk)
        }
        println("\n")
        
        // 添加助手响应到历史
        conversationManager.addAssistantResponse(responseBuilder.toString())
        
        // 显示当前状态
        val stats = conversationManager.getConversationStats()
        println("📈 对话状态:")
        println("   消息数: ${stats.messageCount}")
        println("   Token 使用率: ${String.format("%.1f", stats.utilizationRatio * 100)}%")
        println()
        
        // 如果接近阈值，提示即将压缩
        if (stats.utilizationRatio > compressionConfig.contextPercentageThreshold - 0.1) {
            println("⚡ 接近压缩阈值...\n")
        }
        
        // 模拟延迟
        kotlinx.coroutines.delay(1000)
    }
    
    // 6. 手动触发压缩演示
    println("\n🗜️ 手动压缩演示:")
    val compressionResult = conversationManager.compressHistory(force = true)
    
    when (compressionResult.info.compressionStatus) {
        CompressionStatus.COMPRESSED -> {
            println("✅ 手动压缩成功!")
            println("   原始 tokens: ${compressionResult.info.originalTokenCount}")
            println("   压缩后 tokens: ${compressionResult.info.newTokenCount}")
            println("   压缩比例: ${String.format("%.1f", compressionResult.info.compressionRatio * 100)}%")
        }
        CompressionStatus.NOOP -> {
            println("ℹ️ 无需压缩")
        }
        else -> {
            println("❌ 压缩失败: ${compressionResult.info.errorMessage}")
        }
    }
    
    // 7. 显示最终状态
    println("\n📊 最终状态:")
    val finalStats = conversationManager.getConversationStats()
    println("   总消息数: ${finalStats.messageCount}")
    println("   Token 使用: ${finalStats.tokenInfo.inputTokens} / ${finalStats.maxTokens}")
    println("   使用率: ${String.format("%.1f", finalStats.utilizationRatio * 100)}%")
    
    // 8. 显示压缩后的历史概览
    println("\n📝 压缩后的历史概览:")
    conversationManager.getHistory().forEachIndexed { index, message ->
        val preview = message.content.take(80).replace("\n", " ")
        println("   [$index] ${message.role}: $preview${if (message.content.length > 80) "..." else ""}")
    }
    
    println("\n✨ 演示完成!")
}

// 辅助函数：打印分隔线
fun printSeparator() {
    println("─".repeat(60))
}
