package cc.unitmesh.devins.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.ui.nano.StatefulNanoRenderer
import cc.unitmesh.xuiper.dsl.NanoDSL

/**
 * Tokyo Trip Planner Demo - Demonstrates NanoDSL with Icon support
 * 
 * This demo shows:
 * 1. Icon component usage in NanoDSL
 * 2. Mobile-optimized layout (375dp width)
 * 3. Travel itinerary UI pattern
 * 
 * To run:
 * ```bash
 * cd mpp-ui
 * ../gradlew :mpp-ui:run -PmainClass=cc.unitmesh.devins.demo.TokyoTripDemoKt
 * ```
 */

private val TOKYO_TRIP_DSL = """
component TokyoTripPlanner:
    Card(padding="lg", shadow="md"):
        VStack(spacing="md"):
            Text("Tokyo Trip Plan", style="h2")
            Text("5-Day Itinerary", style="caption")
            
            Divider
            
            VStack(spacing="md"):
                HStack(spacing="sm", align="center"):
                    Icon("calendar", size="md", color="primary")
                    VStack(spacing="sm"):
                        Text("Day 1: Arrival & Shibuya", style="h3")
                        Text("Check-in and explore the famous crossing", style="caption")
                
                HStack(spacing="sm", align="center"):
                    Icon("hotel", size="md", color="blue")
                    VStack(spacing="sm"):
                        Text("Accommodation", style="body")
                        Text("Shibuya Excel Hotel", style="caption")
                
                Divider
                
                HStack(spacing="sm", align="center"):
                    Icon("calendar", size="md", color="primary")
                    VStack(spacing="sm"):
                        Text("Day 2: Cultural Experience", style="h3")
                        Text("Visit temples and traditional gardens", style="caption")
                
                HStack(spacing="sm", align="center"):
                    Icon("place", size="md", color="red")
                    VStack(spacing="sm"):
                        Text("Senso-ji Temple", style="body")
                        Text("Asakusa district", style="caption")
                
                HStack(spacing="sm", align="center"):
                    Icon("restaurant", size="md", color="orange")
                    VStack(spacing="sm"):
                        Text("Lunch at Tsukiji Market", style="body")
                        Text("Fresh sushi and seafood", style="caption")
                
                Divider
                
                HStack(spacing="sm", align="center"):
                    Icon("calendar", size="md", color="primary")
                    VStack(spacing="sm"):
                        Text("Day 3: Modern Tokyo", style="h3")
                        Text("Shopping and technology districts", style="caption")
                
                HStack(spacing="sm", align="center"):
                    Icon("place", size="md", color="red")
                    VStack(spacing="sm"):
                        Text("Akihabara & Harajuku", style="body")
                        Text("Electronics and fashion", style="caption")
                
                Divider
                
                HStack(spacing="sm", align="center"):
                    Icon("calendar", size="md", color="primary")
                    VStack(spacing="sm"):
                        Text("Day 4: Day Trip", style="h3")
                        Text("Mount Fuji or Nikko", style="caption")
                
                HStack(spacing="sm", align="center"):
                    Icon("flight", size="md", color="blue")
                    VStack(spacing="sm"):
                        Text("Transportation", style="body")
                        Text("JR Pass recommended", style="caption")
                
                Divider
                
                HStack(spacing="sm", align="center"):
                    Icon("calendar", size="md", color="primary")
                    VStack(spacing="sm"):
                        Text("Day 5: Departure", style="h3")
                        Text("Last-minute shopping and departure", style="caption")
            
            Divider
            
            HStack(spacing="sm", justify="between"):
                VStack(spacing="sm"):
                    HStack(spacing="sm", align="center"):
                        Icon("star", size="sm", color="yellow")
                        Text("Estimated Cost", style="caption")
                    Text("$2,500 - $3,500", style="h4")
                VStack(spacing="sm"):
                    HStack(spacing="sm", align="center"):
                        Icon("time", size="sm", color="blue")
                        Text("Best Season", style="caption")
                    Text("Spring/Fall", style="h4")
            
            Button("Book This Trip", intent="primary")
""".trimIndent()

@Composable
fun TokyoTripDemo() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Mobile preview container (375dp width like iPhone)
            Box(modifier = Modifier.width(375.dp)) {
                val ir = remember {
                    try {
                        NanoDSL.toIR(TOKYO_TRIP_DSL)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                ir?.let {
                    StatefulNanoRenderer.Render(it)
                }
            }
        }
    }
}

