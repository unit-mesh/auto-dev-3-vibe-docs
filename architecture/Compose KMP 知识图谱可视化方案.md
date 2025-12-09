# **Kotlin Compose Multiplatform 知识图谱可视化架构与实现策略深度研究报告**

## **1\. 执行摘要**

随着跨平台开发技术的不断演进，Kotlin Compose Multiplatform (CMP) 已逐渐成为构建高性能、统一用户界面（UI）的首选框架。本报告旨在深入探讨在 CMP 环境下实现复杂知识图谱（Knowledge Graph）可视化的架构策略、算法实现及性能优化路径。知识图谱作为一种通过节点（实体）和边（关系）展示复杂关联数据的拓扑结构，其可视化需求远超传统的统计图表。它要求系统具备处理数以万计节点的高性能渲染能力、符合物理规律的力导向布局算法，以及跨越移动端（Android/iOS）、桌面端（JVM）及 Web 端（Wasm/JS）的无缝交互体验。

本研究基于广泛的技术文档与社区实践分析，得出以下核心结论：  
首先，渲染层面的抽象是实现跨平台的基石。传统的基于平台原生视图（如 Android View 或 iOS UIView）的构建方式在处理大规模图谱时会遭遇严重的性能瓶颈。相比之下，基于 Skia 图形引擎的 Canvas 组件提供了统一且高效的绘图原语，能够绕过繁重的 UI 对象树，直接在帧缓冲区进行指令级渲染 1。  
其次，算法的可移植性是关键挑战。目前 CMP 生态中尚缺乏类似 JavaScript 领域 d3-force 或 Cytoscape.js 这样成熟的通用图布局库。因此，开发者需要采用“逻辑共享，UI 独立”的策略，在 commonMain 层使用纯 Kotlin 实现物理仿真算法（如 Barnes-Hut 优化的力导向算法），并通过 Kotlin Coroutines 管理计算线程，以确保主线程的流畅度 2。  
最后，Web 端的二元选择需根据项目需求权衡。Compose for Web 目前存在 Wasm（高性能 Canvas 渲染）和 JS（DOM 操作）两种技术路线。对于追求极致性能和代码复用率的项目，Wasm 是未来趋势；而对于依赖现有 Web 生态（如 D3.js）的项目，通过 Kotlin/JS 互操作性进行混合开发则是务实的过渡方案 4。  
本报告将分章节详细阐述从底层数据结构设计、物理引擎实现、渲染管线优化到高级交互处理的全链路解决方案，为技术决策者和架构师提供详实的参考依据。

## **2\. 技术背景与挑战分析**

### **2.1 知识图谱可视化的核心定义**

在计算机科学与数据分析领域，知识图谱的可视化不仅仅是将数据投射到屏幕上，它是一种通过空间布局揭示数据隐含结构的认知工具。与柱状图或饼图不同，图谱可视化没有固定的坐标轴，节点的位置完全由布局算法根据拓扑结构动态计算得出。这种“生成式”的特性对计算能力和渲染效率提出了双重挑战。在移动设备受限的算力和 Web 端多样的运行环境（浏览器引擎）下，如何保持 60FPS 的流畅交互是本次研究的核心议题 6。

### **2.2 Kotlin Compose Multiplatform 的架构优势**

CMP 的核心优势在于其“一次编写，处处运行”的声明式 UI 范式。它并非简单地封装原生控件，而是拥有自己完整的渲染管线：

* **Android:** 直接利用 Jetpack Compose，底层对接 Android Canvas。  
* **iOS:** 通过 Skia 引擎在 UIViewController 或 SwiftUI 视图中绘制，绕过了 UIKit 的复杂性 1。  
* **Desktop (JVM):** 基于 Skia (Skiko) 在 Swing/AWT 窗口中渲染，支持硬件加速。  
* **Web (Wasm):** 将 Kotlin 编译为 WebAssembly，利用 HTML5 Canvas 进行绘制，极大地提升了计算密集型任务（如布局算法）的执行效率 8。

