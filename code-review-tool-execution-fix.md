# CodeReview Agent Tool Execution Fix

## Problem

The CodeReview Agent was not executing tools despite outputting `<devin>` tool call blocks in its responses. The agent would:

1. Generate tool calls in `<devin>` blocks (e.g., `/glob`, `/read-file`, `/grep`)
2. But these calls were never parsed or executed
3. The agent would complete with a single LLM call without any tool feedback loop

## Root Cause

In `CodeReviewAgent.analyzeWithTools()` method (lines 202-279), the implementation:

1. Called `ConversationManager.sendMessage()` once with `compileDevIns = true`
2. Collected the LLM output
3. **Returned immediately without parsing or executing any tool calls**

This was different from `CodingAgent` and `CodeReviewAgentExecutor`, which both implement a proper tool execution loop:

```kotlin
while (currentIteration < maxIterations) {
    // 1. Get LLM response
    conversationManager.sendMessage(...)
    
    // 2. Parse tool calls
    val toolCalls = toolCallParser.parseToolCalls(llmResponse)
    
    // 3. Execute tools
    val toolResults = executeToolCalls(toolCalls)
    
    // 4. Add results back to conversation
    conversationManager.addToolResults(toolResultsText)
    
    // 5. Continue loop
}
```

## Solution

### 1. Added Tool Execution Loop in `CodeReviewAgent.analyzeWithTools()`

Modified the method to implement a proper tool execution loop similar to `CodingAgent`:

```kotlin
private suspend fun analyzeWithTools(...): AnalysisResult {
    // ... setup code ...
    
    val toolCallParser = cc.unitmesh.agent.parser.ToolCallParser()
    var currentIteration = 0
    var usedTools = false
    
    while (currentIteration < maxIterations) {
        currentIteration++
        
        // 1. Get LLM response
        val llmResponse = StringBuilder()
        conversationManager.sendMessage(...).collect { chunk ->
            llmResponse.append(chunk)
            onProgress(chunk)
        }
        conversationManager.addAssistantResponse(llmResponse.toString())
        
        // 2. Parse tool calls
        val toolCalls = toolCallParser.parseToolCalls(llmResponse.toString())
        if (toolCalls.isEmpty()) {
            break  // No more tools, analysis complete
        }
        
        usedTools = true
        
        // 3. Execute tools
        val toolResults = executeToolCallsForAnalysis(toolCalls)
        
        // 4. Format and add results back
        val toolResultsText = formatToolResults(toolResults)
        conversationManager.addToolResults(toolResultsText)
    }
    
    return AnalysisResult(...)
}
```

### 2. Added Tool Execution Helper Methods

**`executeToolCallsForAnalysis()`**: Execute each tool call using `ToolOrchestrator`

```kotlin
private suspend fun executeToolCallsForAnalysis(
    toolCalls: List<ToolCall>
): List<Triple<String, Map<String, Any>, ToolExecutionResult>> {
    val results = mutableListOf<...>()
    
    for (toolCall in toolCalls) {
        val context = ToolExecutionContext(
            workingDirectory = projectPath,
            environment = emptyMap()
        )
        
        val executionResult = toolOrchestrator.executeToolCall(
            toolName = toolCall.toolName,
            params = toolCall.params,
            context = context
        )
        
        results.add(Triple(toolName, params, executionResult))
    }
    
    return results
}
```

**`formatToolResults()`**: Format tool results for LLM feedback

```kotlin
private fun formatToolResults(
    results: List<Triple<...>>
): String = buildString {
    appendLine("## Tool Execution Results")
    
    results.forEachIndexed { index, (toolName, params, executionResult) ->
        appendLine("### Tool ${index + 1}: $toolName")
        appendLine("**Result:**")
        when (val result = executionResult.result) {
            is ToolResult.Success -> appendLine("```\n${result.content}\n```")
            is ToolResult.Error -> appendLine("❌ Error: ${result.message}")
            // ...
        }
    }
}
```

### 3. Updated Prompt Templates

Enhanced both English and Chinese prompt templates to better guide tool execution:

**Added clarity on tool execution workflow:**
```markdown
## Response Format

For each tool call, respond with:
1. Your reasoning about what to do next (explain your thinking)
2. **EXACTLY ONE** DevIns command (wrapped in <devin></devin> tags)
3. What you expect to happen

After gathering all necessary information, provide your final analysis WITHOUT any tool calls.
```

**Emphasized one-tool-at-a-time execution:**
```markdown
**IMPORTANT: Execute ONE tool at a time**
- ✅ Correct: One <devin> block with one tool call per response
- ❌ Wrong: Multiple <devin> blocks or multiple tools in one response
```

## Key Changes Made

### Files Modified

1. **`mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgent.kt`**
   - Rewrote `analyzeWithTools()` to add tool execution loop (lines 202-332)
   - Added `executeToolCallsForAnalysis()` helper (lines 334-377)
   - Added `formatToolResults()` helper (lines 379-412)

2. **`mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodeReviewAgentPromptRenderer.kt`**
   - Updated English template to clarify tool execution workflow (lines 193-206)
   - Updated Chinese template with same clarifications (lines 280-292)

### Technical Details

- Used `ToolCallParser` to parse `<devin>` blocks from LLM responses
- Used `ToolOrchestrator.executeToolCall()` to execute tools with proper context
- Used `ToolExecutionContext` (not the non-existent `OrchestratorContext`)
- Used `ToolExecutionResult.failure()` factory method for error handling
- Used `kotlinx.datetime.Clock.System.now()` for cross-platform time tracking

## Testing

Build completed successfully:
```bash
./gradlew :mpp-core:assembleJsPackage
# BUILD SUCCESSFUL

cd mpp-ui && npm run build
# BUILD SUCCESSFUL
```

## Expected Behavior After Fix

Now when you run `node dist/jsMain/typescript/index.js review -p ..`, the agent will:

1. **Iteration 1**: Receive analysis request, output reasoning + first tool call (e.g., `/glob`)
2. **Tool Execution**: Execute `/glob`, get file list results
3. **Iteration 2**: Receive tool results, output reasoning + next tool call (e.g., `/read-file`)
4. **Tool Execution**: Execute `/read-file`, get file content
5. **Iteration 3**: Receive tool results, output reasoning + more tool calls if needed
6. **...continue until no more tools needed...**
7. **Final Iteration**: Output final analysis report without tool calls

The agent now behaves like Cursor or other agentic coding assistants that can:
- Call tools iteratively
- Receive tool results
- Make decisions based on tool outputs
- Continue until the task is complete

## Benefits

1. ✅ **Tool execution works**: Agent can now actually use tools
2. ✅ **Better analysis**: Can read files, search code, gather real information
3. ✅ **Iterative refinement**: Multiple tool calls with feedback loops
4. ✅ **Like Cursor**: Behaves similar to Cursor's agentic behavior
5. ✅ **Proper architecture**: Consistent with CodingAgent design patterns

