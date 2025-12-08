# 文档阅读器状态流程图

## 状态转换流程

```mermaid
stateDiagram-v2
    [*] --> Initial: 页面初始化
    Initial --> Loading: 开始加载文档
    Loading --> Success: 加载成功且有文档
    Loading --> Empty: 加载成功但无文档
    Loading --> Error: 加载失败
    
    Success --> Success: 用户搜索（过滤文档）
    Success --> Loading: 用户点击刷新
    Empty --> Loading: 用户点击刷新
    Error --> Loading: 用户点击刷新
    
    note right of Success
        显示文档列表
        支持搜索和过滤
    end note
    
    note right of Loading
        显示加载动画
        "加载文档中..."
    end note
    
    note right of Empty
        显示空状态提示
        "暂无文档"
    end note
    
    note right of Error
        显示错误信息
        可重试
    end note
```

## 索引状态流程

```mermaid
stateDiagram-v2
    [*] --> Idle: 索引服务初始化
    Idle --> Indexing: 开始索引
    Indexing --> Indexing: 索引进行中 (n/total)
    Indexing --> Idle: 索引完成
    Idle --> Indexing: 用户点击刷新
    
    note right of Indexing
        显示索引进度
        "索引中: 10/50"
        搜索框禁用
    end note
    
    note right of Idle
        索引完成
        搜索框启用
        支持全文搜索
    end note
```

## 搜索功能流程

```mermaid
flowchart TD
    A[用户输入搜索词] --> B{索引是否完成?}
    B -->|是| C[搜索文件名 + 内容]
    B -->|否| D[仅搜索文件名]
    
    C --> E{有匹配结果?}
    D --> E
    
    E -->|是| F[显示过滤后的文档列表]
    E -->|否| G[显示"未找到匹配的文档"]
    
    F --> H[用户可以选择文档]
    G --> I[用户可以清空搜索]
    
    I --> J[显示全部文档]
```

## 用户交互流程

```mermaid
sequenceDiagram
    participant U as 用户
    participant P as DocumentReaderPage
    participant VM as ViewModel
    participant IS as IndexService
    participant UI as DocumentNavigationPane
    
    U->>P: 进入页面
    activate P
    P->>VM: 创建 ViewModel
    activate VM
    VM->>VM: loadDocuments()
    VM->>IS: indexWorkspace()
    VM-->>UI: documentLoadState = Loading
    UI-->>U: 显示"加载文档中..."
    
    VM->>VM: 文档加载完成
    VM-->>UI: documentLoadState = Success
    UI-->>U: 显示文档列表
    
    par 索引进行中
        IS-->>UI: IndexingStatus.Indexing(10, 50)
        UI-->>U: 显示"索引中: 10/50"
    and 索引完成
        IS-->>UI: IndexingStatus.Idle
        UI-->>U: 启用搜索框
    end
    
    U->>UI: 输入搜索词
    UI->>VM: updateSearchQuery("keyword")
    VM->>VM: filterDocuments()
    VM-->>UI: filteredDocuments 更新
    UI-->>U: 显示过滤后的结果
    
    U->>UI: 点击文档
    UI->>VM: selectDocument(doc)
    VM->>VM: 加载文档内容
    VM-->>P: 显示文档内容
    
    U->>UI: 点击刷新
    UI->>VM: refreshDocuments()
    VM->>VM: loadDocuments()
    VM->>IS: indexWorkspace()
    VM-->>UI: 重新加载和索引
    
    deactivate VM
    deactivate P
```

## 状态对应的 UI 显示

| 状态 | UI 显示 | 用户可操作 |
|------|---------|-----------|
| Initial | 无显示 | - |
| Loading | 加载动画 + "加载文档中..." | - |
| Success (有结果) | 文档树列表 | 搜索、选择、刷新 |
| Success (搜索无结果) | "未找到匹配的文档" | 清空搜索、刷新 |
| Empty | "暂无文档" + 提示信息 | 刷新 |
| Error | 错误图标 + 错误信息 | 刷新 |

## 搜索功能特性

### 搜索范围

1. **文件名搜索**（始终可用）
   - 匹配文件名
   - 匹配文件路径

2. **内容搜索**（索引完成后可用）
   - 搜索已索引的文档内容
   - 仅搜索状态为 "INDEXED" 的文档

### 搜索行为

- **实时搜索**：输入即搜索，无需按回车
- **大小写不敏感**：自动转换为小写匹配
- **OR 逻辑**：文件名或内容匹配即显示
- **空查询**：显示所有文档

### 搜索 UI

- **搜索框状态**
  - 索引中：禁用，显示"索引完成后可搜索..."
  - 索引完成：启用，显示"搜索文档..."
  
- **清空按钮**
  - 有搜索词时显示
  - 点击清空搜索，恢复显示所有文档
  
- **刷新按钮**
  - 始终显示
  - 重新加载文档列表和重建索引

