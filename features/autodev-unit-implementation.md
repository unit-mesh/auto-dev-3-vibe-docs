# AutoDev Unit - Implementation Specification

> **Status**: Draft  
> **Issue**: https://github.com/phodal/auto-dev/issues/526  
> **Author**: AI Assistant  
> **Date**: 2025-12-24

## Executive Summary

AutoDev Unit将 AI 生成的输出转化为**可逆、可执行的 Artifact**，支持从 _生成 → 执行 → 再编辑_ 的完整生命周期。

基于 Xiuper 现有架构（Kotlin Multiplatform、NanoDSL、Session 持久化），本文档提出一个分阶段实施方案。

---

## Architecture Overview

### Existing Infrastructure Analysis

| 组件 | 现状 | 可复用性 |
|------|------|---------|
| **NanoDSL** | AI-native UI DSL with Compose/React renderers | ⭐⭐⭐⭐⭐ |
| **Session Storage** | SQLite-based session persistence (SQLDelight) | ⭐⭐⭐⭐⭐ |
| **Shell Executor** | PTY-based command execution (pty4j) | ⭐⭐⭐⭐ |
| **WASM Build** | Compose for Web with webpack bundling | ⭐⭐⭐⭐ |
| **FileKit** | Cross-platform file operations | ⭐⭐⭐⭐ |
| **Mermaid/WebView** | JCEF-based HTML rendering | ⭐⭐⭐ |
| **DevIns Compiler** | Executable AI Agent scripting language | ⭐⭐⭐⭐⭐ |

### Proposed Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       AutoDev Unit System                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │   AI Agent   │───▶│ ArtifactUnit │───▶│   Runtime    │       │
│  │  (Coding)    │    │  Generator   │    │  (Executor)  │       │
│  └──────────────┘    └──────────────┘    └──────────────┘       │
│         │                   │                    │               │
│         │                   ▼                    │               │
│         │         ┌──────────────┐              │               │
│         │         │  Unit Bundle │              │               │
│         │         │ (.adunit)    │              │               │
│         │         │ ┌──────────┐ │              │               │
│         └────────▶│ │ Source   │ │◀─────────────┘               │
│                   │ │ Context  │ │                               │
│                   │ │ Metadata │ │                               │
│                   │ └──────────┘ │                               │
│                   └──────────────┘                               │
│                          │                                       │
│                          ▼                                       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Export Targets                         │   │
│  ├──────────────────┬──────────────────┬───────────────────┤   │
│  │   Web Artifact   │  Python Script   │   NanoDSL App     │   │
│  │   (.exe/.app)    │  (.exe/.app)     │   (.exe/.app)     │   │
│  │   - Tauri/CEF    │  - PEP 723       │   - Compose       │   │
│  │   - HTML/JS/CSS  │  - UV/PyInstall  │   - Desktop       │   │
│  └──────────────────┴──────────────────┴───────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Use Case 1: Web Artifact (HTML/JS)

### 1.1 Technical Approach

**Option A: Self-Contained HTML (Recommended for Phase 1)**

将生成的 HTML/JS/CSS 打包成单文件 HTML，内嵌 Base64 编码的上下文数据：

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="autodev-unit-version" content="1.0">
  <!-- Embedded context for re-editing -->
  <script type="application/json" id="__AUTODEV_CONTEXT__">
    {
      "version": "1.0",
      "generatedAt": "2025-12-24T10:00:00Z",
      "sessionId": "abc123",
      "conversation": [
        {"role": "user", "content": "Create a dashboard..."},
        {"role": "assistant", "content": "..."}
      ],
      "originalPrompt": "...",
      "sourceCode": {
        "html": "...",
        "js": "...",
        "css": "..."
      }
    }
  </script>
</head>
<body>
  <!-- Generated UI -->
</body>
</html>
```

**Option B: Desktop Executable (Phase 2)**

使用 Tauri (Rust) 或 KCEF (Compose) 将 Web 应用打包为桌面可执行文件：

```kotlin
// mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/artifact/WebArtifactPackager.kt
class WebArtifactPackager {
    suspend fun packageToExecutable(
        htmlContent: String,
        context: ArtifactContext,
        targetFormat: TargetFormat // DMG, MSI, DEB, EXE
    ): File {
        // 1. Create temporary directory with HTML + assets
        // 2. Generate Tauri configuration
        // 3. Build native executable
        // 4. Embed context in executable metadata
    }
}
```

### 1.2 Implementation in Xiuper

**New Module: `mpp-artifact`**

```
mpp-artifact/
├── src/
│   ├── commonMain/kotlin/cc/unitmesh/artifact/
│   │   ├── ArtifactContext.kt          # Serializable context
│   │   ├── ArtifactBundle.kt           # Bundle format
│   │   └── ArtifactMetadata.kt         # Version, timestamp, etc.
│   ├── jvmMain/kotlin/cc/unitmesh/artifact/
│   │   ├── WebArtifactPackager.kt      # JVM desktop packaging
│   │   └── TauriBuilder.kt             # Tauri integration
│   └── jsMain/kotlin/cc/unitmesh/artifact/
│       └── WebArtifactExporter.kt      # Browser download
```

**Integration with Existing Renderer:**

```kotlin
// Extend existing ComposeRenderer in mpp-ui
class ComposeRenderer : CodingAgentRenderer {
    // Existing code...
    
