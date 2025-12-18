# Xiuper-FS Shell Interface Design

## Overview

使用 shell 命令作为入口层，通过 POSIX-like 命令操作所有 xiuper-fs backend（InMemory, DB, REST, MCP）。

## Architecture

```
┌─────────────────────────────────────────┐
│         User Shell Commands             │
│   (ls, cat, echo, mkdir, rm, cd, etc)  │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│      ShellFsInterpreter                 │
│  - Parse shell commands                 │
│  - Map to FsBackend operations          │
│  - Handle pipes, redirects              │
│  - Manage working directory             │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│         FsBackend                       │
│  - InMemoryBackend                      │
│  - DbFsBackend                          │
│  - RestFsBackend                        │
│  - McpBackend                           │
└─────────────────────────────────────────┘
```

## Supported Commands

### Basic File Operations
```bash
# List files
ls /path
ls -l /path          # long format
ls -a /path          # show hidden files

# Read file content
cat /path/to/file
head -n 10 /file     # first 10 lines
tail -n 10 /file     # last 10 lines

# Write file
echo "content" > /path/to/file       # overwrite
echo "content" >> /path/to/file      # append

# Copy/Move/Delete
cp /source /dest
mv /source /dest
rm /path/to/file
rm -r /path/to/dir   # recursive delete

# Create directory
mkdir /path/to/dir
mkdir -p /path/to/nested/dir  # create parents

# Change working directory
cd /path
pwd                  # print working directory
```

### Advanced Operations
```bash
# Find files
find /path -name "*.txt"
find /path -type f   # files only
find /path -type d   # directories only

# Grep content
grep "pattern" /path/to/file
grep -r "pattern" /path/to/dir

# Pipe support
ls /path | grep ".txt"
cat /file | head -n 10

# Redirection
cat /file > /output
ls /path 2> /errors  # stderr redirect
```

### Backend Selection
```bash
# Switch backend
use inmemory
use db
use rest http://api.example.com
use mcp mcp://server-name

# Show current backend
backend status

# Multi-backend operations
# Copy from REST to DB
cp rest:/path/file db:/path/file
```

## Implementation Plan

### Phase 1: Core Shell Interpreter
```kotlin
// xiuper-fs/src/commonMain/kotlin/cc/unitmesh/xiuper/fs/shell/

interface ShellCommand {
    val name: String
    suspend fun execute(args: List<String>, context: ShellContext): ShellResult
}

class ShellContext(
    val backend: FsBackend,
    val workingDirectory: String = "/",
    val environment: MutableMap<String, String> = mutableMapOf(),
    val stdin: String? = null  // for pipe support
)

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)

class ShellFsInterpreter(
    private var currentBackend: FsBackend,
    private var workingDirectory: String = "/"
) {
    private val commands = mutableMapOf<String, ShellCommand>()
    
    init {
        registerBuiltinCommands()
    }
    
    suspend fun execute(commandLine: String): ShellResult {
        val parsed = parseCommand(commandLine)
        val command = commands[parsed.name]
            ?: return ShellResult(127, "", "Command not found: ${parsed.name}")
        
        val context = ShellContext(
            backend = currentBackend,
            workingDirectory = workingDirectory
        )
        
        return command.execute(parsed.args, context)
    }
    
    private fun parseCommand(line: String): ParsedCommand {
        // Handle quotes, pipes, redirects
        // TODO: Implement shell parser
    }
    
    private fun registerBuiltinCommands() {
        register(LsCommand())
        register(CatCommand())
        register(EchoCommand())
        register(MkdirCommand())
        register(RmCommand())
        register(CdCommand())
        register(PwdCommand())
        register(CpCommand())
        register(MvCommand())
        // ... more commands
    }
}
```

### Phase 2: Implement Core Commands

