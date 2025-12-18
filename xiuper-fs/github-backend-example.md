# GitHub API as Filesystem - Complete Example

## Overview

This example demonstrates using GitHub API as a filesystem backend, allowing you to browse and read GitHub repositories using familiar shell commands.

## Architecture

```
┌─────────────────────────────────────┐
│    Shell Commands (ls, cat, cd)    │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│     ShellFsInterpreter              │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│     GitHubFsBackend                 │
│  - GitHub REST API client           │
│  - Path parsing: /owner/repo/ref/   │
│  - Read-only operations             │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│        GitHub API                   │
│  api.github.com/repos/...           │
└─────────────────────────────────────┘
```

## Path Format

GitHub repository structure is mapped to filesystem paths:

```
/owner/repo/ref/path/to/file

Examples:
/microsoft/vscode/main                    → Root of main branch
/microsoft/vscode/main/README.md          → README file
/microsoft/vscode/main/src/vs/base        → Subdirectory
/kubernetes/kubernetes/master/pkg         → Different repo
```

## Quick Start

### Basic Usage

```kotlin
import cc.unitmesh.xiuper.fs.github.GitHubFsBackend
import cc.unitmesh.xiuper.fs.shell.ShellFsInterpreter

fun main() = runBlocking {
    // Create GitHub backend (public repos, no token needed)
    val backend = GitHubFsBackend()
    val shell = ShellFsInterpreter(backend)
    
    // List repository root
    shell.execute("ls /microsoft/vscode/main")
    
    // Read README
    val readme = shell.execute("cat /microsoft/vscode/main/README.md")
    println(readme.stdout)
    
    // Navigate directories
    shell.execute("cd /microsoft/vscode/main/src")
    shell.execute("ls .")
}
```

### With Authentication Token

For higher rate limits and private repositories:

```kotlin
val token = System.getenv("GITHUB_TOKEN")  // ghp_xxxxx
val backend = GitHubFsBackend(token = token)
val shell = ShellFsInterpreter(backend)

// Access private repositories
shell.execute("ls /myorg/private-repo/main")
```

## Complete Examples

### Example 1: Exploring VS Code Repository

```kotlin
suspend fun exploreVSCode() {
    val backend = GitHubFsBackend()
    val shell = ShellFsInterpreter(backend)
    
    println("=== Exploring microsoft/vscode ===")
    
    // Navigate to repository
    shell.execute("cd /microsoft/vscode/main")
    
    // List root files
    val rootFiles = shell.execute("ls .")
    println("Root files:")
    println(rootFiles.stdout)
    
    // Read package.json
    val packageJson = shell.execute("cat package.json")
    println("\nPackage.json:")
    println(packageJson.stdout)
    
    // Explore src directory
    val srcContents = shell.execute("ls src")
    println("\nSource directory:")
    println(srcContents.stdout)
    
    // Read TypeScript config
    val tsConfig = shell.execute("cat tsconfig.json")
    println("\nTypeScript Config:")
    println(tsConfig.stdout)
}
```

### Example 2: Code Search Across Repositories

```kotlin
suspend fun searchAcrossRepos(pattern: String) {
    val backend = GitHubFsBackend()
    val shell = ShellFsInterpreter(backend)
    
    val repos = listOf(
        "/microsoft/vscode/main",
        "/facebook/react/main",
        "/vuejs/core/main"
    )
    
    for (repo in repos) {
        println("\n=== Searching in $repo ===")
        
        // List files in root
        val files = shell.execute("ls $repo")
        val fileList = files.stdout.split("\n")
        
        // Read each file and search for pattern
        for (file in fileList) {
            if (file.endsWith(".md") || file.endsWith(".json")) {
                val content = shell.execute("cat $repo/$file")
                if (content.stdout.contains(pattern, ignoreCase = true)) {
                    println("Found in $file")
                }
            }
        }
    }
}

// Usage
searchAcrossRepos("TypeScript")
```

### Example 3: Documentation Generator

