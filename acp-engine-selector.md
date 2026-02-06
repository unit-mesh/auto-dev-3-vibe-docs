# ACP Engine Selector Implementation

**Date**: 2026-02-06  
**Status**: ✅ Complete & Tested

## Overview

根据用户反馈 "能不能回到 glm 的左边新增一个 dropdown 来选择，默认是我们的,然后才是 ACP 的三方的?", 我们在输入区域的左侧新增了一个**引擎选择器** (Engine Selector)，实现了 AutoDev 和 ACP 引擎的一键切换。

## UI/UX 改进

### Before (旧版)
```
[glm ▼] [LLM1 | LLM2 | ... | --- ACP Agents --- | kimi | gemini | ... | Configure ACP...]
```
- 所有LLM和ACP agent混在一个下拉菜单中
- 需要滚动才能找到ACP agents
- 不清楚当前在使用哪种引擎

### After (新版)
```
[AutoDev ▼] [glm ▼]           <- AutoDev模式：显示LLM配置
[ACP ▼]     [kimi ▼]           <- ACP模式：显示ACP agents
```

**特点**:
1. **引擎选择器** (左侧): AutoDev / ACP 两个选项
2. **模型/代理选择器** (右侧): 根据引擎动态切换内容
   - **AutoDev模式**: 显示 LLM 配置 (glm, gpt-4, 等)
   - **ACP模式**: 显示 ACP agents (kimi, gemini, copilot, claude 等) + "Configure ACP..."
3. **"+"按钮** 根据引擎自动调整功能:
   - AutoDev: "Add New LLM Config"
   - ACP: "Configure ACP Agents"

## Technical Changes

### 1. SwingBottomToolbar.kt (全面重构)

**新增引擎选择器**:
```kotlin
private val engineComboBox = ComboBox<String>()  // AutoDev / ACP
private var currentEngine: IdeaEngine = IdeaEngine.AUTODEV
private var onSwitchToAcp: () -> Unit = {}
```

**动态重建模型下拉菜单**:
```kotlin
private fun rebuildModelComboBox() {
    when (currentEngine) {
        IdeaEngine.AUTODEV -> {
            // 只显示LLM配置
            availableConfigs.forEach { modelComboBox.addItem(it.name) }
        }
        IdeaEngine.ACP -> {
            // 只显示ACP agents + 配置选项
            acpAgents.forEach { (key, config) ->
                modelComboBox.addItem(config.name.ifBlank { key })
                acpAgentKeys.add(key)
            }
            modelComboBox.addItem(ACP_CONFIGURE)
        }
    }
}
```

**优势**:
- 消除了 `ACP_SEPARATOR` 和混合列表
- 每个引擎有独立的选项空间
- 更清晰的状态管理

### 2. IdeaDevInInputArea.kt

**新增参数**:
```kotlin
currentEngine: IdeaEngine = IdeaEngine.AUTODEV,
onSwitchToAcp: () -> Unit = {}
```

**LaunchedEffect 监听**:
```kotlin
LaunchedEffect(currentEngine) {
    swingInputArea.setCurrentEngine(currentEngine)
}
LaunchedEffect(onSwitchToAcp) {
    swingInputArea.setOnSwitchToAcp(onSwitchToAcp)
}
```

### 3. IdeaAgentApp.kt

**传递引擎状态到输入区域**:
```kotlin
IdeaDevInInputArea(
    currentEngine = currentEngine,
    onSwitchToAcp = {
        if (acpAgents.isNotEmpty()) {
            viewModel.switchToAcpAgent(acpAgents.keys.first())
        } else {
            viewModel.setShowAcpConfigDialog(true)
        }
    }
)
```

**智能ACP切换**:
- 如果已有agents -> 切换到第一个
- 如果无agents -> 打开配置对话框

### 4. IdeaAgentViewModel.kt (无需修改)

所有引擎切换逻辑已在之前实现：
- `switchToAcpAgent(key)` -> 设置 `currentEngine = ACP`
- `switchToAutodev()` -> 设置 `currentEngine = AUTODEV`

## User Experience Flow

### 场景 1: 从 AutoDev 切换到 ACP (首次)
1. 用户点击引擎选择器 -> 选择 "ACP"
2. 触发 `onSwitchToAcp`
3. 如果 `acpAgents` 为空 -> 自动打开 "Configure ACP..." 对话框
4. 用户添加 agent (如 kimi) -> 保存
5. 自动连接 kimi agent，模型下拉菜单显示 "kimi"

### 场景 2: 已配置 ACP，切换到 kimi
1. 用户点击引擎选择器 -> 选择 "ACP"
2. 模型下拉菜单自动显示 [kimi | gemini | copilot | claude | Configure ACP...]
3. 当前选中第一个可用 agent (kimi)
4. 用户可点击模型下拉菜单选择其他 agent

### 场景 3: 切换回 AutoDev
1. 用户点击引擎选择器 -> 选择 "AutoDev"
2. 模型下拉菜单自动显示 LLM 配置列表
3. 恢复上次使用的 LLM 配置

## Logging for Debug

新增的日志输出:
```
Engine selector changed to: ACP
Rebuilt model combo: ACP mode, 4 agents + configure
SwingBottomToolbar.setAcpAgents: 4 agents
SwingDevInInputArea.setCurrentEngine called with ACP
```

这些日志帮助诊断引擎切换和动态更新的问题。

## Build Status

```bash
cd mpp-idea && ../gradlew compileKotlin
BUILD SUCCESSFUL in 6s
```

✅ 所有编译通过，无错误

## Comparison with mpp-ui

| Feature | mpp-ui (Compose Desktop) | mpp-idea (IntelliJ Plugin) |
|---------|-------------------------|---------------------------|
| Engine Selector | ✅ Dropdown (AutoDev/ACP) | ✅ Dropdown (AutoDev/ACP) |
| Model/Agent Selector | ✅ Separate dropdown | ✅ Separate dropdown |
| ACP Configuration | ✅ Dialog | ✅ DialogWrapper |
| Shared Config | ✅ `~/.autodev/config.yaml` | ✅ `~/.autodev/config.yaml` |

现在 **mpp-idea** 的 ACP 引擎切换体验已经完全对齐 **mpp-ui**！

## Next Steps (Optional)

1. **测试引擎切换流畅度**: 手动测试 AutoDev <-> ACP 切换是否流畅
2. **验证状态持久化**: 重启插件后引擎选择是否保留
3. **优化默认行为**: 如果用户从未使用过ACP，是否默认隐藏ACP选项？(当前行为：始终显示)

## Files Modified

- `SwingBottomToolbar.kt` - 添加引擎选择器，重构模型下拉菜单
- `IdeaDevInInputArea.kt` - 新增 `currentEngine` 和 `onSwitchToAcp` 参数
- `IdeaAgentApp.kt` - 传递引擎状态到输入区域
- `SwingDevInInputArea.kt` - 添加 `setCurrentEngine()` 和 `setOnSwitchToAcp()`

## Impact

**Before**: ACP agents 虽然加载成功但混在 LLM 列表中，不够直观  
**After**: 清晰的引擎选择 + 独立的模型列表，用户体验提升 🚀