这种架构使得开发者可以在 commonMain 模块中编写统一的绘制逻辑（Draw Scope），而无需为每个平台单独实现 onDraw 方法。然而，这也意味着失去了直接使用平台特定高性能图表库（如 iOS 的 CorePlot 或 Android 的 MPAndroidChart）的便利性，除非通过互操作层进行封装，但这会牺牲跨平台的统一性 9。

### **2.3 现有生态系统的局限性**

通过对现有库的调研发现，虽然 Lets-Plot 11 和 Compose Multiplatform Charts 12 等库在统计图表方面表现优异，但它们缺乏对网络图（Network Graph）布局算法的原生支持。KNodeFlow 13 展示了节点编辑器的可能性，但主要面向流程图而非大规模力导向图。因此，构建一个工业级的知识图谱组件，目前主要依赖于开发者基于底层原语进行自研或移植 14。

## **3\. 渲染架构：从 DOM 到 Canvas 的范式转移**

在处理包含数千个节点和边的知识图谱时，渲染策略的选择直接决定了应用的性能上限。

### **3.1 声明式组件 vs. 即时模式渲染 (Immediate Mode Rendering)**

在 Compose 中，通常使用 @Composable 函数（如 Box, Column）来构建 UI。然而，每个 Composable 函数都会在内存中生成对应的节点对象（LayoutNode），并参与测量（Measure）、布局（Layout）和绘制（Draw）三个阶段 1。  
当图谱节点数量 $N \> 1000$ 时，如果为每个节点创建一个 Box，系统的开销将呈线性增长，导致帧率急剧下降。  
最佳实践：单一 Canvas 架构  
为了实现高性能渲染，必须采用单一 Canvas 组件。在这个 Canvas 的 onDraw 作用域内，我们不再思考“组件”，而是思考“图元”（Primitives）。

Kotlin

Canvas(modifier \= Modifier.fillMaxSize()) {  
    // 批量绘制边  
    graphState.edges.forEach { edge \-\>  
        drawLine(  
            color \= edge.color,  
            start \= edge.source.position,  
            end \= edge.target.position,  
            strokeWidth \= edge.width  
        )  
    }  
    // 批量绘制节点  
    graphState.nodes.forEach { node \-\>  
        drawCircle(  
            color \= node.color,  
            radius \= node.radius,  
            center \= node.position  
        )  
    }  
}

这种方式被称为即时模式渲染。它跳过了 Compose 的 Layout 阶段，直接向 GPU 发送绘图指令。Skia 引擎对这种批量的几何图形绘制进行了高度优化，能够轻松处理数万级别的图元 1。

### **3.2 无限画布与空间导航系统**

知识图谱通常需要在一个“无限”的二维平面上展示。在 CMP 中，这并非通过传统的 ScrollView 实现，而是通过矩阵变换（Matrix Transformation）来模拟摄像机的移动。

实现机制：  
利用 Modifier.pointerInput 监听手势事件（拖拽、双指缩放），并维护一个包含 scale（缩放比例）和 offset（位移向量）的视图状态（Viewport State）16。

Kotlin

var scale by remember { mutableStateOf(1f) }  
var offset by remember { mutableStateOf(Offset.Zero) }

Box(  
    modifier \= Modifier  
       .pointerInput(Unit) {  
            detectTransformGestures { \_, pan, zoom, \_ \-\>  
                scale \*= zoom  
                offset \+= pan  
            }  
        }  
       .graphicsLayer(  
            scaleX \= scale,  
            scaleY \= scale,  
            translationX \= offset.x,  
            translationY \= offset.y  
        )  
) {  
    GraphCanvas(...)  
}

使用 Modifier.graphicsLayer 是性能优化的关键点。它指示渲染引擎在 GPU 层面应用变换，而不是在每一帧重新计算 CPU 层的绘图坐标。这样，即使用户在平移庞大的图谱，只要内容本身没有变化（布局静止），GPU 只需要复用纹理并修改变换矩阵，极大降低了能耗 16。

