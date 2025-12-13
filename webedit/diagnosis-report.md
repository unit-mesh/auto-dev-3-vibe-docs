# WebEdit 问题诊断报告

## 问题描述

用户报告：
1. ❌ 输入网页后不显示 DOM Tree
2. ❌ Inspect 模式不生效

## 诊断过程

### 工具

创建了调试版本的预览程序：
- **文件**: `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/webedit/WebEditPreview.kt`
- **运行**: `./gradlew :mpp-ui:run -PmainClass=cc.unitmesh.devins.ui.webedit.WebEditPreviewKt`
- **文档**: `docs/webedit/debug-guide.md`

### 关键发现

通过详细的日志输出，发现以下流程：

#### ✅ 正常工作的部分

1. **Bridge 创建** ✅
   ```
   [WebEditDebugContainer] 🌉 Creating WebEditBridge...
   [WebEditDebugContainer] ✅ JvmWebEditBridge created
   ```

2. **回调配置** ✅
   ```
   [WebEditView] 🔧 Configuring bridge callbacks...
   [WebEditView] ✅ All bridge callbacks configured
   ```

3. **JS 消息处理器注册** ✅
   ```
   [WebEditView] Registering JS bridge handler: webEditMessage
   ```

4. **页面导航** ✅
   ```
   [JvmWebEditBridge] 🚀 navigateTo called: 'https://www.phodal.com'
   [WebEditView] 🌐 Navigate to: https://www.phodal.com
   ```

5. **页面加载** ✅
   ```
   [WebEditView] Page finished loading: https://www.phodal.com/
   ```

6. **脚本注入** ✅
   ```
   [WebEditView] Injecting bridge script...
   [WebEditView] ✓ Bridge script injected successfully
   ```

7. **Bridge Ready** ✅
   ```
   [JvmWebEditBridge] ✅ Bridge marked as READY
   ```

#### ❌ 问题所在

**关键问题：JavaScript → Kotlin 的消息传递失败**

期望看到但**没有出现**的日志：
```
[WebEditView] 📨 Received JS message:
[WebEditView] 📋 Message type: PageLoaded
[JvmWebEditBridge] 📨 handleMessage: PageLoaded
[JvmWebEditBridge] 🌳 DOM Tree Updated:
```

### 根本原因分析

根据代码和日志分析，问题出在：

**JavaScript 的 `window.kmpJsBridge` 可能不可用或方法不匹配**

#### WebEditBridgeScript.kt 中的 sendToKotlin 实现

```javascript
sendToKotlin: function(type, data) {
    console.log('[WebEditBridge] sendToKotlin called:', type, data);
    console.log('[WebEditBridge] kmpJsBridge available:', typeof window.kmpJsBridge);
    
    if (window.kmpJsBridge && window.kmpJsBridge.callNative) {
        console.log('[WebEditBridge] Calling kmpJsBridge.callNative...');
        try {
            const message = JSON.stringify({ type: type, data: data });
            console.log('[WebEditBridge] Message:', message);
            window.kmpJsBridge.callNative('webEditMessage', message, function(result) {
                console.log('[WebEditBridge] Kotlin callback result:', result);
            });
        } catch (e) {
            console.error('[WebEditBridge] Error calling native:', e);
        }
    } else {
        console.error('[WebEditBridge] kmpJsBridge not available!');
    }
}
```

这段代码尝试调用 `window.kmpJsBridge.callNative()`，但似乎没有成功。

#### 可能的原因

1. **时序问题**: `kmpJsBridge` 在脚本注入时还未准备好
2. **方法名不匹配**: `callNative` 可能不是正确的方法名
3. **WebView 配置**: compose-webview-multiplatform 的 JS bridge 配置可能不完整

## 解决方案

### 方案 1: 检查 kmpJsBridge API

compose-webview-multiplatform 的 JS bridge API 可能与我们假设的不同。需要检查：

