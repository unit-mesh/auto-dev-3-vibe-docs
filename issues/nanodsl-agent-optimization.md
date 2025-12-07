# NanoDSLAgent Optimization: Validation + Rendering

## Summary

Enhance `NanoDSLAgent` with two critical features:
1. **Validation & Retry**: Validate AI-generated code with NanoParser; retry if invalid
2. **UI Rendering**: Render generated NanoDSL code through `CodingAgentRenderer`

## Background

Currently, `NanoDSLAgent` generates NanoDSL code from natural language but:
- Does **not** validate the generated code is syntactically correct
- Does **not** render the result to the UI
- Returns raw LLM output without quality assurance

### Current Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           mpp-core (KMP)                            │
│  ┌──────────────┐                                                   │
│  │ NanoDSLAgent │ ──LLM──> Raw Code String ──────> ToolResult       │
│  └──────────────┘            (no validation)        (no rendering)  │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                         xuiper-ui (JVM-only)                        │
│  ┌────────────┐      ┌────────────┐      ┌──────────────┐          │
│  │ NanoParser │ ───> │  NanoIR    │ ───> │ HtmlRenderer │          │
│  │ (validate) │      │ Converter  │      │ (static)     │          │
│  └────────────┘      └────────────┘      └──────────────┘          │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                         mpp-ui (Platform Renderers)                 │
│  JVM: ComposeNanoRenderer    |    VSCode: NanoRenderer.tsx          │
│  (converts NanoIR → Compose) |    (converts NanoIR → React)         │
└─────────────────────────────────────────────────────────────────────┘
```

### Problem Statement

1. **No Validation**: LLM can generate syntactically invalid NanoDSL (bad indentation, unknown components)
2. **No Retry**: Failed generation is returned as-is without correction attempts  
3. **No UI Rendering**: Generated code is text only, never rendered visually
4. **Cross-Platform Challenge**: `NanoParser` is JVM-only, but `NanoDSLAgent` runs on JS/WASM/iOS/Android

---

## Proposed Solution

### Part 1: Validation & Retry Mechanism

#### 1.1 Create Platform-Agnostic Validation Interface

Create `mpp-core/src/commonMain/.../parser/NanoDSLValidator.kt`:

```kotlin
/**
 * Platform-agnostic NanoDSL validation result
 */
data class NanoDSLValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class ValidationError(
    val message: String,
    val line: Int,
    val suggestion: String? = null
)

/**
 * Basic NanoDSL validator - cross-platform
 * Performs lightweight syntax checks without full parsing
 */
expect class NanoDSLValidator() {
    fun validate(source: String): NanoDSLValidationResult
    fun parse(source: String): NanoDSLParseResult?
}

sealed class NanoDSLParseResult {
    data class Success(val irJson: String) : NanoDSLParseResult()
    data class Failure(val errors: List<ValidationError>) : NanoDSLParseResult()
}
```

#### 1.2 JVM Implementation (Full Validation)

`mpp-core/src/jvmMain/.../parser/NanoDSLValidator.jvm.kt`:

```kotlin
actual class NanoDSLValidator {
    private val parser = IndentParser()
    
    actual fun validate(source: String): NanoDSLValidationResult {
        val result = parser.validate(source)
        return NanoDSLValidationResult(
            isValid = result.isValid,
            errors = result.errors.map { ValidationError(it.message, it.line, it.suggestion) },
            warnings = result.warnings.map { it.message }
        )
    }
    
    actual fun parse(source: String): NanoDSLParseResult? {
        return when (val result = parser.parse(source)) {
            is ParseResult.Success -> {
                val ir = NanoIRConverter.convert(result.ast)
                NanoDSLParseResult.Success(Json.encodeToString(ir))
            }
            is ParseResult.Failure -> {
                NanoDSLParseResult.Failure(result.errors.map { 
                    ValidationError(it.message, it.line) 
                })
            }
        }
    }
}
```

#### 1.3 JS/WASM/iOS/Android Implementation (Basic Validation)

`mpp-core/src/commonMain/.../parser/NanoDSLValidator.common.kt`:

```kotlin
// Default implementation for non-JVM platforms
actual class NanoDSLValidator {
    actual fun validate(source: String): NanoDSLValidationResult {
        val errors = mutableListOf<ValidationError>()
        val lines = source.lines()
        
        // Basic checks
        if (lines.isEmpty() || lines.all { it.isBlank() }) {
            errors.add(ValidationError("Empty source", 0))
            return NanoDSLValidationResult(false, errors)
        }
        
        // Check component definition
        val firstNonBlank = lines.indexOfFirst { it.isNotBlank() }
        if (firstNonBlank >= 0) {
            val firstLine = lines[firstNonBlank].trim()
            if (!firstLine.startsWith("component ") || !firstLine.endsWith(":")) {
                errors.add(ValidationError(
                    "Missing or invalid component definition",
                    firstNonBlank + 1,
                    "Start with 'component ComponentName:'"
                ))
            }
        }
        
        // Check indentation
        lines.forEachIndexed { index, line ->
            if (line.isNotBlank()) {
                val indent = line.takeWhile { it == ' ' }.length
                if (indent % 4 != 0) {
                    errors.add(ValidationError(
                        "Inconsistent indentation (expected multiple of 4)",
                        index + 1,
                        "Use 4 spaces per level"
                    ))
                }
            }
        }
        
        return NanoDSLValidationResult(errors.isEmpty(), errors)
    }
    
    actual fun parse(source: String): NanoDSLParseResult? {
        // Non-JVM platforms cannot do full parsing
        // Return null to indicate parsing not available
        return null
    }
}
```

#### 1.4 Update NanoDSLAgent with Retry Logic

```kotlin
class NanoDSLAgent(...) {
    private val validator = NanoDSLValidator()
    private val maxRetries = 3
    
