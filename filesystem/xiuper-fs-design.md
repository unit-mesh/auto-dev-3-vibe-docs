# xiuper-fs: Agent-VFS-style Virtual File System for Xiuper (KMP)

## Background (from auto-dev#519 + attached report)
Issue #519 proposes **Agent-VFS**: exposing heterogeneous internet services (HTTP APIs / DB / cloud tools) as a **POSIX-like file tree**. The attached report (default.md) defines several interaction patterns:

- **REST-FS**
  - Path ↔ resource hierarchy mapping (`/github/{owner}/{repo}/issues/{id}/...`).
  - **Field projection** as files (e.g. `.../issues/{id}/fields/title`).
  - **Raw JSON view** (`data.json`) and derived views (e.g. `summary.md`).
  - **Magic file** for create: write JSON to `new`, POST on close.
  - **Control file** for query: write params to `query`, read `results/`.
  - **Pagination as infinite dirs** (`page_1`, `page_2`, `next`).

- **DB-FS**
  - DB/schema/table as directories; rows as `{pk}.json`.
  - **Transaction directory** + `commit` trigger file for multi-row atomic updates.
  - `exec` file for running SQL and `result.json/csv` for results.

- **MCP ↔ VFS bridge**
  - MCP `resources/list` ↔ directory listing.
  - MCP `resources/read` ↔ file read.
  - MCP tools ↔ special executable/control files (`/tools/{tool}/args`, `/tools/{tool}/run`).

- **Consistency & safety**
  - Avoid “write succeeded but remote failed”: prefer **direct I/O** semantics or **explicit fsync**.
  - Tokens/credentials must not leak into mounted file tree.
  - Isolation (namespaces/RO mounts/approval flow) is required for dangerous operations.

## Goal
Design a **Kotlin Multiplatform** module `xiuper-fs` usable by Compose apps (Desktop JVM/Android/iOS/JS/WASM) that provides an **in-process virtual filesystem** abstraction inspired by Agent-VFS.

This is *not* a kernel-level FUSE filesystem; it is a **library VFS** that:
- exposes a file-tree API to Xiuper UI and Agents,
- maps file operations to HTTP APIs (first milestone), and
- keeps room for DB-FS and MCP mounts later.

## Non-goals (for this module)
- Implementing OS-level mount points (FUSE) on macOS/Linux/Windows.
- A full universal REST schema discovery engine.
- Shipping a new UI/Explorer by default (xiuper-fs is non-UI; Compose integration is via adapters).

## Platforms
KMP targets:
- JVM Desktop
- Android
- iOS (Darwin)
- JS (Node + browser-safe subset)
- WASM (wasmJs)

Constraints:
- WASM: avoid assuming Node APIs; avoid UTF-8/emoji in code identifiers.
- JS: local disk access only in Node; browser has no fs.

## Module layout
Suggested Gradle module: `xiuper-fs/`

Source sets:
- `commonMain`: core VFS types + routing + REST-FS logic + Ktor-based HTTP client abstraction.
- `jvmMain`, `androidMain`, `iosMain`, `jsMain`, `wasmJsMain`: Ktor engine bindings (`expect/actual`).
- `commonTest`: pure logic tests (path normalization, mount routing, control-file parsing).

## Public API (minimal, POSIX-inspired)
### Core types
- `FsPath`: normalized POSIX path (`/a/b`, no `..` after normalize).
- `FsEntry`:
  - `File(name, size, mime?, modifiedAt?)`
  - `Directory(name)`
  - `Special(name, kind)` where `kind ∈ {MagicNew, ControlQuery, ControlCommit, ToolRun, ...}`

### Operations
`XiuperFileSystem` is asynchronous and side-effectful:

- `suspend fun stat(path: FsPath): FsStat`
- `suspend fun list(path: FsPath): List<FsEntry>`
- `suspend fun read(path: FsPath, options: ReadOptions = ReadOptions()): ReadResult`
- `suspend fun write(path: FsPath, content: ByteArray, options: WriteOptions = WriteOptions()): WriteResult`
- `suspend fun delete(path: FsPath, options: DeleteOptions = DeleteOptions()): Unit`
- `suspend fun mkdir(path: FsPath): Unit`

Consistency knobs:
- `WriteOptions.commit`: `OnClose` | `OnFsync` | `Direct` (maps to “direct I/O” vs “write-back + fsync”).

Error model:
- Prefer typed failures (`FsException(code, message)`) where `code` resembles errno: `ENOENT`, `EACCES`, `EINVAL`, `EIO`, `ENOTSUP`.

## Architecture
### 1) VFS router + mounts
`XiuperVfs` routes a path to a mount backend:

- Mount points are directories at root: `/local`, `/http`, `/mcp`, `/db`, etc.
- `MountTable` performs **longest-prefix** match.

Interfaces:
- `FsBackend` (SPI)
  - responsible for `stat/list/read/write/delete/mkdir` inside its mount.
- `XiuperVfs`
  - validates/normalizes paths
  - applies cross-cutting policies (RO mounts, delete approvals, audit logging)

### 2) REST-FS backend (phase 1)
The REST backend turns a *declared schema* into a navigable tree.

Why schema-first: fully automatic discovery is brittle; schema configuration is small and cacheable.

#### Schema model
- `RestServiceConfig`
  - `baseUrl`
  - `auth: AuthProvider` (inject headers)
  - optional `defaultHeaders`
- `RestSchema`
  - `collections`: list endpoints that can be listed/paged
  - `resources`: how to fetch a resource by id
  - `fields`: how to project JSON fields into `/fields/{name}`
  - `create`: how to POST new resources from `new`
  - `query`: how to translate control file contents into requests

