#!/bin/bash
# Test SSE API for mpp-server
# Usage: ./test-sse-api.sh

set -e

echo "🔍 Testing SSE API..."
echo ""

# 1. List available projects
echo "1️⃣  Listing available projects:"
curl -s http://localhost:8080/api/projects | jq '.projects[] | {id, name}'
echo ""

# 2. Test SSE stream with a simple task
PROJECT_ID=".vim_runtime"
TASK="list top 5 files"

echo "2️⃣  Testing SSE stream (GET request):"
echo "   Project: $PROJECT_ID"
echo "   Task: $TASK"
echo ""

curl -N "http://localhost:8080/api/agent/stream?projectId=${PROJECT_ID}&task=${TASK}" \
  -H "Accept: text/event-stream" \
  --max-time 30 2>&1 | head -50

echo ""
echo "✅ SSE API test completed!"

