#!/bin/bash
# Minimal test to trigger ErrorRecoveryAgent

cd "$(dirname "$0")/.."

echo "📋 Test: Trigger Kotlin ErrorRecoveryAgent"
echo "==========================================="
echo ""
echo "Running agent with a task that executes a failing command..."
echo ""

timeout 120 ./dist/index.js code \
  --path /Users/phodal/IdeaProjects/untitled \
  --task "Execute shell command: './gradlew nonExistentTaskXYZ123'. This will fail."

echo ""
echo "Test complete. Check above for:"
echo "  - '🔧 ACTIVATING KOTLIN ERROR RECOVERY SUBAGENT'"
echo "  - '[DEBUG] Calling JsErrorRecoveryAgent.execute()...'"
echo "  - Recovery analysis from Kotlin SubAgent"
