# WebEdit Debug Guide

## 运行调试版本

我创建了一个带有详细日志的 WebEdit 预览程序来帮助诊断问题。

### 运行方式

```bash
cd /Users/phodal/ai/xiuper
./gradlew :mpp-ui:run -PmainClass=cc.unitmesh.devins.ui.webedit.WebEditPreviewKt
```

或者直接在 IDE 中运行：
```
mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/webedit/WebEditPreview.kt
```

### 调试功能

#### 1. 顶部状态栏
显示实时状态：
- 🌉 Bridge Ready 状态
- ⏳ Loading 状态  
- 🌳 DOM Tree 状态（children 数量）
- 🎯 Selection Mode 状态

#### 2. 自动测试
程序会在启动 2 秒后自动加载 `https://example.com`，方便快速测试。

#### 3. 详细日志

所有关键操作都会输出到控制台，格式如下：

```
═══════════════════════════════════════════════════════════
[WebEditPreview] Starting WebEdit Debug Preview
═══════════════════════════════════════════════════════════
[WebEditPreview] 🚀 Initializing KCEF...
[WebEditPreview] 📁 KCEF Install Dir: /path/to/install
[WebEditPreview] ✅ KCEF Initialized successfully
[WebEditPreview] 🎨 Rendering WebEditPage...

[WebEditDebugContainer] 🌉 Creating WebEditBridge...
[WebEditDebugContainer] ✅ JvmWebEditBridge created
[WebEditDebugContainer] 🌐 URL changed: ''
[WebEditDebugContainer] 📄 Title changed: ''
[WebEditDebugContainer] ⏳ Loading: false
[WebEditDebugContainer] 🎯 Selection Mode: false
[WebEditDebugContainer] 🔲 No element selected
[WebEditDebugContainer] 🌳 DOM Tree: null
[WebEditDebugContainer] 🚦 Bridge Ready: false

[WebEditView] 🔧 Configuring bridge callbacks...
[WebEditView] ✅ Setting up JvmWebEditBridge callbacks
[WebEditView] ✅ All bridge callbacks configured
[WebEditView] 📡 Registering JS message handler...
[WebEditView] ✅ Registered JS handler: webEditMessage

[WebEditDebugContainer] 🧪 Auto-loading test page...
[WebEditDebugContainer] ✅ Navigation initiated to example.com

[JvmWebEditBridge] 🚀 navigateTo called: 'https://example.com'
[JvmWebEditBridge] 📞 Calling navigateCallback...
[JvmWebEditBridge] ✅ navigateCallback invoked

[WebEditView] 🌐 Navigate to: https://example.com
[WebEditView] 🌐 URL changed to: https://example.com (current: about:blank)

[WebEditView] 📊 State changed:
  - isLoading: true
  - lastLoadedUrl: about:blank
  - loadingState: Loading(progress=0.0)
  - scriptInjected: false
[WebEditView] Page is loading...

[WebEditView] 📊 State changed:
  - isLoading: false
  - lastLoadedUrl: https://example.com/
  - loadingState: Finished
  - scriptInjected: false
[WebEditView] Page finished loading: https://example.com/
[WebEditView] Processing loaded page: https://example.com/
[WebEditView] Waiting 300ms for page to stabilize...
[WebEditView] Injecting bridge script...
[WebEditView] ✓ Bridge script injected successfully
[WebEditView] Testing JavaScript execution...

[JvmWebEditBridge] ✅ Bridge marked as READY

[WebEditView] 📨 Received JS message:
  - Params: {"type":"PageLoaded","data":{"url":"https://example.com/","title":"Example Domain"}}...
[WebEditView] 📋 Message type: PageLoaded
[WebEditView] ✅ PageLoaded: Example Domain (https://example.com/)

[JvmWebEditBridge] 📨 handleMessage: PageLoaded
[JvmWebEditBridge] 📄 Page Loaded: Example Domain (https://example.com/)

[WebEditView] 📨 Received JS message:
  - Params: {"type":"DOMTreeUpdated","data":{"root":{...}}}...
[WebEditView] 📋 Message type: DOMTreeUpdated
[WebEditView] ✓ DOMTreeUpdated: 2 children

[JvmWebEditBridge] 📨 handleMessage: DOMTreeUpdated
[JvmWebEditBridge] 🌳 DOM Tree Updated:
  - Root: html
  - Children: 2
  - Selector: html

[WebEditDebugContainer] 🌳 DOM Tree Updated:
  - Root: html
  - Children: 2
  - Selector: html
    └─ head (8 children)
    └─ body (1 children)
```

## 需要检查的关键点

### 1. Bridge 创建
```
[WebEditDebugContainer] 🌉 Creating WebEditBridge...
[WebEditDebugContainer] ✅ JvmWebEditBridge created
```
✅ 应该看到 JvmWebEditBridge 创建成功

