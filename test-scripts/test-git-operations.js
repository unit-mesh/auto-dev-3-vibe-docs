#!/usr/bin/env node

/**
 * 测试 Kotlin/JS 实现的 GitOperations
 * 
 * 运行方式：
 *   cd mpp-ui
 *   npm run build:ts
 *   node test-scripts/test-git-operations.js
 */

const MppCore = require('../build/compileSync/js/main/productionExecutable/kotlin/mpp-core.js');

async function testGitOperations() {
  console.log('🧪 Testing GitOperations (Kotlin/JS implementation)');
  console.log('================================================\n');

  const projectPath = process.cwd();
  console.log(`Project path: ${projectPath}\n`);

  try {
    // Create GitOperations instance
    const { GitOperations } = MppCore.cc.unitmesh.agent.platform;
    const gitOps = new GitOperations(projectPath);

    // Test 1: Check if supported
    console.log('Test 1: Check if Git operations are supported');
    const isSupported = gitOps.isSupported();
    console.log(`   Result: ${isSupported ? '✅ Supported' : '❌ Not supported'}\n`);

    if (!isSupported) {
      console.log('⚠️  Git operations not available in this environment');
      return;
    }

    // Test 2: Get modified files
    console.log('Test 2: Get modified files');
    const modifiedFiles = await gitOps.getModifiedFiles();
    console.log(`   Found ${modifiedFiles.length} modified file(s)`);
    if (modifiedFiles.length > 0) {
      modifiedFiles.forEach(file => console.log(`   - ${file}`));
    }
    console.log();

    // Test 3: Get diff for first modified file
    if (modifiedFiles.length > 0) {
      console.log('Test 3: Get diff for first modified file');
      const firstFile = modifiedFiles[0];
      console.log(`   File: ${firstFile}`);
      
      const diff = await gitOps.getFileDiff(firstFile);
      if (diff) {
        const lines = diff.split('\n').slice(0, 10);
        console.log(`   Diff (first 10 lines):`);
        lines.forEach(line => console.log(`   ${line}`));
        if (diff.split('\n').length > 10) {
          console.log(`   ... (${diff.split('\n').length - 10} more lines)`);
        }
      } else {
        console.log(`   ⚠️  No diff available`);
      }
      console.log();
    }

    console.log('✅ All tests completed successfully!');
  } catch (error) {
    console.error('❌ Test failed:', error.message);
    console.error(error.stack);
    process.exit(1);
  }
}

// Run tests
testGitOperations().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
