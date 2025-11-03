#!/bin/bash

# Test script to verify Kotlin SubAgents work in CLI

echo "========================================="
echo " Testing Kotlin SubAgents Integration"
echo "========================================="
echo ""

PROJECT_PATH="/Users/phodal/IdeaProjects/untitled"

# Test 1: Error Recovery Agent (trigger with a failing command)
echo "Test 1: Error Recovery SubAgent"
echo "--------------------------------"
echo "Task: Run a command that will fail, triggering ErrorRecoveryAgent"
echo ""

./dist/index.js code \
  --path "$PROJECT_PATH" \
  --task "Execute the command './gradlew nonExistentTask' to test error recovery. The command will fail, and the ErrorRecoveryAgent should analyze the failure and provide recovery suggestions." \
  2>&1 | tee /tmp/test-error-recovery.log

echo ""
echo "Check /tmp/test-error-recovery.log for:"
echo "- '🔧 Error Recovery SubAgent (Kotlin)' message"
echo "- Recovery analysis from Kotlin SubAgent"
echo ""

# Test 2: Log Summary Agent (trigger with long output command)
echo "Test 2: Log Summary SubAgent"
echo "----------------------------"
echo "Task: Run a command with very long output"
echo ""

./dist/index.js code \
  --path "$PROJECT_PATH" \
  --task "Execute './gradlew tasks --all' which produces long output. The LogSummaryAgent should automatically summarize it." \
  2>&1 | tee /tmp/test-log-summary.log

echo ""
echo "Check /tmp/test-log-summary.log for:"
echo "- '📊 Log Summary SubAgent (Kotlin)' message"
echo "- Summarized output from Kotlin SubAgent"
echo ""

echo "========================================="
echo " Tests Complete"
echo "========================================="
echo "Review logs:"
echo "  - /tmp/test-error-recovery.log"
echo "  - /tmp/test-log-summary.log"
