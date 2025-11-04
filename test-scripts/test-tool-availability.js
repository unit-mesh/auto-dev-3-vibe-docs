/**
 * Test script to verify actual tool availability based on configuration
 * 
 * This script creates ToolRegistry instances directly and tests tool availability
 */

const mod = require('../../mpp-core/build/compileSync/js/main/productionLibrary/kotlin/autodev-mpp-core.js');

// Access ToolRegistry and related classes
const ToolRegistry = mod.cc.unitmesh.agent.tool.registry.ToolRegistry;
const ToolConfigService = mod.cc.unitmesh.agent.config.ToolConfigService;
const JsToolConfigFile = mod.cc.unitmesh.agent.config.JsToolConfigFile;
const JsChatConfig = mod.cc.unitmesh.agent.config.JsChatConfig;

async function testToolAvailability() {
    console.log('🧪 Testing actual tool availability...');
    
    try {
        // Test 1: ToolRegistry with selective configuration
        await testSelectiveToolAvailability();
        
        // Test 2: ToolRegistry with empty configuration
        await testEmptyToolAvailability();
        
        // Test 3: ToolRegistry without configuration (backward compatibility)
        await testBackwardCompatibility();
        
        console.log('✅ All tool availability tests passed!');
    } catch (error) {
        console.error('❌ Tool availability test failed:', error.message);
        console.error('Stack trace:', error.stack);
        process.exit(1);
    }
}

async function testSelectiveToolAvailability() {
    console.log('\n📋 Test 1: Selective tool availability');
    
    try {
        // Create config with only read-file and grep enabled
        const chatConfig = new JsChatConfig(0.7, "", 4096);
        const toolConfig = new JsToolConfigFile(
            ["read-file", "grep"],  // Only these tools enabled
            [],
            {},
            chatConfig
        );
        
        // Create ToolConfigService
        const configService = new ToolConfigService(toolConfig.toCommon());
        
        // Create ToolRegistry with configuration
        const registry = new ToolRegistry(null, null, configService);
        
        // Get all available tools
        const availableTools = registry.getAllTools();
        const toolNames = Object.keys(availableTools);
        
        console.log('   📝 Available tools:', toolNames);
        
        // Verify only expected tools are available
        const expectedTools = ["read-file", "grep"];
        const unexpectedTools = ["write-file", "glob", "shell"];
        
        expectedTools.forEach(toolName => {
            if (!toolNames.includes(toolName)) {
                throw new Error(`Expected tool '${toolName}' not found`);
            }
        });
        
        unexpectedTools.forEach(toolName => {
            if (toolNames.includes(toolName)) {
                throw new Error(`Unexpected tool '${toolName}' found`);
            }
        });
        
        console.log('   ✅ Tool availability matches configuration');
        
    } catch (error) {
        console.error('   ❌ Selective tool availability test failed:', error.message);
        throw error;
    }
}

async function testEmptyToolAvailability() {
    console.log('\n📋 Test 2: Empty tool availability');
    
    try {
        // Create config with no tools enabled
        const chatConfig = new JsChatConfig(0.7, "", 4096);
        const toolConfig = new JsToolConfigFile(
            [],  // No tools enabled
            [],
            {},
            chatConfig
        );
        
        // Create ToolConfigService
        const configService = new ToolConfigService(toolConfig.toCommon());
        
        // Create ToolRegistry with configuration
        const registry = new ToolRegistry(null, null, configService);
        
        // Get all available tools
        const availableTools = registry.getAllTools();
        const toolNames = Object.keys(availableTools);
        
        console.log('   📝 Available tools:', toolNames);
        
        // Should have no built-in tools (only dynamic tools if any)
        const builtinTools = ["read-file", "write-file", "grep", "glob", "shell"];
        builtinTools.forEach(toolName => {
            if (toolNames.includes(toolName)) {
                throw new Error(`Unexpected built-in tool '${toolName}' found`);
            }
        });
        
        console.log('   ✅ No built-in tools available as expected');
        
    } catch (error) {
        console.error('   ❌ Empty tool availability test failed:', error.message);
        throw error;
    }
}

async function testBackwardCompatibility() {
    console.log('\n📋 Test 3: Backward compatibility (no config)');
    
    try {
        // Create ToolRegistry without configuration
        const registry = new ToolRegistry(null, null, null);
        
        // Get all available tools
        const availableTools = registry.getAllTools();
        const toolNames = Object.keys(availableTools);
        
        console.log('   📝 Available tools:', toolNames);
        
        // Should have all built-in tools available
        const expectedTools = ["read-file", "write-file", "grep", "glob"];
        // Note: shell might not be available depending on environment
        
        expectedTools.forEach(toolName => {
            if (!toolNames.includes(toolName)) {
                throw new Error(`Expected tool '${toolName}' not found`);
            }
        });
        
        console.log('   ✅ All expected built-in tools available (backward compatibility)');
        
    } catch (error) {
        console.error('   ❌ Backward compatibility test failed:', error.message);
        throw error;
    }
}

// Run the tests
testToolAvailability().catch(console.error);
