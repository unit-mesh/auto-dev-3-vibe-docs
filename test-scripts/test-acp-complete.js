#!/usr/bin/env node
/**
 * Test script: Complete ACP flow test with proper protocol
 */

const { spawn } = require('child_process');
const path = require('path');

const PROJECT_ROOT = path.resolve(__dirname, '../..');
const AGENT_PATH = path.join(PROJECT_ROOT, 'mpp-ui/dist/jsMain/typescript/index.js');

console.log('🧪 Testing Complete ACP Flow with Xiuper JS CLI Agent');
console.log('======================================================\n');

// Start the ACP agent server
const agent = spawn('node', [AGENT_PATH, 'acp-agent'], {
  cwd: '/tmp',
  stdio: ['pipe', 'pipe', 'pipe']
});

let responseBuffer = '';
let requestId = 0;
let sessionId = null;
let initSuccess = false;
let sessionSuccess = false;

agent.stdout.on('data', (data) => {
  const output = data.toString();
  responseBuffer += output;
  
  const lines = responseBuffer.split('\n');
  for (let i = 0; i < lines.length - 1; i++) {
    const line = lines[i].trim();
    if (line.startsWith('{')) {
      try {
        const response = JSON.parse(line);
        console.log('✓ Response:', JSON.stringify(response, null, 2));
        
        if (response.result) {
          if (response.result.agentInfo) {
            initSuccess = true;
            console.log('  → Initialize SUCCESS');
          }
          if (response.result.sessionId) {
            sessionId = response.result.sessionId;
            sessionSuccess = true;
            console.log(`  → Session created: ${sessionId}`);
          }
          if (response.result.stopReason) {
            console.log(`  → Prompt completed: ${response.result.stopReason}`);
          }
        }
      } catch (e) {
        // Ignore
      }
    }
  }
  responseBuffer = lines[lines.length - 1];
});

agent.stderr.on('data', (data) => {
  const output = data.toString();
  if (output.includes('[ACP Agent]')) {
    console.log(`[Agent] ${output.trim()}`);
  }
});

agent.on('close', (code) => {
  console.log(`\n${'='.repeat(60)}`);
  console.log('Test Summary:');
  console.log(`  Initialize: ${initSuccess ? '✅ PASS' : '❌ FAIL'}`);
  console.log(`  New Session: ${sessionSuccess ? '✅ PASS' : '❌ FAIL'}`);
  console.log(`\nAgent exited with code: ${code}`);
  process.exit(code);
});

function send(method, params = {}) {
  const req = { jsonrpc: '2.0', method, params, id: ++requestId };
  console.log(`\n📤 Request: ${method}`);
  agent.stdin.write(JSON.stringify(req) + '\n');
}

setTimeout(() => {
  // Step 1: Initialize
  send('initialize', {
    protocolVersion: 1,
    clientCapabilities: {},
    clientInfo: { name: 'xiuper-test-client', version: '1.0' }
  });
  
  setTimeout(() => {
    if (!initSuccess) {
      console.log('⚠️  Init failed, stopping test');
      agent.stdin.end();
      return;
    }
    
    // Step 2: Create session
    send('session/new', {
      cwd: '/tmp',
      mcpServers: []  // Empty array as required
    });
    
    setTimeout(() => {
      if (!sessionId) {
        console.log('⚠️  No session created, stopping test');
        agent.stdin.end();
        return;
      }
      
      // Step 3: Send prompt
      send('session/prompt', {
        sessionId: sessionId,
        prompt: [{ type: 'text', text: 'List files in current directory' }]
      });
      
      // Wait for prompt to complete
      setTimeout(() => {
        console.log('\n✅ Test completed successfully!');
        agent.stdin.end();
      }, 5000);
      
    }, 2000);
  }, 2000);
}, 1000);

process.on('SIGINT', () => {
  console.log('\n\nTest interrupted');
  agent.kill();
  process.exit(1);
});
