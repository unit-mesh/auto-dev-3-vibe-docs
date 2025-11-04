#!/bin/bash

# Script to create a compose release tag and trigger GitHub Actions
# Usage: ./create-compose-release.sh [version]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
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

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ] || [ ! -d "mpp-ui" ]; then
    print_error "Please run this script from the project root directory"
    exit 1
fi

# Get version from argument or prompt user
if [ -n "$1" ]; then
    VERSION="$1"
else
    echo -n "Enter version (e.g., 1.0.0-test): "
    read VERSION
fi

# Validate version format
if [ -z "$VERSION" ]; then
    print_error "Version cannot be empty"
    exit 1
fi

TAG_NAME="compose-v${VERSION}"

print_status "Creating compose release: $TAG_NAME"

# Check if tag already exists
if git tag -l | grep -q "^${TAG_NAME}$"; then
    print_error "Tag $TAG_NAME already exists"
    print_status "Existing tags:"
    git tag -l | grep "^compose-" | tail -5
    exit 1
fi

# Check if there are uncommitted changes
if ! git diff-index --quiet HEAD --; then
    print_warning "You have uncommitted changes:"
    git status --porcelain
    echo -n "Continue anyway? (y/N): "
    read CONTINUE
    if [ "$CONTINUE" != "y" ] && [ "$CONTINUE" != "Y" ]; then
        print_status "Aborted by user"
        exit 1
    fi
fi

# Run local test first (optional)
echo -n "Run local build test first? (Y/n): "
read RUN_TEST
if [ "$RUN_TEST" != "n" ] && [ "$RUN_TEST" != "N" ]; then
    print_status "Running local build test..."
    if ./docs/test-scripts/compose-release-test.sh; then
        print_success "Local build test passed"
    else
        print_error "Local build test failed"
        echo -n "Continue with release anyway? (y/N): "
        read CONTINUE_ANYWAY
        if [ "$CONTINUE_ANYWAY" != "y" ] && [ "$CONTINUE_ANYWAY" != "Y" ]; then
            print_status "Aborted due to test failure"
            exit 1
        fi
    fi
fi

# Create and push tag
print_status "Creating tag: $TAG_NAME"
git tag "$TAG_NAME"

print_status "Pushing tag to origin..."
git push origin "$TAG_NAME"

print_success "🎉 Release tag created and pushed successfully!"
print_status "GitHub Actions workflow should start automatically"
print_status "Monitor the build at: https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[:/]\([^/]*\/[^/]*\).*/\1/' | sed 's/\.git$//')/actions"

print_status "To delete this tag if needed:"
print_status "  git tag -d $TAG_NAME"
print_status "  git push origin --delete $TAG_NAME"
