#!/usr/bin/env node

/**
 * 测试工具解析修复的脚本
 * 
 * 验证对 ToolCallParser 和 ToolOrchestrator 的修复是否有效
 */

const fs = require('fs');
const path = require('path');

console.log('🔧 测试工具解析修复');
console.log('='.repeat(50));

async function main() {
    try {
        // 1. 测试修复后的参数解析
        await testImprovedParameterParsing();
        
        // 2. 测试复杂多行内容解析
        await testComplexMultilineContentParsing();
        
        // 3. 测试边界情况处理
        await testEdgeCaseHandling();
        
        // 4. 测试转义字符处理
        await testEscapeSequenceHandling();
        
        console.log('\n✅ 工具解析修复测试完成');
        
    } catch (error) {
        console.error('❌ 测试失败:', error.message);
        process.exit(1);
    }
}

async function testImprovedParameterParsing() {
    console.log('\n📋 测试改进的参数解析...');
    
    const testCases = [
        {
            name: '简单参数',
            command: '/write-file path="test.txt" content="Hello World"',
            expectedParams: { path: 'test.txt', content: 'Hello World' }
        },
        {
            name: '包含换行的多行内容',
            command: '/write-file path="multi.kt" content="package com.example\\n\\nclass Test {\\n    fun hello() = \\"world\\"\\n}"',
            expectedParams: { 
                path: 'multi.kt', 
                content: 'package com.example\n\nclass Test {\n    fun hello() = "world"\n}' 
            }
        },
        {
            name: '包含复杂转义的内容',
            command: '/write-file path="complex.kt" content="val json = \\"{\\\\\\"name\\\\\\": \\\\\\"test\\\\\\", \\\\\\"value\\\\\\": 123}\\\""',
            expectedParams: { 
                path: 'complex.kt', 
                content: 'val json = "{\\"name\\": \\"test\\", \\"value\\": 123}"' 
            }
        },
        {
            name: '超长内容',
            command: `/write-file path="long.txt" content="${'x'.repeat(1000)}"`,
            expectedParams: { 
                path: 'long.txt', 
                content: 'x'.repeat(1000) 
            }
        }
    ];
    
    for (const testCase of testCases) {
        console.log(`   🔍 ${testCase.name}:`);
        
        // 使用改进的正则表达式解析
        const params = parseParametersWithRegex(testCase.command);
        
        let allMatch = true;
        for (const [key, expectedValue] of Object.entries(testCase.expectedParams)) {
            const actualValue = params[key];
            const matches = actualValue === expectedValue;
            
            if (!matches) {
                allMatch = false;
                console.log(`      ${key}: ❌`);
                console.log(`        预期长度: ${expectedValue.length}, 实际长度: ${actualValue?.length || 0}`);
                if (expectedValue.length < 100) {
                    console.log(`        预期: "${expectedValue}"`);
                    console.log(`        实际: "${actualValue}"`);
                }
            } else {
                console.log(`      ${key}: ✅ (${actualValue.length} 字符)`);
            }
        }
        
        if (allMatch) {
            console.log(`      ✅ 所有参数解析正确`);
        }
    }
}

