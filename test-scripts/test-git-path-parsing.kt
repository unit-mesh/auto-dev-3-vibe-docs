#!/usr/bin/env kotlin

/**
 * Test script to verify git rename/move path parsing logic
 * Tests the regex pattern used in GitOperations.jvm.kt
 */

fun main() {
    // Test cases for git rename/move patterns
    val testCases = listOf(
        // Case 1: Move into subdirectory
        "ai-core/src/main/kotlin/com/phodal/lotus/aicore/client/{ => langchain}/LangChain4jAIClient.kt" to 
            Pair("ai-core/src/main/kotlin/com/phodal/lotus/aicore/client/langchain/LangChain4jAIClient.kt",
                 "ai-core/src/main/kotlin/com/phodal/lotus/aicore/client/LangChain4jAIClient.kt"),
        
        // Case 2: Another move into subdirectory
        "ai-core/src/main/kotlin/com/phodal/lotus/aicore/{token => client/langchain}/LangChain4jTokenCounter.kt" to
            Pair("ai-core/src/main/kotlin/com/phodal/lotus/aicore/client/langchain/LangChain4jTokenCounter.kt",
                 "ai-core/src/main/kotlin/com/phodal/lotus/aicore/token/LangChain4jTokenCounter.kt"),
        
        // Case 3: Simple rename
        "{old.txt => new.txt}" to
            Pair("new.txt", "old.txt"),
        
        // Case 4: Rename with path
        "src/{OldFile.kt => NewFile.kt}" to
            Pair("src/NewFile.kt", "src/OldFile.kt"),
        
        // Case 5: No rename pattern
        "regular/path/to/file.kt" to
            Pair("regular/path/to/file.kt", null)
    )
    
    val renamePattern = Regex("""(.*)\{(.*) => (.*)\}(.*)""")
    
    testCases.forEachIndexed { index, (input, expected) ->
        var path = input
        var oldPath: String? = null
        
        if (path.contains("{ => ") || path.contains(" => }")) {
            val match = renamePattern.find(path)
            if (match != null) {
                val prefix = match.groupValues[1]
                val oldPart = match.groupValues[2].trim()
                val newPart = match.groupValues[3].trim()
                val suffix = match.groupValues[4]
                
                path = prefix + newPart + suffix
                oldPath = prefix + oldPart + suffix
            }
        }
        
        val result = Pair(path, oldPath)
        val passed = result == expected
        
        println("Test case ${index + 1}: ${if (passed) "✓ PASS" else "✗ FAIL"}")
        println("  Input:    $input")
        println("  Expected: $expected")
        println("  Got:      $result")
        if (!passed) {
            println("  ❌ MISMATCH!")
        }
        println()
    }
}

main()
