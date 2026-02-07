#!/usr/bin/env node

/**
 * Test script to manually test ACP protocol with auggie CLI
 * 
 * This script simulates what the ACP client does:
 * 1. Start auggie with --acp
 * 2. Send initialize request
 * 3. Send session/new request
 * 4. Print responses
 */

const { spawn } = require('child_process');
const readline = require('readline');

// Start auggie in ACP mode
const auggie = spawn('auggie', ['--acp'], {
  cwd: '/Users/phodal/IdeaProjects/ddd-lite-example',
  stdio: ['pipe', 'pipe', 'pipe']
});

let requestId = 1;

// Read stderr for debugging
auggie.stderr.on('data', (data) => {
  console.error('STDERR:', data.toString());
});

// Set up line reader for stdout
const rl = readline.createInterface({
  input: auggie.stdout,
  crlfDelay: Infinity
});

rl.on('line', (line) => {
  try {
    const response = JSON.parse(line);
    console.log('Response:', JSON.stringify(response, null, 2));
  } catch (e) {
    console.log('Non-JSON line:', line);
  }
});

auggie.on('error', (error) => {
  console.error('Process error:', error);
  process.exit(1);
});

auggie.on('exit', (code, signal) => {
  console.log(`Process exited with code ${code}, signal ${signal}`);
  process.exit(code || 0);
});

// Wait a bit for process to start
setTimeout(() => {
  console.log('Sending initialize request...');
  
  // Step 1: Send initialize request (matching what our code does)
  const initRequest = {
    jsonrpc: '2.0',
    id: requestId++,
    method: 'initialize',
    params: {
      protocolVersion: 1,
      capabilities: {
        fs: {
          readTextFile: true,
          writeTextFile: true
        },
        terminal: true
      },
      implementation: {
        name: 'test-client',
        version: '1.0.0',
        title: 'Test ACP Client'
      }
    }
  };
  
  auggie.stdin.write(JSON.stringify(initRequest) + '\n');
  
  // Step 2: After 2 seconds, send session/new request
  setTimeout(() => {
    console.log('Sending session/new request...');
    
    const sessionRequest = {
      jsonrpc: '2.0',
      id: requestId++,
      method: 'session/new',
      params: {
        cwd: '/Users/phodal/IdeaProjects/ddd-lite-example',
        mcpServers: []
      }
    };
    
    auggie.stdin.write(JSON.stringify(sessionRequest) + '\n');
    
    // Exit after 5 seconds
    setTimeout(() => {
      console.log('Test complete, closing...');
      auggie.kill();
      process.exit(0);
    }, 5000);
  }, 2000);
}, 1000);
