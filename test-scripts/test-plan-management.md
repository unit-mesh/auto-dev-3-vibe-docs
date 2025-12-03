# Plan Management Tool Test

## Overview

This document describes how to test the Plan Management Tool for CodingAgent.

## Test with CodingCli

```bash
./gradlew :mpp-ui:runCodingCli \
  -PcodingProjectPath=/path/to/your/project \
  -PcodingTask="Create a Tag system with Tag entity, TagRepository, TagService, and TagController"
```

## Tested Scenarios

### Scenario 1: Simple Analysis Task
```bash
./gradlew :mpp-ui:runCodingCli \
  -PcodingProjectPath=/Users/phodal/IdeaProjects/untitled \
  -PcodingTask="分析这个项目的结构，告诉我有哪些文件"
```
**Result**: Agent uses `glob` tool directly without creating a plan (correct for simple tasks)

### Scenario 2: Multi-file Creation Task
```bash
./gradlew :mpp-ui:runCodingCli \
  -PcodingProjectPath=/Users/phodal/IdeaProjects/untitled \
  -PcodingTask="创建一个新的 Tag 标签系统，包括：1. Tag 实体类 2. TagRepository 3. TagService 4. TagController"
```
**Result**: 
- Agent creates a plan with 4 tasks
- Creates all files successfully
- Marks steps as completed using `COMPLETE_STEP`

## Tool Actions

- `CREATE`: Create a new plan from markdown
- `UPDATE`: Update existing plan with new markdown
- `COMPLETE_STEP`: Mark a step as completed (taskIndex and stepIndex are 1-based)
- `FAIL_STEP`: Mark a step as failed
- `VIEW`: View the current plan

## Unit Tests

```bash
./gradlew :mpp-core:jvmTest --tests "cc.unitmesh.agent.plan.*" --tests "cc.unitmesh.agent.tool.impl.PlanManagementToolTest"
```

## Files Created

- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/plan/` - Core plan data models
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/PlanManagementTool.kt` - Tool implementation
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/PlanPanel.kt` - Compose UI component