    /**
     * Export current session as Web Artifact
     */
    fun exportAsWebArtifact(format: WebArtifactFormat): ArtifactBundle {
        val context = ArtifactContext(
            sessionId = currentSessionId,
            conversation = getTimelineSnapshot(),
            originalPrompt = initialPrompt,
            generatedContent = extractGeneratedContent()
        )
        return WebArtifactBundle.create(context, format)
    }
}
```

### 1.3 Re-import Flow

```kotlin
// mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/artifact/ArtifactImporter.kt
class ArtifactImporter {
    /**
     * Import artifact and restore editing context
     */
    suspend fun importArtifact(file: PlatformFile): ImportResult {
        val bundle = ArtifactBundle.parse(file)
        
        // Restore session
        val session = sessionRepository.createSession(
            task = bundle.context.originalPrompt,
            metadata = SessionMetadata(
                importedFrom = bundle.metadata.source,
                originalSessionId = bundle.context.sessionId
            )
        )
        
        // Load conversation history
        bundle.context.conversation.forEach { message ->
            chatHistoryManager.addMessage(message)
        }
        
        return ImportResult(
            sessionId = session.id,
            restoredMessages = bundle.context.conversation.size,
            sourceCode = bundle.sourceCode
        )
    }
}
```

---

## Use Case 2: Python Script Artifact

### 2.1 Technical Approach

**PEP 723 Inline Script Metadata:**

```python
# /// script
# requires-python = ">=3.11"
# dependencies = [
#   "requests>=2.28.0",
#   "pandas>=2.0.0",
# ]
# [tool.autodev-unit]
# version = "1.0"
# session-id = "abc123"
# generated-at = "2025-12-24T10:00:00Z"
# ///

import requests
import pandas as pd

# AI-generated script content...
```

**Executable Packaging Options:**

| Tool | Platform | Size | Speed |
|------|----------|------|-------|
| **uv** (PEP 723) | All | ~50MB | ⚡ Fast |
| **PyInstaller** | All | ~80MB | Medium |
| **Nuitka** | All | ~40MB | Slow (compile) |
| **PyOxidizer** | All | ~60MB | Slow (Rust) |

**Recommended: UV Integration**

```kotlin
// mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/artifact/PythonArtifactPackager.kt
class PythonArtifactPackager {
    
    /**
     * Package Python script with dependencies using UV
     */
    suspend fun packageWithUV(
        script: String,
        context: ArtifactContext
    ): File {
        // 1. Parse PEP 723 metadata from script
        val metadata = PEP723Parser.parse(script)
        
        // 2. Inject AutoDev context into script header
        val enrichedScript = injectContext(script, context)
        
        // 3. Use UV to create self-contained executable
        val result = shellExecutor.execute(
            "uv tool install --script ${enrichedScript.absolutePath}",
            ShellExecutionConfig(timeoutMs = 300_000)
        )
        
        return result.outputFile
    }
}
```

### 2.2 Implementation in Xiuper

**Extend Coding Agent for Python Artifacts:**

```kotlin
// mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/subagent/PythonArtifactAgent.kt
class PythonArtifactAgent(
    private val llmService: LLMService,
    private val shellExecutor: ShellExecutor
) : SubAgent {
    
    override suspend fun execute(
        input: PythonArtifactInput,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        onProgress("🐍 Generating Python script...")
        
        // 1. Generate script with PEP 723 metadata
        val script = generateScript(input)
        
        // 2. Validate syntax and dependencies
        val validation = validateScript(script)
        if (!validation.isValid) {
            return retryGeneration(input, validation.errors, onProgress)
        }
        
        // 3. Test execution in sandbox
        val testResult = sandboxExecutor.execute(script)
        
        // 4. Package as artifact
        val artifact = packager.packageWithUV(script, input.context)
        
        return ToolResult.AgentResult(
            success = true,
            content = "Script generated and packaged: ${artifact.absolutePath}"
        )
    }
}
```

---

## Use Case 3: NanoDSL App Artifact (Bonus)

利用现有 NanoDSL 系统，直接生成 Compose Desktop 应用：

```kotlin
// xiuper-ui/src/commonMain/kotlin/cc/unitmesh/xuiper/artifact/NanoAppPackager.kt
class NanoAppPackager {
    