```kotlin
suspend fun generateDocsIndex() {
    val backend = GitHubFsBackend()
    val shell = ShellFsInterpreter(backend)
    
    // Navigate to docs directory
    shell.execute("cd /microsoft/vscode/main/docs")
    
    // List all markdown files
    val files = shell.execute("ls .")
    val mdFiles = files.stdout.split("\n").filter { it.endsWith(".md") }
    
    println("# Documentation Index\n")
    
    for (file in mdFiles) {
        // Read first line (title) from each file
        val content = shell.execute("cat $file")
        val firstLine = content.stdout.lines().firstOrNull { it.startsWith("#") }
        
        println("- [$firstLine]($file)")
    }
}
```

### Example 4: Compare Files Across Branches

```kotlin
suspend fun compareAcrossBranches(repo: String, filePath: String) {
    val backend = GitHubFsBackend()
    val shell = ShellFsInterpreter(backend)
    
    // Read from main branch
    val mainContent = shell.execute("cat /$repo/main/$filePath")
    
    // Read from develop branch
    val devContent = shell.execute("cat /$repo/develop/$filePath")
    
    println("=== Main Branch ===")
    println(mainContent.stdout)
    
    println("\n=== Develop Branch ===")
    println(devContent.stdout)
    
    // Simple diff
    val mainLines = mainContent.stdout.lines()
    val devLines = devContent.stdout.lines()
    
    if (mainLines != devLines) {
        println("\n=== Differences Found ===")
        println("Main: ${mainLines.size} lines")
        println("Dev: ${devLines.size} lines")
    }
}
```

### Example 5: Integration with LLM Agent

```kotlin
class GitHubRepositoryTool(private val token: String? = null) : Tool {
    override val name = "github-repo"
    override val description = "Browse GitHub repositories using shell commands"
    
    private val backend = GitHubFsBackend(token)
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
agent.addTool(GitHubRepositoryTool(token = System.getenv("GITHUB_TOKEN")))

// LLM can now use commands like:
// "List files in microsoft/vscode" → tool: ls /microsoft/vscode/main
// "Read the React README" → tool: cat /facebook/react/main/README.md
// "Show me TypeScript files in src" → tool: ls /microsoft/typescript/main/src
```

### Example 6: Repository Analysis

```kotlin
data class RepoStats(
    val totalFiles: Int,
    val markdownFiles: Int,
    val codeFiles: Int,
    val configFiles: Int
)

suspend fun analyzeRepository(owner: String, repo: String, ref: String = "main"): RepoStats {
    val backend = GitHubFsBackend()
    val shell = ShellFsInterpreter(backend)
    
    var totalFiles = 0
    var markdownFiles = 0
    var codeFiles = 0
    var configFiles = 0
    
    suspend fun analyzeDir(path: String) {
        val result = shell.execute("ls $path")
        val entries = result.stdout.split("\n").filter { it.isNotBlank() }
        
        for (entry in entries) {
            totalFiles++
            
            when {
                entry.endsWith(".md") -> markdownFiles++
                entry.endsWith(".ts") || entry.endsWith(".js") || 
                entry.endsWith(".kt") || entry.endsWith(".java") -> codeFiles++
                entry.endsWith(".json") || entry.endsWith(".yml") ||
                entry.endsWith(".yaml") || entry.endsWith(".toml") -> configFiles++
            }
        }
    }
    
    analyzeDir("/$owner/$repo/$ref")
    
    return RepoStats(totalFiles, markdownFiles, codeFiles, configFiles)
}

// Usage
val stats = analyzeRepository("microsoft", "vscode")
println("""
    Repository Statistics:
    - Total files: ${stats.totalFiles}
    - Markdown: ${stats.markdownFiles}
    - Code files: ${stats.codeFiles}
    - Config files: ${stats.configFiles}
""".trimIndent())
```

### Example 7: Multi-Repository Dashboard

```kotlin
suspend fun buildDashboard() {
    val backend = GitHubFsBackend()
    val shell = ShellFsInterpreter(backend)
    
    val repos = listOf(
        "microsoft/vscode" to "main",
        "facebook/react" to "main",
        "vuejs/core" to "main",
        "angular/angular" to "main"
    )
    
    println("# GitHub Repositories Dashboard\n")
    
    for ((repo, ref) in repos) {
        println("## $repo")
        
        // Check if README exists
        val readme = shell.execute("cat /$repo/$ref/README.md")
        if (readme.exitCode == 0) {
            val firstParagraph = readme.stdout.lines()
                .dropWhile { it.startsWith("#") }
                .takeWhile { it.isNotBlank() }
                .joinToString(" ")
            println("📝 $firstParagraph")
        }
        
        // Check package.json
        val packageJson = shell.execute("cat /$repo/$ref/package.json")
        if (packageJson.exitCode == 0) {
            println("📦 Has package.json")
        }
        
        // List root structure
        val files = shell.execute("ls /$repo/$ref")
        val fileCount = files.stdout.split("\n").size
        println("📁 $fileCount items in root")
        
        println()
    }
}
```

