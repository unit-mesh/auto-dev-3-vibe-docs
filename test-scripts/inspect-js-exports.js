#!/usr/bin/env node

/**
 * Inspect the JS exports to understand the module structure
 */

try {
    const module = require('../../mpp-core/build/packages/js/autodev-mpp-core');
    
    console.log('🔍 Inspecting JS module exports');
    console.log('='.repeat(50));
    
    console.log('\nModule type:', typeof module);
    console.log('Module keys:', Object.keys(module));
    
    // Check for CodingAgentContext
    if (module.CodingAgentContext) {
        console.log('\n✅ CodingAgentContext found');
        console.log('Type:', typeof module.CodingAgentContext);
        console.log('Keys:', Object.keys(module.CodingAgentContext));
        
        if (module.CodingAgentContext.Companion) {
            console.log('Companion keys:', Object.keys(module.CodingAgentContext.Companion));
        }
    } else {
        console.log('\n❌ CodingAgentContext not found');
    }
    
    // Check for CodingAgentTemplate
    if (module.CodingAgentTemplate) {
        console.log('\n✅ CodingAgentTemplate found');
        console.log('Type:', typeof module.CodingAgentTemplate);
        console.log('Keys:', Object.keys(module.CodingAgentTemplate));
    } else {
        console.log('\n❌ CodingAgentTemplate not found');
    }
    
    // Look for other relevant exports
    const relevantKeys = Object.keys(module).filter(key => 
        key.toLowerCase().includes('agent') || 
        key.toLowerCase().includes('template') ||
        key.toLowerCase().includes('context')
    );
    
    console.log('\nRelevant exports:', relevantKeys);
    
    // Try to find the actual exports
    console.log('\nAll exports:');
    Object.keys(module).forEach(key => {
        console.log(`  ${key}: ${typeof module[key]}`);
    });

    // Deep dive into cc object
    if (module.cc) {
        console.log('\n🔍 Exploring cc object:');
        exploreObject(module.cc, 'cc', 3);
    }

    // Check for CodingAgentTemplate specifically
    if (module.cc && module.cc.unitmesh && module.cc.unitmesh.agent) {
        const agent = module.cc.unitmesh.agent;
        console.log('\n🔍 Checking for CodingAgentTemplate:');
        if (agent.CodingAgentTemplate) {
            console.log('✅ Found CodingAgentTemplate');
            console.log('Keys:', Object.keys(agent.CodingAgentTemplate));
        } else {
            console.log('❌ CodingAgentTemplate not found in agent namespace');
            console.log('Available keys:', Object.keys(agent));
        }
    }
    
} catch (error) {
    console.error('❌ Error inspecting module:', error.message);
}

function exploreObject(obj, path, maxDepth) {
    if (maxDepth <= 0 || typeof obj !== 'object' || obj === null) {
        return;
    }

    Object.keys(obj).forEach(key => {
        const value = obj[key];
        const newPath = `${path}.${key}`;

        if (key.toLowerCase().includes('agent') ||
            key.toLowerCase().includes('template') ||
            key.toLowerCase().includes('context')) {
            console.log(`  🎯 ${newPath}: ${typeof value}`);
            if (typeof value === 'object' && value !== null) {
                console.log(`    Keys: ${Object.keys(value)}`);
            }
        } else {
            console.log(`  ${newPath}: ${typeof value}`);
        }

        if (typeof value === 'object' && value !== null && maxDepth > 1) {
            exploreObject(value, newPath, maxDepth - 1);
        }
    });
}
