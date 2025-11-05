#!/usr/bin/env node

/**
 * 测试 CodingAgentTemplate 系统提示词的健壮性
 * 
 * 这个脚本会测试各种开发场景，验证：
 * 1. 工具调用是否正确
 * 2. 系统提示词是否能引导正确的行为
 * 3. 错误处理是否健壮
 */

const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

const TEST_PROJECT_PATH = '/tmp/test-project';
const CLI_PATH = path.join(process.cwd(), 'mpp-ui/dist/index.js');

// 测试用例定义
const TEST_CASES = [
    {
        name: "基础项目探索",
        task: "Explore the project structure and identify the main components",
        expectedTools: ["glob", "read-file"],
        description: "测试 Agent 是否会首先探索项目结构"
    },
    {
        name: "创建简单功能",
        task: "Create a simple hello world REST endpoint",
        expectedTools: ["glob", "read-file", "write-file"],
        description: "测试基本的代码生成功能"
    },
    {
        name: "添加依赖",
        task: "Add Spring AI dependency to the project",
        expectedTools: ["read-file", "write-file"],
        description: "测试依赖管理场景"
    },
    {
        name: "升级JDK版本",
        task: "Upgrade the project from Java 17 to Java 21",
        expectedTools: ["read-file", "write-file"],
        description: "测试版本升级场景"
    },
    {
        name: "添加测试",
        task: "Add unit tests for the main application class",
        expectedTools: ["read-file", "write-file", "glob"],
        description: "测试测试代码生成"
    },
    {
        name: "重构代码",
        task: "Refactor the application to use configuration properties",
        expectedTools: ["read-file", "write-file", "glob"],
        description: "测试代码重构场景"
    },
    {
        name: "错误修复",
        task: "Fix any compilation errors in the project",
        expectedTools: ["shell", "read-file", "write-file"],
        description: "测试错误诊断和修复"
    }
];

class TestRunner {
    constructor() {
        this.results = [];
        this.totalTests = 0;
        this.passedTests = 0;
    }

    async runTest(testCase) {
        console.log(`\n🧪 Running test: ${testCase.name}`);
        console.log(`📝 Task: ${testCase.task}`);
        console.log(`🔧 Expected tools: ${testCase.expectedTools.join(', ')}`);
        
        const startTime = Date.now();
        
        try {
            const result = await this.executeAgent(testCase.task);
            const duration = Date.now() - startTime;
            
            const analysis = this.analyzeResult(result, testCase);
            
            this.results.push({
                testCase,
                result,
                analysis,
                duration,
                success: analysis.toolsUsedCorrectly && analysis.taskCompleted
            });
            
            if (analysis.toolsUsedCorrectly && analysis.taskCompleted) {
                this.passedTests++;
                console.log(`✅ Test passed (${duration}ms)`);
            } else {
                console.log(`❌ Test failed (${duration}ms)`);
                console.log(`   Issues: ${analysis.issues.join(', ')}`);
            }
            
        } catch (error) {
            console.log(`💥 Test crashed: ${error.message}`);
            this.results.push({
                testCase,
                result: null,
                analysis: { error: error.message },
                duration: Date.now() - startTime,
                success: false
            });
        }
        
        this.totalTests++;
    }

    async executeAgent(task) {
        return new Promise((resolve, reject) => {
            const args = [
                CLI_PATH,
                'code',
                '--path', TEST_PROJECT_PATH,
                '--task', task,
                '--max-iterations', '5',
                '--quiet'
            ];
            
            console.log(`🚀 Executing: node ${args.join(' ')}`);
            
            const child = spawn('node', args, {
                stdio: ['pipe', 'pipe', 'pipe'],
                cwd: process.cwd()
            });
            
            let stdout = '';
            let stderr = '';
            
            child.stdout.on('data', (data) => {
                stdout += data.toString();
            });
            
            child.stderr.on('data', (data) => {
                stderr += data.toString();
            });
            
            const timeout = setTimeout(() => {
                child.kill('SIGTERM');
                reject(new Error('Test timeout after 60 seconds'));
            }, 60000);
            
            child.on('close', (code) => {
                clearTimeout(timeout);
                resolve({
                    exitCode: code,
                    stdout,
                    stderr,
                    success: code === 0
                });
            });
            
            child.on('error', (error) => {
                clearTimeout(timeout);
                reject(error);
            });
        });
    }

