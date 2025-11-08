#!/bin/bash

# Test script to verify chat history saving functionality
# This script runs a simple coding agent task and checks if the chat history is saved

echo "🧪 Testing chat history save functionality..."

# Set up test environment
export OPENAI_API_KEY="${OPENAI_API_KEY:-test-key}"
export OPENAI_BASE_URL="${OPENAI_BASE_URL:-https://api.openai.com/v1}"

# Create a temporary test directory
TEST_DIR=$(mktemp -d)
echo "📁 Test directory: $TEST_DIR"

# Create a simple test file
cat > "$TEST_DIR/test.txt" << 'EOF'
Hello, this is a test file.
EOF

# Run the coding agent with a simple task
echo "🤖 Running coding agent..."
cd "$(dirname "$0")/../../mpp-ui"

# Note: This will fail if no valid API key is set, but we can still check if the code runs
npm run start -- \
  --path "$TEST_DIR" \
  --task "List the files in this directory" \
  2>&1 | tee /tmp/autodev-test-output.log || true

# Check if chat history file was created
echo ""
echo "📝 Checking for chat history files..."
CHAT_HISTORY_DIR="$HOME/.autodev/logs"

if [ -d "$CHAT_HISTORY_DIR" ]; then
  echo "✅ Log directory exists: $CHAT_HISTORY_DIR"
  
  # Find the most recent chat history file
  LATEST_CHAT_HISTORY=$(ls -t "$CHAT_HISTORY_DIR"/chat-history-*.json 2>/dev/null | head -1)
  
  if [ -n "$LATEST_CHAT_HISTORY" ]; then
    echo "✅ Chat history file found: $LATEST_CHAT_HISTORY"
    echo ""
    echo "📄 Chat history content:"
    cat "$LATEST_CHAT_HISTORY" | head -50
    echo ""
    echo "✅ Test PASSED: Chat history was saved successfully!"
  else
    echo "❌ Test FAILED: No chat history file found"
    exit 1
  fi
else
  echo "❌ Test FAILED: Log directory does not exist"
  exit 1
fi

# Cleanup
rm -rf "$TEST_DIR"
echo ""
echo "🧹 Cleanup complete"
