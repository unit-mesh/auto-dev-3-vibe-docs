# Xiuper-FS Shell Interface - Usage Examples

## Quick Start

```kotlin
import cc.unitmesh.xiuper.fs.memory.InMemoryFsBackend
import cc.unitmesh.xiuper.fs.shell.ShellFsInterpreter

val backend = InMemoryFsBackend()
val shell = ShellFsInterpreter(backend)

// Now you can use POSIX-like commands!
shell.execute("mkdir /projects")
shell.execute("echo 'Hello World' > /projects/hello.txt")
shell.execute("cat /projects/hello.txt")  // Output: Hello World
```

## Command Reference

### File Operations

```kotlin
// Create directory
shell.execute("mkdir /mydir")

// Write file
shell.execute("echo 'content' > /file.txt")

// Read file
val result = shell.execute("cat /file.txt")
println(result.stdout)  // content

// Copy file
shell.execute("cp /file.txt /file_copy.txt")

// Move/rename file
shell.execute("mv /file_copy.txt /renamed.txt")

// Delete file
shell.execute("rm /renamed.txt")
```

### Navigation

```kotlin
// Print working directory
shell.execute("pwd")  // Output: /

// Change directory
shell.execute("mkdir /workspace")
shell.execute("cd /workspace")
shell.execute("pwd")  // Output: /workspace

// Use relative paths
shell.execute("mkdir subdir")
shell.execute("echo 'test' > file.txt")
shell.execute("ls .")  // List current directory
```

### Directory Listing

```kotlin
shell.execute("mkdir /data")
shell.execute("echo 'file1 content' > /data/file1.txt")
shell.execute("echo 'file2 content' > /data/file2.txt")

val result = shell.execute("ls /data")
// Output:
// file1.txt
// file2.txt
```

## Backend Integration

### Using with Different Backends

#### InMemory Backend
```kotlin
val memBackend = InMemoryFsBackend()
val shell = ShellFsInterpreter(memBackend)

shell.execute("mkdir /temp")
shell.execute("echo 'temporary data' > /temp/data.txt")
```

#### Database Backend
```kotlin
import cc.unitmesh.xiuper.fs.db.DbFsBackend
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
XiuperFsDatabase.Schema.create(driver)
val database = XiuperFsDatabase(driver)

val dbBackend = DbFsBackend(database)
val shell = ShellFsInterpreter(dbBackend)

shell.execute("mkdir /persistent")
shell.execute("echo 'saved to database' > /persistent/data.txt")
```

#### REST Backend
```kotlin
import cc.unitmesh.xiuper.fs.http.RestFsBackend

val restBackend = RestFsBackend(
    service = RestServiceConfig(baseUrl = "https://api.example.com")
)
val shell = ShellFsInterpreter(restBackend)

shell.execute("ls /")
shell.execute("cat /api/resource")
```

#### MCP Backend
```kotlin
import cc.unitmesh.xiuper.fs.mcp.DefaultMcpBackend
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.Implementation

val mcpClient = Client(
    clientInfo = Implementation(name = "MyApp", version = "1.0.0")
)
// Connect to MCP server...

val mcpBackend = DefaultMcpBackend(mcpClient)
val shell = ShellFsInterpreter(mcpBackend)

shell.execute("ls /")  // List MCP resources
shell.execute("cat /resource/name")  // Read MCP resource
```

### Switching Backends at Runtime

```kotlin
val backend1 = InMemoryFsBackend()
val backend2 = DbFsBackend(database)
val shell = ShellFsInterpreter(backend1)

// Work with memory backend
shell.execute("echo 'in memory' > /file.txt")

// Switch to database backend
shell.switchBackend(backend2)

// Now working with database
shell.execute("echo 'in database' > /file.txt")
```

## Integration with Koog Agent

### As a Tool

```kotlin
import cc.unitmesh.agent.tool.Tool

class FilesystemTool(private val backend: FsBackend) : Tool {
    override val name = "filesystem"
    override val description = "Execute filesystem commands"
    
    private val shell = ShellFsInterpreter(backend)
    
    override suspend fun execute(input: String): String {
        val result = shell.execute(input)
        return if (result.isSuccess) {
            result.stdout
        } else {
            "Error: ${result.stderr}"
        }
    }
}

// Usage in agent
val agent = CodingAgent(...)
agent.addTool(FilesystemTool(InMemoryFsBackend()))

// LLM can now use shell commands:
// "List all Python files" → tool: ls /project | grep .py
// "Read the config file" → tool: cat /config/settings.json
```