## Advanced Usage

### Custom GitHub Enterprise

```kotlin
val backend = GitHubFsBackend(
    token = "your-token",
    baseUrl = "https://github.company.com/api/v3"
)
```

### Error Handling

```kotlin
suspend fun safeRead(path: String): String? {
    val backend = GitHubFsBackend()
    val shell = ShellFsInterpreter(backend)
    
    val result = shell.execute("cat $path")
    
    return when (result.exitCode) {
        0 -> result.stdout
        1 -> {
            logger.error("Failed to read $path: ${result.stderr}")
            null
        }
        else -> null
    }
}
```

### Rate Limiting Considerations

```kotlin
class RateLimitedGitHubBackend(token: String?) : GitHubFsBackend(token) {
    private var lastRequestTime = 0L
    private val minDelayMs = 100L  // GitHub rate limit: 5000 req/hour
    
    override suspend fun read(path: FsPath, options: ReadOptions): ReadResult {
        // Throttle requests
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < minDelayMs) {
            delay(minDelayMs - elapsed)
        }
        
        lastRequestTime = System.currentTimeMillis()
        return super.read(path, options)
    }
}
```

## Limitations

### Read-Only Operations

The GitHub backend is **read-only**. Write operations will fail:

```kotlin
// ❌ These will fail
shell.execute("echo 'test' > /owner/repo/main/file.txt")
shell.execute("mkdir /owner/repo/main/newdir")
shell.execute("rm /owner/repo/main/file.txt")
```

To implement write operations, you would need to use the GitHub Contents API with commits.

### Path Format Requirements

Paths must follow the format: `/owner/repo/ref/path`

```kotlin
// ✅ Valid
"/microsoft/vscode/main"
"/microsoft/vscode/main/src/vs"
"/facebook/react/main/packages/react"

// ❌ Invalid
"/vscode"  // Missing owner
"/microsoft/vscode"  // Missing ref (defaults to "main")
```

## Performance Tips

### 1. Cache Repository Structure

```kotlin
class CachedGitHubBackend(token: String?) : GitHubFsBackend(token) {
    private val listCache = mutableMapOf<String, List<FsEntry>>()
    
    override suspend fun list(path: FsPath): List<FsEntry> {
        return listCache.getOrPut(path.value) {
            super.list(path)
        }
    }
}
```

### 2. Batch Operations

```kotlin
// Read multiple files efficiently
suspend fun readMultipleFiles(paths: List<String>) {
    val backend = GitHubFsBackend()
    val shell = ShellFsInterpreter(backend)
    
    // Launch concurrent reads
    val results = paths.map { path ->
        async { shell.execute("cat $path") }
    }.awaitAll()
    
    results.forEach { result ->
        println(result.stdout)
    }
}
```

## Testing

Run the tests:

```bash
./gradlew :xiuper-fs:jvmTest --tests "cc.unitmesh.xiuper.fs.github.GitHubFsBackendTest"
```

Note: Tests use public GitHub API without authentication, so they may be rate-limited.

## Real-World Use Cases

1. **Documentation Browser**: Navigate and read docs from multiple repos
2. **Code Search**: Search for patterns across repositories
3. **Dependency Analysis**: Read package.json/build files from multiple projects
4. **LLM Training Data**: Extract code examples from open source
5. **Repository Comparison**: Compare files across branches or repos
6. **Automated Audits**: Check for security files (LICENSE, SECURITY.md)

## Next Steps

1. **Implement write operations** using GitHub Contents API
2. **Add caching** for better performance
3. **Support GraphQL API** for more efficient queries
4. **Add search functionality** using GitHub Code Search API
5. **Implement git operations** (clone, pull, diff)

## See Also

- [GitHub REST API Documentation](https://docs.github.com/en/rest)
- [Shell Interface Design](shell-interface-design.md)
- [Shell Usage Examples](shell-usage-examples.md)
