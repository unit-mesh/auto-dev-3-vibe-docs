# 文档索引流程图

## 索引状态转换图

```mermaid
stateDiagram-v2
    [*] --> Idle: 初始化
    
    Idle --> Indexing: 用户点击"索引文档"
    Indexing --> Indexing: 索引每个文档
    Indexing --> Completed: 所有文档索引完成
    Completed --> Idle: 用户点击"关闭"
    
    note right of Idle
        显示"索引文档"按钮
        搜索框禁用
    end note
    
    note right of Indexing
        显示进度条
        显示 current/total
        显示成功/失败统计
        搜索框禁用
    end note
    
    note right of Completed
        显示完成摘要
        显示总成功/失败数
        搜索框启用（支持内容搜索）
    end note
```

## 文档索引详细流程

```mermaid
flowchart TD
    Start([用户进入页面]) --> LoadDocs[加载文档列表]
    LoadDocs --> ShowDocs[显示文档树]
    ShowDocs --> ShowBtn[显示"索引文档"按钮]
    
    ShowBtn --> WaitUser{用户操作}
    WaitUser -->|点击索引按钮| StartIndex[开始索引]
    WaitUser -->|其他操作| ShowBtn
    
    StartIndex --> InitStatus[设置状态: Indexing<br/>current=0, total=N]
    InitStatus --> LoopStart{还有文档?}
    
    LoopStart -->|是| IndexOne[索引一个文档]
    IndexOne --> UseParser[使用 DocumentParserFactory<br/>创建解析器]
    UseParser --> Parse{解析成功?}
    
    Parse -->|成功| SaveIndex[保存到索引数据库<br/>status=INDEXED]
    Parse -->|失败| SaveError[保存错误信息<br/>status=FAILED]
    
    SaveIndex --> IncSuccess[succeeded++]
    SaveError --> IncFailed[failed++]
    
    IncSuccess --> UpdateProgress[更新进度<br/>current++]
    IncFailed --> UpdateProgress
    
    UpdateProgress --> LogResult[输出日志<br/>✓ 或 ✗]
    LogResult --> LoopStart
    
    LoopStart -->|否| SetCompleted[设置状态: Completed<br/>total, succeeded, failed]
    SetCompleted --> EnableSearch[启用搜索框]
    EnableSearch --> ShowSummary[显示完成摘要]
    
    ShowSummary --> WaitClose{用户操作}
    WaitClose -->|点击关闭| ResetStatus[重置状态: Idle]
    WaitClose -->|使用搜索| EnableSearch
    
    ResetStatus --> ShowBtn
```

## UI 状态展示

```mermaid
graph LR
    subgraph "Idle 状态"
        A1[索引文档 按钮]
        A2[搜索框 禁用]
    end
    
    subgraph "Indexing 状态"
        B1["⟳ 索引中... 10/50"]
        B2[进度条 20%]
        B3["✓ 8  ✗ 2"]
        B4[搜索框 禁用]
    end
    
    subgraph "Completed 状态"
        C1["索引完成 [×]"]
        C2["成功: 48  失败: 2"]
        C3[搜索框 启用]
    end
    
    A1 -->|点击| B1
    B3 -->|完成| C1
    C1 -->|点击 ×| A1
```

## 索引与搜索的关系

```mermaid
flowchart TD
    Start([用户想搜索文档]) --> CheckIndex{检查索引状态}
    
    CheckIndex -->|Idle| OnlyName[只能搜索文件名]
    CheckIndex -->|Indexing| Wait[等待索引完成]
    CheckIndex -->|Completed| FullSearch[搜索文件名+内容]
    
    OnlyName --> Filter1[过滤: name.contains<br/>path.contains]
    Filter1 --> ShowResults1[显示匹配的文档]
    
    Wait --> CheckAgain{索引完成?}
    CheckAgain -->|否| Wait
    CheckAgain -->|是| FullSearch
    
    FullSearch --> Filter2[过滤:<br/>1. name.contains<br/>2. path.contains<br/>3. indexedContent.contains]
    Filter2 --> ShowResults2[显示匹配的文档<br/>支持内容匹配]
```

## 索引过程中的数据流

