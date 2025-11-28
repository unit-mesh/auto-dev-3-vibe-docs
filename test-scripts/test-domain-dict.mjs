#!/usr/bin/env node

/**
 * Domain Dictionary Generator Test Script
 * 
 * Tests the domain dictionary generation functionality with the configured LLM.
 * 
 * Usage:
 *   cd mpp-ui && npm run build:kotlin && npm run build:kotlin-deps
 *   node ../docs/test-scripts/test-domain-dict.mjs [project-path]
 * 
 * If no project path is provided, uses the current AutoDev project.
 */

import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';
import * as os from 'os';

// Simple YAML parser for basic config format
function parseSimpleYaml(content) {
    const lines = content.split('\n');
    const result = { configs: [] };
    let currentConfig = null;
    
    for (const line of lines) {
        const trimmed = line.trim();
        if (trimmed.startsWith('#') || !trimmed) continue;
        
        if (trimmed.startsWith('active:')) {
            result.active = trimmed.split(':')[1].trim();
        } else if (trimmed.startsWith('- name:')) {
            if (currentConfig) result.configs.push(currentConfig);
            currentConfig = { name: trimmed.split(':')[1].trim() };
        } else if (currentConfig) {
            const match = trimmed.match(/^(\w+):\s*(.+)$/);
            if (match) {
                let value = match[2].trim();
                // Remove quotes
                if ((value.startsWith('"') && value.endsWith('"')) || 
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.slice(1, -1);
                }
                // Parse numbers
                if (!isNaN(value) && value !== '') {
                    value = parseFloat(value);
                }
                currentConfig[match[1]] = value;
            }
        }
    }
    if (currentConfig) result.configs.push(currentConfig);
    return result;
}

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load mpp-core
const mppCorePath = path.join(__dirname, '../../mpp-core/build/packages/js');
if (!fs.existsSync(mppCorePath)) {
    console.error('❌ mpp-core not built. Run: cd mpp-ui && npm run build:kotlin && npm run build:kotlin-deps');
    process.exit(1);
}

console.log('📦 Loading mpp-core from:', mppCorePath);

// Dynamic import of mpp-core
let MppCore;
try {
    // Try the CJS file with createRequire since it's not ESM
    const { createRequire } = await import('module');
    const require = createRequire(import.meta.url);
    MppCore = require(path.join(mppCorePath, 'autodev-mpp-core.js'));
} catch (e) {
    console.error('❌ Failed to load mpp-core:', e.message);
    console.error('Stack:', e.stack);
    process.exit(1);
}

const { JsDomainDictGenerator, JsModelConfig } = MppCore.cc.unitmesh.llm;

async function loadConfig() {
    const configPath = path.join(os.homedir(), '.autodev', 'config.yaml');
    if (!fs.existsSync(configPath)) {
        throw new Error(`Config not found at ${configPath}. Please configure your LLM first.`);
    }
    
    const content = fs.readFileSync(configPath, 'utf-8');
    const config = parseSimpleYaml(content);
    
    const activeName = config.active || 'default';
    const activeConfig = config.configs?.find(c => c.name === activeName) || config.configs?.[0];
    
    if (!activeConfig) {
        throw new Error('No valid LLM configuration found');
    }
    
    console.log(`📦 Using config: ${activeConfig.name} (${activeConfig.provider}/${activeConfig.model})`);
    return activeConfig;
}

async function testDomainDictGeneration(projectPath) {
    console.log('\n🚀 Domain Dictionary Generator Test');
    console.log('='.repeat(50));
    console.log(`📁 Project: ${projectPath}`);
    
    try {
        // Load LLM config
        const config = await loadConfig();
        
        // Create model config
        const modelConfig = new JsModelConfig(
            config.provider,
            config.model,
            config.apiKey || '',
            config.temperature || 0.7,
            config.maxTokens || 8192,
            config.baseUrl || ''
        );
        
        // Create generator
        console.log('\n📝 Creating Domain Dictionary Generator...');
        const generator = new JsDomainDictGenerator(
            projectPath,
            modelConfig,
            8192  // max token length for context
        );
        
        // Check if dictionary already exists
        console.log('\n🔍 Checking existing dictionary...');
        const existingContent = await generator.loadContent();
        if (existingContent) {
            console.log('📄 Existing dictionary found:');
            console.log('-'.repeat(40));
            const lines = existingContent.split('\n').slice(0, 10);
            lines.forEach(line => console.log(`  ${line}`));
            if (existingContent.split('\n').length > 10) {
                console.log(`  ... (${existingContent.split('\n').length - 10} more lines)`);
            }
            console.log('-'.repeat(40));
        } else {
            console.log('📭 No existing dictionary found');
        }
        
        // Generate new dictionary
        console.log('\n⏳ Generating new domain dictionary (this may take a while)...');
        const startTime = Date.now();
        
        const result = await generator.generateAndSave();
        
        const duration = ((Date.now() - startTime) / 1000).toFixed(1);
        console.log(`⏱️  Generation took ${duration}s`);
        
        if (result.success) {
            console.log('\n✅ Generation successful!');
            console.log('-'.repeat(40));
            console.log('Generated content:');
            console.log('-'.repeat(40));
            
            const lines = result.content.split('\n');
            // Show header and first 20 entries
            lines.slice(0, 21).forEach(line => console.log(`  ${line}`));
            if (lines.length > 21) {
                console.log(`  ... (${lines.length - 21} more entries)`);
            }
            console.log('-'.repeat(40));
            console.log(`📊 Total entries: ${lines.length - 1}`);  // -1 for header
            
            // Verify CSV format
            const validEntries = lines.slice(1).filter(line => {
                const parts = line.split(',');
                return parts.length >= 2;
            });
            console.log(`✓ Valid CSV entries: ${validEntries.length}`);
            
            // Check for pipe-separated code translations
            const entriesWithMultipleCodes = validEntries.filter(line => line.includes(' | '));
            console.log(`✓ Entries with multiple code translations: ${entriesWithMultipleCodes.length}`);
            
            // Show some examples with multiple code translations
            if (entriesWithMultipleCodes.length > 0) {
                console.log('\n📌 Examples with multiple code translations:');
                entriesWithMultipleCodes.slice(0, 5).forEach(line => {
                    console.log(`  ${line}`);
                });
            }
            
            // Save location
            const dictPath = path.join(projectPath, 'prompts', 'domain.csv');
            console.log(`\n💾 Saved to: ${dictPath}`);
            
        } else {
            console.log('\n❌ Generation failed!');
            console.log(`Error: ${result.errorMessage}`);
        }
        
    } catch (error) {
        console.error('\n❌ Error:', error.message);
        if (error.stack) {
            console.error('Stack:', error.stack);
        }
        process.exit(1);
    }
}

// Main
const projectPath = process.argv[2] || path.join(__dirname, '../..');
console.log('Domain Dictionary Generator Test');
console.log('================================');

testDomainDictGeneration(projectPath)
    .then(() => {
        console.log('\n✅ Test completed successfully!');
    })
    .catch(error => {
        console.error('\n❌ Test failed:', error);
        process.exit(1);
    });

