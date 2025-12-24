# Feature Proposal: AutoDev Unit – Reversible Artifact Builder / Skill

## Description

AutoDev Unit aims to turn AI-generated outputs into **reversible, executable artifacts**, enabling a full lifecycle from _generation → execution → re-editing_.

Instead of stopping at source code download, AutoDev Unit packages AI-generated results into runnable desktop artifacts and allows users to load them back later to restore the original generation context and continue iteration.

This proposal focuses on **three core use cases**.

---

## Use Case 1: Web Artifact (HTML / JS)

**As a user**,  
I want to generate a small Web application (dashboard, tool UI, visualizer) with AI, export it as a standalone desktop app, and later load it back to continue editing with full context.

### Expected Behavior

* AI generates a Web app (HTML / JS / CSS).
* User can:  
   * Preview and interact with it.  
   * Export it as a single executable file (`.exe` / `.app`).
* The exported app:  
   * Runs without external dependencies.  
   * Embeds its own source code and generation context.
* User can drag the executable back into AutoDev Unit:  
   * The original chat, prompt, and code are restored.  
   * User continues with: "Add a new chart", "Change layout", etc.

### Problem It Solves

* Web artifacts today are **one-way exports**.
* Once downloaded, the AI context is lost.
* Packaging Web apps usually requires heavy tooling (Electron/Tauri).

---

## Use Case 2: Python Script Artifact

**As a user**,  
I want to generate a Python script with AI, run it locally without setting up Python or dependencies, and later reload it to continue evolving the script.

### Expected Behavior

* AI generates a Python script (e.g. scraper, data processor, automation tool).
* Dependencies are declared inline (e.g. via PEP 723).
* User can:  
   * Run it instantly.  
   * Export it as a standalone executable.
* The executable:  
   * Bootstraps its runtime automatically.  
   * Requires no manual environment setup.
* Dragging the executable back into AutoDev Unit:  
   * Restores source code + chat history.  
   * Allows iterative changes like "Add CSV export" or "Optimize performance".

### Problem It Solves

* End users struggle with Python environments and dependency setup.
* Generated scripts lose context once downloaded.
* Existing packaging solutions are slow or heavyweight.

---

## Use Case 3: NanoDSL App Artifact (New!)

**As a user**,  
I want to generate a native desktop UI component using NanoDSL and export it as a standalone Compose Desktop application.

### Expected Behavior

* AI generates NanoDSL component (token-efficient, AI-friendly DSL)
* User can:
  * Preview UI in real-time with NanoDSL renderer
  * Export as native Compose Desktop app (.app / .exe / .deb)
* The app:
  * Runs natively without browser/runtime
  * Embeds NanoDSL source + generation context
* Re-importing restores full editing capability

---

## Core Idea

**Executable artifacts should be reversible.**

An exported artifact is not a dead binary, but a container that embeds:

* Source code
* Dependency metadata
* Original generation context

This enables a closed-loop workflow:  
**Generate → Run → Export → Reload → Iterate**

---

## Implementation Plan (Based on Xiuper Codebase Analysis)

### Existing Infrastructure Leverage

| Component | Status | Reuse for AutoDev Unit |
|-----------|--------|------------------------|
| **NanoDSL** (`xiuper-ui/`) | ✅ Stable | UI artifact generation, Compose renderer |
| **Session Storage** (SQLDelight) | ✅ Stable | Context persistence |
| **ComposeRenderer** | ✅ Stable | Export/import integration |
| **Shell Executor** (PTY) | ✅ Stable | Python script execution |
| **FileKit** | ✅ Stable | Cross-platform file I/O |
| **WASM Build** | ✅ Stable | Web artifact distribution |

### Artifact Bundle Format (.adunit)

```kotlin
@Serializable
data class ArtifactBundle(
    val version: String = "1.0",
    val type: ArtifactType,           // WEB_HTML, PYTHON_SCRIPT, NANODSL_APP
    val metadata: ArtifactMetadata,   // Timestamp, generator, model info
    val context: ArtifactContext,     // Session ID, conversation, prompt
    val content: ArtifactContent      // Source code, assets
)
```

### Phase 1: Self-Contained HTML Export (2-3 weeks)

**Goal**: Export Web artifacts as single HTML files with embedded context

