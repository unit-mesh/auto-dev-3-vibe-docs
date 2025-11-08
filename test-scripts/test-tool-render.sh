#!/bin/bash

# Tool Render Fix Test Script
# This script tests the tool rendering with multiple tools

set -e

echo "🧪 Tool Render Fix Verification"
echo "=================================="
echo ""

# Check if dist exists
if [ ! -d "dist" ]; then
    echo "❌ dist directory not found. Please run 'npm run build' first."
    exit 1
fi

# Check if CLI executable exists
if [ ! -f "dist/jsMain/typescript/index.js" ]; then
    echo "❌ CLI executable not found at dist/jsMain/typescript/index.js"
    exit 1
fi

echo "✅ Build artifacts verified"
echo ""

# Create a test project directory
TEST_DIR="/tmp/autodev-test-$$"
mkdir -p "$TEST_DIR"

echo "📁 Created test directory: $TEST_DIR"

# Create a simple Java file for testing
cat > "$TEST_DIR/App.java" << 'EOF'
public class App {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
    
    public void method1() {
        // TODO: implement
    }
    
    public void method2() {
        // TODO: implement
    }
}
EOF

echo "✅ Created test Java file"
echo ""

# Create package.json to make it a valid project
cat > "$TEST_DIR/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <groupId>com.example</groupId>
    <artifactId>test-app</artifactId>
    <version>1.0.0</version>
</project>
EOF

echo "✅ Created test project structure"
echo ""

# Now test the CLI with multiple tools
echo "🚀 Running test task with multiple tools..."
echo "Task: Analyze the Java file and find all methods"
echo ""
echo "-------------------------------------------"

# Run with a simple task to test multiple tools
node dist/jsMain/typescript/index.js code \
    --task "分析App.java文件，列出所有方法。只使用read-file工具" \
    -p "$TEST_DIR" \
    --max-iterations 3 \
    2>&1 | head -100

echo ""
echo "-------------------------------------------"
echo ""

# Cleanup
rm -rf "$TEST_DIR"
echo "🧹 Cleaned up test directory"
echo ""

echo "✅ Test completed!"
echo ""
echo "Note: Check the output above for proper tool rendering order."
echo "Expected: Tool info should appear before tool results, in sequence."
