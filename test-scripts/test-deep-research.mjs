#!/usr/bin/env node

/**
 * Domain Dictionary Deep Research Agent Test Script
 * 
 * Tests the DomainDictAgent's 7-step Deep Research methodology:
 * 1. Clarify - Problem Definition
 * 2. Decompose - Research Dimensions
 * 3. Information Map - Planning
 * 4. Iterative Deep Research Loop
 * 5. Second-Order Insights
 * 6. Synthesis - Research Narrative
 * 7. Actionization - Final Deliverables
 * 
 * Usage:
 *   cd /Volumes/source/ai/autocrud
 *   ./gradlew :mpp-core:assembleJsPackage
 *   node docs/test-scripts/test-deep-research.mjs [options]
 * 
 * Options:
 *   --project <path>   Project path (default: current autocrud project)
 *   --query <query>    Research query (default: "Optimize domain dictionary")
 *   --focus <area>     Focus area (e.g., agent, tool, document)
 *   --max <n>          Max iterations (default: 7)
 *   --quick            Quick mode (3 iterations)
 *   --verbose          Verbose output
 */

import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';
import * as os from 'os';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// ============= Configuration =============

const COLORS = {
    reset: '\x1b[0m',
    bright: '\x1b[1m',
    dim: '\x1b[2m',
    red: '\x1b[31m',
    green: '\x1b[32m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    magenta: '\x1b[35m',
    cyan: '\x1b[36m',
    white: '\x1b[37m',
    bgBlue: '\x1b[44m',
    bgGreen: '\x1b[42m',
    bgYellow: '\x1b[43m',
    bgRed: '\x1b[41m'
};

function colorize(text, color) {
    return `${COLORS[color] || ''}${text}${COLORS.reset}`;
}

function printBanner() {
    console.log('\n' + colorize('═'.repeat(70), 'cyan'));
    console.log(colorize('  🔬 Domain Dictionary Deep Research Agent Test', 'bright'));
    console.log(colorize('═'.repeat(70), 'cyan'));
    console.log(colorize('  7-Step Deep Research Methodology:', 'dim'));
    console.log(colorize('  1. Clarify → 2. Decompose → 3. Information Map → 4. Research Loop', 'dim'));
    console.log(colorize('  5. Insights → 6. Synthesis → 7. Actionization', 'dim'));
    console.log(colorize('═'.repeat(70), 'cyan') + '\n');
}

function printStep(step, name, emoji) {
    console.log('\n' + colorize(`${'─'.repeat(60)}`, 'dim'));
    console.log(colorize(`${emoji} Step ${step}/7: ${name}`, 'bright'));
    console.log(colorize(`${'─'.repeat(60)}`, 'dim'));
}

function printProgress(message) {
    // Parse the message to add colors
    if (message.startsWith('##')) {
        // Step headers
        const stepMatch = message.match(/## Step (\d+)\/7: (.+)/);
        if (stepMatch) {
            printStep(stepMatch[1], stepMatch[2], getStepEmoji(parseInt(stepMatch[1])));
            return;
        }
    }
    
    if (message.startsWith('###')) {
        console.log(colorize(message, 'yellow'));
    } else if (message.startsWith('   ✓') || message.startsWith('   ✅')) {
        console.log(colorize(message, 'green'));
    } else if (message.startsWith('   ➕')) {
        console.log(colorize(message, 'cyan'));
    } else if (message.includes('❌')) {
        console.log(colorize(message, 'red'));
    } else if (message.includes('⚠️')) {
        console.log(colorize(message, 'yellow'));
    } else if (message.startsWith('=')) {
        console.log(colorize(message, 'cyan'));
    } else {
        console.log(message);
    }
}

function getStepEmoji(step) {
    const emojis = ['🎯', '🔍', '🗺️', '🔄', '💡', '📖', '🚀'];
    return emojis[step - 1] || '📌';
}

// Simple YAML parser
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
                if ((value.startsWith('"') && value.endsWith('"')) || 
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.slice(1, -1);
                }
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

// Parse command line arguments
function parseArgs() {
    const args = process.argv.slice(2);
    const options = {
        project: path.join(__dirname, '../..'),
        query: 'Optimize domain dictionary based on current codebase',
        focus: null,
        maxIterations: 7,
        verbose: false
    };
    
    for (let i = 0; i < args.length; i++) {
        const arg = args[i];
        if (arg === '--project' && args[i + 1]) {
            options.project = path.resolve(args[++i]);
        } else if (arg === '--query' && args[i + 1]) {
            options.query = args[++i];
        } else if (arg === '--focus' && args[i + 1]) {
            options.focus = args[++i];
        } else if (arg === '--max' && args[i + 1]) {
            options.maxIterations = parseInt(args[++i], 10) || 7;
        } else if (arg === '--quick') {
            options.maxIterations = 3;
        } else if (arg === '--verbose' || arg === '-v') {
            options.verbose = true;
        } else if (arg === '--help' || arg === '-h') {
            printHelp();
            process.exit(0);
        }
    }
    
    return options;
}

function printHelp() {
    console.log(`
Usage: node test-deep-research.mjs [options]

Options:
  --project <path>   Project path to analyze (default: autocrud project)
  --query <query>    Research query/requirement
  --focus <area>     Focus area (e.g., agent, tool, document, auth)
  --max <n>          Maximum iterations (default: 7)
  --quick            Quick mode with 3 iterations
  --verbose, -v      Verbose output
  --help, -h         Show this help

Examples:
  node test-deep-research.mjs --query "Add agent-related terms" --focus agent
  node test-deep-research.mjs --quick --focus tool
  node test-deep-research.mjs --max 5 --query "Improve authentication vocabulary"
`);
}

// Load mpp-core
async function loadMppCore() {
    const mppCorePath = path.join(__dirname, '../../mpp-core/build/packages/js');
    if (!fs.existsSync(mppCorePath)) {
        console.error(colorize('❌ mpp-core not built.', 'red'));
        console.error(colorize('   Run: ./gradlew :mpp-core:assembleJsPackage', 'yellow'));
        process.exit(1);
    }
    
    console.log(colorize(`📦 Loading mpp-core from: ${mppCorePath}`, 'dim'));
    
    try {
        const { createRequire } = await import('module');
        const require = createRequire(import.meta.url);
        return require(path.join(mppCorePath, 'autodev-mpp-core.js'));
    } catch (e) {
        console.error(colorize(`❌ Failed to load mpp-core: ${e.message}`, 'red'));
        if (e.stack) console.error(colorize(e.stack, 'dim'));
        process.exit(1);
    }
}

// Load LLM config
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
    
    console.log(colorize(`📦 Using LLM: ${activeConfig.name} (${activeConfig.provider}/${activeConfig.model})`, 'cyan'));
    return activeConfig;
}

// Main test function
async function runDeepResearchTest(options) {
    printBanner();
    
    console.log(colorize('Configuration:', 'bright'));
    console.log(`  📁 Project: ${colorize(options.project, 'cyan')}`);
    console.log(`  📋 Query: ${colorize(options.query, 'yellow')}`);
    console.log(`  🎯 Focus: ${colorize(options.focus || 'General', 'magenta')}`);
    console.log(`  🔄 Max Iterations: ${colorize(options.maxIterations.toString(), 'green')}`);
    console.log('');
    
    // Verify project exists
    if (!fs.existsSync(options.project)) {
        console.error(colorize(`❌ Project path does not exist: ${options.project}`, 'red'));
        process.exit(1);
    }
    
    // Load mpp-core
    const MppCore = await loadMppCore();
    
    // Load LLM config
    const config = await loadConfig();
    
    // Extract components
    const { JsKoogLLMService, JsModelConfig } = MppCore.cc.unitmesh.llm;
    const { JsDomainDictAgent } = MppCore.cc.unitmesh.agent;
    
    if (!JsDomainDictAgent) {
        console.error(colorize('❌ JsDomainDictAgent not found in mpp-core exports', 'red'));
        console.error(colorize('   Make sure you have rebuilt mpp-core after adding the agent', 'yellow'));
        process.exit(1);
    }
    
    // Create model config
    const modelConfig = new JsModelConfig(
        config.provider,
        config.model,
        config.apiKey || '',
        config.temperature || 0.7,
        config.maxTokens || 8192,
        config.baseUrl || ''
    );
    
    // Create LLM service
    console.log(colorize('\n🔧 Initializing LLM Service...', 'dim'));
    const llmService = JsKoogLLMService.Companion.create(modelConfig);
    
    // Create DomainDictAgent
    console.log(colorize('🔧 Initializing Domain Dictionary Deep Research Agent...', 'dim'));
    const agent = new JsDomainDictAgent(options.project, llmService);
    
    // Check existing dictionary
    console.log(colorize('\n📚 Checking existing dictionary...', 'dim'));
    const existingDict = await agent.getDictionaryContent();
    if (existingDict) {
        const lines = existingDict.split('\n').filter(l => l.trim());
        console.log(colorize(`   Found existing dictionary with ${lines.length} entries`, 'green'));
    } else {
        console.log(colorize('   No existing dictionary found - will create new one', 'yellow'));
    }
    
    // Run Deep Research
    console.log(colorize('\n🚀 Starting Deep Research...', 'bright'));
    console.log(colorize('═'.repeat(60), 'cyan'));
    
    const startTime = Date.now();
    
    try {
        const result = await agent.executeDeepResearch(
            options.query,
            options.focus,
            options.maxIterations,
            printProgress
        );
        
        const duration = ((Date.now() - startTime) / 1000).toFixed(1);
        
        console.log('\n' + colorize('═'.repeat(60), 'cyan'));
        
        if (result.success) {
            console.log(colorize('✅ Deep Research Completed Successfully!', 'green'));
            console.log(colorize('═'.repeat(60), 'cyan'));
            console.log(`  ⏱️  Duration: ${colorize(duration + 's', 'cyan')}`);
            console.log(`  📊 Steps: ${colorize(result.steps + '/7', 'green')}`);
            console.log(`  📝 New Entries: ${colorize(result.newEntries.toString(), 'yellow')}`);
            
            // Show report preview
            console.log('\n' + colorize('📄 Report Preview:', 'bright'));
            console.log(colorize('─'.repeat(60), 'dim'));
            const reportLines = result.report.split('\n').slice(0, 30);
            reportLines.forEach(line => {
                if (line.startsWith('#')) {
                    console.log(colorize(line, 'cyan'));
                } else if (line.startsWith('-')) {
                    console.log(colorize(line, 'dim'));
                } else {
                    console.log(line);
                }
            });
            if (result.report.split('\n').length > 30) {
                console.log(colorize(`\n... (${result.report.split('\n').length - 30} more lines)`, 'dim'));
            }
            console.log(colorize('─'.repeat(60), 'dim'));
            
            // Save full report
            const reportPath = path.join(options.project, 'prompts', 'deep-research-report.md');
            try {
                fs.mkdirSync(path.dirname(reportPath), { recursive: true });
                fs.writeFileSync(reportPath, result.report);
                console.log(colorize(`\n💾 Full report saved to: ${reportPath}`, 'green'));
            } catch (e) {
                console.log(colorize(`\n⚠️  Could not save report: ${e.message}`, 'yellow'));
            }
            
            // Show updated dictionary preview
            console.log('\n' + colorize('📖 Updated Dictionary Preview:', 'bright'));
            const updatedDict = await agent.getDictionaryContent();
            if (updatedDict) {
                const dictLines = updatedDict.split('\n').slice(0, 15);
                console.log(colorize('─'.repeat(60), 'dim'));
                dictLines.forEach(line => console.log(`  ${line}`));
                const totalLines = updatedDict.split('\n').filter(l => l.trim()).length;
                if (totalLines > 15) {
                    console.log(colorize(`  ... (${totalLines - 15} more entries)`, 'dim'));
                }
                console.log(colorize('─'.repeat(60), 'dim'));
                console.log(colorize(`\n📊 Total dictionary entries: ${totalLines}`, 'green'));
            }
            
        } else {
            console.log(colorize('❌ Deep Research Failed', 'red'));
            console.log(colorize('═'.repeat(60), 'cyan'));
            console.log(colorize(`Error: ${result.message}`, 'red'));
        }
        
        // Show agent state summary
        if (options.verbose) {
            console.log('\n' + colorize('📊 Agent State Summary:', 'bright'));
            const state = agent.getStateSummary();
            console.log(JSON.stringify(state, null, 2));
        }
        
        return result.success;
        
    } catch (error) {
        const duration = ((Date.now() - startTime) / 1000).toFixed(1);
        console.log('\n' + colorize('═'.repeat(60), 'cyan'));
        console.log(colorize('❌ Deep Research Failed with Error', 'red'));
        console.log(colorize('═'.repeat(60), 'cyan'));
        console.log(`  ⏱️  Duration: ${duration}s`);
        console.log(colorize(`  Error: ${error.message}`, 'red'));
        if (options.verbose && error.stack) {
            console.log(colorize('\nStack trace:', 'dim'));
            console.log(colorize(error.stack, 'dim'));
        }
        return false;
    }
}

// Interactive mode for iterative testing
async function runInteractiveMode(options) {
    const readline = await import('readline');
    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });
    
    const question = (prompt) => new Promise(resolve => rl.question(prompt, resolve));
    
    console.log(colorize('\n🔄 Interactive Deep Research Mode', 'bright'));
    console.log(colorize('Type "exit" to quit, "help" for commands\n', 'dim'));
    
    let continueLoop = true;
    
    while (continueLoop) {
        const input = await question(colorize('\n📝 Enter research query (or command): ', 'cyan'));
        const trimmed = input.trim().toLowerCase();
        
        if (trimmed === 'exit' || trimmed === 'quit' || trimmed === 'q') {
            continueLoop = false;
        } else if (trimmed === 'help' || trimmed === 'h') {
            console.log(`
Commands:
  exit, quit, q    Exit interactive mode
  help, h          Show this help
  quick            Run quick research (3 iterations)
  focus <area>     Set focus area for next query
  max <n>          Set max iterations
  
Examples:
  Add agent-related terms
  quick Improve tool vocabulary
  focus auth Add authentication terms
`);
        } else if (trimmed.startsWith('focus ')) {
            const parts = trimmed.split(' ');
            options.focus = parts[1];
            options.query = parts.slice(2).join(' ') || options.query;
            console.log(colorize(`Focus set to: ${options.focus}`, 'green'));
            if (parts.length > 2) {
                await runDeepResearchTest(options);
            }
        } else if (trimmed.startsWith('max ')) {
            options.maxIterations = parseInt(trimmed.split(' ')[1], 10) || 7;
            console.log(colorize(`Max iterations set to: ${options.maxIterations}`, 'green'));
        } else if (trimmed.startsWith('quick ')) {
            options.maxIterations = 3;
            options.query = trimmed.substring(6).trim() || options.query;
            await runDeepResearchTest(options);
        } else if (trimmed) {
            options.query = input.trim();
            await runDeepResearchTest(options);
        }
    }
    
    rl.close();
    console.log(colorize('\n👋 Goodbye!', 'cyan'));
}

// Main entry point
async function main() {
    const options = parseArgs();
    
    try {
        const success = await runDeepResearchTest(options);
        
        // Ask if user wants to continue with interactive mode
        if (success) {
            const readline = await import('readline');
            const rl = readline.createInterface({
                input: process.stdin,
                output: process.stdout
            });
            
            const answer = await new Promise(resolve => {
                rl.question(colorize('\n🔄 Continue with interactive mode? (y/n): ', 'cyan'), resolve);
            });
            rl.close();
            
            if (answer.toLowerCase() === 'y' || answer.toLowerCase() === 'yes') {
                await runInteractiveMode(options);
            }
        }
        
        console.log(colorize('\n✅ Test completed!', 'green'));
        process.exit(success ? 0 : 1);
        
    } catch (error) {
        console.error(colorize(`\n❌ Fatal error: ${error.message}`, 'red'));
        if (options.verbose) {
            console.error(colorize(error.stack, 'dim'));
        }
        process.exit(1);
    }
}

main();

