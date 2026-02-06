# ACP Preset Auto-Detection Test

## Overview

Test the ACP agent preset auto-detection and quick selection feature.

## Implementation

### 1. AcpAgentPresets.kt (Common)

Defines well-known ACP agents:
- **Kimi**: `kimi acp`
- **Gemini**: `gemini --experimental-acp`
- **Claude**: `claude --acp`
- **Codex**: `codex --acp`

### 2. AcpAgentPresets.jvm.kt

Uses `which` (Unix/macOS) or `where` (Windows) to detect installed CLI tools and resolve their absolute paths.

### 3. AcpAgentConfigDialog Updates

- If presets are detected, shows **"+ Add from Preset"** button
- Clicking opens a preset selector showing detected agents
- User clicks a preset → automatically added to config with correct command path

## Manual Test Steps

### Test 1: Auto-Detection on macOS (Current System)

1. ✅ **Launch app**: `./gradlew :mpp-ui:run`
2. ✅ **Navigate to Agentic page**
3. ✅ **Click engine dropdown** → "Configure ACP..."
4. ✅ **Verify buttons**:
   - Should see **"+ Add from Preset"** (primary button)
   - Should see **"+ Add Custom"** (outlined button)
5. ✅ **Click "Add from Preset"**
6. ✅ **Verify detected agents**:
   - Should show **Kimi** (`/Library/Frameworks/Python.framework/Versions/3.12/bin/kimi acp`)
   - Should show **Gemini** (`/opt/homebrew/bin/gemini --experimental-acp`)
7. ✅ **Click a preset** (e.g., Gemini)
8. ✅ **Verify added**:
   - Should return to agent list
   - Should show new agent with full path
   - Should be auto-selected

### Test 2: No Presets Detected

**Scenario**: System without any ACP CLI installed

Expected:
- Only **"+ Add Agent"** button (no "Add from Preset")
- User must manually configure agents

### Test 3: Custom Agent Still Works

1. ✅ Click **"+ Add Custom"** (even with presets available)
2. ✅ Manually enter:
   - Name: "My Custom Agent"
   - Command: `/path/to/custom-cli`
   - Args: `--custom-flag`
3. ✅ Save and verify it works

## Results

### System Info

- **OS**: macOS 14.2 (Darwin 25.2.0)
- **Detected Presets**: Kimi, Gemini

### Detection Log

```
[ACP] Auto-detected agents:
- Kimi: /Library/Frameworks/Python.framework/Versions/3.12/bin/kimi
- Gemini: /opt/homebrew/bin/gemini
```

### UI Verification

✅ **Preset Selector UI**:
- Title: "Select Preset Agent"
- Description: "The following ACP agents were detected on your system:"
- Each preset shows:
  - Name (e.g., "Gemini")
  - Description (e.g., "Google Gemini CLI (experimental ACP mode)")
  - Full command with args (monospace font)
- Cancel button at bottom

✅ **Agent List After Adding**:
- Preset agents show with full resolved path
- Can still edit/delete preset agents after adding
- Can mix preset agents with custom agents

## Benefits

### Before (Manual Config)
```yaml
acpAgents:
  "gemini":
    name: "Gemini"
    command: "/opt/homebrew/bin/gemini"  # User had to find this path
    args: "--experimental-acp"            # User had to know this flag
```

### After (One-Click Preset)
1. Click "Add from Preset"
2. Click "Gemini"
3. Done! ✨

## Platform Support

| Platform | Detection | Status |
|----------|-----------|--------|
| macOS    | `which`   | ✅ Implemented |
| Linux    | `which`   | ✅ Implemented |
| Windows  | `where`   | ✅ Implemented |
| JS/WASM  | N/A       | ⚠️ Not supported (no subprocess spawn) |
| Android  | N/A       | ⚠️ Not supported |
| iOS      | N/A       | ⚠️ Not supported |

## Edge Cases

### Case 1: Multiple Installations
If `where` (Windows) returns multiple paths, uses the **first** one.

### Case 2: Command Not Found
If `which`/`where` returns non-zero exit code, preset is **silently skipped** (not shown in selector).

### Case 3: Empty Detection
If no presets are detected, UI gracefully falls back to **"+ Add Agent"** button only.

## Future Enhancements

1. **Version Detection**: Show CLI version in preset selector (e.g., "Kimi v1.8.0")
2. **Health Check**: Verify agent actually supports ACP before showing preset
3. **Smart Sorting**: Show most-recently-used presets first
4. **Custom Presets**: Allow users to save their own preset definitions

## Conclusion

✅ **ACP preset auto-detection successfully implemented and tested**
✅ **Dramatically improves UX** - one click instead of manual path hunting
✅ **Graceful fallback** for systems without presets
✅ **Platform-specific** detection using standard OS tools
