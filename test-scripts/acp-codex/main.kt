/*
 * This file is a **design probe** (not production code).
 *
 * Goal:
 * - Understand how to integrate a Codex-style agent into Xiuper's Compose GUI.
 * - Compare 2 candidate integration layers:
 *   1) ACP (Agent Client Protocol): editor (client) <-> agent server (stdio/websocket)
 *   2) codex "app-server" JSON-RPC v2: client <-> `codex app-server` (stdio)
 *
 * What we learned from the local JetBrains ML-LLM distribution:
 * - `intellij.ml.llm.agents.codex` installs **Codex CLI** and a separate **ACP server**.
 *   It has registry keys:
 *   - llm.chat.agent.codex.install.codex.version = 0.87.0
 *   - llm.chat.agent.codex.install.acp.server.version = 0.0.18
 * - It depends on `agentclientprotocol.acp.*` jars and uses ACP as the editor<->agent transport layer.
 *
 * What we learned from `/Users/phodal/ai/codex`:
 * - Codex ships an experimental `codex app-server` that speaks a JSON-RPC protocol (v2)
 *   over stdio. It supports thread/turn, streaming deltas, and approval requests
 *   (command execution + file changes).
 *
 * Implication for Xiuper:
 * - For the best long-term architecture, implement an ACP **client** inside mpp-ui (Desktop JVM first),
 *   and support launching a local ACP agent server (stdio) configured by the user.
 * - For Codex specifically, since open-source `codex` does not natively speak ACP today, we likely need:
 *   - a Codex ACP server binary (like JetBrains'), OR
 *   - a small "codex-acp-wrapper" that:
 *     - speaks ACP to the editor
 *     - speaks `codex app-server` JSON-RPC to Codex
 *     - maps streaming/tool/approval events between the two protocols
 *
 * This probe sketches the ACP client event loop and how to map ACP session updates to Xiuper's renderer.
 *
 * References:
 * - ACP protocol repo: https://github.com/agentclientprotocol/agent-client-protocol
 * - Codex CLI/app-server: /Users/phodal/ai/codex (see codex-rs/debug-client/)
 */

@file:Suppress("unused")

package docs.test_scripts.acp_codex

import com.agentclientprotocol.client.Client
import com.agentclientprotocol.client.ClientInfo
import com.agentclientprotocol.client.ClientOperationsFactory
import com.agentclientprotocol.common.Event
import com.agentclientprotocol.common.SessionCreationParameters
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.protocol.Protocol
import com.agentclientprotocol.protocol.ProtocolOptions
import com.agentclientprotocol.transport.StdioTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource

/**
 * Usage idea (run with ACP jars on classpath):
 * - Launch an ACP agent server process (stdio), then connect using StdioTransport.
 *
 * NOTE: This is a probe. Xiuper production integration should live under mpp-ui JVM sources
 * and reuse Xiuper's filesystem/shell abstractions instead of directly touching java.io.
 */
fun main() = runBlocking {
    // Placeholder command. For Codex via ACP you need an ACP-compatible agent server binary.
    // JetBrains uses a dedicated "Codex ACP server" (separate from `codex` CLI).
    val cmd = listOf("codex-acp-server", "--stdio")

    val process = ProcessBuilder(cmd)
        .redirectErrorStream(true)
        .start()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val transport = StdioTransport(
        scope = scope,
        ioDispatcher = Dispatchers.IO,
        input = process.inputStream.asSource(),
        output = process.outputStream.asSink(),
        name = "codex-acp-stdio"
    )

    val protocol = Protocol(
        scope = scope,
        transport = transport,
        options = ProtocolOptions(
            requestTimeout = 60_000,
            gracefulRequestCancellationTimeout = 3_000,
            protocolDebugName = "xiuper-acp-probe",
            null
        )
    )

    val client = Client(protocol)
    protocol.start()

    // Minimal initialize handshake.
    client.initialize(
        ClientInfo(
            name = "xiuper",
            title = "Xiuper ACP Probe",
            version = "0.0.0"
        ),
        null
    )

    // In Xiuper, these ops should map to our existing tools:
    // - file system ops -> ToolFileSystem / read/write tools
    // - terminal ops -> ShellExecutor / live terminal sessions
    // - permission requests -> CodingAgentRenderer.renderUserConfirmationRequest
    val opsFactory: ClientOperationsFactory = object : ClientOperationsFactory {
        override fun create(
            sessionId: String,
            parameters: SessionCreationParameters
        ) = object : com.agentclientprotocol.common.ClientSessionOperations {}
    }

    val session = client.newSession(
        parameters = SessionCreationParameters(
            rootPath = ".",
            additionalWriteableDirectories = emptyList(),
            clientId = "xiuper-probe"
        ),
        operationsFactory = opsFactory
    )

    val flow = session.prompt(
        prompt = listOf(ContentBlock.Text("Explain what you will do.")),
        meta = null
    )

    flow.collect { event ->
        when (event) {
            is Event.PromptResponseEvent -> {
                println("[prompt complete] stopReason=${event.response.stopReason}")
            }
            is Event.SessionUpdateEvent -> {
                handleSessionUpdate(event.update)
            }
            else -> {
                // ignore
            }
        }
    }
}

private fun handleSessionUpdate(update: com.agentclientprotocol.model.SessionUpdate) {
    when (update) {
        is SessionUpdate.AgentMessageChunk -> {
            // Map to Xiuper: renderer.renderLLMResponseChunk(...)
            println(update.content)
        }
        is SessionUpdate.AgentThoughtChunk -> {
            // Map to Xiuper: renderer.renderThinkingChunk(...)
            println("[think] ${update.content}")
        }
        is SessionUpdate.ToolCallUpdate -> {
            // Map to Xiuper:
            // - renderer.renderToolCallWithParams(...)
            // - renderer.renderToolResult(...)
            println("[tool] ${update.title} status=${update.status}")
        }
        is SessionUpdate.PlanUpdate -> {
            // Map to Xiuper: renderer.renderPlanSummary(...)
            println("[plan] $update")
        }
        else -> {
            println("[update] $update")
        }
    }
}

