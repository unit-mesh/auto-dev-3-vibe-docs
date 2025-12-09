# Text2SQL Agent - Database Provider

## Overview

Database Provider is the foundational infrastructure layer for the Text2SQL Agent feature. It provides cross-platform database connectivity, schema introspection, and query execution capabilities.

## Architecture

### Cross-Platform Design

```
commonMain/
  └── cc/unitmesh/agent/database/
      └── DatabaseConnection.kt          # Interface & data models

jvmMain/
  └── cc/unitmesh/agent/database/
      └── ExposedDatabaseConnection.kt   # JetBrains Exposed implementation

Platform-specific placeholders:
  - jsMain/
  - androidMain/
  - iosMain/
  - wasmJsMain/
```

### Core Components

#### 1. DatabaseConnection Interface

```kotlin
interface DatabaseConnection {
    suspend fun executeQuery(sql: String): QueryResult
    suspend fun getSchema(): DatabaseSchema
    suspend fun queryScalar(sql: String): Any?
    suspend fun tableExists(tableName: String): Boolean
    suspend fun getTableRowCount(tableName: String): Long
    suspend fun close()
}
```

#### 2. Data Models

- **QueryResult**: SQL query results with CSV and table formatting
- **DatabaseSchema**: Complete database schema with table/column metadata
- **TableSchema**: Individual table structure
- **ColumnSchema**: Column details (type, nullable, primary/foreign keys, comments)
- **DatabaseConfig**: Connection configuration (host, port, credentials, dialect)
- **DatabaseException**: Typed exceptions for connection and query errors

### JVM Implementation

**Technology Stack:**
- **JetBrains Exposed** 0.47.0: Type-safe SQL DSL
- **HikariCP** 6.0.0: High-performance connection pooling
- **MySQL Connector/J** 9.0.0: MySQL/MariaDB JDBC driver
- **H2** 2.2.224 (test only): In-memory testing database

**Key Features:**
- Direct JDBC connection management using HikariDataSource
- JDBC metadata API for schema introspection
- Coroutine-based async operations with `Dispatchers.IO`
- Automatic connection pool management

## Usage Examples

### Basic Connection

```kotlin
val config = DatabaseConfig(
    host = "localhost",
    port = 3306,
    databaseName = "my_database",
    username = "root",
    password = "password",
    dialect = "MySQL"
)

val connection = createDatabaseConnection(config)
```

### Query Execution

```kotlin
// Execute SQL query
val result = connection.executeQuery("SELECT * FROM users WHERE age > 18")

// Format as CSV
println(result.toCsvString())

// Format as table
println(result.toTableString())

// Get scalar value
val count = connection.queryScalar("SELECT COUNT(*) FROM users")
```

### Schema Introspection

```kotlin
// Get complete database schema
val schema = connection.getSchema()

// Find specific table
val usersTable = schema.getTable("users")

// Get natural language description
println(schema.getDescription())

// Check table existence
if (connection.tableExists("orders")) {
    val rowCount = connection.getTableRowCount("orders")
    println("Orders table has $rowCount rows")
}
```

### Natural Language Schema Description

```kotlin
val schema = connection.getSchema()
val description = schema.getDescription()

// Output example:
// Database: my_database
// 
// Table: users
// Comment: User accounts
// Columns:
//   - id: INT, PRIMARY KEY, NOT NULL
//   - name: VARCHAR, NOT NULL
//   - email: VARCHAR, NULL
//   - created_at: TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP
// 
// Table: orders...
```

## Supported Databases

**Current Support:**
- ✅ MySQL 5.7+
- ✅ MariaDB 10.3+

**Future Support:**
- 🔜 PostgreSQL
- 🔜 SQLite
- 🔜 Oracle
- 🔜 SQL Server

## Testing

### Unit Tests

```bash
# Run all database tests
./gradlew :mpp-core:jvmTest --tests "cc.unitmesh.agent.database.*"

# Run specific test
./gradlew :mpp-core:jvmTest --tests "cc.unitmesh.agent.database.DatabaseConnectionTest"
```

