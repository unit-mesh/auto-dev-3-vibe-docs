#!/usr/bin/env kotlin

@file:DependsOn("cc.unitmesh:mpp-core:1.0.0")

import cc.unitmesh.devins.lexer.DevInsLexer

fun main() {
    val testCases = mapOf(
        "Email address" to "user@example.com",
        "Text with email" to "Contact: user@example.com for help",
        "Path" to "Path: /home/user/file.txt",
        "Command" to "/file test.txt",
        "Markdown list" to "- Item with text",
        "Markdown heading" to "## Task Complete: Spring AI Successfully Added"
    )
    
    testCases.forEach { (name, input) ->
        println("\n=== $name: '$input' ===")
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()
        tokens.filter { !it.isEof }.forEach { token ->
            println("  ${token.type.name.padEnd(20)} : '${token.text}'")
        }
    }
}

main()

