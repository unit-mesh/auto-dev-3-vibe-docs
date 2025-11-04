#!/usr/bin/env node

/**
 * Test the new compressed JSON Schema format and DevIns-style examples
 */

const mppCore = require('../../build/js/packages/autodev-mpp-core/kotlin/autodev-mpp-core.js');
const JsToolRegistry = mppCore.cc.unitmesh.llm.JsToolRegistry;
const JsCodingAgentContextBuilder = mppCore.cc.unitmesh.agent.JsCodingAgentContextBuilder;
const JsCodingAgentPromptRenderer = mppCore.cc.unitmesh.agent.JsCodingAgentPromptRenderer;

function testNewFormat() {
    console.log('🔧 Testing New Compressed JSON Schema Format and DevIns Examples');
    console.log('='.repeat(70));
    
    try {
        // Step 1: Create tool registry and generate tool list
        console.log('\n1. Generating Tool List with New Format...');
        const toolRegistry = new JsToolRegistry('/test/project');
        const toolList = toolRegistry.formatToolListForAI();
        
        console.log(`✅ Tool list generated: ${toolList.length} characters`);
        
        // Step 2: Analyze the new format
        console.log('\n2. Analyzing New Format...');
        
        const formatChecks = {
            'Compressed JSON Schema': !toolList.includes('{\n  ') && toolList.includes('{"$schema"'),
            'Single-line JSON': toolList.includes('{"$schema":"http://json-schema.org/draft-07/schema#"'),
            'DevIns-style examples': toolList.includes('/read-file\n```json'),
            'JSON code blocks': toolList.includes('```json\n{') && toolList.includes('}\n```'),
            'No multi-line JSON': !toolList.includes('  "type": "object"'),
            'Markdown headers': toolList.includes('## read-file'),
            'Parameter schemas': toolList.includes('"properties"') && toolList.includes('"required"')
        };
        
        Object.entries(formatChecks).forEach(([check, passed]) => {
            console.log(`  ${passed ? '✅' : '❌'} ${check}`);
        });
        
        // Step 3: Show sample of new format
        console.log('\n3. Sample of New Format:');
        console.log('-'.repeat(60));
        
        // Find read-file tool section
        const readFileStart = toolList.indexOf('## read-file');
        const readFileEnd = toolList.indexOf('## ', readFileStart + 1);
        const readFileSection = readFileEnd > 0 
            ? toolList.substring(readFileStart, readFileEnd)
            : toolList.substring(readFileStart, readFileStart + 800);
        
        console.log(readFileSection);
        console.log('-'.repeat(60));
        
        // Step 4: Create complete template
        console.log('\n4. Creating Complete Template...');
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
        
        console.log(`✅ Template generated: ${template.length} characters`);
        
        // Step 5: Analyze template improvements
        console.log('\n5. Template Improvements Analysis...');
        
        const improvements = {
            'Compact JSON Schema': template.includes('{"$schema":"http://json-schema.org/draft-07/schema#"'),
            'DevIns examples': template.includes('/read-file\n```json'),
            'Reduced line count': !template.includes('  "type": "object",\n'),
            'JSON code blocks': template.includes('```json\n{') && template.includes('}\n```'),
            'Tool commands': template.includes('/write-file') && template.includes('/shell')
        };
        
        Object.entries(improvements).forEach(([improvement, achieved]) => {
            console.log(`  ${achieved ? '✅' : '❌'} ${improvement}`);
        });
        
        // Step 6: Compare sizes
        console.log('\n6. Size Comparison...');
        
        // Estimate old format size (with multi-line JSON)
        const jsonSchemaCount = (toolList.match(/\{".*?"\}/g) || []).length;
        const estimatedOldSize = toolList.length + (jsonSchemaCount * 200); // Rough estimate
        
        console.log(`  Current format: ${toolList.length} characters`);
        console.log(`  Estimated old format: ${estimatedOldSize} characters`);
        console.log(`  Size reduction: ${Math.round((1 - toolList.length / estimatedOldSize) * 100)}%`);
        
        // Step 7: Export results
        console.log('\n7. Exporting Results...');
        const fs = require('fs');
        
        const results = {
            testName: 'New Format Test',
            timestamp: new Date().toISOString(),
            
            metrics: {
                toolListLength: toolList.length,
                templateLength: template.length,
                estimatedSizeReduction: Math.round((1 - toolList.length / estimatedOldSize) * 100)
            },
            
            formatChecks,
            improvements,
            
            samples: {
                readFileSection: readFileSection,
                templatePreview: template.substring(0, 1000)
            }
        };
        
        fs.writeFileSync('/tmp/new-format-test-results.json', JSON.stringify(results, null, 2));
        console.log('✅ Results exported to /tmp/new-format-test-results.json');
        
        // Step 8: Summary
        console.log('\n8. Summary:');
        const allFormatChecksPass = Object.values(formatChecks).every(Boolean);
        const allImprovementsAchieved = Object.values(improvements).every(Boolean);
        
        if (allFormatChecksPass && allImprovementsAchieved) {
            console.log('🎉 SUCCESS: New format is working perfectly!');
            console.log('');
            console.log('✨ Key Improvements:');
            console.log('   📦 Compressed JSON Schema (single-line format)');
            console.log('   🔧 DevIns-style examples (/command + JSON blocks)');
            console.log('   📉 Reduced template size and improved readability');
            console.log('   🎯 Better AI Agent compatibility');
        } else {
            console.log('⚠️  PARTIAL: Some improvements not fully achieved');
            const formatPassed = Object.values(formatChecks).filter(Boolean).length;
            const improvementsPassed = Object.values(improvements).filter(Boolean).length;
            console.log(`   Format checks: ${formatPassed}/${Object.keys(formatChecks).length}`);
            console.log(`   Improvements: ${improvementsPassed}/${Object.keys(improvements).length}`);
        }
        
    } catch (error) {
        console.error('❌ Error during testing:', error);
        console.error(error.stack);
    }
}

// Run the test
testNewFormat();
