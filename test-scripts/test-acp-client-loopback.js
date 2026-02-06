#!/usr/bin/env node
/**
 * Test: ACP Client connecting to ACP Agent (loopback test)
 * This tests our TypeScript ACP client can connect to our TypeScript ACP agent
 */

const path = require('path');
const PROJECT_ROOT = path.resolve(__dirname, '../..');
const CLI_PATH = path.join(PROJECT_ROOT, 'mpp-ui/dist/jsMain/typescript/index.js');

// Import the ACP client
const { AcpClientConnection } = require(path.join(PROJECT_ROOT, 'mpp-ui/dist/jsMain/typescript/agents/acp'));

console.log('🧪 Testing ACP Client → ACP Agent (Loopback)');
console.log('==============================================\n');

async function test() {
  // Create ACP client that will connect to our own CLI as an agent
  const client = new AcpClientConnection('node', [CLI_PATH, 'acp-agent'], {});

  // Set up callbacks
  let receivedText = false;
  let receivedThought = false;

  client.setCallbacks({
    onTextChunk: (text) => {
      console.log(`[Text] ${text}`);
      receivedText = true;
    },
    onThoughtChunk: (text) => {
      console.log(`[Thought] ${text}`);
      receivedThought = true;
    },
    onToolCall: (title, status, input, output) => {
      console.log(`[Tool] ${title} - ${status}`);
    },
    onError: (msg) => {
      console.error(`[Error] ${msg}`);
    }
  });

  try {
    // Connect to agent
    console.log('📡 Connecting to ACP agent...');
    await client.connect('/tmp');
    console.log('✅ Connected!\n');

    // Send a prompt
    console.log('📤 Sending prompt...');
    const result = await client.prompt('List the files in the current directory');
    console.log(`\n✅ Prompt completed: ${result.stopReason}\n`);

    // Disconnect
    await client.disconnect();

    // Summary
    console.log('==============================================');
    console.log('Test Summary:');
    console.log(`  Client connected: ✅`);
    console.log(`  Session created: ✅`);
    console.log(`  Prompt sent: ✅`);
    console.log(`  Received text: ${receivedText ? '✅' : '❌'}`);
    console.log(`  Received thought: ${receivedThought ? '✅' : '❌'}`);
    console.log('\n✅ ACP Client → Agent test PASSED!');
    process.exit(0);

  } catch (error) {
    console.error('\n❌ Test FAILED:', error.message);
    await client.disconnect().catch(() => {});
    process.exit(1);
  }
}

test();