```mermaid
sequenceDiagram
    participant U as 用户
    participant UI as NavigationPane
    participant VM as ViewModel
    participant IS as IndexService
    participant FS as FileSystem
    participant PF as ParserFactory
    participant DB as IndexRepository
    
    U->>UI: 点击"索引文档"
    UI->>VM: startIndexing()
    VM->>IS: indexDocuments(documents)
    
    loop 对每个文档
        IS->>FS: readFile(path)
        FS-->>IS: content
        
        IS->>PF: detectFormat(path)
        PF-->>IS: formatType
        
        IS->>PF: createParser(formatType)
        PF-->>IS: parser
        
        alt 解析成功
            IS->>IS: parser.parse(doc, content)
            IS->>DB: save(INDEXED, content)
            DB-->>IS: ✓
            IS-->>UI: Indexing(current++, succeeded++)
            UI-->>U: 显示进度: ✓ path
        else 解析失败
            IS->>DB: save(FAILED, error)
            DB-->>IS: ✓
            IS-->>UI: Indexing(current++, failed++)
            UI-->>U: 显示进度: ✗ path
        end
    end
    
    IS-->>UI: Completed(total, succeeded, failed)
    UI-->>U: 显示完成摘要
```

## 错误处理流程

```mermaid
flowchart TD
    IndexFile[索引文件] --> DetectFormat{检测格式}
    
    DetectFormat -->|不支持| Skip1[跳过: 返回 false]
    DetectFormat -->|支持| CheckParser{创建解析器}
    
    CheckParser -->|失败| Skip2[跳过: 返回 false]
    CheckParser -->|成功| ReadFile{读取文件}
    
    ReadFile -->|失败| Skip3[跳过: 返回 false]
    ReadFile -->|成功| Parse{解析文档}
    
    Parse -->|成功| SaveSuccess[保存: status=INDEXED<br/>返回 true]
    Parse -->|异常| CatchError[捕获异常]
    
    CatchError --> SaveFailed[保存: status=FAILED<br/>error=message<br/>返回 false]
    
    Skip1 --> LogSkip[日志: 不支持的格式]
    Skip2 --> LogSkip
    Skip3 --> LogSkip
    SaveSuccess --> LogSuccess[日志: ✓ Indexed]
    SaveFailed --> LogFailed[日志: ✗ Failed]
```

## 索引数据结构

```mermaid
classDiagram
    class DocumentIndexRecord {
        +String path
        +String hash
        +Long lastModified
        +String status
        +String? content
        +String? error
        +Long indexedAt
    }
    
    class IndexingStatus {
        <<sealed>>
    }
    
    class Idle {
    }
    
    class Indexing {
        +Int current
        +Int total
        +Int succeeded
        +Int failed
    }
    
    class Completed {
        +Int total
        +Int succeeded
        +Int failed
    }
    
    IndexingStatus <|-- Idle
    IndexingStatus <|-- Indexing
    IndexingStatus <|-- Completed
    
    note for DocumentIndexRecord "存储在数据库中\nstatus: INDEXED | FAILED\ncontent: 提取的文本内容"
    
    note for IndexingStatus "三种状态:\n- Idle: 未索引\n- Indexing: 索引中\n- Completed: 已完成"
```

## 性能考虑

### 1. 索引时机
```
┌─────────────────────────────────────┐
│ 页面加载                             │
│  ↓                                  │
│ 加载文档列表 (searchFiles)          │  <- 快速
│  ↓                                  │
│ 显示文档树                           │
│  ↓                                  │
│ [用户决定何时索引] ← 用户控制        │
│  ↓                                  │
│ 索引文档 (parse + save)              │  <- 耗时
└─────────────────────────────────────┘
```

### 2. 索引优化策略

| 策略 | 说明 | 优势 |
|------|------|------|
| 延迟索引 | 不在加载时自动索引 | 减少初始加载时间 |
| 哈希检测 | 通过哈希判断文档是否变化 | 避免重复索引 |
| 批量处理 | 一次处理多个文档 | 提高吞吐量 |
| 进度反馈 | 实时显示进度 | 改善用户体验 |
| 错误继续 | 单个失败不中断整体 | 提高健壮性 |

### 3. 内存使用

```
每个文档索引时:
1. 读取文件内容 (String)       <- 内存占用
2. 解析文档                    <- 临时对象
3. 提取文本内容                <- 保存到数据库
4. 释放内存                    <- GC
```

## 未来增强功能

### 1. 增量索引
```mermaid
flowchart LR
    A[检查文档哈希] --> B{与数据库对比}
    B -->|相同| C[跳过]
    B -->|不同| D[重新索引]
    B -->|新文档| D
```

### 2. 并行索引
```mermaid
flowchart TD
    A[索引任务队列] --> B[Worker 1]
    A --> C[Worker 2]
    A --> D[Worker 3]
    B --> E[合并结果]
    C --> E
    D --> E
```

### 3. 优先级索引
```mermaid
flowchart LR
    A[文档列表] --> B{分类}
    B -->|常用| C[高优先级队列]
    B -->|普通| D[普通队列]
    B -->|大文件| E[低优先级队列]
    C --> F[先索引]
    D --> G[后索引]
    E --> H[最后索引]
```