    override suspend fun execute(
        input: NanoDSLContext,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        var lastCode: String = ""
        var lastErrors: List<ValidationError> = emptyList()
        
        repeat(maxRetries) { attempt ->
            onProgress("Generation attempt ${attempt + 1}/$maxRetries")
            
            val prompt = if (attempt == 0) {
                buildPrompt(input)
            } else {
                buildRetryPrompt(input, lastCode, lastErrors)
            }
            
            val llmResponse = callLLM(prompt)
            val generatedCode = extractCode(llmResponse)
            lastCode = generatedCode
            
            // Validate
            val validationResult = validator.validate(generatedCode)
            
            if (validationResult.isValid) {
                // Try full parsing on JVM
                val parseResult = validator.parse(generatedCode)
                
                return ToolResult.AgentResult(
                    success = true,
                    content = generatedCode,
                    metadata = mapOf(
                        "attempts" to (attempt + 1).toString(),
                        "irJson" to (parseResult as? NanoDSLParseResult.Success)?.irJson.orEmpty(),
                        "hasIR" to (parseResult is NanoDSLParseResult.Success).toString()
                    )
                )
            }
            
            lastErrors = validationResult.errors
            onProgress("Validation failed: ${lastErrors.firstOrNull()?.message}")
        }
        
        // All retries exhausted
        return ToolResult.AgentResult(
            success = false,
            content = "Failed after $maxRetries attempts. Last errors:\n${lastErrors.joinToString("\n")}",
            metadata = mapOf("lastCode" to lastCode)
        )
    }
    
    private fun buildRetryPrompt(
        input: NanoDSLContext,
        previousCode: String,
        errors: List<ValidationError>
    ): String {
        return """
${buildPrompt(input)}

## Previous Attempt (INVALID - please fix):
```nanodsl
$previousCode
```

## Validation Errors:
${errors.joinToString("\n") { "- Line ${it.line}: ${it.message}" }}

Please generate corrected NanoDSL code that fixes these errors.
""".trim()
    }
}
```

---

### Part 2: UI Rendering Integration

#### 2.1 Add New Method to CodingAgentRenderer

Update `mpp-core/src/commonMain/.../render/CodingAgentRenderer.kt`:

```kotlin
interface CodingAgentRenderer {
    // ... existing methods ...
    
    /**
     * Render NanoDSL generated UI code.
     * Called when NanoDSLAgent generates valid UI code.
     *
     * @param source The NanoDSL source code
     * @param irJson Optional JSON IR for platforms that support full parsing
     * @param metadata Additional metadata (component name, generation stats)
     */
    fun renderNanoDSL(
        source: String,
        irJson: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        // Default: no-op for renderers that don't support NanoDSL
    }
}
```

#### 2.2 Add New TimelineItem

Update `mpp-core/src/commonMain/.../render/RendererModels.kt`:

```kotlin
sealed class TimelineItem(...) {
    // ... existing items ...
    
