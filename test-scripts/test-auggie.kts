#!/usr/bin/env kscript

/**
 * Simple test to verify ACP protocol works with auggie
 */

import java.io.BufferedReader
import java.io.InputStreamReader

val pb = ProcessBuilder("auggie", "--acp")
pb.directory(java.io.File("/Users/phodal/IdeaProjects/ddd-lite-example"))
pb.redirectErrorStream(false)

println("Starting auggie --acp...")
val process = pb.start()

val outputReader = BufferedReader(InputStreamReader(process.inputStream))
val errorReader = BufferedReader(InputStreamReader(process.errorStream))

// Read stderr in background
Thread {
    errorReader.lines().forEach { line ->
        println("STDERR: $line")
    }
}.start()

// Send initialize
val initRequest = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":1,"capabilities":{"fs":{"readTextFile":true,"writeTextFile":true},"terminal":true},"implementation":{"name":"test","version":"1.0","title":"Test"}}}"""

println("Sending: $initRequest")
process.outputStream.write((initRequest + "\n").toByteArray())
process.outputStream.flush()

// Read response
println("Waiting for initialize response...")
val initResponse = outputReader.readLine()
println("Init response: $initResponse")

// Send session/new
Thread.sleep(500)
val sessionRequest = """{"jsonrpc":"2.0","id":2,"method":"session/new","params":{"cwd":"/Users/phodal/IdeaProjects/ddd-lite-example","mcpServers":[]}}"""

println("\nSending: $sessionRequest")
process.outputStream.write((sessionRequest + "\n").toByteArray())
process.outputStream.flush()

// Read all responses for next 5 seconds
println("Reading session responses...")
val startTime = System.currentTimeMillis()
while (System.currentTimeMillis() - startTime < 5000) {
    if (outputReader.ready()) {
        val line = outputReader.readLine()
        if (line != null) {
            println("Response: $line")
        }
    }
    Thread.sleep(100)
}

println("\nKilling process...")
process.destroy()
println("Done!")
