#!/usr/bin/env node

/**
 * Test the fixed tool template generation
 * This should now include proper parameter descriptions and schema information
 */

const mppCore = require('../../mpp-core/build/packages/js/autodev-mpp-core');
const JsToolRegistry = mppCore.cc.unitmesh.llm.JsToolRegistry;
const JsCodingAgentContext = mppCore.cc.unitmesh.agent.JsCodingAgentContext;
const JsAgentTask = mppCore.cc.unitmesh.agent.JsAgentTask;
const JsCodingAgentPromptRenderer = mppCore.cc.unitmesh.agent.JsCodingAgentPromptRenderer;

function testFixedToolTemplate() {
    console.log('🔧 Testing Fixed Tool Template Generation');
    console.log('='.repeat(60));
    
    try {
        // Step 1: Create tool registry and task
        console.log('\n1. Creating Tool Registry and Task...');
        const toolRegistry = new JsToolRegistry('/test/project');
        const task = new JsAgentTask('Test task', '/test/project');
        
        console.log('✅ Tool registry and task created');
        
        // Step 2: Use the new fromTask method
        console.log('\n2. Creating Context using fromTask method...');

        // Check if fromTask is available
        console.log('Available methods:', Object.keys(JsCodingAgentContext));
        let context;
        if (JsCodingAgentContext.Companion && JsCodingAgentContext.Companion.fromTask) {
            console.log('Using Companion.fromTask');
            context = JsCodingAgentContext.Companion.fromTask(task, toolRegistry);
        } else {
            console.log('fromTask not available, using manual approach');
            // Fallback: create context manually with formatted tool list
            const toolList = toolRegistry.formatToolListForAI();
            context = new JsCodingAgentContext(
                null, // currentFile
                '/test/project', // projectPath
                '', // projectStructure
                'Test OS', // osInfo
                new Date().toISOString(), // timestamp
                toolList, // toolList
                '', // agentRules
                '', // buildTool
                '/bin/bash', // shell
                '', // moduleInfo
                '' // frameworkContext
            );
        }
        
        console.log('✅ Context created using fromTask');
        console.log(`Tool list length: ${context.toolList.length} characters`);
        
        // Step 3: Check tool list content
        console.log('\n3. Analyzing Tool List Content...');
        
        const toolList = context.toolList;
        
        // Check for key indicators
        const hasToolTags = toolList.includes('<tool name=');
        const hasParameterTags = toolList.includes('<parameters>');
        const hasSchemaInfo = toolList.includes('<schema>');
        const hasParamTags = toolList.includes('<param name=');
        const hasExampleTags = toolList.includes('<example>');
        const hasDevInsCommands = toolList.includes('/read-file');
        
        console.log(`Has <tool> tags: ${hasToolTags ? '✅' : '❌'}`);
        console.log(`Has <parameters> tags: ${hasParameterTags ? '✅' : '❌'}`);
        console.log(`Has <schema> info: ${hasSchemaInfo ? '✅' : '❌'}`);
        console.log(`Has <param> tags: ${hasParamTags ? '✅' : '❌'}`);
        console.log(`Has <example> tags: ${hasExampleTags ? '✅' : '❌'}`);
        console.log(`Has DevIns commands: ${hasDevInsCommands ? '✅' : '❌'}`);
        
        // Step 4: Show sample of tool list
        console.log('\n4. Sample Tool List Content:');
        console.log('-'.repeat(50));
        console.log(toolList.substring(0, 800));
        console.log('-'.repeat(50));
        
        // Step 5: Generate complete template
        console.log('\n5. Generating Complete Template...');
        const renderer = new JsCodingAgentPromptRenderer();
        const template = renderer.render(context, 'EN');
        
        console.log(`✅ Template generated successfully!`);
        console.log(`Template length: ${template.length} characters`);
        
        // Step 6: Analyze template quality
        console.log('\n6. Template Quality Analysis...');
        
        const templateChecks = {
            'Available Tools section': template.includes('Available Tools'),
            'Parameter descriptions': template.includes('<param name='),
            'Type information': template.includes('<type>'),
            'Usage examples': template.includes('/read-file path='),
            'DevIns format': template.includes('<devin>'),
            'Response format': template.includes('Response Format')
        };
        
        Object.entries(templateChecks).forEach(([check, passed]) => {
            console.log(`  ${passed ? '✅' : '❌'} ${check}`);
        });
        
        // Step 7: Export for inspection
        console.log('\n7. Exporting Results...');
        const fs = require('fs');
        const results = {
            success: true,
            toolListLength: toolList.length,
            templateLength: template.length,
            checks: {
                hasToolTags,
                hasParameterTags,
                hasSchemaInfo,
                hasParamTags,
                hasExampleTags,
                hasDevInsCommands
            },
            templateChecks,
            toolListSample: toolList.substring(0, 1000),
            templateSample: template.substring(0, 1000),
            timestamp: new Date().toISOString()
        };
        
        fs.writeFileSync('/tmp/fixed-tool-template-test.json', JSON.stringify(results, null, 2));
        console.log('✅ Results exported to /tmp/fixed-tool-template-test.json');
        
        // Step 8: Summary
        console.log('\n8. Summary:');
        const allChecksPass = Object.values(templateChecks).every(Boolean);
        const keyFeaturesWork = hasParameterTags && hasSchemaInfo && hasParamTags;
        
        if (allChecksPass && keyFeaturesWork) {
            console.log('🎉 SUCCESS: All checks passed! Tool template generation is fixed.');
        } else {
            console.log('⚠️  PARTIAL: Some issues remain. Check the exported results for details.');
        }
        
    } catch (error) {
        console.error('❌ Error during testing:', error);
        console.error(error.stack);
    }
}

// Run the test
testFixedToolTemplate();