    /**
     * NanoDSL UI code item - displays generated UI code with optional live preview
     */
    data class NanoDSLItem(
        val source: String,
        val irJson: String? = null, // null on non-JVM platforms
        val componentName: String? = null,
        val generationAttempts: Int = 1,
        val isValid: Boolean = true,
        val errors: List<String> = emptyList(),
        override val timestamp: Long = Platform.getCurrentTimestamp(),
        override val id: String = generateId()
    ) : TimelineItem(timestamp, id)
}
```

#### 2.3 Update ComposeRenderer

`mpp-ui/src/commonMain/.../agent/ComposeRenderer.kt`:

```kotlin
class ComposeRenderer : BaseRenderer() {
    // ... existing code ...
    
    override fun renderNanoDSL(
        source: String,
        irJson: String?,
        metadata: Map<String, String>
    ) {
        _timeline.add(
            TimelineItem.NanoDSLItem(
                source = source,
                irJson = irJson,
                componentName = metadata["componentName"],
                generationAttempts = metadata["attempts"]?.toIntOrNull() ?: 1,
                isValid = metadata["hasIR"] == "true" || irJson != null
            )
        )
    }
}
```

#### 2.4 Create Compose UI Component

`mpp-ui/src/jvmMain/.../nano/NanoDSLTimelineItem.kt`:

```kotlin
@Composable
fun NanoDSLTimelineItem(
    item: TimelineItem.NanoDSLItem,
    modifier: Modifier = Modifier
) {
    var showPreview by remember { mutableStateOf(true) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Header with toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Widgets, "UI")
                Spacer(Modifier.width(8.dp))
                Text(
                    item.componentName ?: "Generated UI",
                    style = MaterialTheme.typography.titleMedium
                )
                if (item.generationAttempts > 1) {
                    Badge { Text("${item.generationAttempts} attempts") }
                }
            }
            
            Row {
                IconButton(onClick = { showPreview = !showPreview }) {
                    Icon(
                        if (showPreview) Icons.Default.Code else Icons.Default.Preview,
                        "Toggle view"
                    )
                }
            }
        }
        
        if (showPreview && item.irJson != null) {
            // Live UI Preview
            val ir = remember(item.irJson) { Json.decodeFromString<NanoIR>(item.irJson) }
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                StatefulNanoRenderer.Render(ir)
            }
        } else {
            // Source code view
            CodeBlock(
                code = item.source,
                language = "nanodsl",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

#### 2.5 Update TypeScript Renderers

`mpp-ui/src/jsMain/typescript/agents/render/BaseRenderer.ts`:

```typescript
abstract class BaseRenderer implements JsCodingAgentRenderer {
    // ... existing code ...
    
    /**
     * Render NanoDSL generated UI code
     * Default implementation - subclasses can override
     */
    renderNanoDSL(source: string, irJson?: string, metadata?: Record<string, string>): void {
        // Default: log to console
        console.log('Generated NanoDSL:', source);
    }
}
```

`mpp-ui/src/jsMain/typescript/agents/render/CliRenderer.ts`:

```typescript
class CliRenderer extends BaseRenderer {
    // ... existing code ...
    
    renderNanoDSL(source: string, irJson?: string, metadata?: Record<string, string>): void {
        const componentName = metadata?.componentName || 'UI Component';
        const attempts = metadata?.attempts || '1';
        
        this.outputContent(semanticChalk.heading(`Generated: ${componentName}`));
        if (parseInt(attempts) > 1) {
            this.outputContent(semanticChalk.warn(` (${attempts} attempts)`));
        }
        this.outputNewline();
        
        // Output code block
        this.outputContent(semanticChalk.codeBlock(source, 'nanodsl'));
        this.outputNewline();
    }
}
```

#### 2.6 Update VSCode Integration

`mpp-vscode/src/bridge/mpp-core.ts`:

```typescript
export class VSCodeRenderer {
    // ... existing code ...
    
    renderNanoDSL(source: string, irJson?: string, metadata?: Record<string, string>): void {
        this.postMessage({
            type: 'nanodsl',
            data: {
                source,
                irJson,
                componentName: metadata?.componentName,
                attempts: metadata?.attempts,
                hasIR: metadata?.hasIR === 'true'
            }
        });
    }
}
```

`mpp-vscode/webview/src/components/timeline/NanoDSLItem.tsx`:

```tsx
export const NanoDSLItem: React.FC<NanoDSLItemProps> = ({ data }) => {
    const [showPreview, setShowPreview] = useState(true);
    
    return (
        <div className="nano-dsl-item">
            <div className="header">
                <span className="icon">🎨</span>
                <span className="title">{data.componentName || 'Generated UI'}</span>
                {data.attempts > 1 && <span className="badge">{data.attempts} attempts</span>}
                <button onClick={() => setShowPreview(!showPreview)}>
                    {showPreview ? '< />' : '👁'}
                </button>
            </div>
            
            {showPreview && data.irJson ? (
                <div className="preview">
                    <NanoRenderer ir={JSON.parse(data.irJson)} />
                </div>
            ) : (
                <CodeBlock code={data.source} language="nanodsl" />
            )}
        </div>
    );
};
```

---

## Implementation Checklist

### Phase 1: Validation & Retry
- [ ] Create `NanoDSLValidator` expect/actual classes in mpp-core
- [ ] Implement JVM version with full parsing via xuiper-ui
- [ ] Implement basic validation for JS/WASM/iOS/Android
- [ ] Update `NanoDSLAgent` with retry logic
- [ ] Add error feedback in retry prompt
- [ ] Add tests for validation and retry

### Phase 2: Rendering
- [ ] Add `renderNanoDSL()` to `CodingAgentRenderer` interface
- [ ] Add `NanoDSLItem` to `TimelineItem` sealed class
- [ ] Implement in `ComposeRenderer`
- [ ] Implement in `JewelRenderer`
- [ ] Implement in TypeScript `BaseRenderer`
- [ ] Implement in `CliRenderer`
- [ ] Implement in `TuiRenderer`
- [ ] Implement in VSCode `VSCodeRenderer`
- [ ] Create `NanoDSLTimelineItem.kt` Compose component
- [ ] Create `NanoDSLItem.tsx` React component
- [ ] Add integration tests

### Documentation
- [ ] Update AGENTS.md with new renderer method
- [ ] Add NanoDSL rendering documentation

---

## Files to Modify

### mpp-core (Kotlin Multiplatform)
- `src/commonMain/.../render/CodingAgentRenderer.kt` - Add interface method
- `src/commonMain/.../render/RendererModels.kt` - Add TimelineItem
- `src/commonMain/.../subagent/NanoDSLAgent.kt` - Add validation & retry
- `src/commonMain/.../parser/NanoDSLValidator.kt` - New file (expect)
- `src/jvmMain/.../parser/NanoDSLValidator.jvm.kt` - New file (actual)
- `src/jsMain/.../parser/NanoDSLValidator.js.kt` - New file (actual)
- `src/jsMain/.../RendererExports.kt` - Export new method

### mpp-ui (Compose/TypeScript)
- `src/commonMain/.../agent/ComposeRenderer.kt` - Implement method
- `src/jvmMain/.../nano/NanoDSLTimelineItem.kt` - New file
- `src/jsMain/typescript/agents/render/BaseRenderer.ts` - Add method
- `src/jsMain/typescript/agents/render/CliRenderer.ts` - Implement
- `src/jsMain/typescript/agents/render/TuiRenderer.ts` - Implement

### mpp-idea (IntelliJ Plugin)
- `src/main/.../renderer/JewelRenderer.kt` - Implement method

### mpp-vscode (VSCode Extension)
- `src/bridge/mpp-core.ts` - Implement method
- `webview/src/components/timeline/NanoDSLItem.tsx` - New file

### xuiper-ui (JVM-only parser)
- No changes needed - already has NanoParser/NanoIR

---

## Related Issues

- Depends on: xuiper-ui NanoParser stability
- Enhances: NanoDSL generation quality
- Blocks: Full NanoDSL workflow integration

---

## Acceptance Criteria

1. **Validation Works**:
   - Valid NanoDSL code passes validation on all platforms
   - Invalid code returns meaningful error messages
   - JVM platforms perform full AST parsing

2. **Retry Works**:
   - Agent retries up to 3 times on validation failure
   - Retry prompt includes previous errors
   - Success after retry is tracked in metadata

3. **Rendering Works**:
   - ComposeRenderer shows live UI preview
   - CLI shows formatted code block
   - VSCode shows interactive preview
   - Non-JVM platforms show code only (no preview)

4. **Cross-Platform**:
   - All platforms can generate and validate NanoDSL
   - JVM platforms get full parsing + preview
   - Non-JVM platforms get basic validation + code display

