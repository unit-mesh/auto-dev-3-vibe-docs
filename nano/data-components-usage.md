# NanoDataComponents Usage Guide

## Overview

`NanoDataComponents` provides data visualization components for NanoDSL, extracted from `NanoFeedbackComponents` to better organize data-related functionality.

## Components

### DataChart

Renders various types of charts using the existing `ChartBlockRenderer` infrastructure.

#### Supported Chart Types
- `pie` - Pie charts
- `line` - Line charts
- `column` / `bar` - Column/Bar charts
- `row` - Row charts

#### Properties
- `type`: Chart type (default: "line")
- `data`: Chart data in YAML/JSON format or binding expression
- `title`: Optional chart title

#### Example Usage

```yaml
component: DataChart
props:
  type: "line"
  title: "Sales Trend"
  data: |
    type: line
    title: Sales Over Time
    data:
      lines:
        - label: "Revenue"
          values: [100, 150, 130, 180, 200]
        - label: "Expenses"
          values: [50, 70, 60, 80, 90]
```

#### With State Binding

```yaml
state:
  salesData: |
    type: column
    title: Monthly Sales
    data:
      bars:
        - label: "Q1"
          values:
            - label: "Jan"
              value: 100
            - label: "Feb"
              value: 150
            - label: "Mar"
              value: 130

component: DataChart
props:
  type: "column"
bindings:
  data: "state.salesData"
```

### DataTable

Renders tabular data with columns and rows.

#### Properties
- `columns`: Column names (comma-separated or JSON array)
- `data`: Row data (various formats supported)

#### Supported Data Formats

1. **Comma-separated columns**:
   ```yaml
   columns: "Name,Age,City"
   ```

2. **JSON array columns**:
   ```yaml
   columns: '["Name", "Age", "City"]'
   ```

3. **Row data (semicolon and comma separated)**:
   ```yaml
   data: "John,30,NYC;Jane,25,LA;Bob,35,SF"
   ```

4. **JSON array rows**:
   ```yaml
   data: |
     [
       ["John", "30", "NYC"],
       ["Jane", "25", "LA"],
       ["Bob", "35", "SF"]
     ]
   ```

#### Example Usage

```yaml
component: DataTable
props:
  columns: "Product,Quantity,Price"
  data: "Laptop,5,$1200;Phone,10,$800;Tablet,8,$500"
```

#### With State Binding

```yaml
state:
  tableColumns: "Name,Role,Department"
  tableData: |
    [
      ["Alice", "Engineer", "Tech"],
      ["Bob", "Designer", "UX"],
      ["Charlie", "Manager", "Ops"]
    ]

component: DataTable
bindings:
  columns: "state.tableColumns"
  data: "state.tableData"
```

## Platform Support

### DataChart
- **JVM/Desktop**: Full support via ComposeCharts
- **Android**: Full support via ComposeCharts
- **iOS**: Full support via ComposeCharts
- **WASM**: Full support via ComposeCharts
- **JS**: Fallback display (shows chart code)

### DataTable
- **All platforms**: Full support (native Compose implementation)

## Implementation Details

### Chart Integration

`DataChart` uses the existing `ChartBlockRenderer` infrastructure:
1. Builds chart configuration in YAML format
2. Delegates to `ChartBlockRenderer` for actual rendering
3. Falls back to simple display on unsupported platforms

### Table Rendering

`DataTable` provides a native Compose implementation with:
- Scrollable rows and columns
- Alternating row colors
- Header styling with bold text
- Proper cell alignment

## Migration from NanoFeedbackComponents

The old methods in `NanoFeedbackComponents` are now deprecated:

```kotlin
// Old (deprecated)
NanoFeedbackComponents.RenderDataChart(ir, state, modifier)
NanoFeedbackComponents.RenderDataTable(ir, state, onAction, modifier)

// New
NanoDataComponents.RenderDataChart(ir, state, modifier)
NanoDataComponents.RenderDataTable(ir, state, onAction, modifier)
```

The deprecated methods forward to the new implementations with proper `@Deprecated` annotations and `ReplaceWith` suggestions.

## Related Documentation

- [ChartBlockRenderer](../design/chart-renderer.md) - Chart rendering infrastructure
- [NanoDSL Spec](../nanodsl-spec.md) - Complete NanoDSL specification
- [NanoUI Components](./nano-components.md) - All available NanoUI components
