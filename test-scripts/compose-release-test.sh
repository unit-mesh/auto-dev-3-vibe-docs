#!/bin/bash

# Test script for mpp-ui compose release builds
# This script tests the build process locally before running GitHub Actions

set -e

echo "🚀 Starting Compose Release Test..."

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

# Clean previous builds
print_status "Cleaning previous builds..."
./gradlew :mpp-ui:clean

# Fix yarn lock issues if needed
print_status "Checking and fixing yarn lock..."
./gradlew kotlinUpgradeYarnLock || print_warning "Yarn lock upgrade failed, continuing..."

# Build mpp-core dependency first
print_status "Building mpp-core dependency..."
./gradlew :mpp-core:assemble
if [ $? -eq 0 ]; then
    print_success "mpp-core build completed"
else
    print_error "mpp-core build failed"
    exit 1
fi

# Test Android builds
print_status "Testing Android builds..."

print_status "Building Android Debug APK..."
./gradlew :mpp-ui:assembleDebug
if [ $? -eq 0 ]; then
    print_success "Android Debug APK build completed"
    if [ -f "mpp-ui/build/outputs/apk/debug/mpp-ui-debug.apk" ]; then
        print_success "Debug APK found: $(ls -lh mpp-ui/build/outputs/apk/debug/*.apk)"
    else
        print_warning "Debug APK not found in expected location"
        find mpp-ui/build -name "*.apk" -type f | head -5
    fi
else
    print_error "Android Debug APK build failed"
    exit 1
fi

print_status "Building Android Release APK..."
./gradlew :mpp-ui:assembleRelease
if [ $? -eq 0 ]; then
    print_success "Android Release APK build completed"
    if [ -f "mpp-ui/build/outputs/apk/release/mpp-ui-release-unsigned.apk" ]; then
        print_success "Release APK found: $(ls -lh mpp-ui/build/outputs/apk/release/*.apk)"
    else
        print_warning "Release APK not found in expected location"
        find mpp-ui/build -name "*.apk" -type f | head -5
    fi
else
    print_error "Android Release APK build failed"
    exit 1
fi

# Test Desktop builds (platform-specific)
print_status "Testing Desktop builds..."

case "$(uname -s)" in
    Darwin*)
        print_status "Building macOS DMG package..."
        ./gradlew :mpp-ui:packageDmg
        if [ $? -eq 0 ]; then
            print_success "macOS DMG build completed"
            find mpp-ui/build -name "*.dmg" -type f | head -3
        else
            print_error "macOS DMG build failed"
        fi
        ;;
    Linux*)
        print_status "Building Linux DEB package..."
        ./gradlew :mpp-ui:packageDeb
        if [ $? -eq 0 ]; then
            print_success "Linux DEB build completed"
            find mpp-ui/build -name "*.deb" -type f | head -3
        else
            print_error "Linux DEB build failed"
        fi
        ;;
    CYGWIN*|MINGW32*|MSYS*|MINGW*)
        print_status "Building Windows MSI package..."
        ./gradlew :mpp-ui:packageMsi
        if [ $? -eq 0 ]; then
            print_success "Windows MSI build completed"
            find mpp-ui/build -name "*.msi" -type f | head -3
        else
            print_error "Windows MSI build failed"
        fi
        ;;
    *)
        print_warning "Unknown platform: $(uname -s). Skipping desktop package build."
        ;;
esac

# Summary
print_status "Build test summary:"
echo "📱 Android APKs:"
find mpp-ui/build -name "*.apk" -type f | while read file; do
    echo "  - $(basename "$file"): $(ls -lh "$file" | awk '{print $5}')"
done

echo "🖥️  Desktop packages:"
find mpp-ui/build -name "*.dmg" -o -name "*.deb" -o -name "*.msi" -type f | while read file; do
    echo "  - $(basename "$file"): $(ls -lh "$file" | awk '{print $5}')"
done

print_success "🎉 Compose release test completed successfully!"
print_status "You can now create a tag starting with 'compose-' to trigger the GitHub Action"
print_status "Example: git tag compose-v1.0.0-test && git push origin compose-v1.0.0-test"