    /**
     * Package NanoDSL component as standalone desktop app
     */
    suspend fun packageAsDesktopApp(
        nanoSource: String,
        context: ArtifactContext
    ): File {
        // 1. Parse NanoDSL to IR
        val ir = NanoDSL.toIR(nanoSource)
        
        // 2. Generate Compose code from IR
        val composeCode = ComposeCodeGenerator.generate(ir)
        
        // 3. Create standalone Compose project
        val project = createStandaloneProject(composeCode, context)
        
        // 4. Build with Gradle (already configured in mpp-ui)
        return buildDesktopApp(project)
    }
}
```

---

## Implementation Phases

### Phase 1: Self-Contained HTML Export (2-3 weeks)

**Tasks:**
1. [ ] Create `ArtifactContext` data class with serialization
2. [ ] Implement HTML embedding with context JSON
3. [ ] Add "Export as HTML" button to Sketch toolbar
4. [ ] Implement drag-and-drop import for HTML files
5. [ ] Add context extraction from imported HTML
6. [ ] Restore session from imported context

**Files to modify:**
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ComposeRenderer.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/sketch/SketchRenderer.kt`
- `mpp-idea/src/main/kotlin/cc/unitmesh/devins/idea/renderer/sketch/IdeaSketchRenderer.kt`

### Phase 2: Python Script Artifact (2-3 weeks)

**Tasks:**
1. [ ] Implement PEP 723 parser and generator
2. [ ] Add UV integration for packaging
3. [ ] Create Python sandbox executor
4. [ ] Implement context injection in script header
5. [ ] Add Python artifact export to Coding Agent
6. [ ] Test on all platforms (JVM, JS CLI)

**New files:**
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/artifact/PythonArtifactPackager.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/artifact/PEP723Parser.kt`

### Phase 3: Desktop Executable Packaging (3-4 weeks)

**Tasks:**
1. [ ] Integrate Tauri for Web artifacts
2. [ ] Implement UV tool packaging for Python
3. [ ] Create native installer generation
4. [ ] Add executable metadata embedding
5. [ ] Implement re-import from executable

**New module:**
- `mpp-artifact/` - Cross-platform artifact packaging

### Phase 4: NanoDSL App Artifact (2-3 weeks)

**Tasks:**
1. [ ] Create Compose code generator from NanoIR
2. [ ] Generate standalone Compose project
3. [ ] Integrate with existing desktop build
4. [ ] Add context preservation for NanoDSL apps

---

## API Design

### ArtifactBundle Format (.adunit)

```kotlin
@Serializable
data class ArtifactBundle(
    val version: String = "1.0",
    val type: ArtifactType,
    val metadata: ArtifactMetadata,
    val context: ArtifactContext,
    val content: ArtifactContent
)

@Serializable
enum class ArtifactType {
    WEB_HTML,      // Self-contained HTML
    WEB_EXE,       // Tauri executable
    PYTHON_SCRIPT, // PEP 723 script
    PYTHON_EXE,    // UV/PyInstaller executable
    NANODSL_APP    // Compose desktop app
}

@Serializable
data class ArtifactContext(
    val sessionId: String,
    val conversation: List<ChatMessage>,
    val originalPrompt: String,
    val variables: Map<String, String> = emptyMap(),
    val toolCalls: List<ToolCallRecord> = emptyList()
)

@Serializable
data class ArtifactMetadata(
    val generatedAt: Long,
    val generator: String = "AutoDev Xiuper",
    val generatorVersion: String,
    val llmModel: String?,
    val sourceHash: String // For integrity check
)
```

### Renderer Integration

```kotlin
// Add to CodingAgentRenderer interface
interface CodingAgentRenderer {
    // Existing methods...
    
    /**
     * Export current session as artifact
     */
    fun exportArtifact(type: ArtifactType): ArtifactBundle
    
    /**
     * Import artifact and restore context
     */
    fun importArtifact(bundle: ArtifactBundle)
    
    /**
     * Check if current content is exportable
     */
    fun canExport(type: ArtifactType): Boolean
}
```

---

## Platform-Specific Considerations

| Platform | Web Artifact | Python Script | NanoDSL App |
|----------|-------------|---------------|-------------|
| **JVM Desktop** | ✅ Tauri/KCEF | ✅ UV/PyInstaller | ✅ Native |
| **IDEA Plugin** | ✅ JCEF | ✅ Shell | ❌ |
| **CLI (Node.js)** | ✅ HTML only | ✅ Shell | ❌ |
| **WASM Web** | ✅ Download | ❌ | ❌ |
| **Android** | ✅ HTML only | ❌ | ✅ Native |
| **iOS** | ✅ HTML only | ❌ | ✅ Native |

---

## Security Considerations

1. **Sandbox Execution**: All generated scripts run in sandboxed environment
2. **Dependency Validation**: Verify Python packages against known-safe list
3. **Context Encryption**: Optional encryption for sensitive conversation data
4. **Signature Verification**: Sign artifacts to prevent tampering

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Export time (HTML) | < 2s |
| Export time (EXE) | < 60s |
| Import context restoration | 100% accuracy |
| Re-edit capability | Full conversation history |
| Cross-platform compatibility | 100% |

---

## References

- [PEP 723 - Inline script metadata](https://peps.python.org/pep-0723/)
- [Tauri - Build smaller, faster apps](https://tauri.app/)
- [UV - Fast Python package installer](https://github.com/astral-sh/uv)
- [NanoDSL README](../xiuper-ui/README.md)
- [Session Management SQL](../../mpp-ui/src/commonMain/sqldelight/cc/unitmesh/devins/db/Session.sq)

