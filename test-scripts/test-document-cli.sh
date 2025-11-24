#!/bin/bash

# Test script for Document CLI (JVM with Tika support)
# Usage: ./test-document-cli.sh

set -e

echo "========================================="
echo "Document CLI Test Suite"
echo "========================================="
echo ""

PROJECT_ROOT="/Volumes/source/ai/autocrud"
TEST_DIR="/Users/phodal/MyDriver/material/10 AIFSD Publish/300 Delivery/Playbooks"

# Check if API key is set
if [ -z "$OPENAI_API_KEY" ]; then
    echo "❌ Error: OPENAI_API_KEY environment variable is not set"
    echo "   Please set it: export OPENAI_API_KEY='your-key-here'"
    exit 1
fi

echo "✅ API key is set"
echo ""

# Test 1: PPTX file parsing and query
echo "Test 1: PPTX File Query"
echo "------------------------"
cd "$PROJECT_ROOT"

./gradlew :mpp-server:runDocumentCli \
    -PdocProjectPath="$TEST_DIR" \
    -PdocQuery="What is the main topic of AI-First Software Delivery Playbook?" \
    -PdocPath="AI-First Software Delivery Playbook [AIFSD].pptx" \
    --quiet 2>&1 | grep -v "logback\|ktor\|kotlinx\|Deprecated\|Task :" | tail -80

echo ""
echo "========================================="
echo "Test Complete"
echo "========================================="

