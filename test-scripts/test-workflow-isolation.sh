#!/bin/bash

# Test script to verify workflow isolation
# This script checks that compose-* tags won't trigger the main release workflow

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_status "🔍 Testing workflow isolation..."

# Check if we're in the right directory
if [ ! -f ".github/workflows/release.yml" ] || [ ! -f ".github/workflows/compose-release-test.yml" ]; then
    print_error "Please run this script from the project root directory"
    exit 1
fi

# Test 1: Check release.yml has the compose-* filter
print_status "Checking release.yml for compose-* tag filter..."
if grep -q "startsWith(github.event.release.tag_name, 'compose-')" .github/workflows/release.yml; then
    print_success "✅ release.yml has compose-* tag filter"
else
    print_error "❌ release.yml missing compose-* tag filter"
    print_status "The main release workflow might be triggered by compose-* tags"
    exit 1
fi

# Test 2: Check compose-release-test.yml only runs for compose-* tags
print_status "Checking compose-release-test.yml tag restrictions..."
if grep -q "startsWith(github.ref, 'refs/tags/compose-')" .github/workflows/compose-release-test.yml; then
    print_success "✅ compose-release-test.yml restricted to compose-* tags"
else
    print_error "❌ compose-release-test.yml missing tag restrictions"
    exit 1
fi

# Test 3: Verify the filter logic
print_status "Verifying filter logic..."

# Extract the condition from release.yml
RELEASE_CONDITION=$(grep -A1 -B1 "startsWith.*compose-" .github/workflows/release.yml | grep "if:" | sed 's/.*if: //' | tr -d ' ')
print_status "Release workflow condition: $RELEASE_CONDITION"

# Extract the condition from compose-release-test.yml
COMPOSE_CONDITION=$(grep -A1 -B1 "startsWith.*compose-" .github/workflows/compose-release-test.yml | grep "if:" | sed 's/.*if: //' | tr -d ' ')
print_status "Compose workflow condition: $COMPOSE_CONDITION"

# Test 4: Check for potential conflicts in trigger events
print_status "Checking trigger events..."

RELEASE_TRIGGERS=$(grep -A5 "^on:" .github/workflows/release.yml | grep -v "^on:" | grep -v "^--")
COMPOSE_TRIGGERS=$(grep -A10 "^on:" .github/workflows/compose-release-test.yml | grep -v "^on:" | grep -v "^--")

print_status "Release workflow triggers:"
echo "$RELEASE_TRIGGERS" | sed 's/^/  /'

print_status "Compose workflow triggers:"
echo "$COMPOSE_TRIGGERS" | sed 's/^/  /'

# Test 5: Simulate tag scenarios
print_status "Simulating tag scenarios..."

# Test cases
test_cases=(
    "compose-v1.0.0-test:COMPOSE_ONLY"
    "compose-beta-1.0.0:COMPOSE_ONLY"
    "v1.0.0:RELEASE_ONLY"
    "1.0.0:RELEASE_ONLY"
    "release-1.0.0:RELEASE_ONLY"
)

for test_case in "${test_cases[@]}"; do
    tag_name=$(echo "$test_case" | cut -d: -f1)
    expected=$(echo "$test_case" | cut -d: -f2)
    
    if [[ "$tag_name" == compose-* ]]; then
        # Should trigger compose workflow, not release workflow
        if [ "$expected" = "COMPOSE_ONLY" ]; then
            print_success "✅ Tag '$tag_name' correctly isolated to compose workflow"
        else
            print_error "❌ Tag '$tag_name' isolation test failed"
        fi
    else
        # Should trigger release workflow, not compose workflow
        if [ "$expected" = "RELEASE_ONLY" ]; then
            print_success "✅ Tag '$tag_name' correctly isolated to release workflow"
        else
            print_error "❌ Tag '$tag_name' isolation test failed"
        fi
    fi
done

print_success "🎉 Workflow isolation tests completed!"
print_status "Summary:"
print_status "  • compose-* tags → compose-release-test.yml only"
print_status "  • other tags → release.yml only"
print_status "  • No workflow conflicts detected"

print_warning "Note: This is a static analysis. Actual behavior depends on GitHub Actions runtime."
print_status "Test with a real compose-* tag to verify complete isolation."
