# Android 设备上 NanoDSL 渲染问题分析

## 问题描述

在 `mpp-ui` 模块中，Android 设备无法正确渲染 nano dsl（来自 `xiuper-ui` 模块）。Android 设备上只显示语法高亮的代码，而没有实际的 UI 预览。

## 问题根源

### 1. 依赖缺失

在 `mpp-ui/build.gradle.kts` 中：

- **JVM 平台**（第197行）：正确包含了 `xiuper-ui` 依赖
  ```kotlin
  val jvmMain by getting {
      dependencies {
          implementation(project(":xiuper-ui"))  // ✅ 有依赖
      }
  }
  ```

- **Android 平台**（第267-305行）：**缺少** `xiuper-ui` 依赖
  ```kotlin
  val androidMain by getting {
      dependencies {
          // ❌ 没有 implementation(project(":xiuper-ui"))
      }
  }
  ```

### 2. 实现差异

#### JVM 实现 (`NanoDSLBlockRenderer.jvm.kt`)
- ✅ 使用 `NanoDSL.toIR()` 解析 NanoDSL 代码
- ✅ 使用 `StatefulNanoRenderer.Render()` 渲染实际的 UI 预览
- ✅ 支持预览/代码切换
- ✅ 显示解析错误和验证状态

#### Android 实现 (`NanoDSLBlockRenderer.android.kt`)
- ❌ 只显示语法高亮的代码（通过 `CodeBlockRenderer`）
- ❌ 注释明确说明："Live preview is not available on Android platform as xiuper-ui (full parser) is JVM-only."
- ❌ 没有解析和渲染逻辑

### 3. 模块结构

- `xiuper-ui` 是一个纯 JVM 模块（`kotlin("jvm")`）
- `StatefulNanoRenderer` 位于 `mpp-ui/src/jvmMain/`，使用了 `xiuper-ui` 的类型：
  - `cc.unitmesh.xuiper.ir.NanoIR`
  - `cc.unitmesh.xuiper.ir.NanoActionIR`
  - `cc.unitmesh.xuiper.ir.NanoStateIR`

### 4. 验证器的尝试

`NanoDSLValidator.android.kt` 尝试通过反射来使用 `xiuper-ui`：
- 尝试加载 `cc.unitmesh.xuiper.dsl.NanoDSL` 类
- 如果类不存在，回退到基本验证
- 但这需要 `xiuper-ui` 在 classpath 中

## 解决方案

### 方案 1：添加 Android 依赖（推荐）

由于 Android 也运行在 JVM 上，理论上可以直接使用 `xiuper-ui`：

1. **在 `mpp-ui/build.gradle.kts` 的 `androidMain` 中添加依赖**：
   ```kotlin
   val androidMain by getting {
       dependencies {
           implementation(project(":xiuper-ui"))  // 添加这行
           // ... 其他依赖
       }
   }
   ```

2. **将 `StatefulNanoRenderer` 移到 `commonMain`** 或创建 Android 版本：
   - 选项 A：将 `StatefulNanoRenderer.kt` 从 `jvmMain` 移到 `commonMain`
   - 选项 B：在 `androidMain` 中创建相同的实现

3. **更新 Android 的 `NanoDSLBlockRenderer` 实现**：
   - 复制 JVM 实现的逻辑
   - 使用 `NanoDSL.toIR()` 解析
   - 使用 `StatefulNanoRenderer.Render()` 渲染

### 方案 2：检查 xiuper-ui 的 Android 兼容性

需要确认：
- `xiuper-ui` 是否使用了 Android 不支持的 JVM 特性
- 是否有 Android 特定的限制（如 ProGuard 规则）

### 方案 3：创建 Android 特定的渲染器

如果 `xiuper-ui` 无法在 Android 上使用：
- 创建 Android 特定的解析器（可能通过反射或简化版本）
- 实现 Android 特定的渲染器

## 建议的修复步骤

1. ✅ 检查 `xiuper-ui` 的 Android 兼容性
2. ✅ 在 `androidMain` 中添加 `xiuper-ui` 依赖
3. ✅ 将 `StatefulNanoRenderer` 移到 `commonMain`（如果可能）
4. ✅ 更新 `NanoDSLBlockRenderer.android.kt` 以支持 UI 预览
5. ⏳ 测试 Android 设备上的渲染功能

## 已实施的修复

### 1. 添加 Android 依赖
在 `mpp-ui/build.gradle.kts` 的 `androidMain` 中添加了 `xiuper-ui` 依赖：
```kotlin
val androidMain by getting {
    dependencies {
        implementation(project(":xiuper-ui"))  // ✅ 已添加
        // ... 其他依赖
    }
}
```

### 2. 移动 StatefulNanoRenderer 到 commonMain
将 `StatefulNanoRenderer.kt` 从 `jvmMain` 移动到 `commonMain`，使其可以在 Android 和 JVM 平台上共享：
- 源文件：`mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/nano/StatefulNanoRenderer.kt`
- 目标文件：`mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/nano/StatefulNanoRenderer.kt`

### 3. 更新 Android 实现
更新了 `NanoDSLBlockRenderer.android.kt`，使其与 JVM 实现功能一致：
- ✅ 使用 `NanoDSL.toIR()` 解析 NanoDSL 代码
- ✅ 使用 `StatefulNanoRenderer.Render()` 渲染 UI 预览
- ✅ 支持预览/代码切换
- ✅ 显示解析错误和验证状态

## 预期效果

修复后，Android 设备上的 NanoDSL 渲染应该：
1. ✅ 能够解析 NanoDSL 代码
2. ✅ 显示实时的 UI 预览（而不仅仅是代码）
3. ✅ 支持预览和代码视图之间的切换
4. ✅ 显示解析错误信息（如果有）
5. ✅ 显示验证状态（Valid/Parse Error）

## 测试建议

1. 在 Android 设备或模拟器上运行应用
2. 生成或输入 NanoDSL 代码块
3. 验证：
   - 代码能够正确解析
   - UI 预览能够正确显示
   - 预览/代码切换功能正常
   - 错误处理正常工作

## 相关文件

- `mpp-ui/build.gradle.kts` - 构建配置
- `mpp-ui/src/androidMain/kotlin/cc/unitmesh/devins/ui/compose/sketch/NanoDSLBlockRenderer.android.kt` - Android 实现
- `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/sketch/NanoDSLBlockRenderer.jvm.kt` - JVM 实现（参考）
- `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/nano/StatefulNanoRenderer.kt` - 渲染器实现
- `xiuper-ui/build.gradle.kts` - xiuper-ui 模块配置

