# Ktor Content Negotiation Dependency Fix

## Issue

`NoClassDefFoundError: io/ktor/client/plugins/contentnegotiation/ContentNegotiationKt`

### Error Stack Trace

```
java.lang.NoClassDefFoundError: io/ktor/client/plugins/contentnegotiation/ContentNegotiationKt
	at ai.koog.prompt.executor.clients.openai.base.AbstractOpenAILLMClient.httpClient$lambda$4(AbstractOpenAILLMClient.kt:133)
	at io.ktor.client.HttpClient.config(HttpClient.kt:1440)
	at ai.koog.http.client.ktor.KtorKoogHttpClient.<init>(KtorKoogHttpClient.kt:64)
	...
```

## Root Cause

The error occurred because `ktor-client-content-negotiation` was marked as `compileOnly` in the root project dependencies, meaning it was only available at compile time but not packaged into the plugin at runtime. However, `AbstractOpenAILLMClient` from the `ai.koog:prompt-executor-llms-all` library requires this dependency at runtime to configure HTTP client content negotiation.

## Solution

Changed the Ktor client dependencies from `compileOnly` to `implementation` with proper exclusions of kotlinx dependencies to avoid conflicts with IntelliJ IDEA's bundled libraries.

### Changes Made

1. **Root Project Dependencies** (`mpp-idea/build.gradle.kts`, lines 316-366):
   - Changed `ktor-client-core` from `compileOnly` to `implementation`
   - Changed `ktor-client-cio` from `compileOnly` to `implementation`
   - Changed `ktor-client-content-negotiation` from `compileOnly` to `implementation`
   - Added comprehensive exclusions for all kotlinx dependencies including:
     - `kotlinx-coroutines-core`
     - `kotlinx-serialization-json`
     - `kotlinx-serialization-json-io` (critical for passing dependency conflict check)
     - `kotlinx-serialization-core`
     - `kotlinx-io-core`

2. **mpp-core Dependency Exclusions** (multiple projects):
   - Removed exclusions of `ktor-client-content-negotiation` from mpp-core dependencies
   - Kept exclusions only for `ktor-serialization-kotlinx-json` (provided explicitly by root project)
   - Updated exclusions in:
     - Root project
     - `:mpp-idea-core`
     - `:mpp-idea-exts:ext-database`
     - `:mpp-idea-exts:ext-git`
     - `:mpp-idea-exts:devins-lang`

## Verification

Build completed successfully with:
```bash
cd mpp-idea && ../gradlew buildPlugin --no-configuration-cache
```

Result: `✓ No conflicting dependencies found in runtime classpath`

## Technical Details

### Why This Works

1. **Runtime Availability**: By changing to `implementation`, the Ktor content negotiation classes are now included in the plugin distribution and available at runtime.

2. **Conflict Avoidance**: All kotlinx dependencies (coroutines, serialization, io) are excluded from Ktor dependencies to ensure we use IntelliJ IDEA's bundled versions, preventing version conflicts and ClassLoader issues.

3. **Transitive Dependencies**: The fix ensures that transitive dependencies from Ktor don't bring in conflicting versions of kotlinx libraries.

### Dependency Structure

```
mpp-idea plugin
├── ktor-client-core (implementation, with kotlinx exclusions)
├── ktor-client-cio (implementation, with kotlinx exclusions)
├── ktor-client-content-negotiation (implementation, with kotlinx exclusions) ← Fixed
├── ktor-serialization-kotlinx-json (implementation, with kotlinx exclusions)
└── mpp-core (implementation)
    └── ai.koog:prompt-executor-llms-all
        └── AbstractOpenAILLMClient (requires content-negotiation at runtime)
```

## Related Files

- `mpp-idea/build.gradle.kts` - Main configuration file with dependency declarations
- `AbstractOpenAILLMClient.kt` - Class that requires ContentNegotiation at runtime
- `KtorKoogHttpClient.kt` - HTTP client configuration

## Testing

After applying this fix, the plugin should:
1. ✅ Compile without errors
2. ✅ Pass dependency conflict verification
3. ✅ Load and initialize LLM clients successfully at runtime
4. ✅ Make HTTP requests to OpenAI-compatible endpoints

## Date

December 8, 2025

