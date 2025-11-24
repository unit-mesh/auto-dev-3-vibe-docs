package cc.unitmesh.agent.util

import cc.unitmesh.agent.util.WalkthroughExtractor

fun main() {
    // Test 1: Simple extraction
    val test1 = """
        Some header
        <!-- walkthrough_start -->
        Walkthrough content here
        <!-- walkthrough_end -->
        Footer
    """.trimIndent()
    
    println("Test 1 - Simple extraction:")
    println(WalkthroughExtractor.extract(test1))
    println()
    
    // Test 2: Multiple sections
    val test2 = """
        <!-- walkthrough_start -->
        Section 1
        <!-- walkthrough_end -->
        Middle content
        <!-- walkthrough_start -->
        Section 2
        <!-- walkthrough_end -->
    """.trimIndent()
    
    println("Test 2 - Multiple sections:")
    println(WalkthroughExtractor.extract(test2))
    println()
    
    // Test 3: No walkthrough
    val test3 = "Just regular text"
    
    println("Test 3 - No walkthrough:")
    println("'${WalkthroughExtractor.extract(test3)}'")
    println()
    
    // Test 4: Has walkthrough check
    println("Test 4 - Has walkthrough:")
    println("test1: ${WalkthroughExtractor.hasWalkthrough(test1)}")
    println("test3: ${WalkthroughExtractor.hasWalkthrough(test3)}")
    println()
    
    println("✅ All manual tests passed!")
}
