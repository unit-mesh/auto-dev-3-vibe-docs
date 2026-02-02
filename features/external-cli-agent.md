# External CLI Agent Integration

This document describes the External CLI Agent feature that allows Xiuper to delegate coding tasks to external command-line coding agents like Claude Code (`claude` CLI) and OpenAI Codex (`codex` CLI).

## Overview

The External CLI Agent feature enables users to leverage third-party coding agents through their command-line interfaces. Unlike Xiuper's built-in `CodingAgent`, these external agents run as separate processes and manage their own AI interactions, while Xiuper handles:

- Task delegation via CLI invocation
- Output streaming and rendering
- Workspace change detection via Git
- Unified UX through the existing renderer system

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Xiuper Application                       │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐    ┌──────────────────────────────────────┐   │
│  │ GUI/CLI      │───►│     ExternalCliCodingAgent           │   │
│  │ (Engine      │    │  - buildCommand()                    │   │
│  │  Selector)   │    │  - executeTask()                     │   │
│  └──────────────┘    │  - Workspace snapshot (Git-based)    │   │
│                      └──────────────┬───────────────────────┘   │
│                                     │                           │
│  ┌──────────────────────────────────▼───────────────────────┐   │
│  │                   ShellExecutor                           │   │
│  │  - JVM: ProcessBuilder / PtyShellExecutor                 │   │
│  │  - JS:  child_process.exec                                │   │
│  └──────────────────────────────────┬───────────────────────┘   │
└─────────────────────────────────────┼───────────────────────────┘
                                      │
                    ┌─────────────────▼─────────────────┐
                    │       External CLI Agent          │
                    │  (claude / codex / others)        │
                    │  - Own AI service connection      │
                    │  - Own tool execution             │
                    │  - Direct filesystem access       │
                    └───────────────────────────────────┘
```

## Supported Agents

### Claude Code (`claude` CLI)

- **Binary**: `claude`
- **Non-interactive mode**: `claude -p --output-format text "<prompt>"`
- **Interactive mode**: `claude "<prompt>"`
- **Requirements**: Install via Anthropic's official CLI

### OpenAI Codex (`codex` CLI)

- **Binary**: `codex`
- **Non-interactive mode**: `codex exec --color auto -a never -s workspace-write -C <path> "<prompt>"`
- **Interactive mode**: `codex -C <path> --no-alt-screen "<prompt>"`
- **Requirements**: Install via OpenAI's official CLI or build from source

## Usage

### CLI (Node.js)

```bash
# Use Claude Code
autodev code -p /path/to/project -t "Add unit tests for UserService" \
    --engine claude --engine-mode non-interactive

# Use Codex
autodev code -p /path/to/project -t "Refactor the database layer" \
    --engine codex --engine-mode non-interactive

# With extra arguments
autodev code -p . -t "Fix the login bug" \
    --engine claude --engine-arg "--max-tokens" --engine-arg "4096"

# With timeout
autodev code -p . -t "Implement caching" \
    --engine codex --timeout-ms 3600000
```

### CLI Options

| Option | Description | Default |
|--------|-------------|---------|
| `--engine <engine>` | Engine: `autodev`, `claude`, or `codex` | `autodev` |
| `--engine-mode <mode>` | Mode: `non-interactive` or `interactive` | `non-interactive` |
| `--engine-arg <arg>` | Extra argument (repeatable) | - |
| `--timeout-ms <ms>` | Timeout in milliseconds | `1800000` (30 min) |

### Compose GUI (Desktop)

1. Open the Coding Agent page
2. Click the **Engine** dropdown in the top toolbar
3. Select **Claude** or **Codex**
4. Enter your task and execute

The engine selection persists for the session. External engines do not require local LLM configuration in Xiuper—they use their own API keys configured in their respective CLIs.

## Implementation Details

### Core Classes

#### `ExternalCliCodingAgent`

Location: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/external/ExternalCliCodingAgent.kt`

Implements `CodingAgentService` interface to integrate with Xiuper's agent system:

