# DevIn Block Rendering

## Overview

The `SketchRenderer` now supports rendering DevIn blocks (language id = `devin`) as `CombinedToolItem` components when the block is complete. This allows LLM tool calls to be displayed with a rich, interactive UI.

## How It Works

1. **Parsing**: When a code fence with language id `devin` is detected, the content is parsed using `ToolCallParser`
2. **Tool Call Extraction**: The parser extracts tool calls from within the `<devin>` tags
3. **Rendering**: When `isComplete = true`, each tool call is rendered using `CombinedToolItem`

## Supported Tool Call Formats

### Format 1: Tool Call with JSON Parameters

```
<devin>
/read-file

```json
{
  "path": "mpp-viewer-web/src/commonMain/kotlin/cc/unitmesh/viewer/web/MermaidRenderer.kt"
}
```

</devin>
```

### Format 2: Tool Call with Key-Value Parameters

```
<devin>
/read-file path="config.yaml"
</devin>
```

### Format 3: Shell Command

```
<devin>
/shell command="./gradlew build"
</devin>
```

### Format 4: Multiple Parameters

```
<devin>
/grep pattern="ToolCall" path="mpp-core/"
</devin>
```

## Example Usage

When an LLM streams a response containing a DevIn block:

```markdown
Let me read the file for you:

<devin>
/read-file

```json
{
  "path": "src/main/kotlin/Example.kt"
}
```

</devin>
```

The `SketchRenderer` will:

1. Parse the DevIn block using `CodeFence.parseAll()`
2. Extract the tool call using `ToolCallParser`
3. Render it using `CombinedToolItem` with:
   - Tool name: "read-file"
   - Details: "path=src/main/kotlin/Example.kt"
   - Success status: null (indicating it's pending/executing)

## Components

- **`DevInBlockRenderer`**: Main renderer for DevIn blocks
  - Located: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/sketch/DevInBlockRenderer.kt`
  - Handles parsing and rendering of tool calls within DevIn blocks
  - **Path Resolution**: Automatically resolves relative file paths to absolute paths using the current workspace root
    - Relative paths like `src/Example.kt` are converted to `/workspace/root/src/Example.kt`
    - Absolute paths (starting with `/` or `C:/`) are kept as-is
    - Uses `WorkspaceManager.currentWorkspace?.rootPath` to get the workspace root

- **`CombinedToolItem`**: UI component for displaying tool calls
  - Located: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ToolCallItem.kt`
  - Shows tool name, parameters, status, output, etc.
  - Receives absolute file paths for proper file viewing functionality

- **`ToolCallParser`**: Parser for extracting tool calls
  - Located: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/parser/ToolCallParser.kt`
  - Extracts tool calls from DevIn blocks

## Integration with SketchRenderer

The `SketchRenderer` now includes a case for `"devin"` language id:

```kotlin
"devin" -> {
    if (fence.text.isNotBlank()) {
        DevInBlockRenderer(
            devinContent = fence.text,
            isComplete = blockIsComplete,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
```

## Testing

Tests are located in:
- `mpp-ui/src/commonTest/kotlin/cc/unitmesh/devins/ui/compose/sketch/DevInBlockRendererTest.kt`

Run tests with:
```bash
./gradlew :mpp-ui:jvmTest --tests "DevInBlockRendererTest"
```

## Path Resolution

The `DevInBlockRenderer` automatically resolves relative file paths to absolute paths using the current workspace root. This ensures that file operations (like ReadFile, WriteFile) work correctly even when the LLM provides relative paths.

### How It Works

1. **Get Workspace Root**: Retrieves the current workspace root path from `WorkspaceManager.currentWorkspace?.rootPath`
2. **Check Path Type**:
   - **Absolute paths** (starting with `/` or matching `C:/` pattern): Returned as-is
   - **Relative paths**: Combined with workspace root
3. **Path Combination**: Joins workspace root and relative path with appropriate separator

### Examples

| Workspace Root | LLM Provided Path | Resolved Path |
|----------------|-------------------|---------------|
| `/Users/test/project` | `src/Example.kt` | `/Users/test/project/src/Example.kt` |
| `/Users/test/project` | `/absolute/path/File.kt` | `/absolute/path/File.kt` |
| `C:/Users/test/project` | `src/Example.kt` | `C:/Users/test/project/src/Example.kt` |
| `C:/Users/test/project` | `D:/other/File.kt` | `D:/other/File.kt` |

### Why This Matters

When an LLM returns a tool call like:

```json
{
  "path": "mpp-viewer-web/src/commonMain/kotlin/Example.kt"
}
```

The relative path needs to be resolved to the full absolute path for:
- **File Viewer**: Opening and displaying the file contents
- **File Operations**: Reading, writing, or editing files correctly
- **UI Consistency**: Showing consistent paths across the application

## Notes

- Tool calls are only parsed when the DevIn block is complete (`isComplete = true`)
- If no tool calls are found, the content is rendered as a regular code block
- While streaming (incomplete), the content is displayed as a code block
- Tool type mapping uses `ToolType.fromName()` for backward compatibility
- Path resolution is automatic and transparent to the LLM - it can provide relative paths

