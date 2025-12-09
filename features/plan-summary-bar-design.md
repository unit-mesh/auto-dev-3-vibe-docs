# Cross-Platform Plan Summary Bar Design

## Overview

This document describes the design for displaying Plan status above the input box across all platforms:
- **mpp-ui** (Compose Multiplatform: Desktop, Android, iOS, WASM)
- **mpp-idea** (IntelliJ IDEA Plugin with Jewel)
- **mpp-vscode** (VS Code Extension with React)

## User Experience

When the AI Agent creates a plan using the `/plan` tool, a collapsible summary bar appears above the input box:

```
┌─────────────────────────────────────────────────────────────────┐
│                        Chat Messages                            │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📋 Create Tag System                    [60%] ██████░░░░ │   │
│  │   ✓ Step 1  ✓ Step 2  → Step 3  ○ Step 4  ○ Step 5       │   │
│  │                                      [View] [Collapse]   │   │
│  └─────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│  [File Changes Summary - existing component]                    │
├─────────────────────────────────────────────────────────────────┤
│                      Input Box                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Features
1. **Compact View (Default)**: Shows plan title, progress bar, current step
2. **Expanded View**: Shows all tasks and steps with status
3. **Auto-updates**: Reflects real-time progress as steps complete
4. **Dismissible**: Can be collapsed/hidden when not needed

## Architecture

### Shared State (mpp-core)

```
mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/plan/
├── AgentPlan.kt              # Full plan model (existing)
├── PlanStateService.kt       # State management (existing)
├── PlanUpdateListener.kt     # Listener interface (existing)
└── PlanSummaryData.kt        # NEW: Lightweight summary for UI
```

### Platform Implementations

```
mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/editor/plan/
├── PlanSummaryBar.kt         # Compose component
└── PlanProgressIndicator.kt  # Progress bar component

mpp-idea/src/main/kotlin/cc/unitmesh/devins/idea/toolwindow/plan/
└── IdeaPlanSummaryBar.kt     # Jewel-styled component

mpp-vscode/webview/src/components/plan/
├── PlanSummaryBar.tsx        # React component
├── PlanSummaryBar.css        # Styles
└── types.ts                  # TypeScript interfaces
```

## Data Flow

```
┌─────────────────┐    Tool Executed    ┌─────────────────────┐
│ PlanManagement  │ ─────────────────► │ ComposeRenderer     │
│     Tool        │                     │ .updatePlanFrom     │
└─────────────────┘                     │  ToolCall()         │
                                        └─────────┬───────────┘
                                                  │
                                                  ▼
                                        ┌─────────────────────┐
                                        │ currentPlan state   │
                                        │ (Compose State)     │
                                        └─────────┬───────────┘
                                                  │
                    ┌─────────────────────────────┼─────────────────────────────┐
                    │                             │                             │
                    ▼                             ▼                             ▼
        ┌───────────────────┐       ┌───────────────────────┐       ┌───────────────────┐
        │   mpp-ui          │       │   mpp-idea            │       │   mpp-vscode      │
        │   Compose         │       │   StateFlow →         │       │   postMessage →   │
        │   collectAsState  │       │   Compose State       │       │   React State     │
        └───────────────────┘       └───────────────────────┘       └───────────────────┘
```

## Component API

### PlanSummaryData (mpp-core)

```kotlin
@Serializable
data class PlanSummaryData(
    val planId: String,
    val title: String,
    val totalSteps: Int,
    val completedSteps: Int,
    val failedSteps: Int,
    val progressPercent: Int,
    val status: TaskStatus,
    val tasks: List<TaskSummary>
)

@Serializable
data class TaskSummary(
    val id: String,
    val title: String,
    val status: TaskStatus,
    val completedSteps: Int,
    val totalSteps: Int
)
```

### Compose Component (mpp-ui)

```kotlin
@Composable
fun PlanSummaryBar(
    plan: AgentPlan?,
    modifier: Modifier = Modifier,
    onViewDetails: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
)
```

### React Component (mpp-vscode)

```typescript
interface PlanSummaryBarProps {
  plan: PlanSummaryData | null;
  onViewDetails?: () => void;
  onDismiss?: () => void;
}

export const PlanSummaryBar: React.FC<PlanSummaryBarProps>
```

## Visual States

### 1. No Plan
Component is not rendered (returns null/empty).

### 2. Plan In Progress
```
┌──────────────────────────────────────────────────────────────┐
│ 📋 Create Tag System                         60% ████████░░░ │
│ → Creating TagService class...                    [▼] [×]   │
└──────────────────────────────────────────────────────────────┘
```

### 3. Plan Completed
```
┌──────────────────────────────────────────────────────────────┐
│ ✅ Create Tag System                        100% ██████████ │
│ All 5 steps completed                             [▼] [×]   │
└──────────────────────────────────────────────────────────────┘
```

### 4. Plan Failed
```
┌──────────────────────────────────────────────────────────────┐
│ ❌ Create Tag System                         40% ████░░░░░░ │
│ Step 3 failed: Could not create file             [▼] [×]   │
└──────────────────────────────────────────────────────────────┘
```

### 5. Expanded View
```
┌──────────────────────────────────────────────────────────────┐
│ 📋 Create Tag System                         60% ████████░░ │
├──────────────────────────────────────────────────────────────┤
│ 1. Setup Project Structure                              ✓   │
│    ✓ Create entity                                          │
│    ✓ Create repository                                      │
│                                                              │
│ 2. Implementation                                       →   │
│    ✓ Create service                                         │
│    → Create controller                                      │
│    ○ Add tests                                              │
└──────────────────────────────────────────────────────────────┘
```

## Styling

### Colors (Design System)

| State       | Icon | Progress Bar | Background |
|-------------|------|--------------|------------|
| In Progress | 📋   | Blue (#2196F3) | surfaceVariant |
| Completed   | ✅   | Green (#4CAF50) | surfaceVariant |
| Failed      | ❌   | Red (#F44336) | errorContainer |
| Blocked     | ⚠️   | Orange (#FF9800) | warningContainer |

### Step Status Icons
- ✓ Completed (green)
- → In Progress (blue, animated)
- ○ Todo (gray)
- ✗ Failed (red)

## Integration Points

### mpp-ui: DevInEditorInput.kt
```kotlin
Column {
    // Plan summary above file changes
    PlanSummaryBar(
        plan = renderer?.currentPlan,
        onViewDetails = { showPlanDetails = true }
    )
    
    // Existing file change summary
    FileChangeSummary()
    
    // Input surface
    Surface { ... }
}
```

### mpp-idea: IdeaDevInInputArea.kt
```kotlin
Column {
    // Plan summary bar (Jewel styled)
    IdeaPlanSummaryBar(
        plan = viewModel.renderer.currentPlan,
        project = project
    )
    
    // Existing top toolbar
    IdeaTopToolbar(...)
    
    // Editor
    SwingPanel(...)
}
```

### mpp-vscode: ChatPanel.tsx
```tsx
<div className="chat-panel">
  <MessageList messages={messages} />
  
  {/* Plan summary above input */}
  <PlanSummaryBar plan={currentPlan} />
  
  <DevInInput ... />
</div>
```

## Testing

### Unit Tests
- PlanSummaryData.from(plan) correctly extracts summary
- Progress calculation handles edge cases (empty plan, all completed)

### UI Tests
- Component renders correctly for each state
- Expand/collapse works
- Dismiss removes the bar
- Progress bar animates smoothly

## Related Files
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/plan/AgentPlan.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/PlanManagementTool.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/PlanPanel.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/editor/changes/FileChangeSummary.kt`