#### Standard virtual files/dirs
For any resource directory `/.../{id}/`:
- `data.json` → GET resource JSON (raw)
- `fields/` → virtual dir
- `fields/{key}` → reads value; writes patch (PATCH)

For any collection dir `/.../issues/`:
- `page_1/`, `page_2/` … (lazy)
- `new` (magic)
- optionally `query` + `results/` (control)

#### Commit semantics
- Writes are buffered by `WriteOptions.commit`:
  - `Direct`: send HTTP request per write (rare; mostly for small control files).
  - `OnClose`: accumulate payload and send on write completion.
  - `OnFsync`: require explicit `fsync` equivalent; in library VFS we expose `write(..., commit=OnFsync)` plus `commit(path)` API.

### 3) DB-FS backend (phase 2)
Pluggable backend, not required for initial shipping.

Support:
- `/db/{db}/{schema}/{table}/{pk}.json`
- `transactions/{txId}/...` staging area + `commit` file trigger.

Implementation note:
- Prefer server-side DB gateway (HTTP) rather than embedding DB drivers across KMP.

### 4) MCP backend (phase 3)
Provide a `McpBackend` with a transport abstraction:
- JVM/Android: stdio process transport (like `npx @modelcontextprotocol/...`).
- JS Node: child_process transport.
- iOS/WASM/browser: **HTTP transport only** (if an MCP gateway is provided).

The backend maps:
- `resources/list` ↔ list
- `resources/read` ↔ read
- Tools ↔ `tools/{name}/args` + `tools/{name}/run`

## HTTP stack (Ktor)
Implementation follows the pattern already used in `mpp-core`:
- Provide `expect object HttpClientFactory { fun create(): HttpClient }`
- Actual engines:
  - JVM/Android: `CIO`
  - iOS: `Darwin`
  - JS/WASM: `Js`

Client configuration:
- `expectSuccess = false` (map non-2xx to `FsException(EIO)` with details)
- `ContentNegotiation { json(...) }`
- optional logging controlled by build flags.

## Security & policy
- `AuthProvider` supplies headers; tokens never appear in path.
- `MountPolicy`:
  - read-only mounts
  - delete requires approval (policy hook)
  - path allowlist/denylist
- Audit events emitted for every operation: `FsAuditEvent(op, path, backend, status, latencyMs)`.

## Compose integration (non-UI adapters)
`xiuper-fs` itself ships no UI, but provides:
- `FsRepository` helpers producing `StateFlow` snapshots (directory listing, file content)
- stable data models that Compose can display

Example usage (pseudo):
- list: `val entries by fsRepo.observeDir("/http/github/.../issues/")`
- read: `val text by fsRepo.observeText("/http/github/.../body")`

## Deliverables
- `xiuper-fs` module with:
  - ✅ core types + mount router
  - ✅ REST-FS backend skeleton + schema model
  - ✅ DB-FS backend with SQLDelight + migration framework (v1→v2 xattr support)
  - ✅ Ktor http client expect/actual
  - ✅ Policy & audit system (MountPolicy, FsAuditEvent, FsAuditCollector)
  - ✅ Compose integration adapter (FsRepository for StateFlow-based reactive access)
  - ✅ unit tests for routing + schema parsing + migration + conformance
- Documentation: this file

## Implementation Status (Dec 2024)

### Completed ✅
1. **Core VFS Infrastructure**
   - `XiuperFileSystem` interface with POSIX-inspired operations
   - `FsBackend` SPI for pluggable backends
   - `XiuperVfs` router with mount point resolution
   - `Mount` with read-only flag and policy hooks

2. **Backends**
   - `RestFsBackend`: HTTP-based virtual filesystem with schema support
   - `DbFsBackend`: SQLDelight-backed database filesystem
   - `InMemoryFsBackend`: In-memory testing backend
   - Platform-specific drivers: JVM/Android/iOS (SQLite), WASM (sql.js), JS/Node (explicit unsupported)

3. **Migration Framework**
   - `Migration` interface for database schema upgrades
   - `MigrationRegistry` with path discovery
   - PRAGMA user_version tracking
   - v1→v2 migration with FsXattr table for extended attributes
   - Comprehensive upgrade tests

4. **Security & Policy**
   - `MountPolicy` for access control (AllowAll, ReadOnly presets)
   - `FsAuditEvent` + `FsAuditCollector` for operation tracking
   - Integrated audit logs in XiuperVfs
   - Read-only mount enforcement

5. **Compose Integration**
   - `FsRepository` adapter for reactive filesystem access
   - StateFlow-based `observeDir`/`observeFile`/`observeText` methods
   - Automatic refresh on write/delete operations
   - Cache management and invalidation

6. **Testing**
   - Capability-aware conformance tests (POSIX subset)
   - Migration infrastructure tests (8/8 passing)
   - REST backend conformance validation
   - Cross-platform test coverage (JVM, common)

### Pending / Future Work 🚧
1. **REST-FS Advanced Features**
   - Field projection (`/fields/{name}`)
   - Magic files (`new` for create)
   - Control files (`query` + `results/`)
   - Pagination as infinite directories

2. **MCP Backend (Phase 3)**
   - MCP resources/list ↔ directory listing
   - MCP resources/read ↔ file read
   - MCP tools ↔ executable files
   - Multi-platform transport (stdio on JVM/Android, HTTP on WASM/browser)

3. **Advanced Policies**
   - Path allowlist/denylist (`PathFilterPolicy`)
   - Delete approval workflow (`DeleteApprovalPolicy`)
   - Rate limiting hooks

4. **Monitoring & Observability**
   - Persistent audit log storage
   - Metrics collection (latency histograms, error rates)
   - Integration with analytics services

5. **Documentation**
   - API documentation (KDoc)
   - Migration guide for backend implementers
   - REST schema examples
   - Compose UI integration patterns