```html
<script type="application/json" id="__AUTODEV_CONTEXT__">
{
  "sessionId": "abc123",
  "conversation": [...],
  "originalPrompt": "Create a dashboard..."
}
</script>
```

**Tasks**:
- [ ] Create `ArtifactContext` serialization
- [ ] Implement HTML embedding with context JSON
- [ ] Add "Export as Artifact" to Sketch toolbar
- [ ] Implement drag-and-drop import
- [ ] Context extraction and session restoration

**Files to modify**:
- `mpp-ui/src/commonMain/.../ComposeRenderer.kt`
- `mpp-idea/src/main/.../IdeaSketchRenderer.kt`

### Phase 2: Python Script Artifact (2-3 weeks)

**Goal**: Generate executable Python scripts with PEP 723 metadata

```python
# /// script
# requires-python = ">=3.11"
# dependencies = ["requests>=2.28.0"]
# [tool.autodev-unit]
# session-id = "abc123"
# ///
```

**Tasks**:
- [ ] PEP 723 parser/generator
- [ ] UV integration for packaging
- [ ] Python sandbox executor
- [ ] Context injection in script header

**New files**:
- `mpp-core/.../PythonArtifactPackager.kt`
- `mpp-core/.../PEP723Parser.kt`

### Phase 3: Desktop Executable (3-4 weeks)

**Goal**: Package artifacts as native executables

**Options**:
- **Web**: Tauri (Rust) or KCEF (Compose)
- **Python**: UV tool + PyInstaller fallback
- **NanoDSL**: Native Compose Desktop build

### Phase 4: NanoDSL App Artifact (2-3 weeks)

**Goal**: Export NanoDSL components as standalone Compose apps

**Tasks**:
- [ ] Compose code generator from NanoIR
- [ ] Standalone project template
- [ ] Integration with existing desktop build

---

## Platform Support Matrix

| Platform | Web Artifact | Python Script | NanoDSL App |
|----------|-------------|---------------|-------------|
| **JVM Desktop** | ✅ Tauri/KCEF | ✅ UV/PyInstaller | ✅ Native |
| **IDEA Plugin** | ✅ JCEF | ✅ Shell | ❌ |
| **CLI (Node.js)** | ✅ HTML only | ✅ Shell | ❌ |
| **WASM Web** | ✅ Download | ❌ | ❌ |
| **Android** | ✅ HTML only | ❌ | ✅ Native |
| **iOS** | ✅ HTML only | ❌ | ✅ Native |

---

## API Extension for Renderer System

Following the existing renderer pattern (see `AGENTS.md`), add to `CodingAgentRenderer`:

```kotlin
interface CodingAgentRenderer {
    // Existing methods...
    
    /** Export current session as artifact */
    fun exportArtifact(type: ArtifactType): ArtifactBundle
    
    /** Import artifact and restore context */
    fun importArtifact(bundle: ArtifactBundle)
    
    /** Check if current content is exportable */
    fun canExport(type: ArtifactType): Boolean
}
```

**Implementations to update**:
- `DefaultCodingAgentRenderer`
- `ComposeRenderer`
- `JewelRenderer` (IDEA)
- `ServerSideRenderer`
- `JsRendererAdapter`
- TypeScript: `BaseRenderer.ts`, `CliRenderer.ts`

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Export time (HTML) | < 2s |
| Export time (EXE) | < 60s |
| Import context restoration | 100% accuracy |
| Re-edit capability | Full conversation history |

---

## References

- [PEP 723 - Inline script metadata](https://peps.python.org/pep-0723/)
- [Tauri - Build smaller, faster apps](https://tauri.app/)
- [UV - Fast Python package installer](https://github.com/astral-sh/uv)
- [NanoDSL Architecture](../docs/xiuper-ui/README.md)
- [Implementation Spec](../docs/features/autodev-unit-implementation.md)

---

## Discussion Points

1. **Bundle Format**: Should we use `.adunit` (custom) or leverage existing formats (`.zip`, `.json`)?
2. **Executable Size**: Tauri (~5MB) vs Electron (~100MB) vs KCEF (~50MB)?
3. **Python Runtime**: Embedded Python (~30MB) vs require user to install UV?
4. **Security**: How to handle sensitive conversation data in exported artifacts?

