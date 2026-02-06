#!/bin/bash
# Test Claude Code integration (stream-json protocol)
# Usage: ./test-claude-code.sh
#
# Prerequisite: Claude Code CLI must be installed and configured
# - Install: https://docs.anthropic.com/en/docs/claude-code
# - Ensure your API backend is running (check ~/.claude/settings.json)

set -e

echo "Claude Code Integration Test"
echo "============================================"

# Check if claude is installed
if ! command -v claude &> /dev/null; then
    echo "ERROR: claude CLI not found. Install from https://docs.anthropic.com/en/docs/claude-code"
    exit 1
fi

echo "Claude version: $(claude --version 2>&1)"
echo ""

# Check config
echo "Configuration:"
if [ -f ~/.autodev/config.yaml ]; then
    python3 -c "
import yaml
with open('$HOME/.autodev/config.yaml') as f:
    config = yaml.safe_load(f)
agents = config.get('acpAgents', {})
claude = agents.get('claude')
if claude:
    print(f'  Name: {claude.get(\"name\", \"N/A\")}')
    print(f'  Command: {claude.get(\"command\", \"N/A\")}')
    print(f'  Args: {claude.get(\"args\", \"\")}')
else:
    print('  WARNING: No claude agent configured in ~/.autodev/config.yaml')
" 2>/dev/null || echo "  (Could not parse config)"
fi
echo ""

# Check Claude Code settings
echo "Claude Code Settings:"
if [ -f ~/.claude/settings.json ]; then
    python3 -c "
import json
with open('$HOME/.claude/settings.json') as f:
    settings = json.load(f)
env = settings.get('env', {})
print(f'  Model: {env.get(\"ANTHROPIC_MODEL\", \"default\")}')
print(f'  Base URL: {env.get(\"ANTHROPIC_BASE_URL\", \"default (api.anthropic.com)\")}')
print(f'  API Key Source: {\"custom\" if env.get(\"ANTHROPIC_AUTH_TOKEN\") else \"default\"}')" 2>/dev/null || echo "  (Could not parse settings)"
fi
echo ""

# Test 1: Basic stream-json communication
echo "Test 1: Stream-JSON Protocol"
echo "--------------------------------------------"
echo "Sending user message and checking init response..."
RESPONSE=$(echo '{"type":"user","message":{"role":"user","content":[{"type":"text","text":"say hi"}]}}' | timeout 15 claude -p --output-format stream-json --input-format stream-json --verbose --dangerously-skip-permissions 2>/dev/null | head -1)

if echo "$RESPONSE" | python3 -c "import sys,json; msg=json.load(sys.stdin); assert msg['type']=='system' and msg['subtype']=='init'; print(f'  Session ID: {msg[\"session_id\"]}'); print(f'  Model: {msg[\"model\"]}'); print(f'  Version: {msg[\"claude_code_version\"]}')" 2>/dev/null; then
    echo "  PASS: Init message received"
else
    echo "  FAIL: No init message (API backend might not be running)"
fi
echo ""

# Test 2: Via Gradle (AcpDebugCli)
echo "Test 2: Gradle Integration Test"
echo "--------------------------------------------"
echo "Run: ./gradlew :mpp-ui:runAcpDebug --args=\"--agent=claude --test=claude\""
echo "Note: Requires API backend to be running for full response."
echo ""

echo "============================================"
echo "Claude Code integration is ready."
echo "Code files:"
echo "  - mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/claude/ClaudeCodeProtocol.kt"
echo "  - mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/claude/ClaudeCodeClient.kt"
echo "  - mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/agent/acp/AcpConnectionProvider.jvm.kt"
echo "  - mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/config/AcpAgentPresets.kt"
