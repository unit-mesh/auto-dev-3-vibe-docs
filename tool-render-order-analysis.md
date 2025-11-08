# Tool Rendering Order Issue - Root Cause Analysis

## Problem Statement
When running AutoDev CLI with the DeepSeek LLM, tool calls are not displayed in the correct order. Instead of:
```
AI thinks: ...
● File search
⎿ Searching for files...
● Read file  
⎿ Reading file...
```

We get:
```
AI thinks: [long LLM response]
## Analysis result
[AI summary]
● File search
● Read file
...
```

All tool calls appear at the very end instead of interspersed with the AI's thinking.

## Root Cause Analysis

### The Core Issue
The problem is NOT about buffering or stdout flushing. The real issue is:

**The LLM sends a complete text response that includes the agent's thinking AND the tool calls formatted as text**, before any actual tool execution happens.

### Execution Flow
```
1. LLM generates response: "我将查看项目文件。\n\n● File search\n...● Read file\n..." 
2. This ENTIRE response is:
   - Collected into `llmResponse` buffer (lines 70-77 in CodingAgentExecutor.kt)
   - Streamed through `renderLLMResponseChunk()` (displayed immediately)
   - Then stored with `addAssistantResponse()` (line 79)
3. Only AFTER the LLM response is complete:
   - `parseToolCalls()` extracts tool calls from the response (line 81)
   - `executeToolCalls()` actually executes them (line 83)
   - The tools render themselves when executing
```

### Why Tools Appear at the End
The LLM response that came from DeepSeek includes:
1. Agent's thinking/analysis text
2. Tool calls formatted as markdown (●) included in the LLM text itself

When this entire response is rendered through `renderLLMResponseChunk`, all the tool calls are just treated as text and displayed. Then when parsed tools execute, their `renderToolCall` output appears at the very end.

## The Real Problem with Current Implementation

Looking at the test output:
```
我将按照您的要求一步步分析这个项目的结构。首先让我从根目录开始。
现在让我查看src文件夹下的内容：
...
现在让我使用代码分析代理来深入分析项目架构：
## 项目结构分析结果
...
● File search - pattern matcher     ← Tools appear here, AFTER all thinking
```

The tool calls are being **rendered twice**:
1. First: As text within the LLM response (showing the agent's planned actions)
2. Second: As actual tool execution (showing what really happened)

## Solution Required

The issue is that we need to:

1. **Filter out tool call markdown from LLM responses** - Don't render them as part of the LLM thinking
2. **Only show tool calls when they actually execute** - From `renderToolCall()` during execution

This means we need to:
- Pre-process the LLM response to remove tool call markdown before rendering
- Ensure `renderLLMResponseChunk` doesn't display planned tool calls
- Only render them through `renderToolCall` when they're actually being executed

## Implementation Strategy

Instead of displaying tool calls from the LLM text stream, we should:

1. Filter the LLM response to remove tool call lines (lines starting with `●`)
2. Display only the AI's thinking/analysis
3. When tools actually execute, show them via `renderToolCall`

Example:
```
LLM Response: "我将查看项目。\n● File search\n⎿ Searching"
Filtered: "我将查看项目。"
Rendered: "我将查看项目。"
```

Then when tool executes:
```
● File search - pattern matcher (from renderToolCall during execution)
⎿ Searching for files (from renderToolResult during execution)
```

This gives us the correct order without modifying the streaming architecture.
