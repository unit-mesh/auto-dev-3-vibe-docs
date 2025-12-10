package cc.unitmesh.agent.chatdb

/**
 * Test script to compare NLP tokenizer vs fallback tokenizer for Chinese text.
 * 
 * Run with:
 * ```bash
 * cd mpp-core && ../gradlew jvmTest --tests "cc.unitmesh.agent.chatdb.NlpTokenizerComparisonTest"
 * ```
 */
fun main() {
    val stopWords = setOf(
        "select", "from", "where", "and", "or", "not", "in", "is", "null",
        "order", "by", "group", "having", "limit", "offset", "join", "on",
        "the", "a", "an", "show", "me", "get", "find", "list", "display"
    )
    
    val testQueries = listOf(
        "查询所有用户的订单金额",
        "显示最近一个月的销售数据",
        "统计每个部门的员工人数",
        "找出购买金额最高的前10个客户",
        "Show me the top 10 customers by order amount",
        "查询用户表中年龄大于30的用户",
        "统计2024年每月的销售额",
        "显示所有未支付的订单"
    )
    
    println("=" .repeat(80))
    println("NLP Tokenizer vs Fallback Tokenizer Comparison")
    println("=".repeat(80))
    println()
    
    for (query in testQueries) {
        println("Query: $query")
        println("-".repeat(60))
        
        // NLP Tokenizer (MyNLP on JVM)
        val nlpKeywords = NlpTokenizer.extractKeywords(query, stopWords)
        println("NLP Keywords:      ${nlpKeywords.joinToString(", ")}")
        
        // Fallback Tokenizer
        val fallbackKeywords = FallbackTokenizer.extractKeywords(query, stopWords)
        println("Fallback Keywords: ${fallbackKeywords.joinToString(", ")}")
        
        println()
    }
}

