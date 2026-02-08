#!/bin/bash
# Comprehensive OpenCode ACP integration test
# Tests configuration, preset detection, and IDEA plugin integration

set -e

echo "=========================================="
echo "OpenCode ACP Integration - Full Test Suite"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

pass_count=0
fail_count=0

test_pass() {
    echo -e "${GREEN}✅ PASS:${NC} $1"
    ((pass_count++))
}

test_fail() {
    echo -e "${RED}❌ FAIL:${NC} $1"
    ((fail_count++))
}

test_info() {
    echo -e "${YELLOW}ℹ️  INFO:${NC} $1"
}

echo "=== Test 1: OpenCode Binary ==="
if command -v opencode &> /dev/null; then
    OPENCODE_PATH=$(which opencode)
    OPENCODE_VERSION=$(opencode --version 2>&1)
    test_pass "OpenCode binary found at $OPENCODE_PATH"
    test_info "Version: $OPENCODE_VERSION"
else
    test_fail "OpenCode binary not found"
fi
echo ""

echo "=== Test 2: ACP Command Support ==="
# Test if opencode supports acp command
HELP_OUTPUT=$(opencode --help 2>&1 || true)
if echo "$HELP_OUTPUT" | grep -q "acp"; then
    test_pass "OpenCode supports 'acp' command"
else
    test_info "Checking if 'opencode acp' works directly..."
    if echo '{"jsonrpc":"2.0","id":999,"method":"ping"}' | timeout 3s opencode acp 2>&1 | grep -q "jsonrpc"; then
        test_pass "OpenCode ACP protocol is responsive"
    else
        test_fail "OpenCode ACP command does not respond to JSON-RPC"
    fi
fi
echo ""

echo "=== Test 3: Config File Integration ==="
CONFIG_FILE="$HOME/.autodev/config.yaml"
if [ -f "$CONFIG_FILE" ]; then
    test_pass "Config file exists at $CONFIG_FILE"
    
    if grep -q '"opencode"' "$CONFIG_FILE"; then
        test_pass "OpenCode agent is configured in config.yaml"
        
        # Extract opencode config
        echo "   Configuration details:"
        grep -A 4 '"opencode"' "$CONFIG_FILE" | sed 's/^/   /'
        
        # Check if it's the active agent
        if grep -q 'activeAcpAgent: "opencode"' "$CONFIG_FILE"; then
            test_pass "OpenCode is set as the active ACP agent"
        else
            test_info "OpenCode is configured but not active"
            ACTIVE_AGENT=$(grep 'activeAcpAgent:' "$CONFIG_FILE" | awk '{print $2}')
            test_info "Active agent: $ACTIVE_AGENT"
        fi
    else
        test_fail "OpenCode agent is NOT configured in config.yaml"
    fi
else
    test_fail "Config file not found at $CONFIG_FILE"
fi
echo ""

echo "=== Test 4: Source Code Integration ==="
PRESET_FILE="/Users/phodal/ai/xiuper/mpp-idea/src/main/kotlin/cc/unitmesh/devins/idea/toolwindow/acp/IdeaAcpAgentViewModel.kt"
if [ -f "$PRESET_FILE" ]; then
    test_pass "IdeaAcpAgentViewModel.kt file exists"
    
    if grep -q 'id = "opencode"' "$PRESET_FILE"; then
        test_pass "OpenCode preset is defined in source code"
        
        # Extract preset details
        echo "   Preset configuration:"
        grep -A 6 'id = "opencode"' "$PRESET_FILE" | sed 's/^/   /'
        
        # Check if it's first in the list
        FIRST_PRESET=$(grep -A 2 'ALL_PRESETS = listOf' "$PRESET_FILE" | grep 'id =' | head -1 | awk -F'"' '{print $2}')
        if [ "$FIRST_PRESET" = "opencode" ]; then
            test_pass "OpenCode is the first preset (default)"
        else
            test_info "OpenCode is not the first preset (first: $FIRST_PRESET)"
        fi
    else
        test_fail "OpenCode preset NOT found in source code"
    fi
else
    test_fail "IdeaAcpAgentViewModel.kt file not found"
fi
echo ""