```kotlin
// LsCommand.kt
class LsCommand : ShellCommand {
    override val name = "ls"
    
    override suspend fun execute(args: List<String>, context: ShellContext): ShellResult {
        val options = parseOptions(args)
        val path = resolvePath(args.lastOrNull() ?: ".", context.workingDirectory)
        
        return try {
            val entries = context.backend.list(path)
            val output = formatEntries(entries, options)
            ShellResult(0, output, "")
        } catch (e: Exception) {
            ShellResult(1, "", e.message ?: "Unknown error")
        }
    }
    
    private fun formatEntries(entries: List<FilesystemEntry>, options: LsOptions): String {
        if (options.longFormat) {
            return entries.joinToString("\n") { entry ->
                val type = if (entry.isDirectory()) "d" else "-"
                val perms = "rwxr-xr-x"  // TODO: Get real permissions
                val size = entry.size ?: 0
                val modified = entry.modified?.toString() ?: ""
                "$type$perms $size $modified ${entry.name}"
            }
        } else {
            return entries.joinToString(" ") { it.name }
        }
    }
}

// CatCommand.kt
class CatCommand : ShellCommand {
    override val name = "cat"
    
    override suspend fun execute(args: List<String>, context: ShellContext): ShellResult {
        if (args.isEmpty()) {
            return ShellResult(1, "", "Usage: cat <file>")
        }
        
        val path = resolvePath(args[0], context.workingDirectory)
        
        return try {
            val content = context.backend.read(path)
            ShellResult(0, content.decodeToString(), "")
        } catch (e: Exception) {
            ShellResult(1, "", e.message ?: "Unknown error")
        }
    }
}

// EchoCommand.kt
class EchoCommand : ShellCommand {
    override val name = "echo"
    
    override suspend fun execute(args: List<String>, context: ShellContext): ShellResult {
        val text = args.joinToString(" ")
        
        // Handle redirection
        val (content, redirectPath) = parseRedirection(args)
        
        return if (redirectPath != null) {
            try {
                val path = resolvePath(redirectPath, context.workingDirectory)
                context.backend.write(path, content.encodeToByteArray())
                ShellResult(0, "", "")
            } catch (e: Exception) {
                ShellResult(1, "", e.message ?: "Unknown error")
            }
        } else {
            ShellResult(0, text, "")
        }
    }
}
```

### Phase 3: Pipe and Redirection Support

```kotlin
class PipelineExecutor(private val interpreter: ShellFsInterpreter) {
    suspend fun execute(pipeline: String): ShellResult {
        val commands = pipeline.split("|").map { it.trim() }
        
        var input: String? = null
        var lastResult = ShellResult(0, "", "")
        
        for (cmd in commands) {
            lastResult = interpreter.execute(cmd, input)
            if (lastResult.exitCode != 0) break
            input = lastResult.stdout
        }
        
        return lastResult
    }
}
```

### Phase 4: Integration with mpp-core ShellExecutor

```kotlin
class XiuperShellExecutor(
    private val interpreter: ShellFsInterpreter
) : ShellExecutor {
    
    override suspend fun execute(
        command: String,
        config: ShellExecutionConfig
    ): cc.unitmesh.agent.tool.shell.ShellResult {
        val shellResult = interpreter.execute(command)
        
        return cc.unitmesh.agent.tool.shell.ShellResult(
            exitCode = shellResult.exitCode,
            stdout = shellResult.stdout,
            stderr = shellResult.stderr,
            command = command,
            workingDirectory = config.workingDirectory
        )
    }
    
    override fun isAvailable(): Boolean = true
    
    override fun getDefaultShell(): String = "xiuper-shell"
}
```

## Usage Examples

### Example 1: Basic File Operations with InMemory Backend

```kotlin
val backend = InMemoryBackend()
val shell = ShellFsInterpreter(backend)

// Create directory
shell.execute("mkdir /projects")

// Write file
shell.execute("echo 'Hello World' > /projects/hello.txt")

// List files
val result = shell.execute("ls /projects")
println(result.stdout)  // hello.txt

// Read file
val content = shell.execute("cat /projects/hello.txt")
println(content.stdout)  // Hello World
```

### Example 2: Multi-Backend Operations

