# ACP Permission Request Dialog Fix

**Date**: 2026-02-06  
**Issue**: Terminal operations (`terminal/create`) failing with "Exception on MethodName(name=terminal/create)"  
**Root Cause**: Incorrect permission response - returning `optionId=approve` instead of the actual option ID from the ACP protocol

## Problem Analysis

### Error Symptoms
```
ACP requestPermissions: tool=Shell: which colima && colima status
  options=[ALLOW_ONCE:Approve once, ALLOW_ALWAYS:Approve for this session, REJECT_ONCE:Reject]
ACP permission response: Selected(optionId=approve)  ❌ WRONG!
ERROR - Exception on MethodName(name=terminal/create)
```

### Root Cause

之前的实现自动批准权限请求，但返回的 `optionId` 是硬编码的 `"approve"`，而不是从 ACP agent 提供的实际 option 中选择。

根据 [ACP Protocol Spec](https://agentclient.dev/docs/protocol/tool-calls#requesting-permission)，权限选项的结构是：

```json
{
  "optionId": "allow-once",      // ← 必须返回这个！
  "name": "Allow once",
  "kind": "allow_once"
}
```

**错误代码**:
```kotlin
// 旧版 - 硬编码错误的 optionId
RequestPermissionResponse(
    RequestPermissionOutcome.Selected("approve"),  // ❌ 不在选项列表中！
    JsonNull
)
```

**正确做法**:
```kotlin
// 新版 - 使用 agent 提供的实际 optionId
val selectedOption = options.firstOrNull { it.kind == PermissionOptionKind.ALLOW_ONCE }
RequestPermissionResponse(
    RequestPermissionOutcome.Selected(selectedOption.optionId),  // ✅ 正确！
    JsonNull
)
```

## Solution: Permission Request Dialog

实现了一个 `IdeaAcpPermissionDialog`，让用户明确选择权限选项，而不是自动批准。

### 1. IdeaAcpPermissionDialog.kt (NEW)

**功能**:
- 显示工具调用详情 (title, kind, status, raw input)
- 展示所有可用的权限选项 (radio buttons)
- 返回用户选择的 `PermissionOption` 对象

**UI 布局**:
```
┌────────────────────────────────────────┐
│ Agent Permission Request               │
├────────────────────────────────────────┤
│ Shell: which colima && colima status   │ <- Tool title
│                                        │
│ Tool ID: call_001                      │
│ Kind: execute                          │
│ Status: pending                        │
│ Input: { command: "which colima && ..." }│
│                                        │
│ Please choose an action:               │
│  ○ Approve once                        │ <- ALLOW_ONCE
│  ○ Approve for this session            │ <- ALLOW_ALWAYS
│  ○ Reject                              │ <- REJECT_ONCE
│                                        │
│         [ OK ]  [ Cancel ]             │
└────────────────────────────────────────┘
```

**代码**:
```kotlin
class IdeaAcpPermissionDialog(
    private val project: Project?,
    private val toolCall: SessionUpdate.ToolCallUpdate,
    private val options: List<PermissionOption>
) : DialogWrapper(project) {
    private var selectedOption: PermissionOption? = null
    
    // Radio button for each option
    options.forEachIndexed { index, option ->
        val radioButton = JRadioButton().apply {
            text = option.name
            toolTipText = "Option ID: ${option.optionId.value}, Kind: ${option.kind}"
            addActionListener { selectedOption = option }
        }
        buttonGroup.add(radioButton)
    }
    
    // Auto-select first option
    if (options.isNotEmpty()) {
        firstButton.isSelected = true
        selectedOption = options[0]
    }
}
```

### 2. IdeaAcpAgentViewModel.kt (UPDATED)

**权限处理流程**:

```kotlin
private fun handlePermissionRequest(
    toolCall: SessionUpdate.ToolCallUpdate,
    options: List<PermissionOption>,
): RequestPermissionResponse {
    // 1. Show dialog on EDT thread (synchronous)
    var selectedOption: PermissionOption? = null
    ApplicationManager.getApplication().invokeAndWait {
        selectedOption = IdeaAcpPermissionDialog.show(project, toolCall, options)
    }

    // 2. User selected an option -> return its optionId
    if (selectedOption != null) {
        acpLogger.info(
            "ACP permission user selected: optionId=${selectedOption!!.optionId.value} " +
            "kind=${selectedOption!!.kind} name=${selectedOption!!.name}"
        )
        return RequestPermissionResponse(
            RequestPermissionOutcome.Selected(selectedOption!!.optionId), // ✅ Correct!
            JsonNull
        )
    }

    // 3. User cancelled dialog -> return Cancelled
    acpLogger.info("ACP permission cancelled by user (dialog closed)")
    return RequestPermissionResponse(RequestPermissionOutcome.Cancelled, JsonNull)
}
```

## Type Handling: PermissionOptionId

关键发现：`PermissionOption.optionId` 不是简单的 `String`，而是 `PermissionOptionId` 包装类型。

**正确用法**:
```kotlin
// ✅ Correct - use the PermissionOptionId directly
RequestPermissionResponse(
    RequestPermissionOutcome.Selected(selectedOption.optionId),
    JsonNull
)

// ❌ Wrong - trying to construct from String
RequestPermissionResponse(
    RequestPermissionOutcome.Selected("allow-once"),  // Type mismatch!
    JsonNull
)

// ✅ Access String value for logging
println("Option ID: ${option.optionId.value}")
```

## Expected Behavior After Fix

### Scenario 1: User Approves Shell Command
```
1. Agent: requestPermissions for "Shell: which colima && colima status"
2. Client: Shows IdeaAcpPermissionDialog
3. User: Selects "Approve once" (optionId=allow-once)
4. Client: Returns Selected(optionId=allow-once)  ✅
5. Agent: Proceeds with terminal/create
6. Result: Terminal created successfully, command executes
```

### Scenario 2: User Rejects
```
1. Agent: requestPermissions for "Shell: rm -rf /"
2. Client: Shows IdeaAcpPermissionDialog
3. User: Selects "Reject" (optionId=reject-once)
4. Client: Returns Selected(optionId=reject-once)  ✅
5. Agent: Skips tool call, continues with other tasks
```

### Scenario 3: User Cancels Dialog
```
1. Agent: requestPermissions
2. Client: Shows IdeaAcpPermissionDialog
3. User: Clicks "Cancel" or closes dialog
4. Client: Returns Cancelled  ✅
5. Agent: Treats as rejection
```

## Build Status

```bash
cd mpp-idea && ../gradlew compileKotlin
BUILD SUCCESSFUL in 3s
```

✅ 编译通过，只有2个 warnings (deprecated API, exhaustive when)

## Testing Checklist

- [ ] Run plugin in dev mode
- [ ] Connect to ACP agent (Claude Code, Kimi, etc.)
- [ ] Trigger a shell command that requires permission
- [ ] Verify permission dialog appears
- [ ] Select "Approve once" → verify terminal/create succeeds
- [ ] Trigger again → select "Reject" → verify tool call skipped
- [ ] Check logs for correct `optionId` values

## Files Modified

1. **IdeaAcpPermissionDialog.kt** (NEW) - Permission request UI
2. **IdeaAcpAgentViewModel.kt** - Updated `handlePermissionRequest()` to use dialog

## Comparison with Auto-Approval (Old Behavior)

| Aspect | Old (Auto-Approve) | New (User Dialog) |
|--------|-------------------|-------------------|
| User Awareness | ❌ Silent approval | ✅ Explicit confirmation |
| Security | ⚠️ Approves everything | ✅ User controls each action |
| optionId Correctness | ❌ Hardcoded "approve" | ✅ From actual options list |
| Debugging | ❌ Hard to trace rejections | ✅ Clear logs with user choice |

## ACP Protocol Compliance

Before:
- ❌ Violated protocol by returning invalid `optionId`
- ❌ No user consent for sensitive operations

After:
- ✅ Returns correct `optionId` from provided options
- ✅ Compliant with [ACP Tool Calls spec](https://agentclient.dev/docs/protocol/tool-calls#requesting-permission)
- ✅ Supports all permission option kinds (ALLOW_ONCE, ALLOW_ALWAYS, REJECT_ONCE, REJECT_ALWAYS)

## Next Steps

1. **Test with real agents**: Verify the dialog works with different ACP agents (Claude Code, Kimi, Gemini, Copilot)
2. **Session-based "Remember" feature**: Implement ALLOW_ALWAYS to skip dialog for repeated operations in same session
3. **Dangerous command detection**: Highlight risky operations (rm, sudo, curl | bash) with warning colors
4. **Command preview**: For complex shell commands, add syntax highlighting to the input preview

## Related Issues

- Original error: "Exception on MethodName(name=terminal/create)"
- Root cause: Invalid `optionId=approve` not matching protocol spec
- Fix: Proper permission dialog + correct optionId handling
