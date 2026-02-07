#!/usr/bin/env node

/**
 * Test script to check if we need to respond to session/request_permission
 */

const { spawn } = require('child_process');
const readline = require('readline');

const auggie = spawn('auggie', ['--acp'], {
  cwd: '/Users/phodal/IdeaProjects/ddd-lite-example',
  stdio: ['pipe', 'pipe', 'pipe']
});

let requestId = 1;
let permissionRequestId = null;

auggie.stderr.on('data', (data) => {
  console.error('STDERR:', data.toString());
});

const rl = readline.createInterface({
  input: auggie.stdout,
  crlfDelay: Infinity
});

rl.on('line', (line) => {
  try {
    const msg = JSON.parse(line);
    console.log('<<<', JSON.stringify(msg));
    
    // Check if this is a permission request
    if (msg.method === 'session/request_permission') {
      permissionRequestId = msg.id;
      console.log('\n!!! Received permission request, need to respond!\n');
      
      // Send response after 1 second
      setTimeout(() => {
        const response = {
          jsonrpc: '2.0',
          id: permissionRequestId,
          result: {
            outcome: {
              selected: 'enable' // Choose "enable" option
            }
          }
        };
        console.log('>>> Sending permission response:', JSON.stringify(response));
        auggie.stdin.write(JSON.stringify(response) + '\n');
      }, 1000);
    }
  } catch (e) {
    console.log('Non-JSON:', line);
  }
});

auggie.on('exit', (code) => {
  console.log(`Process exited: ${code}`);
  process.exit(code || 0);
});

setTimeout(() => {
  console.log('>>> Sending initialize');
  const initReq = {
    jsonrpc: '2.0',
    id: requestId++,
    method: 'initialize',
    params: {
      protocolVersion: 1,
      capabilities: {
        fs: { readTextFile: true, writeTextFile: true },
        terminal: true
      },
      implementation: {
        name: 'test',
        version: '1.0',
        title: 'Test Client'
      }
    }
  };
  auggie.stdin.write(JSON.stringify(initReq) + '\n');
  
  setTimeout(() => {
    console.log('>>> Sending session/new');
    const sessionReq = {
      jsonrpc: '2.0',
      id: requestId++,
      method: 'session/new',
      params: {
        cwd: '/Users/phodal/IdeaProjects/ddd-lite-example',
        mcpServers: []
      }
    };
    auggie.stdin.write(JSON.stringify(sessionReq) + '\n');
    
    // Exit after 10 seconds
    setTimeout(() => {
      console.log('\nTest complete');
      auggie.kill();
    }, 10000);
  }, 1000);
}, 1000);