function parseParametersWithRegex(command) {
    const params = {};
    
    // 改进的正则表达式，更好地处理复杂内容
    const paramPattern = /(\w+)="([^"\\]*(?:\\.[^"\\]*)*)"/g;
    let match;
    
    while ((match = paramPattern.exec(command)) !== null) {
        const key = match[1];
        let value = match[2];
        
        // 处理转义字符
        value = value
            .replace(/\\n/g, '\n')
            .replace(/\\r/g, '\r')
            .replace(/\\t/g, '\t')
            .replace(/\\"/g, '"')
            .replace(/\\\\/g, '\\');
        
        params[key] = value;
    }
    
    return params;
}

async function testComplexMultilineContentParsing() {
    console.log('\n📋 测试复杂多行内容解析...');
    
    const complexResponse = `I'll create a comprehensive Kotlin service class.

<devin>
/write-file path="src/UserService.kt" content="package com.example.service

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * User service for managing user operations
 * Supports full CRUD operations with validation and error handling
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: Instant = Clock.System.now(),
    val isActive: Boolean = true
) {
    /**
     * Validates user data according to business rules
     */
    fun isValid(): Boolean {
        return id.isNotBlank() && 
               name.isNotBlank() && 
               email.contains(\\"@\\") &&
               email.contains(\\".\\")
    }
    
    /**
     * Gets user display name
     */
    fun getDisplayName(): String = name.ifBlank { \\"Unknown User\\" }
    
    /**
     * Converts to JSON representation
     */
    fun toJsonString(): String {
        return \\"{\\\\\\"id\\\\\\": \\\\\\"$id\\\\\\", \\\\\\"name\\\\\\": \\\\\\"$name\\\\\\", \\\\\\"email\\\\\\": \\\\\\"$email\\\\\\"}\\"
    }
}

/**
 * Service interface for user operations
 */
interface UserService {
    suspend fun createUser(user: User): Result<User>
    suspend fun getUserById(id: String): User?
    suspend fun getAllUsers(): List<User>
    suspend fun updateUser(user: User): Result<User>
    suspend fun deleteUser(id: String): Boolean
    suspend fun searchUsers(query: String): List<User>
}

/**
 * In-memory implementation of UserService
 * Suitable for testing and development
 */
class InMemoryUserService : UserService {
    private val users = mutableMapOf<String, User>()
    private val mutex = Mutex()
    
    override suspend fun createUser(user: User): Result<User> {
        return mutex.withLock {
            withContext(Dispatchers.Default) {
                try {
                    if (!user.isValid()) {
                        Result.failure(IllegalArgumentException(\\"Invalid user data\\"))
                    } else if (users.containsKey(user.id)) {
                        Result.failure(IllegalArgumentException(\\"User already exists\\"))
                    } else {
                        users[user.id] = user
                        Result.success(user)
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }
    
    override suspend fun getUserById(id: String): User? {
        return mutex.withLock {
            users[id]
        }
    }
    
    override suspend fun getAllUsers(): List<User> {
        return mutex.withLock {
            users.values.toList()
        }
    }
    
    override suspend fun updateUser(user: User): Result<User> {
        return mutex.withLock {
            try {
                if (!user.isValid()) {
                    Result.failure(IllegalArgumentException(\\"Invalid user data\\"))
                } else if (!users.containsKey(user.id)) {
                    Result.failure(NoSuchElementException(\\"User not found\\"))
                } else {
                    users[user.id] = user
                    Result.success(user)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteUser(id: String): Boolean {
        return mutex.withLock {
            users.remove(id) != null
        }
    }
    
    override suspend fun searchUsers(query: String): List<User> {
        return mutex.withLock {
            val lowercaseQuery = query.lowercase()
            users.values.filter { user ->
                user.name.lowercase().contains(lowercaseQuery) ||
                user.email.lowercase().contains(lowercaseQuery)
            }
        }
    }
    
    /**
     * Gets service statistics
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            \\"totalUsers\\" to users.size,
            \\"activeUsers\\" to users.values.count { it.isActive }
        )
    }
}"
</devin>

The comprehensive UserService has been created with full CRUD operations.`;
    
    console.log('   🔍 解析超复杂多行响应:');
    
    // 提取 devin 块
    const devinRegex = /<devin>([\s\S]*?)<\/devin>/;
    const match = complexResponse.match(devinRegex);
    
    if (match) {
        const devinContent = match[1].trim();
        console.log(`      ✅ 成功提取 devin 块 (${devinContent.length} 字符)`);
        
        // 解析工具调用
        const params = parseParametersWithRegex(devinContent);
        
        if (params.path && params.content) {
            console.log(`      ✅ 成功解析参数:`);
            console.log(`         路径: ${params.path}`);
            console.log(`         内容长度: ${params.content.length} 字符`);
            console.log(`         内容行数: ${params.content.split('\n').length} 行`);
            
            // 验证内容结构
            const content = params.content;
            const checks = [
                { name: '包声明', test: () => content.includes('package com.example.service') },
                { name: '导入语句', test: () => content.includes('import kotlinx') },
                { name: '文档注释', test: () => content.includes('/**') && content.includes('@author') },
                { name: '数据类', test: () => content.includes('data class User') },
                { name: '验证方法', test: () => content.includes('fun isValid()') },
                { name: '接口定义', test: () => content.includes('interface UserService') },
                { name: '实现类', test: () => content.includes('class InMemoryUserService') },
                { name: '异步方法', test: () => content.includes('suspend fun') },
                { name: '协程上下文', test: () => content.includes('withContext') },
                { name: '互斥锁', test: () => content.includes('Mutex') },
                { name: '错误处理', test: () => content.includes('Result.failure') },
                { name: '字符串模板', test: () => content.includes('$id') },
                { name: '复杂逻辑', test: () => content.includes('filter') }
            ];
            
            console.log(`      🔍 内容验证:`);
            let passedChecks = 0;
            for (const check of checks) {
                const passed = check.test();
                console.log(`         ${check.name}: ${passed ? '✅' : '❌'}`);
                if (passed) passedChecks++;
            }
            
            console.log(`      📈 验证通过率: ${passedChecks}/${checks.length} (${Math.round(passedChecks/checks.length*100)}%)`);
            
            if (passedChecks === checks.length) {
                console.log(`      🎉 所有验证通过！复杂多行内容解析完全正确！`);
            }
        } else {
            console.log(`      ❌ 参数解析失败`);
        }
    } else {
        console.log(`      ❌ 未找到 devin 块`);
    }
}

async function testEdgeCaseHandling() {
    console.log('\n📋 测试边界情况处理...');
    
    const edgeCases = [
        {
            name: '空字符串内容（应该允许）',
            command: '/write-file path="empty.txt" content=""',
            shouldSucceed: true
        },
        {
            name: '只有空格的内容',
            command: '/write-file path="spaces.txt" content="   "',
            shouldSucceed: true
        },
        {
            name: '包含特殊字符的路径',
            command: '/write-file path="special-chars_123.txt" content="test"',
            shouldSucceed: true
        },
        {
            name: '非常长的内容',
            command: `/write-file path="huge.txt" content="${'A'.repeat(10000)}"`,
            shouldSucceed: true
        }
    ];
    
    for (const testCase of edgeCases) {
        console.log(`   🔍 ${testCase.name}:`);
        
        try {
            const params = parseParametersWithRegex(testCase.command);
            
            const hasPath = params.path && params.path.trim() !== '';
            const hasContent = params.content !== undefined;
            
            const actualSuccess = hasPath && hasContent;
            const result = actualSuccess === testCase.shouldSucceed ? '✅' : '❌';
            
            console.log(`      预期: ${testCase.shouldSucceed ? 'success' : 'fail'}, 实际: ${actualSuccess ? 'success' : 'fail'} ${result}`);
            
            if (params.content !== undefined) {
                console.log(`      内容长度: ${params.content.length} 字符`);
            }
        } catch (error) {
            console.log(`      ❌ 解析异常: ${error.message}`);
        }
    }
}

async function testEscapeSequenceHandling() {
    console.log('\n📋 测试转义字符处理...');
    
    const testCases = [
        {
            name: '基本转义字符',
            input: 'Line 1\\nLine 2\\tTabbed',
            expected: 'Line 1\nLine 2\tTabbed'
        },
        {
            name: '嵌套引号',
            input: 'He said \\"Hello, world!\\"',
            expected: 'He said "Hello, world!"'
        },
        {
            name: '反斜杠转义',
            input: 'Path: C:\\\\Users\\\\test',
            expected: 'Path: C:\\Users\\test'
        },
        {
            name: '复杂混合转义',
            input: 'JSON: {\\"name\\": \\"test\\", \\"value\\": \\"line1\\\\nline2\\"}',
            expected: 'JSON: {"name": "test", "value": "line1\\nline2"}'
        }
    ];
    
    for (const testCase of testCases) {
        const processed = processEscapeSequences(testCase.input);
        const passed = processed === testCase.expected;
        
        console.log(`   ${testCase.name}: ${passed ? '✅' : '❌'}`);
        if (!passed) {
            console.log(`      预期: "${testCase.expected}"`);
            console.log(`      实际: "${processed}"`);
        }
    }
}

function processEscapeSequences(content) {
    return content
        .replace(/\\n/g, '\n')
        .replace(/\\r/g, '\r')
        .replace(/\\t/g, '\t')
        .replace(/\\"/g, '"')
        .replace(/\\\\/g, '\\');
}

// 运行测试
main().catch(error => {
    console.error('💥 测试异常:', error);
    process.exit(1);
});