```kotlin
class ExternalCliCodingAgent(
    private val projectPath: String,
    private val kind: ExternalCliKind,           // CLAUDE or CODEX
    private val renderer: CodingAgentRenderer,   // For output rendering
    private val fileSystem: ToolFileSystem,      // For reading changed files
    private val shellExecutor: ShellExecutor,    // Cross-platform shell
    private val mode: ExternalCliMode,           // NON_INTERACTIVE or INTERACTIVE
    private val timeoutMs: Long,                 // Execution timeout
    private val extraArgs: List<String>          // Additional CLI arguments
) : CodingAgentService
```

#### `GitWorkspaceSnapshot`

Location: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/external/GitWorkspaceSnapshot.kt`

Captures Git status before and after external agent execution to detect file changes:

```kotlin
// Before execution
val beforeSnapshot = GitWorkspaceSnapshot.capture(shellExecutor, projectPath)

// After execution
val afterSnapshot = GitWorkspaceSnapshot.capture(shellExecutor, projectPath)
val changes = GitWorkspaceSnapshot.diff(beforeSnapshot, afterSnapshot)
```

#### `JsExternalCliAgent`

Location: `mpp-core/src/jsMain/kotlin/cc/unitmesh/agent/CodingAgentExports.kt`

JavaScript-friendly wrapper for Node.js CLI usage:

```typescript
const agent = new KotlinCC.unitmesh.agent.JsExternalCliAgent(
    projectPath,
    'codex',           // engine
    'non-interactive', // mode
    1800000,           // timeout
    [],                // extra args
    renderer           // optional renderer
);

const result = await agent.executeTask(task);
```

### Execution Flow

1. **Task Submission**: User provides a task via CLI or GUI
2. **Git Snapshot**: Capture current workspace state via `git status --porcelain`
3. **Command Build**: Construct CLI command with prompt and options
4. **Execution**: Run external agent via `ShellExecutor`
5. **Output Streaming**: Stream agent output to renderer
6. **Change Detection**: Compare Git snapshots to identify file changes
7. **Result Assembly**: Return `AgentResult` with steps and edits

## Modes

### Non-Interactive Mode

- Agent runs to completion without user interaction
- Output is captured and rendered
- Best for automated pipelines and batch processing

### Interactive Mode

- Agent may prompt for confirmations
- Terminal I/O is inherited (PTY support on JVM)
- Best for desktop GUI where user can interact

## Workspace Change Detection

Since external agents edit files directly (bypassing Xiuper's tool system), we use Git to detect changes:

1. Run `git status --porcelain` before execution
2. Run `git status --porcelain` after execution
3. Diff the outputs to identify:
   - New files (CREATE)
   - Modified files (UPDATE)
   - Deleted files (DELETE)

**Limitations**:
- Only works in Git repositories
- Only detects uncommitted changes
- Files in `.gitignore` won't be tracked

## Future Work

### ACP (Agent Client Protocol) Integration

For richer integration with Codex and other agents, we're exploring the Agent Client Protocol (ACP):

- **Location**: `docs/test-scripts/acp-codex/main.kt`
- **Goal**: Replace CLI execution with protocol-based communication
- **Benefits**: Real-time streaming, tool approval, better error handling

See the design probe in `docs/test-scripts/acp-codex/` for details.

### Adding New Agents

To add support for a new external CLI agent:

1. Add entry to `ExternalCliKind` enum
2. Implement command building in `buildCommand()`
3. Update CLI options in `index.tsx`
4. Add GUI option in `GuiAgentEngine` enum

## Troubleshooting

### Agent not found

Ensure the CLI is installed and in your PATH:

```bash
# Check Claude
which claude
claude --version

# Check Codex
which codex
codex --version
```

### Timeout errors

Increase the timeout for long-running tasks:

```bash
# CLI
autodev code -p . -t "..." --timeout-ms 7200000  # 2 hours

# Or set in GUI preferences (future)
```

### No changes detected

- Ensure you're in a Git repository
- Check if files are in `.gitignore`
- External agent may have made no changes
