#!/usr/bin/env kotlin

/**
 * Test script to demonstrate the new LinterSummary output
 */

data class LintIssue(
    val line: Int,
    val column: Int = 0,
    val severity: LintSeverity,
    val message: String,
    val rule: String? = null,
    val suggestion: String? = null,
    val filePath: String? = null
)

enum class LintSeverity {
    ERROR,
    WARNING,
    INFO
}

data class FileLintSummary(
    val filePath: String,
    val linterName: String,
    val totalIssues: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val topIssues: List<LintIssue>,
    val hasMoreIssues: Boolean
)

data class LinterSummary(
    val totalFiles: Int,
    val filesWithIssues: Int,
    val totalIssues: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val fileIssues: List<FileLintSummary>,
    val executedLinters: List<String>
) {
    companion object {
        fun format(linterSummary: LinterSummary): String {
            return buildString {
                appendLine("## Lint Results Summary")
                appendLine("Files analyzed: ${linterSummary.totalFiles} | Files with issues: ${linterSummary.filesWithIssues}")
                appendLine("Total issues: ${linterSummary.totalIssues} (❌ ${linterSummary.errorCount} errors, ⚠️ ${linterSummary.warningCount} warnings, ℹ️ ${linterSummary.infoCount} info)")
                
                if (linterSummary.executedLinters.isNotEmpty()) {
                    appendLine("Linters executed: ${linterSummary.executedLinters.joinToString(", ")}")
                }
                appendLine()

                if (linterSummary.fileIssues.isNotEmpty()) {
                    // Group by severity priority: errors first, then warnings, then info
                    val filesWithErrors = linterSummary.fileIssues.filter { it.errorCount > 0 }
                    val filesWithWarnings = linterSummary.fileIssues.filter { it.errorCount == 0 && it.warningCount > 0 }
                    val filesWithInfo = linterSummary.fileIssues.filter { it.errorCount == 0 && it.warningCount == 0 && it.infoCount > 0 }
                    
                    if (filesWithErrors.isNotEmpty()) {
                        appendLine("### ❌ Files with Errors (${filesWithErrors.size})")
                        filesWithErrors.forEach { file ->
                            appendLine("**${file.filePath}** (${file.errorCount} errors, ${file.warningCount} warnings)")
                            file.topIssues.forEach { issue ->
                                appendLine("  - Line ${issue.line}: ${issue.message} [${issue.rule ?: "unknown"}]")
                            }
                            if (file.hasMoreIssues) {
                                appendLine("  - ... and ${file.totalIssues - file.topIssues.size} more issues")
                            }
                        }
                        appendLine()
                    }
                    
                    if (filesWithWarnings.isNotEmpty()) {
                        appendLine("### ⚠️ Files with Warnings (${filesWithWarnings.size})")
                        filesWithWarnings.forEach { file ->
                            appendLine("**${file.filePath}** (${file.warningCount} warnings)")
                            file.topIssues.take(3).forEach { issue ->
                                appendLine("  - Line ${issue.line}: ${issue.message} [${issue.rule ?: "unknown"}]")
                            }
                            if (file.hasMoreIssues) {
                                appendLine("  - ... and ${file.totalIssues - file.topIssues.size} more issues")
                            }
                        }
                        appendLine()
                    }
                    
                    if (filesWithInfo.isNotEmpty() && filesWithInfo.size <= 5) {
                        appendLine("### ℹ️ Files with Info (${filesWithInfo.size})")
                        filesWithInfo.forEach { file ->
                            appendLine("**${file.filePath}** (${file.infoCount} info)")
                        }
                    }
                } else {
                    appendLine("✅ No issues found!")
                }
            }
        }
    }
}

