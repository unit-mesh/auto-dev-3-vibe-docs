// Example of how to display MCP preloading status in Compose UI

@Composable
fun CodingAgentScreen(
    viewModel: CodingAgentViewModel
) {
    val mcpStatus = viewModel.mcpPreloadingStatus
    val mcpMessage = viewModel.mcpPreloadingMessage
    
    Column {
        // MCP Status Banner
        if (mcpStatus.isPreloading || mcpMessage.isNotEmpty()) {
            McpStatusBanner(
                status = mcpStatus,
                message = mcpMessage,
                isReady = viewModel.areMcpServersReady()
            )
        }
        
        // Main chat interface
        ChatInterface(
            viewModel = viewModel,
            enabled = viewModel.areMcpServersReady() // Optionally disable until MCP is ready
        )
    }
}

@Composable
fun McpStatusBanner(
    status: PreloadingStatus,
    message: String,
    isReady: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isReady && status.preloadedServers.isNotEmpty() -> Color.Green.copy(alpha = 0.1f)
                status.isPreloading -> Color.Blue.copy(alpha = 0.1f)
                else -> Color.Gray.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Icon(
                imageVector = when {
                    isReady && status.preloadedServers.isNotEmpty() -> Icons.Default.CheckCircle
                    status.isPreloading -> Icons.Default.Refresh
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = when {
                    isReady && status.preloadedServers.isNotEmpty() -> Color.Green
                    status.isPreloading -> Color.Blue
                    else -> Color.Gray
                },
                modifier = if (status.isPreloading) {
                    Modifier.rotate(
                        animateFloatAsState(
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        ).value
                    )
                } else Modifier
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Status text
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                if (status.preloadedServers.isNotEmpty()) {
                    Text(
                        text = "Loaded servers: ${status.preloadedServers.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInterface(
    viewModel: CodingAgentViewModel,
    enabled: Boolean
) {
    // Your existing chat interface implementation
    // The 'enabled' parameter can be used to disable input until MCP is ready
    
    Column {
        // Chat messages
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            // Render chat messages from viewModel.renderer
        }
        
        // Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            TextField(
                value = "", // Your input state
                onValueChange = { /* Handle input */ },
                enabled = enabled, // Disable until MCP is ready
                placeholder = {
                    Text(
                        if (enabled) "Type your message..."
                        else "Loading MCP servers..."
                    )
                },
                modifier = Modifier.weight(1f)
            )
            
            Button(
                onClick = { /* Execute task */ },
                enabled = enabled && !viewModel.isExecuting
            ) {
                Text("Send")
            }
        }
    }
}
