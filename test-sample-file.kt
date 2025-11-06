package cc.unitmesh.example

/**
 * This is a test file for demonstrating the File Viewer feature
 * It showcases Kotlin syntax highlighting with RSyntaxTextArea
 */
class TestFileViewer {
    private val greeting = "Hello, File Viewer!"
    
    fun demonstrateSyntaxHighlighting() {
        println(greeting)
        
        // Collections
        val numbers = listOf(1, 2, 3, 4, 5)
        numbers.forEach { num ->
            println("Number: $num")
        }
        
        // Conditionals
        when (numbers.size) {
            in 1..5 -> println("Small list")
            in 6..10 -> println("Medium list")
            else -> println("Large list")
        }
    }
    
    suspend fun demonstrateCoroutines() {
        kotlinx.coroutines.delay(1000)
        println("Coroutine executed!")
    }
    
    data class User(
        val id: Int,
        val name: String,
        val email: String
    )
    
    companion object {
        const val MAX_USERS = 100
        
        @JvmStatic
        fun main(args: Array<String>) {
            val viewer = TestFileViewer()
            viewer.demonstrateSyntaxHighlighting()
        }
    }
}

