#!/bin/bash
# Demo script to show different ACP agents in action

AGENTS=("copilot" "gemini" "kimi")
PROMPT="Write a simple hello world function in Kotlin"

echo "🎬 ACP Agents Demo - Comparison"
echo "================================"
echo ""
echo "Prompt: '$PROMPT'"
echo ""

for agent in "${AGENTS[@]}"; do
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "🤖 Agent: $(echo $agent | tr '[:lower:]' '[:upper:]')"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    # Check if agent is available
    CONFIG_CHECK=$(grep -A 3 "  $agent:" ~/.autodev/config.yaml 2>/dev/null)
    if [ -z "$CONFIG_CHECK" ]; then
        echo "⚠️  $agent is not configured in ~/.autodev/config.yaml"
        echo "   Skipping..."
        echo ""
        continue
    fi
    
    echo "✅ $agent is configured"
    echo "📝 Starting session..."
    echo ""
    
    # Note: This is a demo script. In real usage, you would call the agent
    # through the ACP debug CLI or the application UI
    
    echo "💡 To test $agent manually:"
    echo "   ./gradlew :mpp-ui:runAcpDebug --args=\"--agent=$agent --test=session\""
    echo ""
    
    # Show latest log if exists
    AGENT_CAP=$(echo "$agent" | sed 's/^./\U&/')
    LATEST_LOG=$(ls -t ~/.autodev/acp-logs/${AGENT_CAP}_*.jsonl 2>/dev/null | head -1)
    if [ -n "$LATEST_LOG" ]; then
        echo "📋 Latest log: $(basename "$LATEST_LOG")"
        LOG_SIZE=$(wc -l < "$LATEST_LOG" 2>/dev/null || echo "0")
        echo "   Lines: $LOG_SIZE"
        echo ""
    fi
done

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Demo script complete!"
echo ""
echo "🚀 Next steps:"
echo "   1. Choose an agent in the application UI"
echo "   2. Or test manually using the commands shown above"
echo "   3. Check logs in ~/.autodev/acp-logs/"
echo ""
