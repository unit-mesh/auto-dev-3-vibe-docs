#!/bin/bash
# Test JVM ACP agent server
# This creates a simple Kotlin program to test the ACP agent implementation

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "🧪 Testing JVM ACP Agent Server"
echo "================================="
echo ""

# Build the mpp-core module first
echo "📦 Building mpp-core..."
cd "$PROJECT_ROOT"
./gradlew :mpp-core:jvmTestClasses > /dev/null 2>&1
echo "✅ Build complete"
echo ""

# Run the existing ACP tests
echo "🧪 Running JVM ACP unit tests..."
./gradlew :mpp-core:jvmTest --tests "*Acp*" 2>&1 | tee /tmp/acp-test-output.txt

# Check results
if grep -q "BUILD SUCCESSFUL" /tmp/acp-test-output.txt; then
  echo ""
  echo "✅ JVM ACP tests passed!"
  exit 0
else
  echo ""
  echo "❌ JVM ACP tests failed"
  exit 1
fi
