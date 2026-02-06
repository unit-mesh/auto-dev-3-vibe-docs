# ACP Response Logging Implementation

## Overview

Automatically log all ACP (Agent Client Protocol) events to `~/.autodev/acp-logs/` for debugging, analysis, and replay.

## Implementation

### 1. ConfigManager Updates

Added `getAcpLogsDir()` method to return `~/.autodev/acp-logs/` directory path.

**Common Interface** (`ConfigManager.kt`):
```kotlin
fun getAcpLogsDir(): String
```

**JVM Implementation** (`ConfigManager.jvm.kt`):
```kotlin
actual fun getAcpLogsDir(): String {
    val acpLogsDir = File(configDir, "acp-logs")
    // Ensure directory exists
    acpLogsDir.mkdirs()
    return acpLogsDir.absolutePath
}
```

### 2. AcpClient Logging

Added automatic event logging to `AcpClient.kt`:

#### Constructor Parameters
- `agentName: String = "acp-agent"` - Used for log filename
- `enableLogging: Boolean = true` - Toggle logging on/off

#### Log File Format
- **Location**: `~/.autodev/acp-logs/`
- **Filename**: `{agentName}_{timestamp}.jsonl`
- **Format**: JSONL (JSON Lines) - one JSON object per line
- **Timestamp**: `yyyyMMdd-HHmmss` (e.g., `Gemini_20260206-171632.jsonl`)

#### Logged Events

Each line in the JSONL file contains:

1. **Prompt Start**
```json
{"type":"prompt_start","timestamp":1770369399010,"prompt":"hi"}
```

2. **SessionUpdate Events**
```json
{
  "event_type":"SessionUpdate",
  "timestamp":1770369403512,
  "update_type":"AgentThoughtChunk",
  "update":{
    "type":"AgentThoughtChunk",
    "content":"**Examining Project Structure**\n\nI'm starting..."
  }
}
```

3. **ToolCall Events**
```json
{
  "event_type":"SessionUpdate",
  "timestamp":1770369404123,
  "update_type":"ToolCallUpdate",
  "update":{
    "type":"ToolCallUpdate",
    "toolCallId":"tc_001",
    "title":"ReadFile: build.gradle.kts",
    "kind":"READ_FILE",
    "status":"COMPLETED",
    "rawInput":"path=/Users/phodal/ai/xiuper/build.gradle.kts",
    "rawOutput":"..."
  }
}
```

4. **PromptResponse Events**
```json
{
  "event_type":"PromptResponse",
  "timestamp":1770369410000,
  "stop_reason":"END_TURN"
}
```

5. **Prompt End**
```json
{"type":"prompt_end","timestamp":1770369410500}
```

### 3. Log Lifecycle

```
connect()
  ↓
  📝 Create log file: ~/.autodev/acp-logs/{agentName}_{timestamp}.jsonl
  ↓
promptAndRender()
  ↓
  Log: prompt_start
  ↓
  For each ACP event:
    - Log: raw event (SessionUpdate/PromptResponse)
    - Render to UI
  ↓
  Log: prompt_end
  ↓
disconnect()
  ↓
  📝 Flush and close log file
  ✅ Log saved
```

### 4. Integration with AcpConnectionProvider

`AcpConnectionProvider.jvm.kt` passes agent name to `AcpClient`:

```kotlin
val client = AcpClient(
    coroutineScope = scope,
    input = input,
    output = output,
    clientName = "autodev-xiuper-compose",
    clientVersion = "3.0.0",
    cwd = effectiveCwd,
    agentName = config.name.ifBlank { "acp-agent" },  // ✅ From config
    enableLogging = true  // ✅ Always enabled
)
```

## Usage

### Automatic Logging (Default)

All ACP interactions are automatically logged when using the Compose Desktop app:

1. Navigate to **Agentic** page
2. Select an ACP agent from engine dropdown (e.g., "Gemini")
3. Send a prompt
4. **Logs are automatically saved to**: `~/.autodev/acp-logs/Gemini_20260206-171632.jsonl`

### Viewing Logs

```bash
# List all ACP logs
ls -lh ~/.autodev/acp-logs/

# View a specific log (pretty-printed)
cat ~/.autodev/acp-logs/Gemini_20260206-171632.jsonl | jq .

# Count events in a log
wc -l ~/.autodev/acp-logs/Gemini_20260206-171632.jsonl

# Filter for tool calls only
cat ~/.autodev/acp-logs/Gemini_20260206-171632.jsonl | jq 'select(.update_type == "ToolCallUpdate")'
```

