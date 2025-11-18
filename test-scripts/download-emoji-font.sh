#!/bin/bash

# Download Noto Color Emoji font for WASM support
# This script downloads the emoji font required for UTF-8 support in WASM builds

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FONT_DIR="$PROJECT_ROOT/mpp-ui/src/commonMain/resources/fonts"

echo "=========================================="
echo "Downloading Noto Color Emoji Font"
echo "=========================================="
echo ""

# Create fonts directory if it doesn't exist
mkdir -p "$FONT_DIR"

# Download Noto Color Emoji
echo "Downloading NotoColorEmoji.ttf..."
EMOJI_URL="https://github.com/googlefonts/noto-emoji/raw/main/fonts/NotoColorEmoji.ttf"
EMOJI_TARGET="$FONT_DIR/NotoColorEmoji.ttf"

if [ -f "$EMOJI_TARGET" ]; then
    echo "⚠️  NotoColorEmoji.ttf already exists."
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Skipping download."
        exit 0
    fi
fi

# Download with curl or wget
if command -v curl &> /dev/null; then
    curl -L -o "$EMOJI_TARGET" "$EMOJI_URL"
elif command -v wget &> /dev/null; then
    wget -O "$EMOJI_TARGET" "$EMOJI_URL"
else
    echo "❌ Error: Neither curl nor wget is available."
    echo "Please install curl or wget and try again."
    exit 1
fi

# Verify download
if [ -f "$EMOJI_TARGET" ]; then
    FILE_SIZE=$(du -h "$EMOJI_TARGET" | cut -f1)
    echo ""
    echo "✅ Font downloaded successfully!"
    echo "   File: $EMOJI_TARGET"
    echo "   Size: $FILE_SIZE"
    echo ""
    echo "Next steps:"
    echo "1. Run: ./gradlew :mpp-ui:clean"
    echo "2. Run: ./gradlew :mpp-ui:wasmJsBrowserDistribution"
    echo "3. Test emoji support: 😀 🎉 ✅ ❌ 🚀"
else
    echo "❌ Error: Download failed."
    exit 1
fi

echo ""
echo "=========================================="
echo "Optional: Download CJK Font for Chinese/Japanese/Korean"
echo "=========================================="
echo ""
read -p "Do you want to download Noto Sans CJK for better CJK support? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Downloading NotoSansSC-Regular.otf (Chinese Simplified)..."
    CJK_URL="https://github.com/googlefonts/noto-cjk/raw/main/Sans/OTF/SimplifiedChinese/NotoSansSC-Regular.otf"
    CJK_TARGET="$FONT_DIR/NotoSansSC-Regular.otf"
    
    if command -v curl &> /dev/null; then
        curl -L -o "$CJK_TARGET" "$CJK_URL"
    else
        wget -O "$CJK_TARGET" "$CJK_URL"
    fi
    
    if [ -f "$CJK_TARGET" ]; then
        CJK_SIZE=$(du -h "$CJK_TARGET" | cut -f1)
        echo "✅ CJK font downloaded: $CJK_SIZE"
    fi
fi

echo ""
echo "=========================================="
echo "Font Setup Complete!"
echo "=========================================="

