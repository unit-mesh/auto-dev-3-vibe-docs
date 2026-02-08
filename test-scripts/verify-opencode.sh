#!/bin/bash
# Quick verification script for OpenCode ACP integration

echo "🔍 OpenCode ACP Integration - Quick Check"
echo "=========================================="

# Check 1: Binary
if command -v opencode &>/dev/null; then
    echo "✅ OpenCode binary: $(which opencode)"
    echo "   Version: $(opencode --version)"
else
    echo "❌ OpenCode not found. Install: curl -fsSL https://opencode.ai/install | bash"
    exit 1
fi

# Check 2: Config
if grep -q '"opencode"' ~/.autodev/config.yaml 2>/dev/null; then
    echo "✅ Config file: OpenCode configured"
    if grep -q 'activeAcpAgent: "opencode"' ~/.autodev/config.yaml; then
        echo "   Status: Active agent"
    fi
else
    echo "⚠️  Config file: OpenCode not configured (will be auto-detected)"
fi

# Check 3: Source code
if grep -q 'id = "opencode"' /Users/phodal/ai/xiuper/mpp-idea/src/main/kotlin/cc/unitmesh/devins/idea/toolwindow/acp/IdeaAcpAgentViewModel.kt 2>/dev/null; then
    echo "✅ Source code: OpenCode preset defined"
else
    echo "❌ Source code: OpenCode preset not found"
    exit 1
fi

# Check 4: ACP protocol
echo -n "⏳ Testing ACP protocol... "
RESPONSE=$(echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":1,"capabilities":{},"implementation":{"name":"test"}}}' | timeout 5s opencode acp 2>&1 || true)
if echo "$RESPONSE" | grep -q '"result"'; then
    echo "✅ Working"
else
    echo "❌ Failed"
    exit 1
fi

echo ""
echo "✅ All checks passed! OpenCode ACP integration is ready."
echo ""
echo "To use in IntelliJ IDEA:"
echo "1. Open AutoDev tool window"
echo "2. Go to ACP tab"
echo "3. Select 'OpenCode' from dropdown"
echo "4. Start coding with AI assistance!"
