#!/usr/bin/env node

/**
 * Test script to verify tool template generation issues
 * This will help identify why LLM models are not generating correct tool calls
 */

const mppCore = require('../../mpp-core/build/packages/js/autodev-mpp-core');
const JsCodingAgentContext = mppCore.cc.unitmesh.agent.JsCodingAgentContext;
const JsCodingAgentContextBuilder = mppCore.cc.unitmesh.agent.JsCodingAgentContextBuilder;
const JsCodingAgentPromptRenderer = mppCore.cc.unitmesh.agent.JsCodingAgentPromptRenderer;

// Mock tool data
const mockTools = [
    {
        name: 'read-file',
        description: 'Read file content with optional line range filtering',
        getParameterClass: () => 'ReadFileParams'
    },
    {
        name: 'write-file', 
        description: 'Write content to a file with various options',
        getParameterClass: () => 'WriteFileParams'
    },
    {
        name: 'shell',
        description: 'Execute shell commands with various options',
        getParameterClass: () => 'ShellParams'
    }
];

function testToolTemplateGeneration() {
    console.log('🔍 Testing Tool Template Generation');
    console.log('='.repeat(50));
    
    try {
        // Test 1: Create context with tools
        console.log('\n1. Creating CodingAgentContext...');

        // Use the builder to create context
        const builder = new JsCodingAgentContextBuilder();
        const context = builder
            .setProjectPath('/test/project')
            .setOsInfo('Test OS')
            .setTimestamp('2024-01-01T00:00:00Z')
            .setToolList('Mock tool list for testing')
            .build();
        
        console.log('✅ Context created successfully');
        console.log(`Tool list length: ${context.toolList.length} characters`);
        
        // Test 2: Check tool list content
        console.log('\n2. Analyzing tool list content...');
        
        if (context.toolList.length === 0) {
            console.log('❌ Tool list is empty!');
            return;
        }
        
        // Show first part of tool list
        const toolListPreview = context.toolList.substring(0, 500);
        console.log('Tool list preview:');
        console.log(toolListPreview);
        console.log('...');
        
        // Test 3: Check for XML structure
        console.log('\n3. Checking XML structure...');
        
        const hasToolTags = context.toolList.includes('<tool name=');
        const hasParameterTags = context.toolList.includes('<parameters>');
        const hasExampleTags = context.toolList.includes('<example>');
        
        console.log(`Has <tool> tags: ${hasToolTags ? '✅' : '❌'}`);
        console.log(`Has <parameters> tags: ${hasParameterTags ? '✅' : '❌'}`);
        console.log(`Has <example> tags: ${hasExampleTags ? '✅' : '❌'}`);
        
        // Test 4: Generate complete template
        console.log('\n4. Generating complete template...');

        try {
            const renderer = new JsCodingAgentPromptRenderer();
            const template = renderer.render(context, 'EN');
            console.log(`Template generated successfully!`);
            console.log(`Template length: ${template.length} characters`);

            // Check if tools are included
            const hasToolSection = template.includes('Available Tools');
            console.log(`Has Available Tools section: ${hasToolSection ? '✅' : '❌'}`);

        } catch (error) {
            console.log(`❌ Error generating template: ${error.message}`);
        }
        
        // Test 5: Check specific tool formatting
        console.log('\n5. Checking specific tool formatting...');
        
        mockTools.forEach(tool => {
            const toolInList = context.toolList.includes(`<tool name="${tool.name}">`);
            console.log(`${tool.name} in tool list: ${toolInList ? '✅' : '❌'}`);
        });
        
        // Test 6: Look for potential issues
        console.log('\n6. Looking for potential issues...');
        
        // Check for common problems
        const issues = [];
        
        if (!context.toolList.includes('DevIns')) {
            issues.push('Missing DevIns command format');
        }
        
        if (!context.toolList.includes('/')) {
            issues.push('Missing command prefix (/)');
        }
        
        if (context.toolList.includes('undefined')) {
            issues.push('Contains undefined values');
        }
        
        if (context.toolList.includes('null')) {
            issues.push('Contains null values');
        }
        
        if (issues.length > 0) {
            console.log('❌ Issues found:');
            issues.forEach(issue => console.log(`  - ${issue}`));
        } else {
            console.log('✅ No obvious issues found');
        }
        
        // Test 7: Export sample for manual inspection
        console.log('\n7. Exporting sample for manual inspection...');
        
        const fs = require('fs');
        const sampleOutput = {
            toolList: context.toolList,
            template: template.substring(0, 1000) + '...',
            timestamp: new Date().toISOString()
        };
        
        fs.writeFileSync('/tmp/tool-template-sample.json', JSON.stringify(sampleOutput, null, 2));
        console.log('✅ Sample exported to /tmp/tool-template-sample.json');
        
    } catch (error) {
        console.error('❌ Error during testing:', error);
        console.error(error.stack);
    }
}

// Run the test
testToolTemplateGeneration();
