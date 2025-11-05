#!/usr/bin/env node

/**
 * 基础 Agent 测试脚本
 * 
 * 用于验证 AutoDev CLI Agent 的基本功能
 */

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');

const CLI_PATH = './dist/jsMain/typescript/index.js';

async function createTestProject() {
    const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'agent-test-'));
    
    // 创建基本的 Gradle Spring Boot 项目结构
    const dirs = [
        'src/main/java/com/example',
        'src/main/resources',
        'src/test/java/com/example'
    ];
    
    for (const dir of dirs) {
        fs.mkdirSync(path.join(tempDir, dir), { recursive: true });
    }
    
    // 创建 build.gradle.kts
    const buildGradle = `
plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
`.trim();

    fs.writeFileSync(path.join(tempDir, 'build.gradle.kts'), buildGradle);
    
    // 创建主应用类
    const mainClass = `
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
`.trim();

    fs.writeFileSync(path.join(tempDir, 'src/main/java/com/example/TestApplication.java'), mainClass);
    
    // 创建 application.properties
    const appProperties = `
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
`.trim();

    fs.writeFileSync(path.join(tempDir, 'src/main/resources/application.properties'), appProperties);
    
    return tempDir;
}

async function runAgentTest(projectPath, task, timeout = 60000) {
    return new Promise((resolve, reject) => {
        const args = [
            CLI_PATH,
            'code',
            '--path', projectPath,
            '--task', task,
            '--max-iterations', '3',
            '--quiet'
        ];
        
        console.log(`🚀 执行: node ${args.join(' ')}`);
        
        const child = spawn('node', args, {
            stdio: ['pipe', 'pipe', 'pipe'],
            cwd: process.cwd()
        });
        
        let stdout = '';
        let stderr = '';
        
        child.stdout.on('data', (data) => {
            stdout += data.toString();
            process.stdout.write(data); // 实时输出
        });
        
        child.stderr.on('data', (data) => {
            stderr += data.toString();
            process.stderr.write(data); // 实时输出
        });
        
        const timeoutHandle = setTimeout(() => {
            child.kill('SIGTERM');
            reject(new Error(`测试超时 (${timeout}ms)`));
        }, timeout);
        
        child.on('close', (code) => {
            clearTimeout(timeoutHandle);
            resolve({
                exitCode: code,
                stdout,
                stderr,
                success: code === 0
            });
        });
        
        child.on('error', (error) => {
            clearTimeout(timeoutHandle);
            reject(error);
        });
    });
}

async function main() {
    console.log('🧪 开始基础 Agent 测试...\n');
    
    let testProject;
    
    try {
        // 创建测试项目
        testProject = await createTestProject();
        console.log(`📁 测试项目创建于: ${testProject}\n`);
        
        // 测试1: 简单的项目探索
        console.log('🔍 测试1: 项目探索');
        const result1 = await runAgentTest(
            testProject,
            'List all files in the project to understand the structure',
            90000 // 90秒超时
        );
        
        console.log(`✅ 测试1完成 - 退出码: ${result1.exitCode}`);
        console.log(`📊 输出长度: stdout=${result1.stdout.length}, stderr=${result1.stderr.length}\n`);

        console.log('🎉 基础测试完成!');
        
    } catch (error) {
        console.error('❌ 测试失败:', error.message);
        process.exit(1);
    } finally {
        // 清理测试项目
        if (testProject) {
            try {
                fs.rmSync(testProject, { recursive: true, force: true });
                console.log(`🧹 已清理测试项目: ${testProject}`);
            } catch (error) {
                console.warn(`⚠️  清理失败: ${error.message}`);
            }
        }
    }
}

if (require.main === module) {
    main().catch(console.error);
}