```kotlin
// 在 WebEditView.jvm.kt 中
val jsBridge = rememberWebViewJsBridge()

jsBridge.register(object : IJsMessageHandler {
    override fun methodName(): String = "webEditMessage"
    override fun handle(...) { ... }
})
```

JavaScript 侧正确的调用方式可能是：
```javascript
// 不是 window.kmpJsBridge.callNative()
// 而是其他方式？
```

### 方案 2: 添加延迟和重试

在 PageLoaded 事件后再注入脚本，确保 bridge 已准备好：

```kotlin
// 等待更长时间
kotlinx.coroutines.delay(1000)  // 从 300ms 改为 1000ms

// 或者监听 WebView ready 事件
```

### 方案 3: 手动触发消息

在脚本注入后手动触发一次，绕过可能的初始化问题：

```kotlin
webViewNavigator.evaluateJavaScript("""
    // 手动触发 PageLoaded
    window.webEditBridge?.getDOMTree();
""")
```

### 方案 4: 使用 WebViewNavigator.evaluateJavaScript 返回值

某些 WebView bridge 需要通过 `evaluateJavaScript` 的返回值来传递数据：

```kotlin
webViewNavigator.evaluateJavaScript("document.title") { result ->
    println("Page title: $result")
}
```

## 下一步行动

### 立即执行

1. **检查 compose-webview-multiplatform 文档**
   - 确认 JS → Kotlin 通信的正确方式
   - 查看示例代码

2. **添加更多 JS console 日志**
   - 在浏览器开发者工具中查看 JS console 输出
   - 确认 `window.kmpJsBridge` 是否存在
   - 确认它有哪些方法

3. **测试简单的 bridge 调用**
   ```kotlin
   // 测试最基本的 bridge 功能
   jsBridge.register(object : IJsMessageHandler {
       override fun methodName(): String = "testMessage"
       override fun handle(...) {
           println("Test message received!")
       }
   })
   
   webViewNavigator.evaluateJavaScript("""
       window.kmpJsBridge.callNative('testMessage', 'hello', function(r) {});
   """)
   ```

### 验证方法

运行调试版本并检查：

1. JS console 是否有 `[WebEditBridge]` 日志？
2. 是否有 "kmpJsBridge not available!" 错误？
3. 是否有 "Error calling native" 错误？

## 临时解决方案

如果 JS bridge 确实有问题，可以考虑使用替代方案：

### 方案 A: 使用 URL拦截

```kotlin
// 拦截特定 URL 模式作为消息
webViewState.urlHandler = { url ->
    if (url.startsWith("webedit://")) {
        val message = parseMessage(url)
        bridge.handleMessage(message)
        false // 不实际导航
    } else {
        true // 正常导航
    }
}
```

JavaScript 侧：
```javascript
// 通过导航发送消息
window.location.href = 'webedit://PageLoaded?url=' + encodeURIComponent(window.location.href);
```

### 方案 B: 使用 console.log 拦截

某些 WebView 支持拦截 console 输出：

```kotlin
// 监听 console.log
webViewState.consoleMessageHandler = { message ->
    if (message.startsWith("[WebEditMessage]")) {
        val data = parseConsoleMessage(message)
        bridge.handleMessage(data)
    }
}
```

JavaScript 侧：
```javascript
console.log('[WebEditMessage] ' + JSON.stringify({ type: 'PageLoaded', ... }));
```

## 参考资料

- compose-webview-multiplatform GitHub: https://github.com/KevinnZou/compose-webview-multiplatform
- WebView JS Bridge 示例
- KCEF 文档

##总结

问题不在于 WebEdit 的架构或 UI 设计，而在于 **JavaScript 和 Kotlin 之间的 bridge 通信没有建立起来**。

需要：
1. 确认 compose-webview-multiplatform 的 JS bridge API
2. 修复 `sendToKotlin` 函数的实现
3. 确保消息能从 JS 传递到 Kotlin

一旦这个通信建立，DOM Tree 显示和 Inspect 模式都应该能正常工作。