### Integration with ShellExecutor

```kotlin
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutionConfig
import cc.unitmesh.agent.tool.shell.ShellResult

class XiuperShellExecutor(
    private val interpreter: ShellFsInterpreter
) : ShellExecutor {
    
    override suspend fun execute(
        command: String,
        config: ShellExecutionConfig
    ): ShellResult {
        val result = interpreter.execute(command)
        
        return ShellResult(
            exitCode = result.exitCode,
            stdout = result.stdout,
            stderr = result.stderr,
            command = command,
            workingDirectory = config.workingDirectory
        )
    }
    
    override fun isAvailable(): Boolean = true
    
    override fun getDefaultShell(): String = "xiuper-shell"
}

// Use with ShellSessionManager
val executor = XiuperShellExecutor(shell)
val result = executor.execute(
    "ls /data && cat /data/file.txt",
    ShellExecutionConfig()
)
```

## Advanced Usage

### Custom Commands

```kotlin
import cc.unitmesh.xiuper.fs.shell.ShellCommand
import cc.unitmesh.xiuper.fs.shell.ShellContext
import cc.unitmesh.xiuper.fs.shell.ShellResult

class GrepCommand : ShellCommand {
    override val name = "grep"
    override val description = "Search for pattern in files"
    
    override suspend fun execute(args: List<String>, context: ShellContext): ShellResult {
        if (args.size < 2) {
            return ShellResult(1, "", "grep: missing pattern or file")
        }
        
        val pattern = args[0]
        val file = args[1]
        
        // Implement grep logic...
        return ShellResult(0, "matching lines", "")
    }
}

// Register custom command
shell.registerCommand(GrepCommand())
shell.execute("grep 'TODO' /src/main.kt")
```

### Error Handling

```kotlin
val result = shell.execute("cat /nonexistent/file.txt")

when {
    result.exitCode == 0 -> {
        println("Success: ${result.stdout}")
    }
    result.exitCode == 1 -> {
        println("Error: ${result.stderr}")
    }
    result.exitCode == 127 -> {
        println("Command not found: ${result.stderr}")
    }
}
```

### Building Complex Workflows

```kotlin
suspend fun setupProject(shell: ShellFsInterpreter) {
    // Create directory structure
    shell.execute("mkdir /project")
    shell.execute("cd /project")
    shell.execute("mkdir src")
    shell.execute("mkdir docs")
    shell.execute("mkdir tests")
    
    // Create initial files
    shell.execute("echo '# My Project' > docs/README.md")
    shell.execute("echo 'fun main() {}' > src/Main.kt")
    shell.execute("echo 'import kotlin.test.*' > tests/MainTest.kt")
    
    // Verify structure
    val files = shell.execute("ls /project")
    println("Created project with: ${files.stdout}")
}
```

### Multi-Backend File Transfer

```kotlin
suspend fun syncBackends() {
    val memBackend = InMemoryFsBackend()
    val dbBackend = DbFsBackend(database)
    
    val memShell = ShellFsInterpreter(memBackend)
    val dbShell = ShellFsInterpreter(dbBackend)
    
    // Create data in memory
    memShell.execute("mkdir /cache")
    memShell.execute("echo 'cached data' > /cache/data.txt")
    
    // Read from memory
    val data = memShell.execute("cat /cache/data.txt")
    
    // Write to database
    dbShell.execute("mkdir /persistent")
    dbShell.execute("echo '${data.stdout}' > /persistent/data.txt")
}
```

## Testing

### Unit Tests

```kotlin
@Test
fun `test file operations`() = runTest {
    val backend = InMemoryFsBackend()
    val shell = ShellFsInterpreter(backend)
    
    // Test mkdir
    val mkdirResult = shell.execute("mkdir /test")
    assertEquals(0, mkdirResult.exitCode)
    
    // Test echo
    val echoResult = shell.execute("echo 'hello' > /test/file.txt")
    assertEquals(0, echoResult.exitCode)
    
    // Test cat
    val catResult = shell.execute("cat /test/file.txt")
    assertEquals(0, catResult.exitCode)
    assertEquals("hello", catResult.stdout)
}
```