### Integration Tests

Integration tests use H2 in-memory database to test real database operations:

```bash
./gradlew :mpp-core:jvmTest --tests "cc.unitmesh.agent.database.ExposedDatabaseConnectionIntegrationTest"
```

**Test Coverage:**
- ✅ Query execution with conditions
- ✅ Schema retrieval and introspection
- ✅ Table existence checks
- ✅ Empty result handling
- ✅ Result formatting (CSV, table)
- ✅ Invalid SQL error handling
- ✅ Join queries
- ⏸️ Row count operations (known H2 timing issue)

## Configuration

### Connection Pooling (HikariCP)

Default settings:
- Maximum pool size: 10
- Minimum idle connections: 2
- Connection timeout: 30 seconds
- Idle timeout: 10 minutes
- Max lifetime: 30 minutes

### Additional Parameters

```kotlin
val config = DatabaseConfig(
    host = "localhost",
    port = 3306,
    databaseName = "test_db",
    username = "root",
    password = "password",
    dialect = "MySQL",
    additionalParams = mapOf(
        "useSSL" to "true",
        "serverTimezone" to "UTC"
    )
)
```

## Error Handling

```kotlin
try {
    val result = connection.executeQuery("SELECT * FROM users")
} catch (e: DatabaseException) {
    when {
        e.message?.contains("connection failed") == true -> {
            // Handle connection errors
        }
        e.message?.contains("Query failed") == true -> {
            // Handle query errors
        }
    }
}
```

## Platform Limitations

### JS/Browser/Node.js
Not supported. Use server-side API instead:
```kotlin
// Use HTTP client to call backend Text2SQL API
val response = httpClient.post("/api/text2sql") {
    // ...
}
```

### Android
Not supported directly. Options:
1. Use Room database for local SQLite
2. Use server-side API for remote databases

### iOS
Not supported directly. Options:
1. Use Swift + SQLite.swift for local databases
2. Use server-side API for remote databases

### WASM
Not supported. Use server-side API.

## Next Steps (Stage 2)

1. **Schema Linking**: Map natural language to database schema elements
2. **Reasoning Enhancement**: LLM-based intent understanding
3. **SQL Generation**: Template-based or LLM-powered SQL generation
4. **Query Validation**: Syntax and semantic validation
5. **Result Interpretation**: Natural language response generation

## Dependencies

### Production

```kotlin
// JetBrains Exposed
implementation("org.jetbrains.exposed:exposed-core:0.47.0")
implementation("org.jetbrains.exposed:exposed-dao:0.47.0")
implementation("org.jetbrains.exposed:exposed-jdbc:0.47.0")

// MySQL Driver
implementation("com.mysql:mysql-connector-j:9.0.0")

// Connection Pooling
implementation("com.zaxxer:HikariCP:6.0.0")
```

### Testing

```kotlin
// H2 Database
testImplementation("com.h2database:h2:2.2.224")
```

## Security Considerations

1. **SQL Injection Prevention**:
   - Currently using string concatenation for table names
   - TODO: Implement prepared statement for dynamic table names

2. **Connection Security**:
   - Supports SSL/TLS via additional parameters
   - Password encryption at rest recommended

3. **Access Control**:
   - Database user should have minimum required privileges
   - READ-ONLY access recommended for Text2SQL operations

## Performance

- **Connection Pooling**: Reuses connections efficiently
- **Async Operations**: Non-blocking coroutine-based API
- **Schema Caching**: TODO - implement schema cache to avoid repeated metadata queries

## Contributing

When adding new database support:

1. Add JDBC driver dependency to `build.gradle.kts`
2. Implement dialect-specific SQL generation in `DatabaseConfig.getJdbcUrl()`
3. Test with integration tests
4. Update documentation

## License

[Project License]

## References

- [JetBrains Exposed Documentation](https://github.com/JetBrains/Exposed)
- [HikariCP GitHub](https://github.com/brettwooldridge/HikariCP)
- [MySQL Connector/J Documentation](https://dev.mysql.com/doc/connector-j/en/)
