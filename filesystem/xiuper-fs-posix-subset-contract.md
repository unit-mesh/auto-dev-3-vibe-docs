# xiuper-fs POSIX 子集合同（可测试规范）

> 目标：定义一组“POSIX-like”的最小语义，使 `XiuperFileSystem` 在不同 backend（内存/HTTP/DB/…）和不同 KMP 平台上表现一致，并能用 conformance tests 自动验证。
>
> 非目标：把 xiuper-fs 做成可被 OS `ls/cat/vim` 直接访问的内核级 POSIX 文件系统（那需要 FUSE/内核挂载层，且 JS/WASM/iOS 不可用）。

## 1. 范围（Scope）

本合同覆盖以下 API（以 [xiuper-fs/src/commonMain/kotlin/cc/unitmesh/xiuper/fs/XiuperFileSystem.kt](../../xiuper-fs/src/commonMain/kotlin/cc/unitmesh/xiuper/fs/XiuperFileSystem.kt) 为准）：

- `stat(path)`
- `list(path)`
- `read(path, options)`
- `write(path, bytes, options)`
- `mkdir(path)`
- `delete(path)`
- `commit(path)`（可选一致性钩子）

## 2. 路径语义（Path semantics）

### 2.1 绝对路径

- 所有路径必须为绝对路径，以 `/` 开头。
- 传入非绝对路径应抛出 `FsException(FsErrorCode.EINVAL)`。

### 2.2 归一化

实现应将路径按 POSIX 风格归一化（等价于 `FsPath.of(path.value)` 的结果），至少包含：

- 多个 `/` 视为单个 `/`
- 消除 `.`
- 解析 `..`（不允许越过根目录）

> 注意：本合同只约束语义；具体实现可选择在 API 边界统一归一化（如 `XiuperVfs` 做），或在 backend 内部归一化（推荐）。

### 2.3 根目录

- `/` 永远存在
- `/` 必须是目录

## 3. 目录/文件模型（Directory & file model）

- 目录包含 0..N 个子项，子项名称不包含 `/`。
- 文件是字节序列（byte array）。
- 本合同不包含：符号链接、硬链接、权限位（chmod）、所有者（chown）、rename/atomic move、文件描述符（fd/seek/mmap）。

## 4. 操作语义与错误码（可测试）

> 下面所有错误码均为“必须满足的行为”。如果实现无法区分错误原因（例如远端服务返回模糊错误），可以退化为 `EIO`，但 **不得** 在本合同覆盖的 in-memory/backend 上退化。

### 4.1 `stat(path)`

- 若 `path` 存在：返回 `FsStat(path, isDirectory=...)`
- 若不存在：抛 `ENOENT`

### 4.2 `list(path)`（类似 `readdir`）

- 若 `path` 是目录：返回子项列表
- 若 `path` 不存在：抛 `ENOENT`
- 若 `path` 是文件：抛 `ENOTDIR`

排序规则：
- 本合同 **不要求** 特定排序（backend 可以返回任意顺序）；如果某处需要稳定排序，应由调用方排序。

### 4.3 `read(path)`

- 若 `path` 是文件：返回文件内容
- 若 `path` 不存在：抛 `ENOENT`
- 若 `path` 是目录：抛 `EISDIR`

### 4.4 `write(path, bytes)`（类似 `open(O_CREAT|O_TRUNC) + write + close`）

- 若 `path` 的父目录不存在：抛 `ENOENT`
- 若父路径存在但不是目录：抛 `ENOTDIR`
- 若 `path` 指向目录：抛 `EISDIR`
- 成功：
  - 若文件不存在：创建文件
  - 若文件已存在：以 `bytes` 完整覆盖（truncate + write）

### 4.5 `mkdir(path)`（类似 `mkdir(2)`）

- 若 `path` 已存在：抛 `EEXIST`
- 若父目录不存在：抛 `ENOENT`
- 若父路径存在但不是目录：抛 `ENOTDIR`
- 若 `path == "/"`：抛 `EEXIST`
- 成功：创建空目录

### 4.6 `delete(path)`（类似 `unlink(2)` + `rmdir(2)` 的合并操作）

- 若 `path` 不存在：抛 `ENOENT`
- 若 `path == "/"`：抛 `EACCES`
- 若 `path` 是文件：删除文件
- 若 `path` 是目录：
  - 若目录为空：删除目录
  - 若目录非空：抛 `ENOTEMPTY`

### 4.7 `commit(path)`

- 默认语义：成功返回 `WriteResult(ok=true)`
- 若 backend 支持 staged writes（例如 `WriteCommitMode.OnExplicitCommit`），则 `commit` 是“对外可见”的边界。

## 5. 一致性与原子性（Consistency & atomicity）

- 单次 `write()` 成功返回后，后续 `read()` 必须能读到写入内容（同一 `XiuperFileSystem` 实例内）。
- `write()` 失败不得产生“部分写入可见”的状态。

> 对于远程 REST/DB backend：如果不能保证这点，应通过 `WriteCommitMode` 或事务目录等方式显式暴露一致性边界，并在文档中声明。

## 6. Conformance tests

本合同对应的测试套件位于：

- [xiuper-fs/src/commonTest/kotlin/cc/unitmesh/xiuper/fs/conformance/PosixSubsetConformanceTest.kt](../../xiuper-fs/src/commonTest/kotlin/cc/unitmesh/xiuper/fs/conformance/PosixSubsetConformanceTest.kt)

任何新的 backend（REST/DB/本地）都应至少在 JVM/JS 上通过该套件（或提供标注的已知差异）。