### Integration Tests

```kotlin
@Test
fun `test MCP backend integration`() = runTest {
    val mcpClient = createTestMcpClient()
    val backend = DefaultMcpBackend(mcpClient)
    val shell = ShellFsInterpreter(backend)
    
    // Test listing MCP resources
    val listResult = shell.execute("ls /")
    assertTrue(listResult.exitCode == 0)
    assertTrue(listResult.stdout.isNotEmpty())
    
    // Test reading MCP resource
    val readResult = shell.execute("cat /resource/test")
    assertTrue(readResult.exitCode == 0)
}
```

## Best Practices

### 1. Always Check Exit Codes

```kotlin
val result = shell.execute("rm /important/file")
if (result.exitCode != 0) {
    logger.error("Failed to delete file: ${result.stderr}")
    // Handle error
}
```

### 2. Use Absolute Paths for Critical Operations

```kotlin
// Avoid
shell.execute("cd /project")
shell.execute("rm file.txt")  // Which file?

// Better
shell.execute("rm /project/file.txt")  // Clear and explicit
```

### 3. Validate Input Before Execution

```kotlin
fun safeExecute(shell: ShellFsInterpreter, userInput: String): ShellResult {
    // Validate user input
    if (userInput.contains("rm -rf /") || userInput.contains("..")) {
        return ShellResult(1, "", "Dangerous command blocked")
    }
    
    return shell.execute(userInput)
}
```

### 4. Use Working Directory for User Sessions

```kotlin
class UserSession(private val userId: String) {
    private val backend = InMemoryFsBackend()
    private val shell = ShellFsInterpreter(backend, initialWorkingDirectory = "/users/$userId")
    
    suspend fun executeCommand(command: String) = shell.execute(command)
}
```

## Platform Support

The shell interface is **fully cross-platform** and works on all Kotlin Multiplatform targets:

- ✅ JVM
- ✅ Android
- ✅ iOS
- ✅ JavaScript
- ✅ WASM

No platform-specific code is required - everything works the same across all platforms!

## Performance Considerations

### For Large Files
```kotlin
// Avoid reading large files directly into memory
// Instead, use streaming or chunking in your backend implementation
val result = shell.execute("cat /large/file.bin")
```

### For Many Operations
```kotlin
// Batch operations when possible
shell.execute("mkdir /a && mkdir /b && mkdir /c")

// Better than:
shell.execute("mkdir /a")
shell.execute("mkdir /b")
shell.execute("mkdir /c")
```

## Future Enhancements

Planned features:
- [ ] Pipe support: `ls /data | grep .txt`
- [ ] Redirection: `cat /file > /output`
- [ ] Glob patterns: `rm /logs/*.log`
- [ ] Command chaining: `mkdir /dir && cd /dir && echo 'hi' > file.txt`
- [ ] Shell scripting: Execute `.xsh` script files
- [ ] Tab completion: Auto-complete paths and commands
- [ ] Command history: `!!` to repeat last command

## Troubleshooting

### Command Not Found
```kotlin
val result = shell.execute("invalid-command")
println(result.stderr)  // xiuper-shell: command not found: invalid-command
```

### Path Not Found
```kotlin
val result = shell.execute("cat /nonexistent/file")
println(result.stderr)  // cat: /nonexistent/file: No such path
```

### Permission Denied (with VFS)
```kotlin
val readOnlyBackend = InMemoryFsBackend()
val mount = Mount(
    mountPoint = FsPath.of("/readonly"),
    backend = readOnlyBackend,
    readOnly = true
)
val vfs = XiuperVfs(listOf(mount))
// Writing to /readonly will fail
```

## See Also

- [Shell Interface Design](shell-interface-design.md) - Architecture and implementation details
- [MCP Backend Usage](mcp-backend-usage.md) - Using MCP servers as filesystem backends
- [REST-FS Documentation](../docs/rest-fs-design.md) - REST API backend
- [Database Backend](../docs/db-backend.md) - SQLite persistent storage
