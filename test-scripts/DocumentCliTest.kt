package cc.unitmesh.test

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.document.DocumentAgent
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.devins.document.DocumentFile
import cc.unitmesh.devins.document.DocumentFormatType
import cc.unitmesh.devins.document.DocumentMetadata
import cc.unitmesh.devins.document.DocumentParserFactory
import cc.unitmesh.devins.document.DocumentRegistry
import cc.unitmesh.llm.JvmKoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.readBytes

/**
 * JVM CLI for testing DocumentAgent with PPTX, DOCX, PDF files
 * 
 * Usage:
 * ```bash
 * ./gradlew :docs:test-scripts:runDocumentCli -PprojectPath=/path/to/docs -Pquery="What is this about?"
 * ```
 */
object DocumentCliTest {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("=".repeat(80))
        println("AutoDev Document CLI (JVM - Tika Support)")
        println("=".repeat(80))
        
        // Parse arguments
        val projectPath = System.getProperty("projectPath") ?: args.getOrNull(0) ?: "."
        val query = System.getProperty("query") ?: args.getOrNull(1) ?: "Summarize the documents"
        val documentPath = System.getProperty("documentPath") ?: args.getOrNull(2)
        
        println("Project Path: $projectPath")
        println("Query: $query")
        if (documentPath != null) {
            println("Document: $documentPath")
        }
        println()
        
        runBlocking {
            try {
                // Initialize platform parsers
                println("🔧 Initializing document parsers (Tika)...")
                DocumentRegistry.initializePlatformParsers()
                println("✅ Parsers initialized")
                println()
                
                // Scan and register documents
                val projectDir = File(projectPath).absoluteFile
                if (!projectDir.exists()) {
                    System.err.println("❌ Project path does not exist: $projectPath")
                    return@runBlocking
                }
                
                val documents = scanDocuments(projectDir)
                println("📖 Found ${documents.size} documents")
                println()
                
                if (documents.isEmpty()) {
                    println("⚠️  No documents found in: $projectPath")
                    return@runBlocking
                }
                
                // Register documents
                println("📝 Registering documents...")
                var registeredCount = 0
                for (doc in documents) {
                    val relativePath = doc.relativeTo(projectDir).path
                    if (registerDocument(doc, relativePath)) {
                        println("  ✓ $relativePath")
                        registeredCount++
                    } else {
                        println("  ✗ $relativePath (no parser)")
                    }
                }
                println("✅ Registered $registeredCount/${documents.size} documents")
                println()
                
                // Create DocumentAgent
                println("🧠 Creating DocumentAgent...")
                
                // Create a dummy LLM service (you'll need to provide real config)
                val llmService = JvmKoogLLMService(
                    ModelConfig(
                        provider = "openai",
                        model = "gpt-4",
                        apiKey = System.getenv("OPENAI_API_KEY") ?: "",
                        temperature = 0.7,
                        maxTokens = 4096
                    )
                )
                
                val renderer = object : CodingAgentRenderer {
                    override fun renderToolCall(toolName: String, params: Map<String, Any>) {
                        println("🔧 Tool: $toolName")
                        println("   Params: $params")
                    }
                    
                    override fun renderToolResult(toolName: String, result: String) {
                        println("   Result: ${result.take(200)}${if (result.length > 200) "..." else ""}")
                    }
                    
                    override fun renderLLMResponseChunk(chunk: String) {
                        print(chunk)
                    }
                    
                    override fun renderError(error: String) {
                        System.err.println("❌ Error: $error")
                    }
                    
                    override fun renderDebug(message: String) {
                        println("🐛 $message")
                    }
                    
                    override fun renderAgentThinking(thinking: String) {
                        println("💭 $thinking")
                    }
                }
                
                val mcpConfigService = McpToolConfigService(null)
                val dummyParser = DocumentParserFactory.createParserForFile("dummy.md")!!
                
                val agent = DocumentAgent(
                    llmService = llmService,
                    parserService = dummyParser,
                    renderer = renderer,
                    fileSystem = null,
                    shellExecutor = null,
                    mcpToolConfigService = mcpConfigService,
                    enableLLMStreaming = true
                )
                
                println("✅ Agent created")
                println()
                
                // Execute query
                println("🔍 Executing query...")
                println()
                
                val result = agent.execute(
                    cc.unitmesh.agent.document.DocumentTask(
                        query = query,
                        documentPath = documentPath
                    ),
                    onProgress = { progress ->
                        // Progress callback
                    }
                )
                
                println()
                println("=".repeat(80))
                println("📊 Result:")
                println("=".repeat(80))
                println(result.content)
                println()
                
                if (result.success) {
                    println("✅ Query completed successfully")
                } else {
                    println("❌ Query failed")
                }
                
            } catch (e: Exception) {
                System.err.println("❌ Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Scan directory for documents
     */
    private fun scanDocuments(dir: File, extensions: List<String> = listOf(".md", ".pdf", ".docx", ".pptx", ".txt")): List<File> {
        val documents = mutableListOf<File>()
        val skipDirs = setOf("node_modules", ".git", "build", "dist", "target", ".gradle", "bin")
        
        fun scanRecursive(current: File) {
            if (!current.canRead()) return
            
            if (current.isDirectory) {
                if (skipDirs.contains(current.name) || current.name.startsWith(".")) {
                    return
                }
                current.listFiles()?.forEach { scanRecursive(it) }
            } else if (current.isFile) {
                val ext = current.extension.lowercase()
                if (extensions.any { it.endsWith(ext) }) {
                    documents.add(current)
                }
            }
        }
        
        scanRecursive(dir)
        return documents
    }
    
    /**
     * Register a document with the DocumentRegistry
     */
    private suspend fun registerDocument(file: File, relativePath: String): Boolean {
        try {
            // Get parser for this file type
            val parser = DocumentParserFactory.createParserForFile(file.name) ?: return false
            
            // Detect format
            val formatType = DocumentParserFactory.detectFormat(file.name) ?: DocumentFormatType.PLAIN_TEXT
            
            // Read file content
            val content = when (formatType) {
                DocumentFormatType.MARKDOWN, DocumentFormatType.PLAIN_TEXT -> {
                    file.readText()
                }
                else -> {
                    // For binary formats (PDF, DOCX, PPTX), read as bytes and convert to ISO-8859-1
                    // This is how Tika expects binary data
                    val bytes = file.readBytes()
                    String(bytes, Charsets.ISO_8859_1)
                }
            }
            
            // Create DocumentFile
            val metadata = DocumentMetadata(
                lastModified = file.lastModified(),
                fileSize = file.length(),
                formatType = formatType
            )
            
            val documentFile = DocumentFile(
                name = file.name,
                path = relativePath,
                metadata = metadata
            )
            
            // Parse document
            val parsedDoc = parser.parse(documentFile, content)
            
            // Register in registry
            DocumentRegistry.registerDocument(relativePath, parsedDoc, parser)
            
            return true
        } catch (e: Exception) {
            System.err.println("Failed to register $relativePath: ${e.message}")
            return false
        }
    }
}