fun main() {
    println("=" .repeat(80))
    println("LINTER SUMMARY TEST - User-Focused Output")
    println("=" .repeat(80))
    println()
    
    // Test Case 1: Realistic code review scenario with multiple files
    println("TEST CASE 1: Multiple files with various issues")
    println("-" .repeat(80))
    
    val fileIssues1 = listOf(
        FileLintSummary(
            filePath = "src/main/kotlin/UserService.kt",
            linterName = "detekt",
            totalIssues = 5,
            errorCount = 2,
            warningCount = 3,
            infoCount = 0,
            topIssues = listOf(
                LintIssue(15, 0, LintSeverity.ERROR, "Nullable type expected but non-null type found", "type-mismatch"),
                LintIssue(23, 8, LintSeverity.ERROR, "Unresolved reference: userId", "unresolved-reference"),
                LintIssue(45, 0, LintSeverity.WARNING, "Function name should be in camelCase", "naming-convention"),
                LintIssue(67, 12, LintSeverity.WARNING, "Consider using let instead of if-null check", "idiomatic-kotlin"),
                LintIssue(89, 0, LintSeverity.WARNING, "Magic number should be extracted to constant", "magic-number")
            ),
            hasMoreIssues = false
        ),
        FileLintSummary(
            filePath = "src/main/kotlin/ProductRepository.kt",
            linterName = "detekt",
            totalIssues = 3,
            errorCount = 1,
            warningCount = 2,
            infoCount = 0,
            topIssues = listOf(
                LintIssue(12, 0, LintSeverity.ERROR, "Missing return statement", "missing-return"),
                LintIssue(34, 5, LintSeverity.WARNING, "Unused import", "unused-import"),
                LintIssue(56, 0, LintSeverity.WARNING, "Function too complex, consider refactoring", "complexity")
            ),
            hasMoreIssues = false
        ),
        FileLintSummary(
            filePath = "src/main/kotlin/utils/StringExtensions.kt",
            linterName = "detekt",
            totalIssues = 2,
            errorCount = 0,
            warningCount = 2,
            infoCount = 0,
            topIssues = listOf(
                LintIssue(8, 0, LintSeverity.WARNING, "Function parameter could be immutable", "var-could-be-val"),
                LintIssue(15, 0, LintSeverity.WARNING, "Documentation missing for public function", "missing-kdoc")
            ),
            hasMoreIssues = false
        )
    )
    
    val summary1 = LinterSummary(
        totalFiles = 5,
        filesWithIssues = 3,
        totalIssues = 10,
        errorCount = 3,
        warningCount = 7,
        infoCount = 0,
        fileIssues = fileIssues1,
        executedLinters = listOf("detekt")
    )
    
    println(LinterSummary.format(summary1))
    println()
    
    // Test Case 2: Clean code with no issues
    println("\nTEST CASE 2: Clean code - no issues found")
    println("-" .repeat(80))
    
    val summary2 = LinterSummary(
        totalFiles = 3,
        filesWithIssues = 0,
        totalIssues = 0,
        errorCount = 0,
        warningCount = 0,
        infoCount = 0,
        fileIssues = emptyList(),
        executedLinters = listOf("detekt", "ktlint")
    )
    
    println(LinterSummary.format(summary2))
    println()
    
    // Test Case 3: File with many issues (truncated)
    println("\nTEST CASE 3: File with many issues (shows top 5 + truncation)")
    println("-" .repeat(80))
    
    val manyIssues = (1..10).map { i ->
        LintIssue(
            line = i * 10,
            column = 0,
            severity = if (i <= 3) LintSeverity.ERROR else LintSeverity.WARNING,
            message = "Issue $i: Sample problem description",
            rule = "rule-$i"
        )
    }
    
    val fileIssues3 = listOf(
        FileLintSummary(
            filePath = "src/main/kotlin/LegacyCode.kt",
            linterName = "detekt",
            totalIssues = 10,
            errorCount = 3,
            warningCount = 7,
            infoCount = 0,
            topIssues = manyIssues.take(5),
            hasMoreIssues = true
        )
    )
    
    val summary3 = LinterSummary(
        totalFiles = 1,
        filesWithIssues = 1,
        totalIssues = 10,
        errorCount = 3,
        warningCount = 7,
        infoCount = 0,
        fileIssues = fileIssues3,
        executedLinters = listOf("detekt")
    )
    
    println(LinterSummary.format(summary3))
    println()
    
    // Test Case 4: Multiple linters
    println("\nTEST CASE 4: Multiple linters on different file types")
    println("-" .repeat(80))
    
    val fileIssues4 = listOf(
        FileLintSummary(
            filePath = "src/components/App.tsx",
            linterName = "eslint",
            totalIssues = 2,
            errorCount = 1,
            warningCount = 1,
            infoCount = 0,
            topIssues = listOf(
                LintIssue(25, 10, LintSeverity.ERROR, "React Hook \"useState\" is called conditionally", "react-hooks/rules-of-hooks"),
                LintIssue(42, 5, LintSeverity.WARNING, "Missing dependency in useEffect hook", "react-hooks/exhaustive-deps")
            ),
            hasMoreIssues = false
        ),
        FileLintSummary(
            filePath = "src/services/api.py",
            linterName = "ruff",
            totalIssues = 1,
            errorCount = 0,
            warningCount = 1,
            infoCount = 0,
            topIssues = listOf(
                LintIssue(18, 0, LintSeverity.WARNING, "Line too long (95 > 88 characters)", "E501")
            ),
            hasMoreIssues = false
        )
    )
    
    val summary4 = LinterSummary(
        totalFiles = 4,
        filesWithIssues = 2,
        totalIssues = 3,
        errorCount = 1,
        warningCount = 2,
        infoCount = 0,
        fileIssues = fileIssues4,
        executedLinters = listOf("eslint", "ruff", "detekt")
    )
    
    println(LinterSummary.format(summary4))
    println()
    
    println("=" .repeat(80))
    println("USER PERSPECTIVE ANALYSIS")
    println("=" .repeat(80))
    println("""
    Key observations about the new summary format:
    
    1. ✅ PRIORITY FOCUS: Errors are shown first, then warnings, then info
       - Users immediately see critical issues that block the build
       - Can quickly assess if they need to fix something before code review
    
    2. ✅ ACTIONABLE INFORMATION: Each issue shows:
       - Exact line number (where to look)
       - Clear message (what's wrong)
       - Rule name (for reference/learning)
       - No noise about linter installation/availability
    
    3. ✅ CONCISE BUT COMPLETE:
       - Top-level stats give quick overview
       - Issues are truncated to top 5 per file (with "more" indicator)
       - Avoids overwhelming output while showing what matters
    
    4. ✅ MULTI-FILE CLARITY:
       - Files grouped by severity
       - Easy to see which files need immediate attention
       - File paths are prominent
    
    5. ✅ MULTI-LINTER SUPPORT:
       - Shows which linters actually ran
       - Handles different linters for different file types
       - Unified output format regardless of linter source
    
    6. ✅ CLEAN CODE FEEDBACK:
       - When no issues found, gives positive confirmation
       - Users know linters ran successfully
    
    What we DON'T show (and don't need to):
    - Linter installation instructions (not relevant during code review)
    - Which files could be linted but weren't (not actionable)
    - Linter version numbers (rarely matters for the review)
    - File-to-linter mappings (implicit from execution)
    
    This summary provides exactly what a developer needs during code review:
    - What files have problems?
    - How serious are they?
    - What are the specific issues?
    - Where do I need to look?
    """.trimIndent())
}

main()