```kotlin
val memBackend = InMemoryBackend()
val dbBackend = DbFsBackend(database)
val shell = ShellFsInterpreter(memBackend)

// Write to memory
shell.execute("echo 'data' > /temp/data.txt")

// Switch to DB backend
shell.switchBackend(dbBackend)

// Copy from memory to DB (cross-backend)
shell.execute("cp mem:/temp/data.txt /persistent/data.txt")

// List DB files
shell.execute("ls /persistent")
```

### Example 3: Integration with Koog Agent

```kotlin
// In your agent tool
class FilesystemTool(private val backend: FsBackend) : Tool {
    private val shell = ShellFsInterpreter(backend)
    
    override suspend fun execute(input: String): String {
        val result = shell.execute(input)
        return if (result.exitCode == 0) {
            result.stdout
        } else {
            "Error: ${result.stderr}"
        }
    }
}

// Agent can now use shell commands
agent.addTool(FilesystemTool(McpBackend(mcpClient)))

// LLM: "List all Python files in the project"
// Tool call: ls /project | grep ".py"
```

### Example 4: MCP Server Control

```kotlin
val mcpClient = mcpClientManager.createClient(config)
val mcpBackend = McpBackend(mcpClient)
val shell = ShellFsInterpreter(mcpBackend)

// List MCP resources
shell.execute("ls /")

// Read MCP resource
shell.execute("cat /resource/name")

// Call MCP tool via special syntax
shell.execute("mcp-tool invoke tool-name '{\"arg\": \"value\"}'")

// Or treat tools as executables
shell.execute("/tools/fetch-url --url https://example.com")
```

## Benefits

1. **Familiar Interface**: Developers can use standard Unix commands
2. **Cross-Platform**: Works on all KMP targets (JVM, JS, iOS, Android, WASM)
3. **Composable**: Pipe and redirect support enables complex workflows
4. **Multi-Backend**: Switch between backends transparently
5. **LLM-Friendly**: Natural language → shell commands is well-understood by LLMs
6. **Testable**: Easy to write integration tests with shell commands

## Testing Strategy

```kotlin
class ShellFsInterpreterTest {
    @Test
    fun `basic file operations`() = runTest {
        val backend = InMemoryBackend()
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
        assertEquals("hello", catResult.stdout.trim())
        
        // Test ls
        val lsResult = shell.execute("ls /test")
        assertEquals(0, lsResult.exitCode)
        assertTrue(lsResult.stdout.contains("file.txt"))
    }
    
    @Test
    fun `pipe support`() = runTest {
        val backend = InMemoryBackend()
        val shell = ShellFsInterpreter(backend)
        
        shell.execute("echo 'line1' > /file1.txt")
        shell.execute("echo 'line2' > /file2.txt")
        
        val result = shell.execute("ls / | grep 'file'")
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("file1.txt"))
        assertTrue(result.stdout.contains("file2.txt"))
    }
}
```

## Future Extensions

1. **Shell Scripting**: Support `.xsh` scripts with variables, loops, conditionals
2. **Permissions**: Implement `chmod`, `chown` for access control
3. **Symlinks**: Support `ln -s` for symbolic links
4. **Compression**: `tar`, `gzip`, `zip` commands
5. **Watch**: `watch` command for monitoring changes
6. **History**: Command history and `!!` support
7. **Aliases**: User-defined command aliases
8. **Autocomplete**: Tab completion for paths and commands

## Implementation Priority

**P0 (MVP):**
- [ ] ShellFsInterpreter core
- [ ] Basic commands: ls, cat, echo, mkdir, rm, cd, pwd
- [ ] Path resolution
- [ ] Error handling

**P1 (Useful):**
- [ ] Pipe support (|)
- [ ] Redirection (>, >>)
- [ ] cp, mv commands
- [ ] find, grep commands
- [ ] Backend switching

**P2 (Advanced):**
- [ ] Shell scripting
- [ ] History and aliases
- [ ] Permissions
- [ ] Tab completion

## Open Questions

1. Should we support full bash syntax or a simplified subset?
2. How to handle async operations (background jobs with &)?
3. Should we integrate with system shell or be completely isolated?
4. How to handle platform-specific path separators (/, \)?
