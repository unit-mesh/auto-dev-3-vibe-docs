// Test file with UTF-8 characters including emojis
package cc.unitmesh.test

/**
 * 🤖 Auto-starting analysis with multiple UTF-8 characters
 * This class tests parsing of files with emojis and other multi-byte UTF-8 characters.
 */
class TestClass {
    // 测试中文注释
    fun helloWorld() {
        println("Hello 世界 🌍")
        println("Testing emoji 🚀 parsing")
    }
    
    // Function with emoji in name is not valid Kotlin, but emoji in comments is fine
    fun processData() {
        // 处理数据
        val message = "Success ✅"
        val error = "Error ❌"
        val warning = "Warning ⚠️"
    }
    
    /**
     * Multi-line comment with emojis
     * 🔍 Analyzing modified code structure...
     * ✅ Code analysis complete
     */
    fun analyze() {
        // This should not cause "Range out of bounds" error
        println("Analysis 完成 🎉")
    }
}

