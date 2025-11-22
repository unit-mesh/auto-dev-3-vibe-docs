# Test Environment Inheritance

## Purpose
Verify that `PtyShellExecutor` and `McpClientManager` correctly inherit user's login shell environment, specifically Homebrew paths.

## Implementation Summary

### Architecture
Created shared utility class `ShellEnvironmentUtils.kt` to avoid code duplication:
- **Location**: `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/tool/shell/ShellEnvironmentUtils.kt`
- **Used by**: `PtyShellExecutor`, `McpClientManager`
- **Key functions**:
  - `loadLoginShellEnvironment()` - Load env from login shell with caching
  - `applyLoginEnvironment()` - Apply to existing environment map
  - `mergePath()` - Smart PATH merging with deduplication
  - `ensureHomebrewPath()` - Ensure `/opt/homebrew/bin` on macOS

### Configuration
- `ShellExecutionConfig.inheritLoginEnv: Boolean = true` - Enable/disable feature
- Applies to both direct execution and live sessions

## Test Steps

### 1. Test PATH Inheritance
In the AutoDev CLI, run:
```
which detekt
```

Expected: Should resolve to `/opt/homebrew/bin/detekt` (or wherever Homebrew installed it)

### 2. Test Other Homebrew Tools
```
which brew
```

Expected: Should resolve to `/opt/homebrew/bin/brew`

### 3. Test Custom User Exports
If you have custom PATH additions in `.zshrc` or `.zprofile`, verify they're available:
```
echo $PATH
```

Expected: Should include both system paths and user-added paths (e.g., `/opt/homebrew/bin`, custom tool directories)

### 4. Test Disable inheritLoginEnv
To verify the feature can be disabled, modify code temporarily:
```kotlin
val config = ShellExecutionConfig(
    workingDirectory = workingDirectory?.absolutePath,
    timeoutMs = 60000L,
    inheritLoginEnv = false  // Disable login env inheritance
)
```

Then run `which detekt` again - it should fail to resolve if detekt is only in Homebrew path.

## Implementation Details

The fix adds:
- `ShellExecutionConfig.inheritLoginEnv: Boolean = true` - Config flag
- `loadLoginShellEnvironment(shell: String)` - Spawns login shell to capture env
- `mergePath()` - Merges PATH values with deduplication
- `ensureHomebrewPath()` - Ensures `/opt/homebrew/bin` is present on macOS
- Environment cache - Avoids repeated shell spawns

## Verification Log

Date: 2025-11-22

### Before Fix
```
> which detekt
detekt not found
```

### After Fix
```
> which detekt
/opt/homebrew/bin/detekt
```

✅ Test passed - Environment inheritance working correctly
