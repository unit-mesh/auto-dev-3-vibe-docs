#!/usr/bin/env node
/**
 * Test script: Test ACP protocol with our JS CLI agent
 * This sends proper ACP JSON-RPC messages to test the agent
 */

const { spawn } = require('child_process');
const path = require('path');

const PROJECT_ROOT = path.resolve(__dirname, '../..');
const AGENT_PATH = path.join(PROJECT_ROOT, 'mpp-ui/dist/jsMain/typescript/index.js');

console.log('🧪 Testing JS CLI ACP Agent with proper ACP protocol');
console.log('=====================================================\n');

// Start the ACP agent server
const agent = spawn('node', [AGENT_PATH, 'acp-agent'], {
  cwd: '/tmp',
  stdio: ['pipe', 'pipe', 'pipe']
});

let responseBuffer = '';
let requestId = 0;
let receivedInit = false;
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
        console.log('✓ Received JSON-RPC response:', JSON.stringify(response, null, 2));
        
        // Handle different responses
        if (response.result && response.result.agentInfo) {
          receivedInit = true;
          console.log('  → Agent initialized successfully!');
        } else if (response.result && response.result.sessionId) {
          sessionId = response.result.sessionId;
          console.log(`  → Session created: ${sessionId}`);
        }
      } catch (e) {
        console.log('[Raw line]:', line);
      }
    }
  }
  responseBuffer = lines[lines.length - 1];
});

agent.stderr.on('data', (data) => {
  const output = data.toString();
  if (!output.startsWith('[ACP Agent]')) {
    console.log('[Agent stderr]:', output);
  }
});

agent.on('close', (code) => {
  console.log(`\nAgent process exited with code ${code}`);
  process.exit(code);
});

// Helper to send JSON-RPC request
function sendRequest(method, params = {}) {
  const request = {
    jsonrpc: '2.0',
    method,
    params,
    id: ++requestId
  };
  console.log(`\n📤 Sending ${method} request`);
  agent.stdin.write(JSON.stringify(request) + '\n');
}

// Wait a bit for agent to start
setTimeout(() => {
  console.log('\n🚀 Starting ACP protocol handshake...\n');
  
  // Step 1: Initialize (protocolVersion is a number: 0.0.1 format)
  sendRequest('initialize', {
    protocolVersion: 0.7,  // Version 0.7 as a number
    clientCapabilities: {
      tools: ['read_file', 'list_directory']
    },
    clientInfo: {
      name: 'test-client',
      version: '1.0.0'
    }
  });
  
  // Step 2: Create session (after init)
  setTimeout(() => {
    if (!receivedInit) {
      console.log('⚠️  Init failed, but continuing...');
    }
    
    sendRequest('newSession', {
      cwd: '/tmp'
    });
    
    // Step 3: Send a prompt
    setTimeout(() => {
      if (!sessionId) {
        console.log('⚠️  No session ID, cannot send prompt');
      } else {
        sendRequest('prompt', {
          sessionId: sessionId,
          prompt: [{
            type: 'text',
            text: 'Hello! List the files in the current directory.'
          }]
        });
      }
      
      // Let it run for a bit, then close
      setTimeout(() => {
        console.log('\n\n✅ Test completed. Closing connection...');
        agent.stdin.end();
      }, 5000);
      
    }, 2000);
  }, 2000);
  
}, 500);

// Handle script termination
process.on('SIGINT', () => {
  console.log('\n\nTest interrupted. Cleaning up...');
  agent.kill();
  process.exit(0);
});
