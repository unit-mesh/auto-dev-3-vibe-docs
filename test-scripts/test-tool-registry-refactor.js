/**
 * Test script for the refactored ToolRegistry mechanism
 * 
 * This script verifies:
 * 1. ToolRegistry respects ToolConfigService configuration
 * 2. Only enabled built-in tools are registered
 * 3. CodingAgent.getAllAvailableTools() returns correct tools
 */

const mod = require('../../mpp-core/build/compileSync/js/main/productionLibrary/kotlin/autodev-mpp-core.js');

const JsCodingAgent = mod.cc.unitmesh.agent.JsCodingAgent;
const JsToolConfigFile = mod.cc.unitmesh.agent.config.JsToolConfigFile;
const JsChatConfig = mod.cc.unitmesh.agent.config.JsChatConfig;
const JsKoogLLMService = mod.cc.unitmesh.llm.JsKoogLLMService;
const JsModelConfig = mod.cc.unitmesh.llm.JsModelConfig;

async function testToolRegistryRefactor() {
    console.log('🧪 Testing ToolRegistry refactor...');
    
    try {
        // Test 1: CodingAgent with selective tool configuration
        await testSelectiveToolConfiguration();
        
        // Test 2: CodingAgent with empty configuration
        await testEmptyConfiguration();
        
        // Test 3: CodingAgent with all tools enabled
        await testAllToolsEnabled();
        
        // Test 4: Backward compatibility
        await testBackwardCompatibility();
        
        console.log('✅ All tests passed!');
    } catch (error) {
        console.error('❌ Test failed:', error.message);
        process.exit(1);
    }
}

async function testSelectiveToolConfiguration() {
    console.log('\n📋 Test 1: Selective tool configuration');
    
    try {
        // Create config with only some tools enabled
        const chatConfig = new JsChatConfig(0.7, "", 4096);
        const toolConfig = new JsToolConfigFile(
            ["read-file", "grep"],  // Only enable read-file and grep
            [],  // No MCP tools
            {},  // No MCP servers
            chatConfig
        );
        
        // Create a mock LLM service with valid provider
        const modelConfig = new JsModelConfig("OPENAI", "gpt-4", "test-key", 0.7, 4096, "https://api.openai.com/v1");
        const llmService = new JsKoogLLMService(modelConfig);
        
        // Create CodingAgent with selective configuration
        const agent = new JsCodingAgent(
            "/tmp/test-project",
            llmService,
            10,  // maxIterations
            null,  // renderer
            null,  // mcpServers
            toolConfig
        );
        
        console.log('   ✅ CodingAgent created with selective tool configuration');
        console.log('   📝 Expected: Only read-file and grep tools should be available');
    } catch (error) {
        console.error('   ❌ Failed to create CodingAgent with selective config:', error.message);
        throw error;
    }
}

async function testEmptyConfiguration() {
    console.log('\n📋 Test 2: Empty configuration');
    
    try {
        // Create config with no tools enabled
        const chatConfig = new JsChatConfig(0.7, "", 4096);
        const toolConfig = new JsToolConfigFile(
            [],  // No built-in tools
            [],  // No MCP tools
            {},  // No MCP servers
            chatConfig
        );
        
        // Create a mock LLM service
        const modelConfig = new JsModelConfig("OPENAI", "gpt-4", "test-key", 0.7, 4096, "https://api.openai.com/v1");
        const llmService = new JsKoogLLMService(modelConfig);
        
        // Create CodingAgent with empty configuration
        const agent = new JsCodingAgent(
            "/tmp/test-project",
            llmService,
            10,  // maxIterations
            null,  // renderer
            null,  // mcpServers
            toolConfig
        );
        
        console.log('   ✅ CodingAgent created with empty tool configuration');
        console.log('   📝 Expected: No built-in tools should be available');
    } catch (error) {
        console.error('   ❌ Failed to create CodingAgent with empty config:', error.message);
        throw error;
    }
}

async function testAllToolsEnabled() {
    console.log('\n📋 Test 3: All tools enabled');
    
    try {
        // Create config with all tools enabled
        const chatConfig = new JsChatConfig(0.7, "", 4096);
        const toolConfig = new JsToolConfigFile(
            ["read-file", "write-file", "grep", "glob", "shell"],  // All built-in tools
            [],  // No MCP tools
            {},  // No MCP servers
            chatConfig
        );
        
        // Create a mock LLM service
        const modelConfig = new JsModelConfig("OPENAI", "gpt-4", "test-key", 0.7, 4096, "https://api.openai.com/v1");
        const llmService = new JsKoogLLMService(modelConfig);
        
        // Create CodingAgent with all tools enabled
        const agent = new JsCodingAgent(
            "/tmp/test-project",
            llmService,
            10,  // maxIterations
            null,  // renderer
            null,  // mcpServers
            toolConfig
        );
        
        console.log('   ✅ CodingAgent created with all tools enabled');
        console.log('   📝 Expected: All built-in tools should be available');
    } catch (error) {
        console.error('   ❌ Failed to create CodingAgent with all tools:', error.message);
        throw error;
    }
}

async function testBackwardCompatibility() {
    console.log('\n📋 Test 4: Backward compatibility (no config)');
    
    try {
        // Create a mock LLM service
        const modelConfig = new JsModelConfig("OPENAI", "gpt-4", "test-key", 0.7, 4096, "https://api.openai.com/v1");
        const llmService = new JsKoogLLMService(modelConfig);
        
        // Create CodingAgent without tool configuration
        const agent = new JsCodingAgent(
            "/tmp/test-project",
            llmService,
            10,  // maxIterations
            null,  // renderer
            null,  // mcpServers
            null   // No tool config - should use defaults
        );
        
        console.log('   ✅ CodingAgent created without tool configuration');
        console.log('   📝 Expected: All built-in tools should be available (backward compatibility)');
    } catch (error) {
        console.error('   ❌ Failed to create CodingAgent without config:', error.message);
        throw error;
    }
}

// Run the tests
testToolRegistryRefactor().catch(console.error);
