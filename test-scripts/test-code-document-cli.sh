#!/bin/bash

# Test script for DocumentCli with source code indexing

cd /Volumes/source/ai/autocrud

# Query directory containing DocQL code
PROJECT_PATH="/Volumes/source/ai/autocrud/mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/docql"
QUERY="What is DocQL and how does it work?"

echo "==============================================="
echo "Testing DocumentCli with Source Code Indexing"
echo "==============================================="
echo ""
echo "Project Path: $PROJECT_PATH"
echo "Query: $QUERY"
echo ""

# Build and run using Gradle
./gradlew :mpp-ui:run --args="$PROJECT_PATH \"$QUERY\""

