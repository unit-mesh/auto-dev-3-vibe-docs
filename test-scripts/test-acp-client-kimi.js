#!/usr/bin/env node
/**
 * Test: ACP Client connecting to Kimi CLI as agent
 */

const path = require('path');
const PROJECT_ROOT = path.resolve(__dirname, '../..');

// Import the ACP client
const { AcpClientConnection } = require(path.join(PROJECT_ROOT, 'mpp-ui/dist/jsMain/typescript/agents/acp'));

console.log('🧪 Testing ACP Client → Kimi CLI');
console.log('==================================\n');

async function test() {
  // Create ACP client that will connect to Kimi CLI
  const client = new AcpClientConnection('kimi', ['acp'], {});

  // Set up callbacks
  let receivedSomeOutput = false;

  client.setCallbacks({
    onTextChunk: (text) => {
      console.log(`[Kimi Text] ${text}`);
      receivedSomeOutput = true;
    },
    onThoughtChunk: (text) => {
      console.log(`[Kimi Thought] ${text}`);
      receivedSomeOutput = true;
    },
    onToolCall: (title, status, input, output) => {
      console.log(`[Kimi Tool] ${title} - ${status}`);
      receivedSomeOutput = true;
    },
    onError: (msg) => {
      console.error(`[Error] ${msg}`);
    }
  });

  try {
    // Connect to Kimi CLI agent
    console.log('📡 Connecting to Kimi CLI ACP agent...');
    await client.connect('/tmp');
    console.log('✅ Connected to Kimi CLI!\n');

    // Send a simple prompt
    console.log('📤 Sending prompt to Kimi...');
    const result = await client.prompt('Hello, what is 2+2?');
    console.log(`\n✅ Prompt completed: ${result.stopReason}\n`);

    // Disconnect
    await client.disconnect();

    // Summary
    console.log('==================================');
    console.log('Test Summary:');
    console.log(`  Connected to Kimi CLI: ✅`);
    console.log(`  Session created: ✅`);
    console.log(`  Prompt sent: ✅`);
    console.log(`  Received output: ${receivedSomeOutput ? '✅' : '⚠️  (no output)'}`);
    console.log('\n✅ ACP Client → Kimi CLI test PASSED!');
    process.exit(0);

  } catch (error) {
    console.error('\n❌ Test FAILED:', error.message);
    if (error.message.includes('ENOENT') || error.message.includes('spawn')) {
      console.error('\n💡 Note: Make sure Kimi CLI is installed and authenticated:');
      console.error('   1. Install: pip install kimi-cli (or use pipx)');
      console.error('   2. Login: kimi login');
    }
    await client.disconnect().catch(() => {});
    process.exit(1);
  }
}

test().catch(err => {
  console.error('Unhandled error:', err);
  process.exit(1);
});
