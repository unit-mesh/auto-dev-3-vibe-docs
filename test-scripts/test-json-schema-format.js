#!/usr/bin/env node

/**
 * Test the new JSON Schema format for tool descriptions
 * This should now generate proper JSON Schema instead of XML
 */

const mppCore = require('../../mpp-core/build/packages/js/autodev-mpp-core');
const JsToolRegistry = mppCore.cc.unitmesh.llm.JsToolRegistry;
const JsCodingAgentContextBuilder = mppCore.cc.unitmesh.agent.JsCodingAgentContextBuilder;
const JsCodingAgentPromptRenderer = mppCore.cc.unitmesh.agent.JsCodingAgentPromptRenderer;

function testJsonSchemaFormat() {
    console.log('🔧 Testing JSON Schema Format for Tool Descriptions');
    console.log('='.repeat(60));
    
    try {
        // Step 1: Create tool registry
        console.log('\n1. Creating Tool Registry...');
        const toolRegistry = new JsToolRegistry('/test/project');
        
        // Step 2: Generate tool list with new format
        console.log('\n2. Generating Tool List with JSON Schema Format...');
        const toolList = toolRegistry.formatToolListForAI();
        
        console.log(`✅ Tool list generated successfully!`);
        console.log(`Tool list length: ${toolList.length} characters`);
        
        // Step 3: Analyze the new format
        console.log('\n3. Analyzing New Format...');
        
        const formatChecks = {
            'Uses Markdown headers (##)': toolList.includes('## '),
            'Has JSON Schema blocks': toolList.includes('```json'),
            'Contains $schema field': toolList.includes('"$schema"'),
            'Has draft-07 schema': toolList.includes('draft-07/schema#'),
            'Contains type object': toolList.includes('"type": "object"'),
            'Has properties field': toolList.includes('"properties"'),
            'Has required field': toolList.includes('"required"'),
            'Has additionalProperties': toolList.includes('"additionalProperties"'),
            'No XML tags': !toolList.includes('<tool name='),
            'No XML parameters': !toolList.includes('<parameters>'),
            'Has example blocks': toolList.includes('**Example:**')
        };
        
        Object.entries(formatChecks).forEach(([check, passed]) => {
            console.log(`  ${passed ? '✅' : '❌'} ${check}`);
        });
        
        // Step 4: Show sample of new format
        console.log('\n4. Sample of New Format:');
        console.log('-'.repeat(50));
        console.log(toolList.substring(0, 1200));
        console.log('-'.repeat(50));
        
        // Step 5: Create context and generate template
        console.log('\n5. Generating Complete Template...');
        const builder = new JsCodingAgentContextBuilder();
        const context = builder
            .setProjectPath('/test/project')
            .setOsInfo('macOS 14.0')
            .setTimestamp(new Date().toISOString())
            .setToolList(toolList)
            .setBuildTool('gradle')
            .setShell('/bin/zsh')
            .build();
        
        const renderer = new JsCodingAgentPromptRenderer();
        const template = renderer.render(context, 'EN');
        
        console.log(`✅ Template generated successfully!`);
        console.log(`Template length: ${template.length} characters`);
        
        // Step 6: Analyze template quality
        console.log('\n6. Template Quality Analysis...');
        
        const templateChecks = {
            'Available Tools section': template.includes('Available Tools'),
            'JSON Schema format': template.includes('```json'),
            'Standard schema format': template.includes('draft-07/schema#'),
            'Proper tool structure': template.includes('## read-file'),
            'Parameter descriptions': template.includes('"description"'),
            'Type information': template.includes('"type"'),
            'Required fields': template.includes('"required"'),
            'Example usage': template.includes('**Example:**')
        };
        
        Object.entries(templateChecks).forEach(([check, passed]) => {
            console.log(`  ${passed ? '✅' : '❌'} ${check}`);
        });
        
        // Step 7: Export results
        console.log('\n7. Exporting Results...');
        const fs = require('fs');
        const results = {
            success: true,
            format: 'JSON Schema',
            toolListLength: toolList.length,
            templateLength: template.length,
            formatChecks,
            templateChecks,
            toolListSample: toolList.substring(0, 2000),
            templateSample: template.substring(0, 2000),
            timestamp: new Date().toISOString()
        };
        
        fs.writeFileSync('/tmp/json-schema-format-test.json', JSON.stringify(results, null, 2));
        console.log('✅ Results exported to /tmp/json-schema-format-test.json');
        
        // Step 8: Summary
        console.log('\n8. Summary:');
        const allFormatChecksPass = Object.values(formatChecks).every(Boolean);
        const allTemplateChecksPass = Object.values(templateChecks).every(Boolean);
        
        if (allFormatChecksPass && allTemplateChecksPass) {
            console.log('🎉 SUCCESS: JSON Schema format is working perfectly!');
            console.log('   - Tool descriptions now use standard JSON Schema format');
            console.log('   - Markdown formatting for better readability');
            console.log('   - Proper $schema field with draft-07 specification');
            console.log('   - Complete parameter type and requirement information');
        } else {
            console.log('⚠️  PARTIAL: Some format checks failed. See exported results for details.');
        }
        
    } catch (error) {
        console.error('❌ Error during testing:', error);
        console.error(error.stack);
    }
}

// Run the test
testJsonSchemaFormat();
