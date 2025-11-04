#!/usr/bin/env node

/**
 * Analyze the actual tool template generation problem
 * This will create a real tool list and examine the generated template
 */

const mppCore = require('../../mpp-core/build/packages/js/autodev-mpp-core');
const JsToolRegistry = mppCore.cc.unitmesh.llm.JsToolRegistry;
const JsCodingAgentContextBuilder = mppCore.cc.unitmesh.agent.JsCodingAgentContextBuilder;
const JsCodingAgentPromptRenderer = mppCore.cc.unitmesh.agent.JsCodingAgentPromptRenderer;

function createToolListForAI(agentTools) {
    return agentTools.map(tool => {
        return `<tool name="${tool.name}">
  <description>${tool.description}</description>
  <example>${tool.example}</example>
</tool>`;
    }).join('\n\n');
}

function analyzeToolTemplateGeneration() {
    console.log('🔍 Analyzing Tool Template Generation Problem');
    console.log('='.repeat(60));
    
    try {
        // Step 1: Create a real tool registry
        console.log('\n1. Creating Tool Registry...');
        const toolRegistry = new JsToolRegistry('/test/project');
        console.log('✅ Tool registry created');
        
        // Step 2: Get available tools
        console.log('\n2. Getting Available Tools...');
        const availableToolNames = toolRegistry.getAvailableTools();
        const agentTools = toolRegistry.getAgentTools();
        console.log(`Found ${availableToolNames.length} tool names:`, availableToolNames);
        console.log(`Found ${agentTools.length} agent tools:`);
        agentTools.forEach(tool => {
            console.log(`  - ${tool.name}: ${tool.description.substring(0, 50)}...`);
        });

        // Step 3: We need to create tool list manually since formatToolListForAI is not available
        console.log('\n3. Creating Tool List Manually...');
        const toolListForAI = createToolListForAI(agentTools);
        console.log(`Tool list length: ${toolListForAI.length} characters`);
        
        // Show first part of the tool list
        console.log('\nFirst 500 characters of tool list:');
        console.log('-'.repeat(50));
        console.log(toolListForAI.substring(0, 500));
        console.log('-'.repeat(50));
        
        // Step 4: Create context with real tool list
        console.log('\n4. Creating Context with Real Tool List...');
        const builder = new JsCodingAgentContextBuilder();
        const context = builder
            .setProjectPath('/test/project')
            .setOsInfo('macOS 14.0')
            .setTimestamp(new Date().toISOString())
            .setToolList(toolListForAI)
            .setBuildTool('gradle')
            .setShell('/bin/zsh')
            .build();
        
        console.log('✅ Context created with real tool list');
        console.log(`Context tool list length: ${context.toolList.length} characters`);
        
        // Step 5: Generate complete template
        console.log('\n5. Generating Complete Template...');
        const renderer = new JsCodingAgentPromptRenderer();
        const template = renderer.render(context, 'EN');
        
        console.log(`✅ Template generated successfully!`);
        console.log(`Template length: ${template.length} characters`);
        
        // Step 6: Analyze template structure
        console.log('\n6. Analyzing Template Structure...');
        analyzeTemplateStructure(template);
        
        // Step 7: Export for manual inspection
        console.log('\n7. Exporting for Manual Inspection...');
        const fs = require('fs');
        const analysis = {
            toolCount: agentTools.length,
            toolList: toolListForAI,
            template: template,
            analysis: {
                hasAvailableToolsSection: template.includes('Available Tools'),
                hasDevInsCommands: template.includes('/'),
                hasToolTags: template.includes('<tool name='),
                hasParameterTags: template.includes('<parameters>'),
                hasExampleTags: template.includes('<example>'),
                templateLength: template.length,
                toolListLength: toolListForAI.length
            },
            timestamp: new Date().toISOString()
        };
        
        fs.writeFileSync('/tmp/tool-template-analysis.json', JSON.stringify(analysis, null, 2));
        console.log('✅ Analysis exported to /tmp/tool-template-analysis.json');
        
        // Step 8: Identify specific issues
        console.log('\n8. Identifying Specific Issues...');
        identifyIssues(template, toolListForAI);
        
    } catch (error) {
        console.error('❌ Error during analysis:', error);
        console.error(error.stack);
    }
}

function analyzeTemplateStructure(template) {
    const sections = [
        'Environment Information',
        'Available Tools',
        'Task Execution Guidelines',
        'Response Format',
        'Making Code Changes'
    ];
    
    sections.forEach(section => {
        const hasSection = template.includes(section);
        console.log(`  ${hasSection ? '✅' : '❌'} ${section}`);
    });
    
    // Check for tool-related content
    const toolIndicators = [
        'DevIns commands',
        '<tool name=',
        '<parameters>',
        '<example>',
        '/read-file',
        '/write-file',
        '/shell'
    ];
    
    console.log('\nTool-related content:');
    toolIndicators.forEach(indicator => {
        const hasIndicator = template.includes(indicator);
        console.log(`  ${hasIndicator ? '✅' : '❌'} ${indicator}`);
    });
}

function identifyIssues(template, toolList) {
    const issues = [];
    
    if (!template.includes('<tool name=')) {
        issues.push('Missing XML tool structure (<tool name=...)');
    }
    
    if (!template.includes('/read-file')) {
        issues.push('Missing read-file command example');
    }
    
    if (!template.includes('<parameters>')) {
        issues.push('Missing parameter descriptions');
    }
    
    if (!template.includes('<example>')) {
        issues.push('Missing usage examples');
    }
    
    if (toolList.length < 100) {
        issues.push('Tool list seems too short');
    }
    
    if (!toolList.includes('DevIns')) {
        issues.push('Missing DevIns command format explanation');
    }
    
    if (issues.length > 0) {
        console.log('❌ Issues found:');
        issues.forEach(issue => console.log(`  - ${issue}`));
    } else {
        console.log('✅ No obvious issues found');
    }
    
    return issues;
}

// Run the analysis
analyzeToolTemplateGeneration();
