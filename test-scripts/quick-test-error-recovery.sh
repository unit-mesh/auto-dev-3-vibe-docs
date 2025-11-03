#!/bin/bash
# Quick test - trigger Error Recovery SubAgent

cd "$(dirname "$0")/.."

echo "Testing Kotlin ErrorRecoveryAgent..."
echo ""

./dist/index.js code \
  --path /Users/phodal/IdeaProjects/untitled \
  --task "Run this shell command: './gradlew invalidTaskName'. This will fail and should trigger the ErrorRecoveryAgent." \
  2>&1 | grep -E "(Error Recovery|🔧|SubAgent|Kotlin|shouldAbort|shouldRetry)" | head -20

echo ""
echo "---"
echo "If you see '🔧 Error Recovery SubAgent (Kotlin)' above, the agent is working!"
