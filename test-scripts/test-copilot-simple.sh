#!/bin/bash
# Simple test to verify GitHub Copilot CLI ACP integration

set -e

echo "🧪 Testing GitHub Copilot ACP Integration"
echo "=========================================="
echo ""

cd "$(dirname "$0")/../.."

# Test: Simple session test with a basic question
echo "📝 Running simple session test..."
echo "Question: 'What is 2+2?'"
echo ""

./gradlew :mpp-ui:runAcpDebug --args="--agent=Copilot --test=session" 2>&1 | head -100

echo ""
echo "✅ Test completed!"
echo ""
echo "📋 Check ACP logs at: ~/.autodev/acp-logs/"
echo "🔍 Look for files starting with: Copilot_"