echo "=== Test 5: ACP Protocol Communication ==="
echo "   Testing ACP initialize handshake..."
INIT_REQUEST='{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":1,"capabilities":{"fs":{"readTextFile":true,"writeTextFile":true},"terminal":true},"implementation":{"name":"test-client","version":"1.0.0","title":"Test Client"}}}'

RESPONSE=$(echo "$INIT_REQUEST" | timeout 10s opencode acp 2>&1 || true)

if echo "$RESPONSE" | grep -q '"result"'; then
    test_pass "ACP initialize request succeeded"
    
    # Check for expected fields in response
    if echo "$RESPONSE" | grep -q '"protocolVersion"'; then
        test_pass "Response contains protocolVersion"
    fi
    
    if echo "$RESPONSE" | grep -q '"agentInfo"'; then
        test_pass "Response contains agentInfo"
        
        # Extract agent info
        VERSION=$(echo "$RESPONSE" | grep -o '"version":"[^"]*"' | head -1 | cut -d'"' -f4)
        NAME=$(echo "$RESPONSE" | grep -o '"name":"[^"]*"' | head -1 | cut -d'"' -f4)
        test_info "Agent: $NAME v$VERSION"
    fi
    
    echo "   Response preview:"
    echo "$RESPONSE" | jq '.' 2>/dev/null | head -20 | sed 's/^/   /' || echo "$RESPONSE" | head -5 | sed 's/^/   /'
else
    test_fail "ACP initialize request failed or invalid response"
    echo "   Response:"
    echo "$RESPONSE" | head -10 | sed 's/^/   /'
fi
echo ""

echo "=== Test 6: Build Verification ==="
PROJECT_DIR="/Users/phodal/ai/xiuper"
if [ -d "$PROJECT_DIR" ]; then
    test_pass "Project directory exists"
    
    # Check if compiled classes exist
    COMPILED_CLASS="$PROJECT_DIR/mpp-idea/build/classes/kotlin/main/cc/unitmesh/devins/idea/toolwindow/acp/IdeaAcpAgentViewModel.class"
    if [ -f "$COMPILED_CLASS" ]; then
        test_pass "IdeaAcpAgentViewModel compiled successfully"
    else
        test_info "Compiled classes not found (may need to build)"
    fi
    
    # Check build status
    if [ -f "$PROJECT_DIR/mpp-idea/build/libs"/*.jar ]; then
        test_pass "IDEA plugin JAR exists"
        ls -lh "$PROJECT_DIR/mpp-idea/build/libs"/*.jar | tail -1 | sed 's/^/   /'
    else
        test_info "Plugin JAR not found (run './gradlew :mpp-idea:buildPlugin')"
    fi
else
    test_fail "Project directory not found"
fi
echo ""

echo "=== Test 7: Preset Detection Simulation ==="
echo "   Simulating IdeaAcpAgentPreset.detectInstalled()..."
DETECTED_AGENTS=()
for cmd in "opencode" "kimi" "gemini" "claude" "copilot" "codex" "auggie"; do
    if command -v "$cmd" &> /dev/null; then
        DETECTED_AGENTS+=("$cmd")
        test_info "Detected: $cmd ($(which $cmd))"
    fi
done

if [[ " ${DETECTED_AGENTS[@]} " =~ " opencode " ]]; then
    test_pass "OpenCode would be detected by preset detection"
else
    test_fail "OpenCode would NOT be detected by preset detection"
fi

echo "   Total detected agents: ${#DETECTED_AGENTS[@]}"
echo ""

echo "=========================================="
echo "Test Results Summary"
echo "=========================================="
echo -e "${GREEN}Passed: $pass_count${NC}"
echo -e "${RED}Failed: $fail_count${NC}"
echo ""

if [ $fail_count -eq 0 ]; then
    echo -e "${GREEN}✅ All tests passed! OpenCode ACP integration is ready.${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Build the plugin: ./gradlew :mpp-idea:buildPlugin"
    echo "2. Install in IntelliJ IDEA: Settings > Plugins > Install Plugin from Disk"
    echo "3. Restart IDEA and open the ACP Agent panel"
    echo "4. Select 'OpenCode' from the dropdown and start a conversation"
    exit 0
else
    echo -e "${YELLOW}⚠️  Some tests failed. Please review the errors above.${NC}"
    exit 1
fi
