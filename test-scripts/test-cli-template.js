#!/usr/bin/env node

/**
 * Test CLI template generation to verify the new format works correctly
 */

const mppCore = require('../../build/js/packages/autodev-mpp-core/kotlin/autodev-mpp-core.js');
const JsToolRegistry = mppCore.cc.unitmesh.llm.JsToolRegistry;
const JsCodingAgentContextBuilder = mppCore.cc.unitmesh.agent.JsCodingAgentContextBuilder;
const JsCodingAgentPromptRenderer = mppCore.cc.unitmesh.agent.JsCodingAgentPromptRenderer;

function testCliTemplate() {
    console.log('🧪 Testing CLI Template Generation');
    console.log('='.repeat(50));
    
    try {
        // Step 1: Create realistic scenario
        console.log('\n1. Creating Realistic Scenario...');
        const projectPath = '/tmp/test-hello-world';
        const task = 'Create a simple hello world';
        
        const toolRegistry = new JsToolRegistry(projectPath);
        const toolList = toolRegistry.formatToolListForAI();
        
        console.log(`✅ Project: ${projectPath}`);
        console.log(`✅ Task: ${task}`);
        console.log(`✅ Tool list: ${toolList.length} characters`);
        
        // Step 2: Create complete context
        console.log('\n2. Creating Complete Context...');
        const builder = new JsCodingAgentContextBuilder();
        const context = builder
            .setProjectPath(projectPath)
            .setOsInfo('macOS 14.0')
            .setTimestamp(new Date().toISOString())
            .setToolList(toolList)
            .setBuildTool('npm')
            .setShell('/bin/zsh')
            .build();
        
        console.log('✅ Context created');
        
        // Step 3: Generate template
        console.log('\n3. Generating Template...');
        const renderer = new JsCodingAgentPromptRenderer();
        const template = renderer.render(context, 'EN');
        
        console.log(`✅ Template generated: ${template.length} characters`);
        
        // Step 4: Analyze template for AI Agent compatibility
        console.log('\n4. Analyzing Template for AI Agent Compatibility...');
        
        const compatibility = {
            'Has environment info': template.includes('Environment Information'),
            'Has available tools': template.includes('Available Tools'),
            'Has JSON Schema format': template.includes('```json'),
            'Has DevIns examples': template.includes('/read-file\n```json'),
            'Has task guidelines': template.includes('Task Execution Guidelines'),
            'Has one tool per response rule': template.includes('One Tool Per Response'),
            'Has proper example format': template.includes('{"path": "src/main.ts"}'),
            'Has response format': template.includes('Response Format'),
            'Mentions JSON Schema validation': template.includes('JSON Schema for parameter validation'),
            'Has tool usage format': template.includes('Tool Usage Format')
        };
        
        Object.entries(compatibility).forEach(([check, passed]) => {
            console.log(`  ${passed ? '✅' : '❌'} ${check}`);
        });
        
        // Step 5: Show key sections
        console.log('\n5. Key Template Sections:');
        console.log('-'.repeat(40));
        
        // Show tool usage format section
        const toolUsageStart = template.indexOf('## Tool Usage Format');
        const toolUsageEnd = template.indexOf('## Task Execution Guidelines');
        if (toolUsageStart >= 0 && toolUsageEnd >= 0) {
            const toolUsageSection = template.substring(toolUsageStart, toolUsageEnd);
            console.log('Tool Usage Format Section:');
            console.log(toolUsageSection.trim());
            console.log('-'.repeat(40));
        }
        
        // Show a sample tool description
        const readFileStart = template.indexOf('## read-file');
        const readFileEnd = template.indexOf('## ', readFileStart + 1);
        if (readFileStart >= 0) {
            const readFileSection = readFileEnd > 0 
                ? template.substring(readFileStart, readFileEnd)
                : template.substring(readFileStart, readFileStart + 600);
            console.log('Sample Tool Description:');
            console.log(readFileSection.trim());
            console.log('-'.repeat(40));
        }
        
        // Step 6: Export template for manual inspection
        console.log('\n6. Exporting Template...');
        const fs = require('fs');
        
        const exportData = {
            testInfo: {
                projectPath,
                task,
                timestamp: new Date().toISOString()
            },
            metrics: {
                toolListLength: toolList.length,
                templateLength: template.length
            },
            compatibility,
            template: template
        };
        
        fs.writeFileSync('/tmp/cli-template-test.json', JSON.stringify(exportData, null, 2));
        fs.writeFileSync('/tmp/cli-template-test.md', template);
        
        console.log('✅ Template exported to:');
        console.log('   - /tmp/cli-template-test.json (with metadata)');
        console.log('   - /tmp/cli-template-test.md (template only)');
        
        // Step 7: Summary
        console.log('\n7. Summary:');
        const allCompatibilityChecksPass = Object.values(compatibility).every(Boolean);
        
        if (allCompatibilityChecksPass) {
            console.log('🎉 SUCCESS: Template is fully compatible with AI Agents!');
            console.log('');
            console.log('✨ Key Features:');
            console.log('   📋 Clear tool usage format with JSON Schema');
            console.log('   🔧 DevIns-style examples (/command + JSON)');
            console.log('   📖 Comprehensive guidelines and constraints');
            console.log('   🎯 One tool per response rule for better control');
            console.log('   📏 Compact format for efficient token usage');
            console.log('');
            console.log('🚀 Ready for CLI testing!');
        } else {
            console.log('⚠️  PARTIAL: Some compatibility issues found');
            const passedChecks = Object.values(compatibility).filter(Boolean).length;
            console.log(`   Passed: ${passedChecks}/${Object.keys(compatibility).length} checks`);
        }
        
    } catch (error) {
        console.error('❌ Error during CLI template testing:', error);
        console.error(error.stack);
    }
}

// Run the test
testCliTemplate();
