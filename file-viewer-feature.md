# File Viewer Feature

## Overview

The agent UI now supports clicking on file paths in tool call items to view the file content with syntax highlighting.

## Features

### JVM Platform (Desktop)
- Uses [RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea) for professional syntax highlighting
- Supports 30+ programming languages
- Features:
  - Syntax highlighting based on file extension
  - Code folding
  - Line numbers
  - 900x700 window size
  - Read-only mode by default

### Android Platform
- Placeholder implementation (to be completed)
- Currently logs file path to console

### JS Platform
- Placeholder implementation (to be completed)
- Currently logs file path to browser console

## Usage

1. When the agent performs a `read-file` or `write-file` operation, a small eye icon (👁️) appears next to the tool name
2. Click the eye icon to open the file viewer
3. The file viewer opens in a new window with syntax highlighting
4. Close the window when done

## Implementation Details

### File Path Extraction
- File paths are extracted from tool parameters for `ReadFile` and `WriteFile` tool types
- Stored in `ComposeRenderer.TimelineItem.ToolCallItem`

### Platform-Specific Viewers
- **Common**: `FileViewer` expect class defines the interface
- **JVM**: `FileViewer.jvm.kt` implements using RSyntaxTextArea
- **Android**: `FileViewer.android.kt` (placeholder)
- **JS**: `FileViewer.js.kt` (placeholder)

### Supported Languages (JVM)
Java, Kotlin, JavaScript, TypeScript, Python, XML, HTML, CSS, JSON, YAML, Markdown, SQL, Bash, C, C++, Go, Rust, Swift, Ruby, PHP, C#, Scala, Groovy, Dockerfile, and more.

## Future Enhancements

1. **Android Implementation**:
   - Use WebView with Monaco Editor or similar
   - Or integrate with system text editors

2. **JS Implementation**:
   - Integrate Monaco Editor or CodeMirror
   - Support for Node.js CLI environment

3. **Additional Features**:
   - Edit mode (not just read-only)
   - Search and replace within viewer
   - Line-by-line diff for write operations
   - Syntax error highlighting

