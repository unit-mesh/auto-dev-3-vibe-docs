# Sales Dashboard NanoDSL Example

## 完整的销售数据仪表板示例

这个示例展示了如何在 NanoDSL 中使用 `DataTable` 和 `DataChart` 组件来创建一个功能完整的销售数据仪表板。

### 完整代码

```nanodsl
component SalesDashboard:
    state:
        salesData: list = [
            {"product": "笔记本电脑", "sales": 45000, "quantity": 15, "date": "2024-01-15"},
            {"product": "智能手机", "sales": 32000, "quantity": 40, "date": "2024-01-16"},
            {"product": "平板电脑", "sales": 28000, "quantity": 20, "date": "2024-01-17"},
            {"product": "智能手表", "sales": 18000, "quantity": 30, "date": "2024-01-18"},
            {"product": "无线耳机", "sales": 12000, "quantity": 50, "date": "2024-01-19"}
        ]
        chartData: list = [
            {"month": "一月", "sales": 45000},
            {"month": "二月", "sales": 52000},
            {"month": "三月", "sales": 48000},
            {"month": "四月", "sales": 61000},
            {"month": "五月", "sales": 58000},
            {"month": "六月", "sales": 67000}
        ]

    VStack(spacing="lg", padding="md"):
        Text("销售数据仪表板", style="h1")
        
        Card(padding="md", shadow="sm"):
            VStack(spacing="md"):
                HStack(justify="between", align="center"):
                    Text("销售数据明细", style="h2")
                    Badge("共 5 条记录", color="blue")
                
                DataTable(
                    columns=[
                        {"key": "product", "title": "产品名称", "sortable": true},
                        {"key": "sales", "title": "销售额", "sortable": true, "format": "currency"},
                        {"key": "quantity", "title": "数量", "sortable": true},
                        {"key": "date", "title": "日期", "sortable": true}
                    ],
                    data=state.salesData
                )
        
        Card(padding="md", shadow="sm"):
            VStack(spacing="md"):
                Text("销售额趋势", style="h2")
                DataChart(
                    type="bar",
                    data=state.chartData,
                    xField="month",
                    yField="sales"
                )
```

## 组件说明

### DataTable

`DataTable` 组件支持显示结构化的表格数据。

#### 属性

- `columns`: 列定义，支持两种格式：
  - **简单格式**：字符串列表，如 `"Name,Age,City"`
  - **复杂格式**：对象数组，支持更多配置：
    ```nanodsl
    columns=[
        {"key": "product", "title": "产品名称", "sortable": true},
        {"key": "price", "title": "价格", "format": "currency"}
    ]
    ```
    - `key`: 数据字段名（必需）
    - `title`: 显示的列标题（可选，默认使用 key）
    - `sortable`: 是否可排序（可选，默认 false）
    - `format`: 单元格格式化（可选，支持 `currency`、`percent`）

- `data`: 数据源，支持：
  - **状态绑定**：`data=state.salesData` - 从状态中获取对象列表
  - **内联数据**：`data="John,30,NYC;Jane,25,LA"` - 使用分号分隔行，逗号分隔列

#### 示例

**简单表格**：
```nanodsl
component SimpleTable:
    state:
        users: list = [
            {"name": "Alice", "age": "30", "city": "NYC"},
            {"name": "Bob", "age": "25", "city": "LA"}
        ]
    
    DataTable(
        columns="name,age,city",
        data=state.users
    )
```

**带格式化的表格**：
```nanodsl
DataTable(
    columns=[
        {"key": "name", "title": "姓名"},
        {"key": "salary", "title": "工资", "format": "currency"},
        {"key": "rate", "title": "增长率", "format": "percent"}
    ],
    data=state.employees
)
```

### DataChart

`DataChart` 组件支持多种图表类型的数据可视化。

#### 属性

- `type`: 图表类型
  - `line`: 折线图
  - `bar` / `column`: 柱状图
  - `pie`: 饼图
  - `row`: 横向条形图

- `data`: 数据源，支持：
  - **状态绑定**：`data=state.chartData` - 从状态中获取对象列表
  - **YAML 格式**：完整的图表配置

- `xField`: X 轴数据字段名（对于 bar/column/line 图表）
- `yField`: Y 轴数据字段名（对于 bar/column/line 图表）
- `title`: 图表标题（可选）

#### 示例

**柱状图**：
```nanodsl
component BarChartDemo:
    state:
        monthlyData: list = [
            {"month": "一月", "revenue": 45000},
            {"month": "二月", "revenue": 52000},
            {"month": "三月", "revenue": 48000}
        ]
    
    DataChart(
        type="bar",
        data=state.monthlyData,
        xField="month",
        yField="revenue",
        title="月度营收"
    )
```

**折线图**：
```nanodsl
DataChart(
    type="line",
    data=state.salesTrend,
    xField="date",
    yField="sales"
)
```

**饼图**：
```nanodsl
component PieChartDemo:
    state:
        categoryData: list = [
            {"category": "电子产品", "value": 45000},
            {"category": "服装", "value": 32000},
            {"category": "食品", "value": 28000}
        ]
    
    DataChart(
        type="pie",
        data=state.categoryData,
        xField="category",
        yField="value"
    )
```

## 数据格式要求

### 对象列表格式

状态中的数据应该是对象列表，每个对象包含所需的字段：

```nanodsl
state:
    salesData: list = [
        {"product": "Laptop", "sales": 1200, "quantity": 5},
        {"product": "Phone", "sales": 800, "quantity": 10}
    ]
```

### 字段映射

- **DataTable**: 使用 `columns` 的 `key` 属性来提取对象中的字段
- **DataChart**: 使用 `xField` 和 `yField` 来指定 X 轴和 Y 轴的数据字段

## 运行测试

要验证这个示例，可以运行：

```bash
./gradlew :mpp-ui:jvmTest --tests "*NanoDataComponentsTest*" --no-daemon
```

主测试 `should parse sales dashboard with DataTable and DataChart` 应该通过。

## 实现细节

- **DataTable**: 自动将对象列表转换为表格行，支持单元格格式化（货币、百分比）
- **DataChart**: 将对象列表转换为 ChartBlockRenderer 所需的 YAML 格式
- **跨平台支持**: 
  - DataTable: 所有平台（原生 Compose 实现）
  - DataChart: JVM/Desktop、Android、iOS、WASM（使用 ComposeCharts），JS 降级显示

## 参考文档

- [NanoDataComponents Usage Guide](./data-components-usage.md)
- [ChartBlockRenderer Documentation](../design/chart-renderer.md)
- [NanoDSL Specification](../nanodsl-spec.md)