    analyzeResult(result, testCase) {
        const analysis = {
            taskCompleted: result.success,
            toolsUsedCorrectly: false,
            toolsUsed: [],
            issues: []
        };
        
        // 分析输出中的工具使用情况
        const output = result.stdout + result.stderr;
        
        // 查找工具调用模式
        const toolCallPatterns = [
            /Tool called: (\w+)/gi,
            /Executing tool: (\w+)/gi,
            /\/(\w+)/gi  // DevIns 格式的工具调用
        ];
        
        toolCallPatterns.forEach(pattern => {
            const matches = output.matchAll(pattern);
            for (const match of matches) {
                if (match[1] && !analysis.toolsUsed.includes(match[1])) {
                    analysis.toolsUsed.push(match[1]);
                }
            }
        });
        
        // 检查是否使用了预期的工具
        const expectedToolsUsed = testCase.expectedTools.filter(tool => 
            analysis.toolsUsed.some(usedTool => 
                usedTool.toLowerCase().includes(tool.toLowerCase()) ||
                tool.toLowerCase().includes(usedTool.toLowerCase())
            )
        );
        
        analysis.toolsUsedCorrectly = expectedToolsUsed.length >= Math.ceil(testCase.expectedTools.length * 0.5);
        
        if (!analysis.taskCompleted) {
            analysis.issues.push('Task not completed successfully');
        }
        
        if (!analysis.toolsUsedCorrectly) {
            analysis.issues.push(`Expected tools not used. Expected: ${testCase.expectedTools.join(', ')}, Used: ${analysis.toolsUsed.join(', ')}`);
        }
        
        if (output.includes('error') || output.includes('Error')) {
            analysis.issues.push('Errors detected in output');
        }
        
        return analysis;
    }

    generateReport() {
        console.log('\n' + '='.repeat(80));
        console.log('📊 TEST RESULTS SUMMARY');
        console.log('='.repeat(80));
        console.log(`Total tests: ${this.totalTests}`);
        console.log(`Passed: ${this.passedTests}`);
        console.log(`Failed: ${this.totalTests - this.passedTests}`);
        console.log(`Success rate: ${((this.passedTests / this.totalTests) * 100).toFixed(1)}%`);
        
        console.log('\n📋 DETAILED RESULTS:');
        this.results.forEach((result, index) => {
            console.log(`\n${index + 1}. ${result.testCase.name}`);
            console.log(`   Status: ${result.success ? '✅ PASS' : '❌ FAIL'}`);
            console.log(`   Duration: ${result.duration}ms`);
            if (result.analysis.toolsUsed.length > 0) {
                console.log(`   Tools used: ${result.analysis.toolsUsed.join(', ')}`);
            }
            if (result.analysis.issues && result.analysis.issues.length > 0) {
                console.log(`   Issues: ${result.analysis.issues.join('; ')}`);
            }
        });
        
        // 生成改进建议
        this.generateImprovementSuggestions();
    }

    generateImprovementSuggestions() {
        console.log('\n💡 IMPROVEMENT SUGGESTIONS:');
        
        const failedTests = this.results.filter(r => !r.success);
        if (failedTests.length === 0) {
            console.log('🎉 All tests passed! The system prompt is working well.');
            return;
        }
        
        const commonIssues = {};
        failedTests.forEach(test => {
            if (test.analysis.issues) {
                test.analysis.issues.forEach(issue => {
                    commonIssues[issue] = (commonIssues[issue] || 0) + 1;
                });
            }
        });
        
        Object.entries(commonIssues)
            .sort(([,a], [,b]) => b - a)
            .forEach(([issue, count]) => {
                console.log(`- ${issue} (${count} tests affected)`);
            });
    }
}

async function main() {
    console.log('🚀 Starting CodingAgent robustness tests...');
    console.log(`📁 Test project: ${TEST_PROJECT_PATH}`);
    console.log(`🤖 CLI path: ${CLI_PATH}`);
    
    // 检查CLI是否存在
    if (!fs.existsSync(CLI_PATH)) {
        console.error(`❌ CLI not found at ${CLI_PATH}`);
        console.error('Please run: cd mpp-ui && npm run build:ts');
        process.exit(1);
    }
    
    // 检查测试项目是否存在
    if (!fs.existsSync(TEST_PROJECT_PATH)) {
        console.error(`❌ Test project not found at ${TEST_PROJECT_PATH}`);
        process.exit(1);
    }
    
    const runner = new TestRunner();
    
    // 运行所有测试
    for (const testCase of TEST_CASES) {
        await runner.runTest(testCase);
        // 短暂延迟避免资源竞争
        await new Promise(resolve => setTimeout(resolve, 1000));
    }
    
    // 生成报告
    runner.generateReport();
    
    // 退出码
    process.exit(runner.passedTests === runner.totalTests ? 0 : 1);
}

if (require.main === module) {
    main().catch(console.error);
}
