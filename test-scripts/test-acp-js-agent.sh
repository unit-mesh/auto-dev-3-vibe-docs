#!/bin/bash
# Test script: JS CLI as ACP agent server, Kimi CLI as client
# This tests that external ACP clients can connect to our JS CLI agent

set -e

cd "$(dirname "$0")/../.."
PROJECT_ROOT="$(pwd)"

echo "🧪 Testing JS CLI as ACP Agent Server"
echo "======================================="
echo ""

# Build the JS CLI
echo "📦 Building JS CLI..."
cd mpp-ui
npm run build
cd "$PROJECT_ROOT"

# Create a test workspace
TEST_DIR="/tmp/xiuper-acp-test-$$"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

echo "# Test Project" > README.md
echo "This is a test file for ACP integration." >> README.md

cat > hello.js << 'EOF'
function greet(name) {
  console.log("Hello, " + name);
}

greet("World");
EOF

echo ""
echo "📂 Test workspace: $TEST_DIR"
echo ""

# Create ACP config file for Kimi CLI
ACP_CONFIG="$TEST_DIR/acp.json"
cat > "$ACP_CONFIG" << EOF
{
  "mcpServers": {
    "xiuper": {
      "command": "node",
      "args": ["$PROJECT_ROOT/mpp-ui/dist/jsMain/typescript/index.js", "acp-agent"]
    }
  }
}
EOF

echo "📝 ACP Config created:"
cat "$ACP_CONFIG"
echo ""

echo "🚀 Starting test with Kimi CLI..."
echo "Note: This will attempt to connect to Xiuper JS CLI as an ACP agent."
echo ""
echo "Running: kimi --mcp-config-file $ACP_CONFIG"
echo ""
echo "You should be able to ask Kimi to use the 'xiuper' agent."
echo "Try asking: 'List the files in this directory using xiuper'"
echo ""

# Note: kimi requires interactive mode, so we just show the command
echo "⚠️  Manual test required - run the following command:"
echo "    cd $TEST_DIR"
echo "    kimi --mcp-config-file $ACP_CONFIG"
echo ""
echo "To clean up after testing:"
echo "    rm -rf $TEST_DIR"
