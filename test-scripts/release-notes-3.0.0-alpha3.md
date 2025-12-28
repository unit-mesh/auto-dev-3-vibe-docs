# Release Notes - v3.0.0-alpha3

## Highlights

This release introduces the **AutoDev Unit Artifact System** - a major new feature for creating reversible, executable artifacts with live preview support.

## New Features

### AutoDev Unit Artifact System (#526, #528, #530)
- **`.unit` Bundle Format**: New artifact packaging format with ZIP-based serialization
- **Artifact Executor Framework**: Support for Node.js, Python, and Web artifacts
- **Live Preview**: Streaming preview with WebView integration (JVM/iOS/Android)
- **Process Manager**: Long-running artifact execution with start/stop controls
- **macOS File Association**: Double-click `.unit` files to open in AutoDev

### LLM-based Run Config Analysis (#531)
- **IDE-like Run Button**: Floating run button with dark theme support
- **Cross-platform Process Output**: Real-time process output reading with tooltip support
- **Compact RunOutputDock UI**: Reduced padding and smaller text styles

### NanoDSL Language Support (#524, #525)
- **Syntax Highlighting**: Full syntax highlighting for NanoDSL in IntelliJ IDEA
- **TravelPlan DSL**: Grammar support for TravelPlan DSL features
- **Split Editor**: Preview editor with enhanced syntax highlighting

### NanoUI Component System
- **Jewel NanoUI Components**: New components for IntelliJ IDEA renderer
- **Platform-independent Theme System**: Cross-platform theming support
- **NanoNodeRegistry**: Platform-specific renderer registries for stateful rendering
- **Stateful Session Abstraction**: Markdown inline parser for multi-platform UI

### Terminal & Shell Improvements
- **Long-running Shell Sessions**: Handle partial output and UI preservation
- **Process Termination Controls**: Clipboard functionality and termination controls
- **Dynamic Theme Support**: Terminal scrollbar with fullscreen dialog
- **Parallel Tool Execution**: Improved result formatting

## Improvements

### Desktop UI
- Compact markdown typography
- Content font scale control
- Enhanced error display with expandable notices
- De-emphasized retrieval tool calls
- Streaming markdown blocks rendered as plain text to prevent crashes

### Artifact System
- Console UI improvements (colors + repeat counter)
- Deduplicated console logs via idempotent console bridge
- Improved export and load-back support
- Streaming optimization with throttled content updates

### Cross-platform
- Android, iOS, and WASM implementations for ArtifactBundlePacker
- MCP backend platform-specific implementations
- expect/actual pattern for cross-platform file open handler

## Bug Fixes

- Fix markdown rendering crashes with defensive fallback and bounds checking
- Fix `.unit` bundle ZIP serialization
- Fix streaming on dangling `<` character
- Suppress noisy kmpJsBridge onCallback(-1) logs
- Fix console.log capture in artifact preview
- Remove "type": "module" from Node.js artifacts for CommonJS/ES module compatibility

## Breaking Changes

- GenAction feature temporarily disabled (being redesigned)
- Some NanoUI Compose renderer components removed in favor of new registry system

## Full Changelog

See [compare view](https://github.com/unit-mesh/auto-dev/compare/compose-v3.0.0-alpha2...compose-v3.0.0-alpha3) for all 104 commits.
