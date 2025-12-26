#!/usr/bin/env kotlin

/**
 * 手动JVM测试脚本 - 验证 Custom OpenAI (GLM) 支持
 * 
 * 这个脚本验证以下功能：
 * 1. CUSTOM_OPENAI_BASE provider type 是否正确定义
 * 2. ModelConfig 验证逻辑是否正确
 * 3. ExecutorFactory 是否能创建正确的 executor
 * 4. ModelRegistry 是否能创建正确的模型
 */

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import cc.unitmesh.llm.*
import cc.unitmesh.llm.clients.CustomOpenAILLMClient
import ai.koog.prompt.llm.LLMProvider

fun main() {
    println("🧪 JVM 平台测试 - Custom OpenAI (GLM) 支持")
    println("=" * 60)
    
    var passed = 0
    var failed = 0
    
    // Test 1: LLMProviderType 枚举
    println("\n📋 Test 1: LLMProviderType.CUSTOM_OPENAI_BASE 存在性检查")
    try {
        val provider = LLMProviderType.CUSTOM_OPENAI_BASE
        println("✅ CUSTOM_OPENAI_BASE provider type 已定义")
        println("   - displayName: ${provider.displayName}")
        passed++
    } catch (e: Exception) {
        println("❌ CUSTOM_OPENAI_BASE provider type 未找到: ${e.message}")
        failed++
    }
    
    // Test 2: ModelConfig 验证 - 有效配置
    println("\n📋 Test 2: ModelConfig 验证 - 有效的 GLM 配置")
    try {
        val validConfig = ModelConfig(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            apiKey = "7145ac1bf6474f2783e8b4d52b335ab0.gfq0BBvvFy04iwTb",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            temperature = 0.7,
            maxTokens = 8192
        )
        
        if (validConfig.isValid()) {
            println("✅ 有效的 GLM 配置通过验证")
            println("   - provider: ${validConfig.provider}")
            println("   - modelName: ${validConfig.modelName}")
            println("   - baseUrl: ${validConfig.baseUrl}")
            passed++
        } else {
            println("❌ 有效配置未通过验证")
            failed++
        }
    } catch (e: Exception) {
        println("❌ 配置验证失败: ${e.message}")
        failed++
    }
    
    // Test 3: ModelConfig 验证 - 缺少 baseUrl
    println("\n📋 Test 3: ModelConfig 验证 - 缺少 baseUrl 应失败")
    try {
        val invalidConfig = ModelConfig(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            apiKey = "test-key",
            baseUrl = ""
        )
        
        if (!invalidConfig.isValid()) {
            println("✅ 缺少 baseUrl 的配置正确失败")
            passed++
        } else {
            println("❌ 缺少 baseUrl 的配置应该失败但通过了")
            failed++
        }
    } catch (e: Exception) {
        println("❌ 测试异常: ${e.message}")
        failed++
    }
    
    // Test 4: ModelConfig 验证 - 缺少 apiKey
    println("\n📋 Test 4: ModelConfig 验证 - 缺少 apiKey 应失败")
    try {
        val invalidConfig = ModelConfig(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            apiKey = "",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4"
        )
        
        if (!invalidConfig.isValid()) {
            println("✅ 缺少 apiKey 的配置正确失败")
            passed++
        } else {
            println("❌ 缺少 apiKey 的配置应该失败但通过了")
            failed++
        }
    } catch (e: Exception) {
        println("❌ 测试异常: ${e.message}")
        failed++
    }
    
    // Test 5: ExecutorFactory 创建
    println("\n📋 Test 5: ExecutorFactory 能创建 Custom OpenAI executor")
    try {
        val config = ModelConfig(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            apiKey = "test-key",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4"
        )
        
        val executor = ExecutorFactory.create(config)
        println("✅ ExecutorFactory 成功创建 executor")
        println("   - executor type: ${executor::class.simpleName}")
        passed++
    } catch (e: Exception) {
        println("❌ ExecutorFactory 创建失败: ${e.message}")
        e.printStackTrace()
        failed++
    }
    
    // Test 6: ModelRegistry 创建通用模型
    println("\n📋 Test 6: ModelRegistry 创建 Generic Model")
    try {
        val model = ModelRegistry.createGenericModel(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            contextLength = 128000L
        )
        
        println("✅ ModelRegistry 成功创建模型")
        println("   - model id: ${model.id}")
        println("   - provider: ${model.provider}")
        println("   - contextLength: ${model.contextLength}")
        
        if (model.provider == LLMProvider.OpenAI && model.id == "glm-4-plus") {
            passed++
        } else {
            println("❌ 模型属性不正确")
            failed++
        }
    } catch (e: Exception) {
        println("❌ ModelRegistry 创建失败: ${e.message}")
        failed++
    }
    
    // Test 7: CustomOpenAILLMClient 创建
    println("\n📋 Test 7: CustomOpenAILLMClient 实例化")
    try {
        val client = CustomOpenAILLMClient(
            apiKey = "test-api-key",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            chatCompletionsPath = "chat/completions"
        )
        
        println("✅ CustomOpenAILLMClient 成功实例化")
        println("   - provider: ${client.llmProvider()}")
        
        if (client.llmProvider() == LLMProvider.OpenAI) {
            passed++
        } else {
            println("❌ Client provider 应该是 OpenAI")
            failed++
        }
    } catch (e: Exception) {
        println("❌ CustomOpenAILLMClient 实例化失败: ${e.message}")
        e.printStackTrace()
        failed++
    }
    
    // Test 8: LLMService 验证无效配置
    println("\n📋 Test 8: LLMService 应拒绝无效配置")
    try {
        val invalidConfig = ModelConfig(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            apiKey = "",
            baseUrl = ""
        )
        
        try {
            LLMService.create(invalidConfig)
            println("❌ LLMService 应该拒绝无效配置")
            failed++
        } catch (e: IllegalArgumentException) {
            println("✅ LLMService 正确拒绝无效配置")
            println("   - error message: ${e.message}")
            passed++
        }
    } catch (e: Exception) {
        println("❌ 测试异常: ${e.message}")
        failed++
    }
    
    // 总结
    println("\n" + "=" * 60)
    println("📊 测试总结")
    println("=" * 60)
    println("✅ 通过: $passed")
    println("❌ 失败: $failed")
    println("📈 通过率: ${(passed * 100.0 / (passed + failed)).toInt()}%")
    
    if (failed == 0) {
        println("\n🎉 所有测试通过！Custom OpenAI (GLM) 支持在 JVM 平台上正常工作！")
    } else {
        println("\n⚠️  有 $failed 个测试失败，需要修复")
        System.exit(1)
    }
}

operator fun String.times(count: Int) = this.repeat(count)