对于缩放操作，必须处理“以鼠标/手指为中心缩放”的数学逻辑。这涉及到将屏幕坐标系（Screen Space）转换为世界坐标系（World Space），调整偏移量以保持焦点稳定。具体公式如下：

$$Offset\_{new} \= Offset\_{old} \+ (Focus\_{screen} \- Offset\_{old}) \\times (1 \- \\frac{Scale\_{new}}{Scale\_{old}})$$

这一逻辑在 Web 和移动端通用，是实现“地图级”交互体验的基础 18。

## **4\. 算法核心：力导向布局的 Kotlin 实现**

知识图谱的美学价值和可读性依赖于布局算法。最经典且适用性最广的是**力导向算法（Force-Directed Layout）**。该算法模拟物理系统：节点作为带电粒子相互排斥，边作为弹簧牵引相连节点 2。

### **4.1 物理模型的数学构建**

在 Kotlin 中实现该算法，需要定义三个核心力：

1. 库仑斥力 (Coulomb Repulsion): 防止节点重叠。

   $$F\_{rep} \= k\_{rep} \\frac{1}{d^2}$$

   其中 $d$ 是两节点间的距离。该力作用于所有节点对之间。  
2. 胡克引力 (Hooke's Attraction): 保持关联节点的紧凑。

   $$F\_{att} \= k\_{att} \\cdot (d \- L)$$

   其中 $L$ 是边的理想长度。该力仅作用于相连的节点。  
3. 中心引力/重力 (Center Gravity): 防止图谱漂移出可视区域。

   $$F\_{grav} \= k\_{grav} \\cdot |P \- Center|$$

### **4.2 纯 Kotlin 仿真循环**

为了支持多平台，物理引擎必须完全与 UI 解耦，并在 commonMain 中实现。

Kotlin

class PhysicsEngine(  
    private val nodes: List\<Node\>,  
    private val edges: List\<Edge\>  
) {  
    fun step(dt: Float) {  
        // 1\. 初始化合力  
        nodes.forEach { it.force \= Vector2.Zero }  
          
        // 2\. 计算斥力 (O(N^2))  
        for (i in nodes.indices) {  
            for (j in i \+ 1 until nodes.size) {  
                applyRepulsion(nodes\[i\], nodes\[j\])  
            }  
        }  
          
        // 3\. 计算引力 (O(E))  
        edges.forEach { applySpringForce(it) }  
          
        // 4\. 韦尔莱积分 (Verlet Integration) 更新位置  
        nodes.forEach { node \-\>  
            val velocity \= (node.position \- node.prevPosition) \* damping \+ node.force \* dt \* dt  
            node.prevPosition \= node.position  
            node.position \+= velocity  
        }  
    }  
}

此代码结构允许算法在 JVM 上进行单元测试，并在 Android/iOS 的后台线程中运行，互不干扰 2。

### **4.3 与 Compose 帧循环的同步**

为了实现动画效果，仿真循环需要由 Compose 的时钟驱动。withFrameNanos 是实现这一同步的关键函数 21。

Kotlin

LaunchedEffect(simulationState) {  
    while (isActive) {  
        withFrameNanos { time \-\>  
            // 计算下一帧的物理状态  
            physicsEngine.step(timeStep)  
            // 更新 UI 状态，触发重绘  
            graphState.updatePositions()  
        }  
    }  
}

这种模式确保了物理计算的步进与屏幕刷新率（通常 60Hz）保持一致，避免了“幽灵帧”或动画抖动。

## **5\. 大规模数据的性能优化策略**

当节点数量从几百增加到几千甚至上万时，朴素的力导向算法（$O(N^2)$ 复杂度）会迅速导致计算瓶颈。针对 CMP 环境，我们需要引入空间索引和渲染剔除技术。

### **5.1 Barnes-Hut 算法与 QuadTree (四叉树)**

Barnes-Hut 算法通过将远处的节点群视为一个“超级节点”来近似计算斥力，将复杂度降低至 $O(N \\log N)$。实现这一点的核心数据结构是 **QuadTree** 3。

在 Kotlin 中实现 QuadTree 需要注意对象分配的开销。

* **结构设计：** QuadTree 递归地将 2D 空间划分为四个象限（NW, NE, SW, SE）。每个节点存储其包含的所有子节点的质心（Center of Mass）和总质量。  
* **Kotlin 优化：** 避免使用大量的递归函数调用，改用迭代方式或对象池（Object Pooling）技术来复用 QuadTree 节点对象，减少垃圾回收（GC）压力，这在 Wasm 和移动端尤为重要 1。

### **5.2 视口剔除 (Viewport Culling)**

在渲染阶段，不需要绘制视口之外的节点。  
算法逻辑：

1. 根据当前的平移 offset 和缩放 scale 计算出可见的世界坐标区域 visibleRect。  
2. 利用 QuadTree 快速查询该区域内的节点（范围查询，Range Query）22。  
3. 仅将查询到的节点提交给 Canvas 进行绘制。  
   这种技术可以将渲染开销从 $O(N)$ 降低到 $O(V)$，其中 $V$ 是可见节点的数量。即使图谱有 10 万个节点，如果用户只缩放到查看其中 50 个，渲染性能依然能保持在 60FPS。

### **5.3 细节层次 (LOD \- Level of Detail)**

随着缩放比例的变化，绘制的细节应当动态调整 1。

* **Zoom \< 0.5:** 仅绘制节点为点（drawPoints），不绘制边和文字标签。  
* **0.5 \< Zoom \< 1.0:** 绘制节点形状和边，隐藏标签。  
* Zoom \> 1.0: 绘制完整细节，包括文字标签和图标。  
  这不仅提升了渲染性能（文字渲染通常是最耗时的），也改善了用户体验，避免了视觉混乱。

## **6\. 交互设计与状态管理**

### **6.1 状态管理架构 (MVI)**

在 CMP 中，推荐使用单向数据流（UDF）架构。对于知识图谱，状态对象通常较为庞大，因此必须谨慎处理不可变性 23。

**状态定义：**

Kotlin

data class GraphUiState(  
    val nodes: SnapshotStateList\<NodeUiModel\>, // 使用 SnapshotStateList 实现细粒度更新  
    val edges: SnapshotStateList\<EdgeUiModel\>,  
    val isSimulating: Boolean \= false,  
    val selectedNodeId: String? \= null  
)

使用 SnapshotStateList 而非标准的 List 是关键。当物理引擎更新某一节点的坐标时，如果整个 List 被重新赋值，会导致整个 Canvas 重绘。而 SnapshotStateList 允许 Compose 追踪到具体的元素变化，虽然在 Canvas 中通常还是触发整体重绘，但在配合 DerivedState 使用时能提供更好的性能隔离 25。

### **6.2 碰撞检测与点击拾取**

用户点击屏幕时，需要判断点击了哪个节点。

* **坐标逆变换：** 首先将屏幕像素坐标 $(x, y)$ 通过逆变换矩阵映射回世界坐标 $(x', y')$。  
* 空间查询： 使用前文构建的 QuadTree 进行最近邻搜索（Nearest Neighbor Search），在 $O(\\log N)$ 时间内找到距离点击点最近且在半径范围内的节点 26。  
  这种方法比遍历所有节点计算距离要快几个数量级，保证了点击响应的瞬时性。

### **6.3 无障碍支持 (Accessibility)**

Canvas 最大的缺点是它对辅助技术（如屏幕阅读器）是不可见的“黑盒”。  
解决方案： 使用 Modifier.semantics 手动构建语义树。  
开发者可以为 Canvas 添加一个不可见的子组件树，或者直接在 Canvas 的 modifier 上定义语义属性。对于图谱，更高级的做法是根据视口内的节点动态生成语义节点（SemanticsNode），描述节点的内容和关系，使得盲人用户也能通过“下一个/上一个”操作遍历图谱结构 27。

## **7\. Web 端特定策略：Wasm 与 JS 的抉择**

CMP 的 Web 目标平台目前处于快速发展期，提供了 Kotlin/Wasm 和 Kotlin/JS 两种编译目标。

### **7.1 Kotlin/Wasm (Canvas 路线)**

这是 CMP 的原生路线。Kotlin 代码编译为 WebAssembly 字节码，UI 通过 Canvas 绘制。

* **优势：** 计算性能极高，适合运行复杂的力导向算法；代码与移动端 100% 共享。  
* **挑战：** 初始加载体积较大（包含 Skia 库）；对 CSS/DOM 的交互能力较弱；目前处于 Alpha/Beta 阶段，浏览器兼容性需关注（需要支持 GC 的 Wasm 浏览器）4。

### **7.2 Kotlin/JS (Interop 路线)**

如果项目必须依赖现有的 Web 生态（如 D3.js 强大的交互能力或 Cytoscape.js 的丰富插件），可以使用 Kotlin/JS。

* **实现：** 利用 Kotlin 的 external 关键字声明 JS 库的接口 28。  
  Kotlin  
  // 定义 Cytoscape.js 的 Kotlin 接口  
  external class Cytoscape(options: dynamic) {  
      fun add(elements: dynamic)  
      fun layout(options: dynamic)  
  }

* **架构模式：** 在这种模式下，Android/iOS 使用 Skia Canvas，而 Web 端使用 DomNode 包裹的 Cytoscape 实例。这就需要在 commonMain 中定义一个抽象的 GraphRenderer 接口，在 androidMain 和 jsMain 中分别实现。  
* **数据转换：** 需要将 Kotlin 的数据模型序列化为 JSON 格式传递给 JS 库 30。

**决策建议：** 对于追求极致统一体验和高性能计算的知识图谱应用，推荐使用 **Kotlin/Wasm \+ 自研/移植 Canvas 渲染**。这种方式避免了维护两套渲染逻辑的痛苦，且随着 Wasm 的成熟，性能优势将愈发明显。

## **8\. 现有库与自研方案对比**

### **8.1 现有库分析**

* **Lets-Plot:** 专注于统计数据可视化，虽然支持基本的散点图，但缺乏交互式的节点拖拽和力导向布局，不适合动态知识图谱 11。  
* **Compose Multiplatform Charts:** 主要提供饼图、柱状图等商业图表，未覆盖网络图领域 12。  
* **KNodeFlow:** 提供了一个节点编辑器的雏形，证明了 Compose 处理节点连接的可行性，但其架构更偏向于手动布局而非算法自动布局 13。

### **8.2 自研/移植路径**

基于 kotlin-graphs 14 处理图算法，结合 kbox2d 32 或自写的物理引擎，并在 Compose Canvas 上绘制，是目前最可行的路径。  
虽然开发成本较高，但它赋予了开发者对渲染管线的完全控制权，能够针对特定业务需求（如特殊的节点形状、复杂的边动画）进行深度定制。

## **9\. 结论**

在 Kotlin Compose Multiplatform 上实现知识图谱可视化，是一项融合了计算机图形学、物理仿真算法与现代响应式 UI 架构的系统工程。通过摒弃传统的平台原生视图，转而拥抱基于 Skia 的 Canvas 渲染范式，开发者能够构建出在性能和体验上高度一致的跨平台应用。虽然目前缺乏开箱即用的“全家桶”库，但 Compose 提供的底层原语（Canvas, Gestures, GraphicsLayer）与 Kotlin 强大的语言特性（Coroutines, Wasm 支持）相结合，完全具备了支撑工业级大规模图谱渲染的能力。

随着 Kotlin/Wasm 生态的成熟，未来我们可以预见更多基于 WebAssembly 的高性能图算法库将被移植到 Kotlin 生态中，届时知识图谱可视化的开发门槛将大幅降低。对于当下的先行者，采取“核心算法共享 \+ Canvas 统一渲染 \+ 空间索引优化”的策略，是通往成功的最佳路径。

## ---

**附录：数据对比表**

### **表 1：不同渲染方案的特性对比**

| 特性 | Compose Canvas (原生自研) | Kotlin/JS (D3.js/Cytoscape Wrapper) | 传统 View (Android/iOS Native) |
| :---- | :---- | :---- | :---- |
| **跨平台能力** | **极高** (Android, iOS, Desktop, Web/Wasm) | **低** (仅限 Web，移动端需 WebView) | **无** (需维护多套代码) |
| **渲染性能** | **高** (GPU 加速，Skia 引擎) | **中/高** (依赖浏览器 Canvas/WebGL) | **低** (大量对象开销) |
| **算法性能** | **高** (Wasm/Native 编译，接近 C++) | **中** (JS 引擎 JIT) | **高** (Native) |
| **开发成本** | **高** (需实现物理引擎和交互) | **低** (复用成熟库) | **极高** (多平台重复开发) |
| **包体积** | **中** (包含 Skia 运行时) | **小** (依赖浏览器能力) | **小** (利用系统库) |
| **可定制性** | **完全控制** | **受限于库 API** | **受限于系统 API** |

### **表 2：大规模图谱优化技术栈**

| 瓶颈领域 | 常见问题 | 解决方案 | 算法复杂度优化 | 参考来源 |
| :---- | :---- | :---- | :---- | :---- |
| **物理计算** | $O(N^2)$ 斥力计算导致卡顿 | **Barnes-Hut 算法 / QuadTree** | $O(N^2) \\to O(N \\log N)$ | 3 |
| **渲染绘制** | 视口外节点无效绘制 | **视口剔除 (Viewport Culling)** | $O(N) \\to O(V)$ | 1 |
| **交互响应** | 点击检测遍历耗时 | **空间索引查找** | $O(N) \\to O(\\log N)$ | 26 |
| **内存管理** | 频繁对象创建触发 GC | **对象池 / 结构体数组 (SoA)** | 减少内存抖动 | 1 |
| **主线程阻塞** | 计算占用 UI 线程 | **Kotlin Coroutines (Default Dispatcher)** | 异步并行计算 | 21 |

#### **Works cited**

1. Mastering Compose Multi-platform Rendering: Under the Hood, Performance, and Best Practices | by praveen sharma | Medium, accessed December 8, 2025, [https://medium.com/@sharmapraveen91/mastering-compose-multi-platform-rendering-under-the-hood-performance-and-best-practices-c3a8c785a0c9](https://medium.com/@sharmapraveen91/mastering-compose-multi-platform-rendering-under-the-hood-performance-and-best-practices-c3a8c785a0c9)  
2. Force-Directed Graph Layout \- yWorks, accessed December 8, 2025, [https://www.yworks.com/pages/force-directed-graph-layout](https://www.yworks.com/pages/force-directed-graph-layout)  
3. Spatial-Partitioning-Quadtree \- GitHub Pages, accessed December 8, 2025, [https://carlosupc.github.io/Spatial-Partitioning-Quadtree/](https://carlosupc.github.io/Spatial-Partitioning-Quadtree/)  
4. How viable is Compose Multiplatform for web? : r/Kotlin \- Reddit, accessed December 8, 2025, [https://www.reddit.com/r/Kotlin/comments/1gnb3co/how\_viable\_is\_compose\_multiplatform\_for\_web/](https://www.reddit.com/r/Kotlin/comments/1gnb3co/how_viable_is_compose_multiplatform_for_web/)  
5. Using Compose on the Web without Wasm \- helw.net, accessed December 8, 2025, [https://helw.net/2023/06/04/using-compose-on-the-web-without-wasm/](https://helw.net/2023/06/04/using-compose-on-the-web-without-wasm/)  
6. Directed Graph with Kotlin. A graph is a mathematical structure… | by Arilson José de Oliveira Júnior | Medium, accessed December 8, 2025, [https://medium.com/@arilsonjr/directed-graph-with-kotlin-292913911529](https://medium.com/@arilsonjr/directed-graph-with-kotlin-292913911529)  
7. Implement Graph in Kotlin \- SSOJet, accessed December 8, 2025, [https://ssojet.com/data-structures/implement-graph-in-kotlin/](https://ssojet.com/data-structures/implement-graph-in-kotlin/)  
8. Kotlin/Wasm | Kotlin Documentation, accessed December 8, 2025, [https://kotlinlang.org/docs/wasm-overview.html](https://kotlinlang.org/docs/wasm-overview.html)  
9. Adopting Kotlin Multiplatform without Chaos — Part. 2: How to introduce Kotlin Multiplatform? | by Santiago Mattiauda \- Medium, accessed December 8, 2025, [https://medium.com/@santimattius/adopting-kotlin-multiplatform-without-chaos-part-2-how-to-introduce-kotlin-multiplatform-29db5c68070e](https://medium.com/@santimattius/adopting-kotlin-multiplatform-without-chaos-part-2-how-to-introduce-kotlin-multiplatform-29db5c68070e)  
10. How to convert Kotlin project to Kotlin Multiplatform Mobile after the project completion?, accessed December 8, 2025, [https://stackoverflow.com/questions/71510247/how-to-convert-kotlin-project-to-kotlin-multiplatform-mobile-after-the-project-c](https://stackoverflow.com/questions/71510247/how-to-convert-kotlin-project-to-kotlin-multiplatform-mobile-after-the-project-c)  
11. Data visualization with Lets-Plot for Kotlin, accessed December 8, 2025, [https://kotlinlang.org/docs/lets-plot.html](https://kotlinlang.org/docs/lets-plot.html)  
12. netguru/compose-multiplatform-charts: Charts library for Kotlin multiplatform applications \- GitHub, accessed December 8, 2025, [https://github.com/netguru/compose-multiplatform-charts](https://github.com/netguru/compose-multiplatform-charts)  
13. Visual Node Editor for Compose Multiplatform : r/androiddev \- Reddit, accessed December 8, 2025, [https://www.reddit.com/r/androiddev/comments/1mbudvz/visual\_node\_editor\_for\_compose\_multiplatform/](https://www.reddit.com/r/androiddev/comments/1mbudvz/visual_node_editor_for_compose_multiplatform/)  
14. Directed, undirected, weighted and unweighted graph algorithms for Kotlin Multiplatform. \- GitHub, accessed December 8, 2025, [https://github.com/alexandrepiveteau/kotlin-graphs](https://github.com/alexandrepiveteau/kotlin-graphs)  
15. Practical performance problem solving in Jetpack Compose \- Android Developers, accessed December 8, 2025, [https://developer.android.com/codelabs/jetpack-compose-performance](https://developer.android.com/codelabs/jetpack-compose-performance)  
16. Multitouch: Panning, zooming, rotating | Jetpack Compose \- Android Developers, accessed December 8, 2025, [https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch)  
17. How to properly scale a Jetpack Compose Canvas game across all Android screen sizes (no stretching)? : r/Kotlin \- Reddit, accessed December 8, 2025, [https://www.reddit.com/r/Kotlin/comments/1ohh6x0/how\_to\_properly\_scale\_a\_jetpack\_compose\_canvas/](https://www.reddit.com/r/Kotlin/comments/1ohh6x0/how_to_properly_scale_a_jetpack_compose_canvas/)  
18. Infinite HTML canvas with zoom and pan | Sandro Maglione, accessed December 8, 2025, [https://www.sandromaglione.com/articles/infinite-canvas-html-with-zoom-and-pan](https://www.sandromaglione.com/articles/infinite-canvas-html-with-zoom-and-pan)  
19. mxalbert1996/Zoomable: Content zooming with dragging, double tap and dismiss gesture support for Compose Multiplatform. \- GitHub, accessed December 8, 2025, [https://github.com/mxalbert1996/Zoomable](https://github.com/mxalbert1996/Zoomable)  
20. Force-directed graph drawing \- Wikipedia, accessed December 8, 2025, [https://en.wikipedia.org/wiki/Force-directed\_graph\_drawing](https://en.wikipedia.org/wiki/Force-directed_graph_drawing)  
21. Jetpack Compose Animations in Real Time | by Halil Ozercan | ProAndroidDev, accessed December 8, 2025, [https://proandroiddev.com/compose-animations-in-real-time-6068f10595ba](https://proandroiddev.com/compose-animations-in-real-time-6068f10595ba)  
22. Implement Quadtree in Kotlin \- SSOJet, accessed December 8, 2025, [https://ssojet.com/data-structures/implement-quadtree-in-kotlin/](https://ssojet.com/data-structures/implement-quadtree-in-kotlin/)  
23. State Management in Kotlin Multiplatform: My Complete Survival Guide \- Medium, accessed December 8, 2025, [https://medium.com/@hiren6997/state-management-in-kotlin-multiplatform-my-complete-survival-guide-c03b32c08038](https://medium.com/@hiren6997/state-management-in-kotlin-multiplatform-my-complete-survival-guide-c03b32c08038)  
24. Mastering Global State Management in Android with Jetpack Compose \- droidcon, accessed December 8, 2025, [https://www.droidcon.com/2025/01/23/mastering-global-state-management-in-android-with-jetpack-compose/](https://www.droidcon.com/2025/01/23/mastering-global-state-management-in-android-with-jetpack-compose/)  
25. \[Jetpack Compose\] How to animate and improve performance for a smoother animation | by Mnasrallah | Medium, accessed December 8, 2025, [https://medium.com/@mnasrallah301/jetpack-compose-how-to-animate-and-improve-performance-for-a-smoother-animation-4421479f31d9](https://medium.com/@mnasrallah301/jetpack-compose-how-to-animate-and-improve-performance-for-a-smoother-animation-4421479f31d9)  
26. The magic of quad trees (spatial partitioning) \- Zach Thompson, accessed December 8, 2025, [https://www.zachmakesgames.com/node/22](https://www.zachmakesgames.com/node/22)  
27. What's new in Compose Multiplatform 1.9.3 \- Kotlin, accessed December 8, 2025, [https://kotlinlang.org/docs/multiplatform/whats-new-compose-190.html](https://kotlinlang.org/docs/multiplatform/whats-new-compose-190.html)  
28. Interoperability with JavaScript | Kotlin Documentation, accessed December 8, 2025, [https://kotlinlang.org/docs/wasm-js-interop.html](https://kotlinlang.org/docs/wasm-js-interop.html)  
29. Kotlin/Wasm interop with Javascript \- Touchlab, accessed December 8, 2025, [https://touchlab.co/kotlin-wasm-js-interop](https://touchlab.co/kotlin-wasm-js-interop)  
30. Cytoscape tools for the web age: D3.js and Cytoscape.js exporters \- PMC \- NIH, accessed December 8, 2025, [https://pmc.ncbi.nlm.nih.gov/articles/PMC4264639/](https://pmc.ncbi.nlm.nih.gov/articles/PMC4264639/)  
31. Introducing Compose Multiplatform Charts: Solution for Seamless Charts Integration To APP | by Yash Kalariya | Mobile Innovation Network | Medium, accessed December 8, 2025, [https://medium.com/mobile-innovation-network/introducing-compose-multiplatform-charts-solution-for-seamless-charts-integration-to-app-488355a6e098](https://medium.com/mobile-innovation-network/introducing-compose-multiplatform-charts-solution-for-seamless-charts-integration-to-app-488355a6e098)  
32. soywiz-archive/kbox2d: Port of jbox2d to multiplatform kotlin. Used in korge as extension: https://github.com/korlibs/korge/tree/master/korge-box2d \- GitHub, accessed December 8, 2025, [https://github.com/soywiz-archive/kbox2d](https://github.com/soywiz-archive/kbox2d)