#!/usr/bin/env node
/**
 * Test script: Test ACP protocol with proper method names (session/ prefix)
 */

const { spawn } = require('child_process');
const path = require('path');

const PROJECT_ROOT = path.resolve(__dirname, '../..');
const AGENT_PATH = path.join(PROJECT_ROOT, 'mpp-ui/dist/jsMain/typescript/index.js');

console.log('🧪 Testing JS CLI ACP Agent with session/ prefixed methods');
console.log('============================================================\n');

// Start the ACP agent server
const agent = spawn('node', [AGENT_PATH, 'acp-agent'], {
  cwd: '/tmp',
  stdio: ['pipe', 'pipe', 'pipe']
});

let responseBuffer = '';
let requestId = 0;
let sessionId = null;

agent.stdout.on('data', (data) => {
  const output = data.toString();
  responseBuffer += output;
  
  // Try to parse JSON-RPC responses (newline-delimited)
  const lines = responseBuffer.split('\n');
  for (let i = 0; i < lines.length - 1; i++) {
    const line = lines[i].trim();
    if (line.startsWith('{')) {
      try {
        const response = JSON.parse(line);
        console.log('✓ Received:', JSON.stringify(response, null, 2));
        
        if (response.result && response.result.sessionId) {
          sessionId = response.result.sessionId;
        }
      } catch (e) {
        console.log('[Non-JSON]:', line);
      }
    }
  }
  responseBuffer = lines[lines.length - 1];
});

agent.stderr.on('data', (data) => {
  const output = data.toString();
  if (!output.startsWith('[ACP Agent]')) {
    console.log('[stderr]:', output);
  }
});

agent.on('close', (code) => {
  console.log(`\nAgent exited: ${code}`);
  process.exit(code);
});

function send(method, params = {}) {
  const req = { jsonrpc: '2.0', method, params, id: ++requestId };
  console.log(`\n📤 ${method}:`, JSON.stringify(params));
  agent.stdin.write(JSON.stringify(req) + '\n');
}

setTimeout(() => {
  // Test with different method name variations
  send('initialize', {
    protocolVersion: 1,
    clientCapabilities: {},
    clientInfo: { name: 'test', version: '1.0' }
  });
  
  setTimeout(() => {
    send('session/new', { cwd: '/tmp' });
    
    setTimeout(() => {
      send('session/prompt', {
        sessionId: sessionId || 'test',
        prompt: [{ type: 'text', text: 'Hello' }]
      });
      
      setTimeout(() => {
        console.log('\n✅ Done');
        agent.stdin.end();
      }, 2000);
    }, 1500);
  }, 1500);
}, 500);
