# Code Review with Git Diff Support

## Overview

`CodeReviewAgentExecutor` now supports Git diff information in `ReviewTask`, enabling true code review based on actual code changes rather than entire files.

## What Changed

### Before
```kotlin
val task = ReviewTask(
    projectPath = "/path/to/project",
    filePaths = listOf("src/File1.kt", "src/File2.kt"),
    reviewType = ReviewType.COMPREHENSIVE
)
```

**Problem**: The executor would read entire files, making it unclear what actually changed.

### After
```kotlin
// Get git diff information
val gitOps = GitOperations.create("/path/to/project")
val diffInfo = gitOps.getDiff("main", "feature-branch")

val task = ReviewTask(
    projectPath = "/path/to/project",
    reviewType = ReviewType.COMPREHENSIVE,
    gitDiffInfo = diffInfo  // ✅ Now includes actual code changes
)
```

**Benefit**: The executor receives the actual diff, showing exactly what changed with context.

## Data Structure

### GitDiffInfo
```kotlin
@Serializable
data class GitDiffInfo(
    val files: List<GitDiffFile>,
    val totalAdditions: Int,
    val totalDeletions: Int
)
```

### GitDiffFile
```kotlin
@Serializable
data class GitDiffFile(
    val path: String,
    val oldPath: String? = null,  // For renamed files
    val status: GitFileStatus,     // ADDED, DELETED, MODIFIED, RENAMED, COPIED
    val additions: Int,
    val deletions: Int,
    val diff: String              // The actual unified diff
)
```

### GitFileStatus
```kotlin
@Serializable
enum class GitFileStatus {
    ADDED,
    DELETED,
    MODIFIED,
    RENAMED,
    COPIED
}
```

## Usage Examples

### 1. Review Changes Between Branches
```kotlin
val gitOps = GitOperations.create(projectPath)
val diffInfo = gitOps.getDiff("main", "feature-branch")

val task = ReviewTask(
    projectPath = projectPath,
    reviewType = ReviewType.COMPREHENSIVE,
    gitDiffInfo = diffInfo
)

val executor = CodeReviewAgentExecutor(
    projectPath = projectPath,
    llmService = llmService,
    toolOrchestrator = orchestrator,
    renderer = renderer
)

val result = executor.execute(task, systemPrompt)
```

### 2. Review a Specific Commit
```kotlin
val gitOps = GitOperations.create(projectPath)
val diffInfo = gitOps.getCommitDiff("abc123def")

val task = ReviewTask(
    projectPath = projectPath,
    reviewType = ReviewType.SECURITY,
    gitDiffInfo = diffInfo
)

// Execute review...
```

### 3. Review Unstaged Changes
```kotlin
val gitOps = GitOperations.create(projectPath)
val changedFiles = gitOps.getChangedFiles()

// Collect diffs for each changed file
val diffs = changedFiles.map { file ->
    gitOps.getDiffForFile(file)
}

// Aggregate into GitDiffInfo...
```

## How It Works

### 1. Initial Message Generation

When `gitDiffInfo` is provided, the executor includes the diff in the initial prompt:

```
## Code Changes (Git Diff)

**Summary**: 3 files changed, +25 additions, -18 deletions

### src/main/kotlin/File1.kt (ADDED)
**Changes**: +20 -0

```diff
@@ -0,0 +1,20 @@
+new content
```

### src/main/kotlin/File2.kt (DELETED)
**Changes**: +0 -15

```diff
@@ -1,15 +0,0 @@
-deleted content
```
```

### 2. Reduced Tool Usage

With diff information, the LLM:
- ✅ **Does NOT** need to read entire files
- ✅ **Can** immediately review the changes
- ✅ **Only uses** `read-file` tool for additional context if needed

### 3. Fallback Behavior

If `gitDiffInfo` is `null`, the executor falls back to the old behavior:
- Lists file paths
- Instructs LLM to use `read-file` tool

## Benefits

1. **Accurate Reviews**: LLM sees exactly what changed, with context lines
2. **Efficient**: No need to read entire files
3. **Better Context**: Includes line numbers, additions/deletions
4. **Multi-platform**: Works on JVM, JS, and WASM platforms
5. **Serializable**: Can be passed through JSON APIs

## Migration Guide

### For CLI/UI Code

Update code review invocations to include diff:

```typescript
// Before
const task = {
  projectPath: "/path/to/project",
  filePaths: ["file1.kt", "file2.kt"],
  reviewType: "COMPREHENSIVE"
};

// After
import { GitOperations } from "@autodev/mpp-core";

const gitOps = GitOperations.create(projectPath);
const diffInfo = await gitOps.getDiff("main", "HEAD");

const task = {
  projectPath: "/path/to/project",
  reviewType: "COMPREHENSIVE",
  gitDiffInfo: diffInfo  // ✅ Add this
};
```

### For Testing

Use the helper in tests:

```kotlin
val gitDiff = GitDiffInfo(
    files = listOf(
        GitDiffFile(
            path = "test.kt",
            status = GitFileStatus.MODIFIED,
            additions = 5,
            deletions = 2,
            diff = "@@ -1,2 +1,5 @@\n-old\n+new"
        )
    ),
    totalAdditions = 5,
    totalDeletions = 2
)

val task = ReviewTask(
    projectPath = "/test",
    gitDiffInfo = gitDiff
)
```

## Platform Support

All KMP platforms support the Git diff data structures:
- ✅ JVM
- ✅ JS
- ✅ WASM
- ✅ iOS (via GitOperations.ios.kt)

The `GitOperations` platform-specific implementations handle fetching diffs.
