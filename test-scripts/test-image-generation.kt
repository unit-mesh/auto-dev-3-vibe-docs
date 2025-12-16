import cc.unitmesh.config.ConfigManager
import cc.unitmesh.llm.image.ImageGenerationService
import kotlinx.coroutines.runBlocking

/**
 * Test script to verify ImageGenerationService
 *
 * This script:
 * 1. Loads GLM config from ConfigManager
 * 2. Creates ImageGenerationService
 * 3. Tests image generation with a sample prompt
 */
fun main() = runBlocking {
    println("=== Testing ImageGenerationService ===\n")

    // 1. Load GLM config
    println("1. Loading GLM configuration...")
    val configWrapper = ConfigManager.load()
    val glmConfig = configWrapper.getModelConfigByProvider("glm")

    if (glmConfig == null) {
        println("❌ No GLM provider configured in ~/.autodev/config.yaml")
        println("   Please add a GLM configuration with CogView-3-Flash support")
        println("\n   Example config:")
        println("   configs:")
        println("     - name: glm")
        println("       provider: glm")
        println("       model: cogview-3-flash")
        println("       apiKey: your-api-key-here")
        println("       baseUrl: https://open.bigmodel.cn/api/paas/v4")
        return@runBlocking
    }

    println("✅ GLM config loaded:")
    println("   Provider: ${glmConfig.provider}")
    println("   Model: ${glmConfig.modelName}")
    println("   API Key: ${glmConfig.apiKey.take(10)}...")
    println("   Base URL: ${glmConfig.baseUrl}")

    // 2. Create ImageGenerationService
    println("\n2. Creating ImageGenerationService...")
    val imageService = ImageGenerationService.create(glmConfig)
    println("✅ ImageGenerationService created")

    // 3. Test image generation
    println("\n3. Testing image generation...")
    val testPrompts = listOf(
        "Marina Bay Sands, iconic Singapore landmark, modern architecture, waterfront",
        "Singapore skyline at sunset, cityscape, skyscrapers",
        "Merlion statue in Singapore, tourist attraction"
    )

    for ((index, prompt) in testPrompts.withIndex()) {
        println("\n--- Test ${index + 1} ---")
        println("Prompt: $prompt")

        try {
            val result = imageService.generateImage(prompt)
            when (result) {
                is cc.unitmesh.llm.image.ImageGenerationResult.Success -> {
                    println("✅ SUCCESS: Image generated")
                    println("   URL: ${result.imageUrl}")
                }
                is cc.unitmesh.llm.image.ImageGenerationResult.Error -> {
                    println("❌ ERROR: ${result.message}")
                }
            }
        } catch (e: Exception) {
            println("❌ EXCEPTION: ${e.message}")
            e.printStackTrace()
        }
    }

    println("\n=== Test Complete ===")
}