### 2. 回调注册
```
[WebEditView] 🔧 Configuring bridge callbacks...
[WebEditView] ✅ All bridge callbacks configured
[WebEditView] 📡 Registering JS message handler...
[WebEditView] ✅ Registered JS handler: webEditMessage
```
✅ 所有回调都应该注册成功

### 3. 页面加载
```
[WebEditView] Page finished loading: https://example.com/
[WebEditView] Injecting bridge script...
[WebEditView] ✓ Bridge script injected successfully
```
✅ 应该看到脚本注入成功

### 4. Bridge Ready
```
[JvmWebEditBridge] ✅ Bridge marked as READY
[WebEditDebugContainer] 🚦 Bridge Ready: true
```
✅ Bridge 应该变为 Ready 状态

### 5. JS 消息接收
```
[WebEditView] 📨 Received JS message:
  - Params: {"type":"PageLoaded",...}
[WebEditView] 📋 Message type: PageLoaded
[WebEditView] ✅ PageLoaded: Example Domain (https://example.com/)
```
✅ 应该收到 PageLoaded 消息

### 6. DOM Tree 更新
```
[JvmWebEditBridge] 🌳 DOM Tree Updated:
  - Root: html
  - Children: 2
[WebEditDebugContainer] 🌳 DOM Tree Updated:
  - Root: html
  - Children: 2
    └─ head (8 children)
    └─ body (1 children)
```
✅ 应该看到 DOM 树更新，包含 html、head、body 等元素

### 7. Selection Mode
点击工具栏的选择模式按钮后，应该看到：
```
[JvmWebEditBridge] 🎯 setSelectionMode: true
[JvmWebEditBridge] 📜 Executing JS: window.webEditBridge?.setSelectionMode(true);
[WebEditDebugContainer] 🎯 Selection Mode: true
```

## 常见问题诊断

### 问题 1: DOM Tree 始终为 null
**可能原因：**
1. JS 脚本未注入或注入失败
2. JS 消息未发送或未接收
3. DOMTreeUpdated 消息解析失败

**检查日志：**
- 是否有 "Bridge script injected successfully"？
- 是否有 "Received JS message" 和 "DOMTreeUpdated"？
- 是否有 parsing 错误？

### 问题 2: Selection Mode 不工作
**可能原因：**
1. Bridge 未 ready
2. executeJavaScript 回调未设置
3. JS 脚本执行失败

**检查日志：**
- 是否有 "Bridge marked as READY"？
- 是否有 "setSelectionMode" 日志？
- 是否有 "executeJavaScript is null!" 警告？

### 问题 3: 页面无法加载
**可能原因：**
1. KCEF 未初始化
2. URL 格式错误
3. 网络问题

**检查日志：**
- 是否有 "KCEF Initialized successfully"？
- 是否有 "navigateTo called" 和 "navigateCallback invoked"？
- 是否有 "Navigation error"？

## 调试技巧

### 1. 手动输入 URL
在程序运行后，可以在 URL 输入框中手动输入其他网址测试：
- https://example.com （简单页面）
- https://github.com （复杂页面）
- http://localhost:8080 （本地服务）

### 2. 观察状态栏
顶部状态栏实时显示关键状态，可以快速判断问题：
- Bridge 是否 Ready
- DOM 是否已加载
- Selection Mode 是否激活

### 3. 查看控制台
所有日志都会输出到控制台，使用 emoji 标记便于识别：
- 🚀 启动/导航
- ✅ 成功操作
- ❌ 错误
- ⚠️ 警告
- 📨 消息接收
- 🌳 DOM 相关
- 🎯 选择模式

### 4. 使用 IntelliJ IDEA 调试器
可以在关键位置打断点：
- `WebEditBridge.handleMessage()` - 检查消息接收
- `WebEditView` 的 `IJsMessageHandler.handle()` - 检查 JS 消息
- `navigateTo()` - 检查导航流程

## 预期行为

正常运行时，应该看到：
1. ✅ KCEF 初始化成功
2. ✅ WebEditPage 渲染
3. ✅ Bridge 创建并配置
4. ✅ 2秒后自动加载 example.com
5. ✅ 页面加载完成
6. ✅ JS 脚本注入
7. ✅ Bridge 标记为 Ready
8. ✅ 收到 PageLoaded 消息
9. ✅ 收到 DOMTreeUpdated 消息
10. ✅ DOM 树显示在右侧边栏（2个子节点：head 和 body）
11. ✅ 点击选择模式按钮可以激活/停用
12. ✅ 鼠标悬停页面元素时会高亮

如果某个步骤失败，日志会清楚地显示在哪里出了问题。
