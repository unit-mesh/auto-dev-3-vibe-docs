# Document Index Provider 解决方案

## 问题描述

当 `DocumentAgentExecutor` 调用 `DocQLTool` 时，已经通过 `DocumentIndexService` 索引过的文档没有出现在搜索结果中。

### 根本原因

1. `DocQLTool` 只查询 `DocumentRegistry.getRegisteredPaths()`，这只返回当前会话中在内存里注册的文档
2. 已索引的文档存储在数据库中（通过 `DocumentIndexService`），但 `DocQLTool` 无法访问这些文档

### 架构问题

- `DocQLTool` 在 `mpp-core` 模块
- `DocumentIndexService` 在 `mpp-ui` 模块
- `mpp-core` 不能直接依赖 `mpp-ui`（违反模块依赖规则）

## 解决方案设计

采用**提供者模式（Provider Pattern）**来桥接两个模块：

```
┌─────────────────────────────────────────────────────────────┐
│                        mpp-core                              │
│                                                              │
│  ┌──────────────┐      ┌─────────────────────┐            │
│  │ DocQLTool    │─────>│ DocumentRegistry    │            │
│  └──────────────┘      │ (内存缓存)           │            │
│                        │                     │            │
│                        │  ┌───────────────┐ │            │
│                        │  │ IndexProvider │ │ (接口)     │
│                        │  └───────┬───────┘ │            │
│                        └──────────┼─────────┘            │
└───────────────────────────────────┼──────────────────────┘
                                   │
                                   │ 实现
                                   │
┌───────────────────────────────────┼──────────────────────┐
│                        mpp-ui     │                       │
│                                   ▼                       │
│  ┌──────────────────────────────────────────┐            │
│  │ DocumentIndexServiceProvider             │            │
│  │ (实现 DocumentIndexProvider)             │            │
│  └──────────────────┬───────────────────────┘            │
│                     │                                     │
│                     ▼                                     │
│  ┌──────────────────────────────────────────┐            │
│  │ DocumentIndexService                     │            │
│  │ (使用 DocumentIndexRepository)           │            │
│  └──────────────────┬───────────────────────┘            │
│                     │                                     │
│                     ▼                                     │
│            ┌─────────────────┐                            │
│            │    Database     │                            │
│            │  (SQLDelight)   │                            │
│            └─────────────────┘                            │
└─────────────────────────────────────────────────────────┘
```

## 实现详情

### 1. DocumentIndexProvider 接口 (mpp-core)

```kotlin
// mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/DocumentIndexProvider.kt
interface DocumentIndexProvider {
    suspend fun getIndexedPaths(): List<String>
    suspend fun loadIndexedDocument(path: String): Pair<String?, DocumentFormatType?>
    suspend fun isIndexed(path: String): Boolean
}
```

**职责**：定义访问已索引文档的接口

### 2. DocumentRegistry 扩展 (mpp-core)

```kotlin
// mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/DocumentRegistry.kt
object DocumentRegistry {
    private var indexProvider: DocumentIndexProvider? = null
    
    fun setIndexProvider(provider: DocumentIndexProvider?)
    fun getIndexProvider(): DocumentIndexProvider?
    suspend fun getAllAvailablePaths(): List<String>
    suspend fun loadFromIndex(path: String): Boolean
    
    suspend fun queryDocument(documentPath: String, docqlQuery: String): DocQLResult? {
        // 先从内存查找
        var docPair = getDocument(documentPath)
        
        // 如果不在内存，从索引加载
        if (docPair == null && indexProvider != null) {
            loadFromIndex(documentPath)
            docPair = getDocument(documentPath)
        }
        
        // 执行查询
        // ...
    }
}
```

**职责**：
- 管理内存中的文档缓存
- 接受并使用 `DocumentIndexProvider`
- 按需从索引加载文档到内存

### 3. DocQLTool 修改 (mpp-core)

```kotlin
// mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/DocQLTool.kt
private suspend fun queryAllDocuments(query: String): ToolResult {
    // 获取所有可用文档路径（内存 + 索引）
    val availablePaths = DocumentRegistry.getAllAvailablePaths()
    
    for (path in availablePaths) {
        // queryDocument 会自动从索引加载
        val result = DocumentRegistry.queryDocument(path, query)
        // ...
    }
}
```

**职责**：查询所有可用文档（内存 + 索引）

### 4. DocumentIndexServiceProvider 实现 (mpp-ui)

