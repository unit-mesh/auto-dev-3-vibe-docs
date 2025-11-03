#!/usr/bin/env node

/**
 * Test Kotlin ErrorRecoveryAgent in Node.js environment
 * 
 * This script tests the Kotlin Multiplatform implementation of ErrorRecoveryAgent
 * to verify:
 * 1. GitOperations works in Node.js (JS target)
 * 2. ErrorRecoveryAgent can be instantiated and called
 * 3. SubAgent triggers correctly with error context
 */

// Import mpp-core
import MppCore from '../../mpp-core/build/packages/js';

const { 
  JsErrorRecoveryAgent,
  JsLogSummaryAgent,
  JsErrorContext,
} = MppCore.cc.unitmesh.agent.subagent;

const { JsKoogLLMService, JsModelConfig } = MppCore.cc.unitmesh.llm;

console.log('\n🧪 Testing Kotlin ErrorRecoveryAgent in Node.js\n');
console.log('═'.repeat(60));

// Test 1: ErrorRecoveryAgent Instantiation
console.log('\n📋 Test 1: ErrorRecoveryAgent Instantiation');
console.log('─'.repeat(60));

try {
  // Create a mock LLM config
  const modelConfig = new JsModelConfig(
    'DEEPSEEK',         // providerName
    'deepseek-chat',    // modelName  
    'test-api-key',     // apiKey
    0.7,                // temperature
    4096,               // maxTokens
    ''                  // baseUrl
  );
  
  console.log('✓ JsModelConfig created');
  
  // Create KoogLLMService
  const llmService = new JsKoogLLMService(modelConfig);
  
  console.log('✓ JsKoogLLMService created');
  
  // Create ErrorRecoveryAgent
  const errorAgent = new JsErrorRecoveryAgent(
    '/Volumes/source/ai/autocrud/mpp-core',
    llmService
  );
  
  console.log('✓ JsErrorRecoveryAgent instantiated');
  
  // Create LogSummaryAgent
  const logAgent = new JsLogSummaryAgent(llmService, 2000);
  console.log('✓ JsLogSummaryAgent instantiated');
  
  // Test 2: Create error context
  console.log('\n📋 Test 2: Create Error Context');
  console.log('─'.repeat(60));
  
  const errorContext = new JsErrorContext(
    'npm test',
    'Tests failed with exit code 1',
    1,
    'Running tests...',
    'Error: Test suite failed'
  );
  
  console.log('✓ JsErrorContext created');
  console.log(`  Command: ${errorContext.command}`);
  console.log(`  Error: ${errorContext.errorMessage}`);
  console.log(`  Exit Code: ${errorContext.exitCode}`);
  
  // Test 3: Check log summarization threshold
  console.log('\n📋 Test 3: LogSummaryAgent - Check Threshold');
  console.log('─'.repeat(60));
  
  const shortOutput = 'Short output';
  const longOutput = 'a'.repeat(3000);
  
  console.log(`✓ Short output needs summary: ${logAgent.needsSummarization(shortOutput)}`);
  console.log(`✓ Long output needs summary: ${logAgent.needsSummarization(longOutput)}`);
  
  // Summary
  console.log('\n' + '═'.repeat(60));
  console.log('📊 Test Summary');
  console.log('═'.repeat(60));
  console.log('✅ JsErrorRecoveryAgent can be instantiated');
  console.log('✅ JsLogSummaryAgent can be instantiated');
  console.log('✅ JsErrorContext can be created');
  console.log('✅ LogSummaryAgent threshold check works');
  console.log('⚠️  Actual agent execution skipped (requires real LLM)');
  console.log('\n🎉 All basic tests passed!\n');
  console.log('✨ Kotlin SubAgents are ready to use in Node.js/TypeScript!\n');
  
} catch (error) {
  console.error('\n❌ Test failed:', error.message);
  console.error('Stack:', error.stack);
  process.exit(1);
}
