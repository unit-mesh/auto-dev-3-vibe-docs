# CodeReview Bug Fixes

## Summary

Fixed two critical bugs in the CodeReview feature:

1. **Byte-to-Character Offset Bug**: TreeSitter parser was using byte offsets with character indices, causing "Range out of bounds" errors when parsing files with UTF-8 characters (emojis, Chinese characters, etc.)
2. **Patch Application Bug**: The diff patch application logic had incorrect line index tracking, causing patches to be applied at wrong positions

## Bug 1: Range Out of Bounds Error

### Problem

Error messages like:
```
Failed to parse mpp-core/src/commonMain/kotlin/.../CodeReviewAgentTemplate.kt: 
Range [10881, 12961) out of bounds for length 12263
```

### Root Cause

TreeSitter returns **byte offsets** (`startIndex`, `endIndex`), but Kotlin's `String.substring()` expects **character indices**. When files contain multi-byte UTF-8 characters:
- Emoji "🤖" = 1 character but 4 bytes
- Chinese "中文" = 2 characters but 6 bytes

This causes byte offset 10881 to exceed the character length of 12263.

### Files Fixed

1. **mpp-codegraph/src/jsMain/kotlin/cc/unitmesh/codegraph/parser/js/JsCodeParser.kt**
   - Line 248-252: `extractNodeText()` function

2. **mpp-codegraph/src/jvmMain/kotlin/cc/unitmesh/codegraph/parser/jvm/JvmCodeParser.kt**
   - Line 216-220: `extractNodeText()` function

### Solution

Convert byte offsets to character indices by working with byte arrays:

```kotlin
// BEFORE (incorrect)
private fun extractNodeText(node: dynamic, sourceCode: String): String {
    val startByte = node.startIndex as Int
    val endByte = node.endIndex as Int
    return sourceCode.substring(startByte, endByte)  // ❌ Uses byte offsets as char indices
}

// AFTER (correct)
private fun extractNodeText(node: dynamic, sourceCode: String): String {
    val startByte = node.startIndex as Int
    val endByte = node.endIndex as Int
    
    // TreeSitter returns byte offsets, but String.substring uses character indices
    // Convert byte offsets to character offsets
    val bytes = sourceCode.encodeToByteArray()
    
    // Validate byte offsets
    if (startByte < 0 || endByte > bytes.size || startByte > endByte) {
        return ""
    }
    
    // Extract the byte range and convert back to string
    return bytes.sliceArray(startByte until endByte).decodeToString()  // ✅ Correct
}
```

Note: **WasmJsCodeParser** already used `node.text` which handles this correctly internally.

## Bug 2: Patch Application Index Tracking

### Problem

When applying diff patches, the code modified `currentLines` in-place without tracking how insertions/deletions shift subsequent line numbers. This caused patches to be applied at incorrect positions.

### Root Cause

```kotlin
// BEFORE (incorrect)
hunk.lines.forEach { diffLine ->
    when (diffLine.type) {
        DiffLineType.DELETED -> {
            currentLines.removeAt(oldLineIndex)  // Shifts all subsequent lines up
            // ❌ oldLineIndex doesn't account for the shift
        }
        DiffLineType.ADDED -> {
            currentLines.add(oldLineIndex, diffLine.content)  // Shifts subsequent lines down
            oldLineIndex++
            // ❌ Subsequent operations use wrong indices
        }
    }
}
```

### Files Fixed

1. **mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/CodeReviewViewModel.kt**
   - Lines 847-903: `applyDiffPatchToFile()` function

### Solution

Track the cumulative offset between original and current line numbers:

```kotlin
// AFTER (correct)
var lineOffset = 0  // Track cumulative line shifts

fileDiff.hunks.forEach { hunk ->
    // Convert original line numbers to current line numbers
    var currentLineIndex = maxOf(0, hunk.oldStartLine - 1) + lineOffset
    var oldLineNum = maxOf(1, hunk.oldStartLine)

    hunk.lines.forEach { diffLine ->
        when (diffLine.type) {
            DiffLineType.CONTEXT -> {
                currentLineIndex++
                oldLineNum++
            }
            DiffLineType.DELETED -> {
                if (currentLineIndex < currentLines.size) {
                    currentLines.removeAt(currentLineIndex)
                    lineOffset--  // ✅ Track that subsequent lines shifted up
                    oldLineNum++
                }
            }
            DiffLineType.ADDED -> {
                if (currentLineIndex <= currentLines.size) {
                    currentLines.add(currentLineIndex, diffLine.content)
                    lineOffset++  // ✅ Track that subsequent lines shifted down
                    currentLineIndex++
                }
            }
        }
    }
}
```

### Additional Fixes

- **Empty File Handling**: `String.lines()` returns `[""]` for empty strings, not an empty list
- **New File Handling**: For new files, `oldStartLine` is 0, so we use `maxOf(0, oldStartLine - 1)`

## Tests Added

### 1. Patch Application Tests

**File**: `mpp-ui/src/commonTest/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/PatchApplicationTest.kt`

Tests cover:
- Simple additions and deletions
- Multiple changes in one hunk
- Multiple hunks
- UTF-8 characters and emojis in patches
- Complex Kotlin code patches
- Empty file additions

### 2. UTF-8 Parsing Tests

**File**: `mpp-codegraph/src/commonTest/kotlin/cc/unitmesh/codegraph/parser/Utf8ParsingTest.kt`

Tests verify:
- Parsing Kotlin files with emojis and Chinese characters
- Parsing Java files with UTF-8 characters
- Correct content extraction with emojis

### 3. Test Script

**File**: `docs/test-scripts/test-utf8-parsing.kt`

Sample Kotlin file with emojis and UTF-8 characters for manual testing.

## Testing & Verification

All tests pass:

```bash
# Patch application tests
./gradlew :mpp-ui:jvmTest --tests "PatchApplicationTest"  # ✅ 9 tests passed

# UTF-8 parsing tests
./gradlew :mpp-codegraph:jvmTest --tests "Utf8ParsingTest"  # ✅ 3 tests passed

# Full test suites
./gradlew :mpp-core:jvmTest  # ✅ All tests passed
./gradlew :mpp-codegraph:jvmTest  # ✅ All tests passed
./gradlew :mpp-ui:jvmTest  # ✅ All tests passed
```

## Impact

### Before
- ❌ CodeReview failed on files with emojis or UTF-8 characters
- ❌ Patch application applied changes at wrong line positions
- ❌ "Range out of bounds" errors in logs

### After
- ✅ CodeReview correctly parses all UTF-8 files
- ✅ Patches applied at correct positions
- ✅ No range errors
- ✅ Comprehensive test coverage

## How to Test

### Test with Detekt (as user suggested)

```bash
# 1. Build mpp-core
./gradlew :mpp-core:assembleJsPackage

# 2. Run CodeReview on files with UTF-8 characters
cd mpp-ui && npm run build && npm run start

# 3. Use gitlot + detekt linter
# The parsing should now work correctly with files containing emojis and UTF-8 characters
```

### Manual Test with Sample File

```bash
# Use the test script
cat docs/test-scripts/test-utf8-parsing.kt

# Run CodeReview on a commit containing this file
```

## Related Files

- CodeAnalyzer: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/analysis/CodeAnalyzer.kt`
- DiffParser: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/diff/DiffModels.kt`
- SuggestedFixesSection: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/SuggestedFixesSection.kt`
- DetektLinter: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/linter/linters/DetektLinter.kt`

## Notes

- The WasmJs implementation was already correct (using `node.text`)
- The fixes are minimal and focused on the root causes
- All existing tests continue to pass
- The fixes are backward compatible

