# ACP Integration in IDEA Plugin

## Overview

ACP (Agent Client Protocol) agents are now fully integrated into the CODING tab as first-class engines, matching the mpp-ui implementation.

## User Guide

### Where to Find ACP

1. Open the **Xiuper Agents** tool window (right sidebar)
2. Click the **Agentic** tab (formerly "CODING")
3. Look at the **model selector dropdown** in the bottom toolbar (left side, next to + and refresh buttons)

### Initial Setup

When you first open the plugin:

1. The model dropdown shows your configured LLM models (e.g., "glm")
2. At the bottom, you'll see:
   - `--- ACP Agents ---` (separator)
   - `Configure ACP...` (click this)

### Configuring ACP Agents

Click "Configure ACP..." to open the ACP Configuration Dialog:

**Option 1: Add from Presets (Recommended)**
- Click the **+ Preset** button
- Select from auto-detected agents:
  - Codex CLI (`codex --acp`)
  - Kimi CLI (`kimi acp`)
  - Gemini CLI (`gemini --acp`)
  - Claude Code (`claude --acp`)
  - GitHub Copilot (`github-copilot --acp`)
- Agent is automatically added and selected

**Option 2: Add Custom Agent**
- Click **Add** button
- Fill in the form:
  - **Display Name**: Human-readable name (e.g., "My Custom Agent")
  - **Command**: Executable path (e.g., `codex` or `/usr/local/bin/codex`)
  - **Arguments**: Space-separated args (e.g., `--acp --verbose`)
  - **Env Vars**: Environment variables, one per line (`KEY=VALUE`)
- Click **Apply Changes**

### Using ACP Agents

Once configured:

1. **Select Agent**: Open the model dropdown
   - Your LLM configs appear first
   - Below the separator: `ACP: Codex CLI`, `ACP: Kimi CLI`, etc.
2. **Switch Engine**: Click an ACP agent
   - Model selector updates to show agent name
   - Agent auto-connects on first message
3. **Send Messages**: Type in the input area and click Send
   - Messages route to the ACP agent
   - Responses stream to the shared timeline (same UI as AutoDev)
4. **Switch Back**: Select any LLM config from the dropdown to switch back to AutoDev engine

### Features

- **Unified Timeline**: ACP output renders alongside AutoDev output
- **Auto-Connect**: Agent connects automatically when you send the first message
- **Process Reuse**: Agent process is reused across multiple prompts (no restart overhead)
- **Multi-Turn**: Full conversation context is maintained within a session
- **New Chat**: Click the New Chat button to disconnect and start fresh
- **Cancel**: Stop button works with ACP agents
- **Plan Display**: ACP plan updates appear in the same plan UI
- **Tool Calls**: Tool execution results render inline
- **File Operations**: ACP agents can read/write files (auto-approved for MVP)
- **Terminal**: ACP agents can run shell commands (auto-approved for MVP)
- **MCP Tools**: Configured MCP servers are passed to ACP agents

## Technical Details

### Architecture

```
IdeaAgentViewModel
├── currentEngine: IdeaEngine (AUTODEV | ACP)
├── acpViewModel: IdeaAcpAgentViewModel (shares renderer)
└── sendMessage() → routes to acpViewModel when engine=ACP

SwingBottomToolbar (Model Selector)
├── LLM Configs (AutoDev)
├── --- ACP Agents ---
├── ACP: Codex CLI
├── ACP: Kimi CLI
└── Configure ACP...
```

### State Flow

1. `IdeaAgentViewModel.init()` → `loadAcpAgents()` → loads from `~/.autodev/config.yaml`
2. `acpAgents` StateFlow updates
3. `IdeaAgentApp` collects via `IdeaLaunchedEffect`
4. Passes `acpAgents` to `IdeaDevInInputArea`
5. `SwingDevInInputArea.setAcpAgents()` → `SwingBottomToolbar.setAcpAgents()`
6. ComboBox rebuilds with ACP entries

### Configuration File

Location: `~/.autodev/config.yaml`

```yaml
acpAgents:
  codex:
    name: "Codex CLI"
    command: "codex"
    args: "--acp"
    env: ""
  kimi:
    name: "Kimi CLI"
    command: "kimi"
    args: "acp --work-dir /path/to/project"
    env: "KIMI_API_KEY=xxx"
activeAcpAgent: codex
```

### Debugging

Enable debug logging by checking:

1. IDEA logs (`~/.autodev/logs/autodev-app.log` or Help > Show Log)
2. Console output:
   - `IdeaAgentApp: acpAgents changed, size=N`
   - `SwingDevInInputArea.setAcpAgents called with N agents`
   - `SwingBottomToolbar.setAcpAgents: received N agents`

If "Configure ACP..." doesn't appear:
1. Check that `availableConfigs` is not empty (need at least one LLM config)
2. Restart plugin or reload window
3. Check logs for exceptions during `loadAcpAgents()`

## Comparison with mpp-ui

| Feature | mpp-ui (Compose) | mpp-idea (Swing+Jewel) |
|---------|------------------|------------------------|
| Engine Selection | Dropdown in `DevInEditorInput` | Dropdown in `SwingBottomToolbar` |
| Config Dialog | `AcpAgentConfigDialog` (Material3) | `IdeaAcpConfigDialogWrapper` (DialogWrapper) |
| Preset Detection | `AcpAgentPresets.kt` (commonMain) | `IdeaAcpAgentPreset` (idea-only) |
| State Management | `CodingAgentViewModel` | `IdeaAgentViewModel` + `IdeaAcpAgentViewModel` |
| Renderer | `AcpRenderer` (extends `BaseRenderer`) | `JewelRenderer` (shared) |

## Known Limitations

- **MVP: Auto-Approve Permissions**: All permission requests (file read/write, shell) are auto-approved. Future: show confirmation dialog.
- **No Session History**: ACP sessions don't persist to chat history (yet).
- **No Resume**: Can't resume a disconnected ACP session (must start fresh).

## Future Enhancements

- [ ] Session history integration
- [ ] Permission confirmation UI
- [ ] Session resume after disconnect
- [ ] Agent health monitoring UI
- [ ] Per-agent connection status in dropdown
- [ ] Agent-specific settings (temperature, max tokens, etc.)
