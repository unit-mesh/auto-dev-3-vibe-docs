#!/bin/bash
# Test GitHub Copilot CLI with ACP support

set -e

echo "🚀 Testing GitHub Copilot CLI ACP Integration"
echo "=" | awk '{for(i=1;i<=60;i++) printf "="}; {print ""}'

# Check if copilot is installed
if ! command -v copilot &> /dev/null; then
    echo "❌ Error: copilot CLI is not installed"
    echo "📦 Install it with: npm install -g @github/copilot"
    exit 1
fi

echo "✅ Copilot CLI found: $(which copilot)"

# Check if --acp is supported
if ! copilot --help 2>&1 | grep -q "\-\-acp"; then
    echo "❌ Error: copilot CLI does not support --acp flag"
    exit 1
fi

echo "✅ ACP support confirmed"
echo ""

# Test 1: Basic session test
echo "📍 Test 1: Basic Session Test"
echo "-" | awk '{for(i=1;i<=60;i++) printf "-"}; {print ""}'
cd "$(dirname "$0")/../.."
./gradlew :mpp-ui:runAcpDebug --args="--agent=Copilot --test=session" --quiet 2>&1 | tee /tmp/copilot-acp-test-session.log

# Test 2: Wildcard test
echo ""
echo "📍 Test 2: Wildcard/Glob Pattern Test"
echo "-" | awk '{for(i=1;i<=60;i++) printf "-"}; {print ""}'
./gradlew :mpp-ui:runAcpDebug --args="--agent=Copilot --test=wildcard" --quiet 2>&1 | tee /tmp/copilot-acp-test-wildcard.log

# Test 3: Bash commands test
echo ""
echo "📍 Test 3: Bash Commands Test"
echo "-" | awk '{for(i=1;i<=60;i++) printf "-"}; {print ""}'
./gradlew :mpp-ui:runAcpDebug --args="--agent=Copilot --test=bash" --quiet 2>&1 | tee /tmp/copilot-acp-test-bash.log

echo ""
echo "=" | awk '{for(i=1;i<=60;i++) printf "="}; {print ""}'
echo "✅ All tests completed!"
echo ""
echo "📋 Test logs saved to:"
echo "   - /tmp/copilot-acp-test-session.log"
echo "   - /tmp/copilot-acp-test-wildcard.log"
echo "   - /tmp/copilot-acp-test-bash.log"
echo ""
echo "🔍 ACP logs available at: ~/.autodev/acp-logs/"
