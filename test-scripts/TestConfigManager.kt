package cc.unitmesh.devins.test

import cc.unitmesh.devins.ui.config.ConfigManager
import kotlinx.coroutines.runBlocking

/**
 * Simple test script to verify ConfigManager loading
 * Run with: ./gradlew :mpp-ui:runTestConfigManager
 */
fun main() {
    println("Testing ConfigManager.load()...")
    
    runBlocking {
        try {
            val wrapper = ConfigManager.load()
            println("✅ ConfigManager.load() succeeded")
            println("   Config path: ${ConfigManager.getConfigPath()}")
            println("   Active config: ${wrapper.getActiveName()}")
            println("   All configs: ${wrapper.getAllConfigs().map { it.name }}")
            
            val activeConfig = wrapper.getActiveConfig()
            if (activeConfig != null) {
                println("   Active provider: ${activeConfig.provider}")
                println("   Active model: ${activeConfig.model}")
                println("   Is valid: ${wrapper.isValid()}")
            } else {
                println("   ⚠️ No active config found")
            }
            
            val modelConfig = wrapper.getActiveModelConfig()
            if (modelConfig != null) {
                println("   ModelConfig provider: ${modelConfig.provider}")
                println("   ModelConfig model: ${modelConfig.modelName}")
                println("   ModelConfig isValid: ${modelConfig.isValid()}")
            } else {
                println("   ⚠️ No ModelConfig found")
            }
        } catch (e: Exception) {
            println("❌ ConfigManager.load() failed: ${e.message}")
            e.printStackTrace()
        }
    }
}

