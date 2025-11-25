#!/bin/bash

# Test multiple code queries against the autocrud codebase

cd /Volumes/source/ai/autocrud

PROJECT_PATH="/Volumes/source/ai/autocrud/mpp-core/src/commonMain/kotlin"

echo "==============================================="
echo "Testing DocumentCli with Multiple Code Queries"
echo "==============================================="
echo ""

# Test 1: Understanding DocQL
echo "📝 Test 1: Understanding DocQL"
echo "Query: What is DocQL and how does it work?"
echo "Expected: Should find DocQL classes and explain the query language"
echo ""

# Test 2: Finding specific implementation
echo "📝 Test 2: Finding DocumentAgent implementation"
echo "Query: How does DocumentAgent execute queries?"
echo "Expected: Should find DocumentAgent.execute() method"
echo ""

# Test 3: Finding related classes
echo "📝 Test 3: Finding tool implementations"
echo "Query: What tools are available in the agent framework?"
echo "Expected: Should find tool registry and tool implementations"
echo ""

# Test 4: Understanding architecture
echo "📝 Test 4: Understanding architecture"
echo "Query: How are documents parsed and indexed?"
echo "Expected: Should find DocumentParser, DocumentRegistry, DocumentIndexProvider"
echo ""

# Test 5: Finding error handling
echo "📝 Test 5: Finding error handling"
echo "Query: How does the system handle parsing errors?"
echo "Expected: Should find error handling in parsers and ParseStatus"
echo ""

echo "Run individual tests by uncommenting the corresponding ./gradlew command below"
echo ""

# Uncomment to run tests:
# ./gradlew :mpp-ui:run --args="$PROJECT_PATH 'What is DocQL and how does it work?'"
# ./gradlew :mpp-ui:run --args="$PROJECT_PATH 'How does DocumentAgent execute queries?'"
# ./gradlew :mpp-ui:run --args="$PROJECT_PATH 'What tools are available in the agent framework?'"
# ./gradlew :mpp-ui:run --args="$PROJECT_PATH 'How are documents parsed and indexed?'"
# ./gradlew :mpp-ui:run --args="$PROJECT_PATH 'How does the system handle parsing errors?'"

