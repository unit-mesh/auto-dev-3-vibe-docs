
#!/usr/bin/env node

/**
 * 简化的 CodingAgent 健壮性测试
 * 测试系统提示词是否能正确引导工具调用
 */

const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

const TEST_PROJECT_PATH = '/tmp/test-project';
const CLI_PATH = path.join(process.cwd(), 'mpp-ui/dist/index.js');

// 简化的测试用例
const TEST_CASES = [
    {
        name: "基础项目探索",
        task: "List all files in the project to understand the structure",
        expectedBehavior: "应该使用 glob 工具列出文件"
    },
    {
        name: "读取文件内容",
        task: "Read the pom.xml file to understand the project configuration",
        expectedBehavior: "应该使用 read-file 工具读取 pom.xml"
    },
    {
        name: "创建新文件",
        task: "Create a README.md file with project description",
        expectedBehavior: "应该使用 write-file 工具创建文件"
    }
];

async function runSingleTest(testCase) {
    console.log(`\n🧪 测试: ${testCase.name}`);
    console.log(`📝 任务: ${testCase.task}`);
    console.log(`🎯 预期: ${testCase.expectedBehavior}`);
    
    return new Promise((resolve) => {
        const args = [
            CLI_PATH,
            'code',
            '--path', TEST_PROJECT_PATH,
            '--task', testCase.task,
            '--max-iterations', '3',
            '--quiet'
        ];
        
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
            console.log('⏰ 测试超时');
            resolve({ success: false, reason: 'timeout', stdout, stderr });
        }, 60000);
        
        child.on('close', (code) => {
            clearTimeout(timeout);
            const success = code === 0;
            const output = stdout + stderr;
            
            // 简单的成功判断
            const hasToolCalls = output.includes('●') || output.includes('Tool') || output.includes('File search') || output.includes('read file') || output.includes('edit file');
            const hasErrors = output.includes('Error') || output.includes('Failed') || output.includes('❌');
            
            console.log(`${success ? '✅' : '❌'} 退出码: ${code}`);
            console.log(`🔧 包含工具调用: ${hasToolCalls ? '是' : '否'}`);
            console.log(`⚠️  包含错误: ${hasErrors ? '是' : '否'}`);
            
            if (hasToolCalls && !hasErrors) {
                console.log('🎉 测试通过 - Agent 正确使用了工具');
            } else if (!hasToolCalls) {
                console.log('❌ 测试失败 - Agent 没有调用任何工具');
            } else if (hasErrors) {
                console.log('⚠️  测试部分成功 - Agent 调用了工具但有错误');
            }
            
            resolve({
                success: success && hasToolCalls && !hasErrors,
                reason: !hasToolCalls ? 'no_tools' : hasErrors ? 'has_errors' : 'ok',
                stdout,
                stderr
            });
        });
        
        child.on('error', (error) => {
            clearTimeout(timeout);
            console.log(`💥 进程错误: ${error.message}`);
            resolve({ success: false, reason: 'process_error', error: error.message });
        });
    });
}

async function main() {
    console.log('🚀 开始 CodingAgent 系统提示词健壮性测试');
    console.log(`📁 测试项目: ${TEST_PROJECT_PATH}`);
    console.log(`🤖 CLI 路径: ${CLI_PATH}`);
    
    // 检查前置条件
    if (!fs.existsSync(CLI_PATH)) {
        console.error(`❌ CLI 不存在: ${CLI_PATH}`);
        console.error('请运行: cd mpp-ui && npm run build:ts');
        process.exit(1);
    }
    
    if (!fs.existsSync(TEST_PROJECT_PATH)) {
        console.error(`❌ 测试项目不存在: ${TEST_PROJECT_PATH}`);
        process.exit(1);
    }
    
    let passed = 0;
    let total = TEST_CASES.length;
    
    // 运行测试
    for (const testCase of TEST_CASES) {
        const result = await runSingleTest(testCase);
        if (result.success) {
            passed++;
        }
        
        // 短暂延迟避免资源竞争
        await new Promise(resolve => setTimeout(resolve, 2000));
    }
    
    // 生成报告
    console.log('\n' + '='.repeat(60));
    console.log('📊 测试结果汇总');
    console.log('='.repeat(60));
    console.log(`总测试数: ${total}`);
    console.log(`通过: ${passed}`);
    console.log(`失败: ${total - passed}`);
    console.log(`成功率: ${((passed / total) * 100).toFixed(1)}%`);
    
    if (passed === total) {
        console.log('\n🎉 所有测试通过！系统提示词工作正常。');
    } else {
        console.log('\n⚠️  部分测试失败，需要检查系统提示词或工具调用逻辑。');
    }
    
    console.log('\n💡 改进建议:');
    if (passed < total) {
        console.log('- 检查系统提示词是否清楚地指导了工具使用');
        console.log('- 验证工具调用格式是否正确');
        console.log('- 确认 JSON 参数解析是否正常工作');
    } else {
        console.log('- 系统提示词健壮性良好');
        console.log('- 工具调用机制运行正常');
        console.log('- 可以考虑添加更复杂的测试场景');
    }
    
    process.exit(passed === total ? 0 : 1);
}

if (require.main === module) {
    main().catch(console.error);
}
