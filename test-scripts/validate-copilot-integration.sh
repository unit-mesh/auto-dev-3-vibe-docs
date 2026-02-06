#!/bin/bash
# Quick validation script for GitHub Copilot ACP integration

echo "🔍 GitHub Copilot ACP Integration - Quick Validation"
echo "====================================================="
echo ""

# Check Copilot installation
echo "1️⃣ Checking Copilot CLI installation..."
if command -v copilot &> /dev/null; then
    COPILOT_PATH=$(which copilot)
    echo "   ✅ Found: $COPILOT_PATH"
else
    echo "   ❌ Copilot CLI not found"
    exit 1
fi

# Check ACP support
echo ""
echo "2️⃣ Checking ACP support..."
if copilot --help 2>&1 | grep -q "\-\-acp"; then
    echo "   ✅ ACP flag supported"
else
    echo "   ❌ ACP flag not found"
    exit 1
fi

# Check configuration
echo ""
echo "3️⃣ Checking configuration file..."
if [ -f ~/.autodev/config.yaml ]; then
    if grep -q "copilot:" ~/.autodev/config.yaml; then
        echo "   ✅ Copilot configured in ~/.autodev/config.yaml"
    else
        echo "   ⚠️  Copilot not in config, but file exists"
    fi
else
    echo "   ⚠️  Config file doesn't exist yet"
fi

# Check preset in code
echo ""
echo "4️⃣ Checking code integration..."
PRESET_FILE="$(dirname "$0")/../../mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/config/AcpAgentPresets.kt"
if [ -f "$PRESET_FILE" ]; then
    if grep -q 'id = "copilot"' "$PRESET_FILE"; then
        echo "   ✅ Copilot preset found in AcpAgentPresets.kt"
    else
        echo "   ❌ Copilot preset not found in code"
        exit 1
    fi
else
    echo "   ❌ AcpAgentPresets.kt not found"
    exit 1
fi

# Check ACP logs directory
echo ""
echo "5️⃣ Checking ACP logs..."
if [ -d ~/.autodev/acp-logs ]; then
    COPILOT_LOGS=$(ls -t ~/.autodev/acp-logs/Copilot_*.jsonl 2>/dev/null | head -3)
    if [ -n "$COPILOT_LOGS" ]; then
        echo "   ✅ Copilot ACP logs found:"
        echo "$COPILOT_LOGS" | while read log; do
            echo "      - $(basename "$log")"
        done
    else
        echo "   ℹ️  No Copilot logs yet (run a test to generate)"
    fi
else
    echo "   ℹ️  ACP logs directory doesn't exist yet"
fi

echo ""
echo "====================================================="
echo "✅ Validation Complete!"
echo ""
echo "📝 Next steps:"
echo "   1. Run session test: ./gradlew :mpp-ui:runAcpDebug --args=\"--agent=copilot --test=session\""
echo "   2. Check logs: tail -f ~/.autodev/acp-logs/Copilot_*.jsonl"
echo "   3. Use in app: Select Copilot as ACP agent in settings"
echo ""
