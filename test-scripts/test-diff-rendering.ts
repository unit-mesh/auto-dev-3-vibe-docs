#!/usr/bin/env node
/**
 * Test script to verify diff rendering in CliRenderer and ServerRenderer
 */

import { CliRenderer } from '../../mpp-ui/src/jsMain/typescript/agents/render/CliRenderer.js';
import { ServerRenderer } from '../../mpp-ui/src/jsMain/typescript/agents/render/ServerRenderer.js';

console.log('=== Testing Diff Rendering ===\n');

// Test CliRenderer
console.log('--- Testing CliRenderer ---');
const cliRenderer = new CliRenderer();

// Test 1: write-file with new file (additions only)
console.log('\n1. New file creation:');
cliRenderer.renderToolCall('write-file', 'path="test.txt" content="Line 1\\nLine 2\\nLine 3"');
cliRenderer.renderToolResult(
  'write-file',
  true,
  'Successfully created file: test.txt (24 chars, 3 lines)',
  null
);

// Test 2: write-file with overwrite (simulating diff)
console.log('\n2. File overwrite with diff:');
cliRenderer.renderToolCall('write-file', 'path="existing.txt"');
const diffOutput = `Successfully overwrote file: existing.txt (100 chars, 10 lines)
--- a/existing.txt
+++ b/existing.txt
@@ -1,5 +1,7 @@
 Line 1
-Line 2 old
+Line 2 new
+Line 2.5 added
 Line 3
 Line 4
-Line 5 removed
+Line 5 modified`;

cliRenderer.renderToolResult('write-file', true, diffOutput, null);

// Test 3: edit-file with clear additions and deletions
console.log('\n3. Edit file with clear diff:');
cliRenderer.renderToolCall('edit-file', 'path="app.js"');
const editOutput = `File updated successfully
+Added import statement
+Added function definition
-Removed old code
+Added new implementation`;

cliRenderer.renderToolResult('edit-file', true, editOutput, null);

// Test ServerRenderer
console.log('\n\n--- Testing ServerRenderer ---');
const serverRenderer = new ServerRenderer();

// Test 1: write-file with new file
console.log('\n1. New file creation:');
serverRenderer.renderEvent({
  type: 'tool_call',
  toolName: 'write-file',
  params: '{"path":"test.txt","content":"Line 1\\nLine 2\\nLine 3"}'
});
serverRenderer.renderEvent({
  type: 'tool_result',
  toolName: 'write-file',
  success: true,
  output: 'Successfully created file: test.txt (24 chars, 3 lines)'
});

// Test 2: edit-file with diff
console.log('\n2. Edit file with diff:');
serverRenderer.renderEvent({
  type: 'tool_call',
  toolName: 'edit-file',
  params: '{"path":"config.yaml"}'
});
serverRenderer.renderEvent({
  type: 'tool_result',
  toolName: 'edit-file',
  success: true,
  output: diffOutput
});

console.log('\n=== Test Complete ===');
