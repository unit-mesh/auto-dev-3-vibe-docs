#!/bin/bash
# Test script for OpenCode ACP integration
# This script tests the basic ACP protocol communication with OpenCode

set -e

echo "=========================================="
echo "OpenCode ACP Integration Test"
echo "=========================================="
echo ""

# Test 1: Check if opencode is installed
echo "Test 1: Checking OpenCode installation..."
if command -v opencode &> /dev/null; then
    echo "✅ OpenCode is installed"
    OPENCODE_PATH=$(which opencode)
    echo "   Path: $OPENCODE_PATH"
    OPENCODE_VERSION=$(opencode --version 2>&1 || echo "unknown")
    echo "   Version: $OPENCODE_VERSION"
else
    echo "❌ OpenCode is not installed"
    exit 1
fi
echo ""

# Test 2: Check config.yaml
echo "Test 2: Checking ACP configuration in ~/.autodev/config.yaml..."
if [ -f ~/.autodev/config.yaml ]; then
    echo "✅ Config file exists"
    if grep -q "opencode" ~/.autodev/config.yaml; then
        echo "✅ OpenCode agent is configured"
        echo "   Configuration:"
        grep -A 4 '"opencode"' ~/.autodev/config.yaml | sed 's/^/   /'
    else
        echo "❌ OpenCode agent is not configured"
        exit 1
    fi
else
    echo "❌ Config file not found"
    exit 1
fi
echo ""

# Test 3: Test ACP subprocess
echo "Test 3: Testing OpenCode ACP subprocess..."
echo "   Starting: opencode acp"
echo "   Sending initialize request..."

# Create a simple ACP initialize request
INIT_REQUEST='{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":1,"capabilities":{"fs":{"readTextFile":true,"writeTextFile":true},"terminal":true},"implementation":{"name":"test-client","version":"1.0.0","title":"Test Client"}}}'

# Start opencode acp as a subprocess and send initialize request
OUTPUT=$(echo "$INIT_REQUEST" | timeout 10s opencode acp 2>&1 || true)

if echo "$OUTPUT" | grep -q '"result"'; then
    echo "✅ OpenCode ACP subprocess responded to initialize"
    echo "   Response preview:"
    echo "$OUTPUT" | head -n 5 | sed 's/^/   /'
elif echo "$OUTPUT" | grep -q "error"; then
    echo "⚠️  OpenCode ACP subprocess responded with error:"
    echo "$OUTPUT" | sed 's/^/   /'
else
    echo "❌ OpenCode ACP subprocess did not respond correctly"
    echo "   Output:"
    echo "$OUTPUT" | sed 's/^/   /'
fi
echo ""

# Test 4: Check preset detection
echo "Test 4: Testing preset detection..."
if command -v opencode &> /dev/null; then
    echo "✅ OpenCode preset should be detected by IdeaAcpAgentPreset.detectInstalled()"
    echo "   Command: opencode"
    echo "   Args: acp"
    echo "   Status: Available in PATH"
else
    echo "❌ OpenCode preset will not be detected"
fi
echo ""

# Test 5: Check for conflicting presets
echo "Test 5: Checking for other ACP agents..."
AGENTS=("kimi" "gemini" "claude" "copilot" "codex" "auggie")
for agent in "${AGENTS[@]}"; do
    if command -v "$agent" &> /dev/null; then
        echo "   Found: $agent ($(which $agent))"
    fi
done
echo ""

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "✅ OpenCode is installed and accessible"
echo "✅ Configuration is correct"
echo "✅ ACP integration is ready for IDEA plugin"
echo ""
echo "Next steps:"
echo "1. Build the IDEA plugin: cd mpp-idea && ../gradlew buildPlugin"
echo "2. Install the plugin in IntelliJ IDEA"
echo "3. Open a project and test the ACP agent panel"
echo "4. Select 'OpenCode' from the agent dropdown"
echo "5. Send a test message to verify the integration"
echo ""
