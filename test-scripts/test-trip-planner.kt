import cc.unitmesh.xuiper.render.HtmlRenderer
import java.io.File

fun main() {
    val nanoDSLCode = """
component TripPlanner:
    state:
        departure_date: str = ""
        return_date: str = ""
        destination: str = "Singapore"
        flight_class: str = "economy"
        departure_city: str = ""

    VStack(spacing="lg"):
        Text("Singapore Trip Planner", style="h1")

        Card:
            VStack(spacing="md"):
                Text("Flight Booking", style="h2")
                HStack(align="center", justify="between"):
                    VStack:
                        Text("From", style="body")
                        Input(value:=state.departure_city, placeholder="Departure city")
                    VStack:
                        Text("To", style="body")
                        Input(value:=state.destination, placeholder="Singapore")
                HStack(align="center", justify="between"):
                    VStack:
                        Text("Departure", style="body")
                        Input(value:=state.departure_date, placeholder="Select date")
                    VStack:
                        Text("Return", style="body")
                        Input(value:=state.return_date, placeholder="Select date")
                HStack(align="center"):
                    Text("Class", style="body")
                    Button("Economy", intent="secondary"):
                        on_click: state.flight_class = "economy"
                    Button("Business", intent="secondary"):
                        on_click: state.flight_class = "business"
                Button("Search Flights", intent="primary"):
                    on_click: Fetch(url="/api/flights", method="GET")
""".trimIndent()

    val renderer = HtmlRenderer()
    val html = renderer.render(nanoDSLCode)

    // Save to file for inspection
    val outputFile = File("docs/test-scripts/trip-planner-output.html")
    outputFile.writeText("""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Trip Planner Test - HStack Layout Fix</title>
    <style>
        body {
            font-family: system-ui, -apple-system, sans-serif;
            padding: 20px;
            max-width: 1200px;
            margin: 0 auto;
            background: #f5f5f5;
        }
        #app {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .test-info {
            background: #e3f2fd;
            padding: 16px;
            border-radius: 4px;
            margin-bottom: 20px;
            border-left: 4px solid #2196f3;
        }
    </style>
</head>
<body>
    <div class="test-info">
        <h2>🧪 HStack Layout Test</h2>
        <p><strong>Testing:</strong> Text components in HStack should display horizontally, not vertically (one character per line)</p>
        <p><strong>Expected:</strong> "From", "To", "Departure", "Return", "Class" should all display normally</p>
        <p><strong>Bug:</strong> Previously, text like "From" was displayed as F-R-O-M (vertically)</p>
    </div>
    <div id="app">
        $html
    </div>
</body>
</html>
""".trimIndent())

    println("✅ HTML output saved to: ${outputFile.absolutePath}")
    println("\n=== Test Instructions ===")
    println("1. Open the HTML file in a browser")
    println("2. Check that 'From', 'To', 'Departure', 'Return' display horizontally")
    println("3. Check that Input fields are side-by-side, not stacked")
    println("4. Check that 'Class' label and buttons are in one row")
    println("\n=== Generated HTML Preview ===")
    println(html.take(500) + "...")
}

