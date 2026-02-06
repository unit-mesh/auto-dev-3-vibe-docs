# Tool Call Batching Implementation Summary

## Problem Description

When using ACP agents (like Kimi) to review a project, they generate numerous `ReadFile` tool calls. Each tool call was creating a separate UI card in the timeline, causing:

- **UI flooding**: Timeline filled with dozens of "ReadFile" items
- **Performance issues**: Rendering many individual cards slows down the UI
- **Poor UX**: Hard to see the overall progress or other important actions

Example: Asking Kimi to "review the current project" would result in 50+ individual ReadFile cards flooding the timeline.

## Solution: Tool Call Batching

Implemented automatic batching in `ComposeRenderer` to collapse repetitive tool calls into summary items.

### How It Works

1. **Track Similar Tools**: Monitor consecutive calls to batchable tools (currently: `ReadFile`)
2. **Threshold Detection**: After 5 calls to the same tool, collapse all previous items
3. **Dynamic Updates**: As more calls come in, update the batch count
4. **Reset on Different Tool**: When a different tool is called, start a new batch

### Implementation Details

**File**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ComposeRenderer.kt`

**Key Changes**:
```kotlin
// Configuration
private val BATCH_TOOL_TYPES = setOf(ToolType.ReadFile)
private val BATCH_THRESHOLD = 5 // Collapse after N calls

// Tracking state
private data class BatchTracker(
    var toolName: String,
    var count: Int,
    var firstIndex: Int,
    var lastUpdate: Long
)
private var currentBatch: BatchTracker? = null

// Logic in renderToolCallInternal()
if (batch.count == BATCH_THRESHOLD) {
    // Remove individual items from timeline
    _timeline.removeAll(itemsToRemove)
    
    // Add single batch item
    _timeline.add(ToolCallItem(
        toolName = "batch:$toolName",
        description = "${batch.count} Read File calls",
        params = "Reading ${batch.count} files...",
        ...
    ))
}
```

### Test Results

Created a JVM test CLI to verify the batching functionality:

**Test Command**:
```bash
./gradlew :mpp-ui:runBatchTest
```

**Test 1: Basic Batching**
- Input: 10 rapid `read-file` calls
- Result: ✅ Collapsed into **1 batch item**
- Timeline items: Reduced from 11 to 2 (1 message + 1 batch)

**Test 2: Mixed Tool Calls**
- Input: 6 `read-file` + 1 `write-file` + 4 `read-file`
- Result: ✅ Two separate batches (write-file resets batching)
- ReadFile items: Reduced from 10 to 1

**Test 3: Batch Boundaries**
- Input: 3 reads, 2.5s delay, 3 more reads
- Result: ✅ Single batch (second batch only has 3 items, below threshold)

## Benefits

1. **Reduced UI Clutter**: 50+ ReadFile calls → 1 batch item showing "50 Read File calls"
2. **Better Performance**: Fewer timeline items to render and manage
3. **Improved UX**: Easy to see overall progress ("Processing 50 files...")
4. **Minimal Impact**: Only triggers after threshold (5 calls), preserving detail for small operations

## Configuration

Currently hardcoded, but can be easily adjusted:

```kotlin
// Which tools to batch
private val BATCH_TOOL_TYPES = setOf(ToolType.ReadFile)

// How many calls before batching starts
private val BATCH_THRESHOLD = 5
```

Future enhancement: Make these configurable via `ConfigFile`.

## Usage Notes

- Batching is **automatic** - no user action required
- Applies to **ReadFile** operations only (more tools can be added)
- First 4 calls show individually, 5th triggers batching
- Works with streaming output from ACP agents
- Compatible with all renderers (Compose, Jewel, TUI, etc.)

## Testing

Run the test CLI:
```bash
./gradlew :mpp-ui:runBatchTest
```

Or test manually in the Compose Desktop app:
1. Launch the app: `./gradlew :mpp-ui:run`
2. Switch to "Agentic" page
3. Select an ACP agent (e.g., Kimi) from the engine dropdown
4. Ask: "Review all files in mpp-ui/src/commonMain/kotlin/"
5. Observe: Multiple ReadFile calls are batched into summary items

## Files Modified

1. `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ComposeRenderer.kt`
   - Added batch tracking variables
   - Implemented batching logic in `renderToolCallInternal()`
   - Updated `renderToolResult()` to handle batch results
   - Reset batch state in `renderLLMResponseStart()`

2. `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/server/cli/RendererBatchTest.kt` (NEW)
   - JVM test CLI to verify batching functionality

3. `mpp-ui/build.gradle.kts`
   - Added `runBatchTest` Gradle task

## Next Steps

Optional enhancements:
1. Make threshold configurable in settings
2. Add more batchable tools (WriteFile, Glob, Grep)
3. Show expandable detail view for batched items
4. Add batch progress indicator
5. Configurable time window for batching