```kotlin
// mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/service/DocumentIndexServiceProvider.kt
class DocumentIndexServiceProvider(
    private val repository: DocumentIndexRepository
) : DocumentIndexProvider {
    
    override suspend fun getIndexedPaths(): List<String> {
        return repository.getAll()
            .filter { it.status == "INDEXED" }
            .map { it.path }
    }
    
    override suspend fun loadIndexedDocument(path: String): Pair<String?, DocumentFormatType?> {
        val record = repository.get(path)
        if (record?.status == "INDEXED" && record.content != null) {
            val formatType = DocumentParserFactory.detectFormat(path)
            return record.content to formatType
        }
        return null to null
    }
}
```

**职责**：通过 `DocumentIndexRepository` 访问数据库中的已索引文档

### 5. 注入 Provider (mpp-ui)

#### DocumentReaderViewModel

```kotlin
init {
    DocumentRegistry.initializePlatformParsers()
    
    // 注册 provider
    val provider = DocumentIndexServiceProvider(indexRepository)
    DocumentRegistry.setIndexProvider(provider)
    
    loadDocuments()
    initializeLLMService()
}
```

#### DocumentCli (JVM)

```kotlin
// 在创建 DocumentAgent 之前
val provider = DocumentIndexServiceProvider(indexRepository)
DocumentRegistry.setIndexProvider(provider)
```

## 工作流程

### 文档索引流程

```
1. 用户在 UI 中选择文档进行索引
   ↓
2. DocumentIndexService.indexDocuments()
   ↓
3. 解析文档内容
   ↓
4. 存储到 DocumentIndexRepository (数据库)
   ↓
5. 状态更新为 "INDEXED"
```

### 文档查询流程

```
1. 用户发送 DocQL 查询
   ↓
2. DocQLTool.queryAllDocuments()
   ↓
3. DocumentRegistry.getAllAvailablePaths()
   ├─> 内存中的文档路径 (getRegisteredPaths)
   └─> 索引中的文档路径 (provider.getIndexedPaths)
   ↓
4. 对每个路径执行查询
   ├─> 如果在内存中：直接查询
   └─> 如果不在内存：
       ├─> loadFromIndex(path)
       │   ├─> provider.loadIndexedDocument(path)
       │   ├─> 解析文档
       │   └─> registerDocument(内存)
       └─> 查询文档
   ↓
5. 返回查询结果
```

## 优势

1. **模块解耦**：mpp-core 不依赖 mpp-ui
2. **按需加载**：文档只在查询时才从数据库加载到内存
3. **透明性**：DocQLTool 无需知道文档来自内存还是数据库
4. **可扩展性**：可以添加其他 provider 实现（如远程索引、向量数据库等）
5. **性能优化**：
   - 内存缓存优先
   - 已加载的文档不会重复从数据库读取
   - 只加载需要查询的文档

## 测试验证

### 编译测试

✅ `./gradlew :mpp-core:compileKotlinJvm` - 通过  
✅ `./gradlew :mpp-core:compileKotlinJs` - 通过  
✅ `./gradlew :mpp-ui:compileKotlinJvm` - 通过

### 功能测试场景

1. **场景 1：查询已在内存中的文档**
   - 文档已通过 `selectDocument()` 加载
   - DocQL 查询直接命中内存缓存
   - 预期：快速响应，无数据库访问

2. **场景 2：查询已索引但未在内存的文档**
   - 文档通过 `indexDocuments()` 索引
   - DocQL 查询触发从数据库加载
   - 预期：首次查询较慢（加载），后续查询快速（缓存）

3. **场景 3：查询所有文档**
   - 混合内存和索引文档
   - `getAllAvailablePaths()` 返回两者合集
   - 预期：返回所有可用文档的查询结果

## 未来优化

1. **LRU 缓存**：限制内存中文档数量
2. **预加载策略**：根据使用频率预加载热门文档
3. **异步加载**：并行加载多个文档
4. **增量索引**：只索引变更的文档
5. **向量搜索**：集成向量数据库进行语义搜索

## 相关文件

### 新增文件
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/DocumentIndexProvider.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/service/DocumentIndexServiceProvider.kt`

### 修改文件
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/DocumentRegistry.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/DocQLTool.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/document/DocumentAgent.kt`
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/document/DocumentReaderViewModel.kt`
- `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/server/cli/DocumentCli.kt`

## 总结

通过引入 `DocumentIndexProvider` 接口和实现类 `DocumentIndexServiceProvider`，我们成功地：

1. ✅ 解决了 DocQLTool 无法访问已索引文档的问题
2. ✅ 保持了模块间的正确依赖关系
3. ✅ 实现了内存缓存和持久化存储的无缝集成
4. ✅ 提供了可扩展的架构供未来优化

用户现在可以：
- 索引文档到数据库（持久化）
- 通过 DocQL 查询所有文档（内存 + 索引）
- 享受内存缓存带来的性能优势