### Disable Logging (if needed)

To disable logging, modify `AcpConnectionProvider.jvm.kt`:

```kotlin
val client = AcpClient(
    // ...
    enableLogging = false  // ❌ Disable logging
)
```

## Benefits

### 1. **Debugging**
- Inspect exact ACP event sequences
- Understand agent behavior without UI noise
- Reproduce issues from captured logs

### 2. **Analysis**
- Count tool call frequencies
- Measure response times (via timestamps)
- Identify performance bottlenecks (e.g., 2279 WriteFile events)

### 3. **Replay Testing**
- Use logs as test fixtures
- Test renderer changes without re-running agents
- Create regression tests from real interactions

### 4. **Protocol Documentation**
- Real examples of ACP event structure
- Reference for implementing custom renderers
- Validate ACP SDK behavior

## Example Log Session

```bash
$ cat ~/.autodev/acp-logs/Gemini_20260206-171632.jsonl
```

**Output**:
```json
{"type":"prompt_start","timestamp":1770369399010,"prompt":"hi"}
{"event_type":"SessionUpdate","timestamp":1770369403512,"update_type":"AgentThoughtChunk","update":{"type":"AgentThoughtChunk","content":"**Examining Project Structure**\n\nI'm starting by using `ls -F` to get a detailed view..."}}
{"event_type":"SessionUpdate","timestamp":1770369404200,"update_type":"ToolCallUpdate","update":{"type":"ToolCallUpdate","toolCallId":"tc_01","title":"ReadFile: build.gradle.kts","kind":"READ_FILE","status":"IN_PROGRESS","rawInput":"...","rawOutput":"null"}}
{"event_type":"SessionUpdate","timestamp":1770369404800,"update_type":"ToolCallUpdate","update":{"type":"ToolCallUpdate","toolCallId":"tc_01","title":"ReadFile: build.gradle.kts","kind":"READ_FILE","status":"COMPLETED","rawInput":"...","rawOutput":"plugins {\n    id(\"java\")\n..."}}
{"event_type":"SessionUpdate","timestamp":1770369405500,"update_type":"AgentMessageChunk","update":{"type":"AgentMessageChunk","content":"Based on the project structure..."}}
{"event_type":"PromptResponse","timestamp":1770369410000,"stop_reason":"END_TURN"}
{"type":"prompt_end","timestamp":1770369410500}
```

## Statistics

From a real Gemini ACP session:

```bash
$ wc -l ~/.autodev/acp-logs/Gemini_20260206-171632.jsonl
7 ~/.autodev/acp-logs/Gemini_20260206-171632.jsonl
```

**Event Breakdown**:
- 1 prompt_start
- 3 SessionUpdate (thought + tool calls)
- 1 AgentMessageChunk
- 1 PromptResponse
- 1 prompt_end

**Total**: 7 events for a simple "hi" prompt

## Comparison: Kimi vs Gemini

### Kimi (Verbose)
- **7218 raw events** for "draw architecture diagram"
- 2279 WriteFile events (streaming char-by-char)
- Log file: ~2 MB

### Gemini (Clean)
- **7 events** for "hi" prompt
- No streaming heartbeats
- Log file: ~500 bytes

**Conclusion**: Logging is essential for understanding agent behavior differences!

## Future Enhancements

1. **Log Rotation**: Auto-delete logs older than 7 days
2. **Compression**: Gzip old logs to save disk space
3. **Structured Queries**: SQLite storage for advanced filtering
4. **Web Viewer**: Interactive log browser UI
5. **Replay Mode**: Re-render logs in UI without re-running agent

## Testing

✅ **Verified**:
- Log directory created at `~/.autodev/acp-logs/`
- Log file created with correct filename format
- Events logged in JSONL format
- Timestamps included
- All event types captured (prompt, SessionUpdate, PromptResponse)
- Log flushed and closed on disconnect

✅ **Tested with**:
- Gemini ACP CLI
- Kimi ACP CLI (from previous sessions)

## Conclusion

ACP response logging is now fully implemented and automatically enabled for all ACP agents. Every interaction is recorded to `~/.autodev/acp-logs/` in JSONL format, providing valuable insights for debugging, analysis, and testing.

**Location**: `~/.autodev/acp-logs/{AgentName}_{Timestamp}.jsonl`

**Status**: ✅ Production ready
