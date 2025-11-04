import cc.unitmesh.agent.CodingAgentContext;
import cc.unitmesh.agent.CodingAgentTemplate;
import cc.unitmesh.agent.tool.ExecutableTool;
import cc.unitmesh.agent.tool.registry.ToolRegistry;
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem;
import cc.unitmesh.devins.compiler.template.TemplateCompiler;
import cc.unitmesh.devins.compiler.variable.VariableTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Test JVM version of tool template generation with JSON Schema format
 */
public class TestJvmToolTemplate {
    
    public static void main(String[] args) {
        System.out.println("🔧 Testing JVM Tool Template Generation with JSON Schema");
        System.out.println("=".repeat(60));
        
        try {
            // Step 1: Create tool registry
            System.out.println("\n1. Creating Tool Registry...");
            String projectPath = "/test/kotlin-project";
            DefaultToolFileSystem fileSystem = new DefaultToolFileSystem(projectPath);
            ToolRegistry toolRegistry = new ToolRegistry(fileSystem);
            
            List<ExecutableTool<?, ?>> availableTools = toolRegistry.getAllTools().values().stream().toList();
            System.out.println("✅ Tool registry created with " + availableTools.size() + " tools:");
            for (ExecutableTool<?, ?> tool : availableTools) {
                String desc = tool.getDescription();
                String shortDesc = desc.length() > 50 ? desc.substring(0, 50) + "..." : desc;
                System.out.println("   - " + tool.getName() + ": " + shortDesc);
            }
            
            // Step 2: Generate tool list with new JSON Schema format
            System.out.println("\n2. Generating Tool List with JSON Schema Format...");
            String toolList = CodingAgentContext.Companion.formatToolListForAI(availableTools);
            
            System.out.println("✅ Tool list generated successfully!");
            System.out.println("Tool list length: " + toolList.length() + " characters");
            
            // Step 3: Analyze the format
            System.out.println("\n3. Analyzing JSON Schema Format...");
            Map<String, Boolean> formatChecks = Map.of(
                "Uses Markdown headers (##)", toolList.contains("## "),
                "Has JSON Schema blocks", toolList.contains("```json"),
                "Contains $schema field", toolList.contains("\"$schema\""),
                "Has draft-07 schema", toolList.contains("draft-07/schema#"),
                "Contains type object", toolList.contains("\"type\": \"object\""),
                "Has properties field", toolList.contains("\"properties\""),
                "Has required field", toolList.contains("\"required\""),
                "Has additionalProperties", toolList.contains("\"additionalProperties\""),
                "No XML tags", !toolList.contains("<tool name="),
                "No XML parameters", !toolList.contains("<parameters>"),
                "Has example blocks", toolList.contains("**Example:**")
            );
            
            formatChecks.forEach((check, passed) -> {
                System.out.println("  " + (passed ? "✅" : "❌") + " " + check);
            });
            
            // Step 4: Show sample of new format
            System.out.println("\n4. Sample of New Format:");
            System.out.println("-".repeat(50));
            System.out.println(toolList.substring(0, Math.min(1200, toolList.length())));
            System.out.println("-".repeat(50));
            
            // Step 5: Create complete context and template
            System.out.println("\n5. Creating Complete Context and Template...");
            CodingAgentContext context = new CodingAgentContext(
                null, // currentFile
                projectPath,
                "", // projectStructure
                "Linux Ubuntu 22.04",
                "2024-01-01T00:00:00Z",
                toolList,
                "", // agentRules
                "gradle",
                "/bin/bash",
                "", // moduleInfo
                "" // frameworkContext
            );
            
            VariableTable variableTable = context.toVariableTable();
            TemplateCompiler compiler = new TemplateCompiler(variableTable);
            String template = compiler.compile(CodingAgentTemplate.INSTANCE.getEN());
            
            System.out.println("✅ Template compiled successfully!");
            System.out.println("Template length: " + template.length() + " characters");
            
            // Step 6: Analyze template quality
            System.out.println("\n6. Template Quality Analysis...");
            Map<String, Boolean> templateChecks = Map.of(
                "Available Tools section", template.contains("Available Tools"),
                "JSON Schema format", template.contains("```json"),
                "Standard schema format", template.contains("draft-07/schema#"),
                "Proper tool structure", template.contains("## read-file"),
                "Parameter descriptions", template.contains("\"description\""),
                "Type information", template.contains("\"type\""),
                "Required fields", template.contains("\"required\""),
                "Example usage", template.contains("**Example:**")
            );
            
            templateChecks.forEach((check, passed) -> {
                System.out.println("  " + (passed ? "✅" : "❌") + " " + check);
            });
            
            // Step 7: Export results
            System.out.println("\n7. Exporting Results...");
            StringBuilder results = new StringBuilder();
            results.append("# JVM Tool Template Test Results\n");
            results.append("Generated at: ").append(LocalDateTime.now()).append("\n\n");
            results.append("## Format Checks\n");
            formatChecks.forEach((check, passed) -> {
                results.append("- ").append(passed ? "✅" : "❌").append(" ").append(check).append("\n");
            });
            results.append("\n## Template Checks\n");
            templateChecks.forEach((check, passed) -> {
                results.append("- ").append(passed ? "✅" : "❌").append(" ").append(check).append("\n");
            });
            results.append("\n## Metrics\n");
            results.append("- Tool count: ").append(availableTools.size()).append("\n");
            results.append("- Tool list length: ").append(toolList.length()).append(" characters\n");
            results.append("- Template length: ").append(template.length()).append(" characters\n");
            results.append("\n## Tool List Sample\n```\n");
            results.append(toolList.substring(0, Math.min(1000, toolList.length())));
            results.append("\n```\n");
            
            Files.write(Paths.get("/tmp/jvm-tool-template-test.md"), results.toString().getBytes());
            System.out.println("✅ Results exported to /tmp/jvm-tool-template-test.md");
            
            // Step 8: Summary
            System.out.println("\n8. Summary:");
            boolean allFormatChecksPass = formatChecks.values().stream().allMatch(Boolean::booleanValue);
            boolean allTemplateChecksPass = templateChecks.values().stream().allMatch(Boolean::booleanValue);
            
            if (allFormatChecksPass && allTemplateChecksPass) {
                System.out.println("🎉 SUCCESS: JVM version works perfectly with JSON Schema format!");
                System.out.println("   - All format checks passed");
                System.out.println("   - All template checks passed");
                System.out.println("   - Tool list length: " + toolList.length() + " characters");
                System.out.println("   - Template length: " + template.length() + " characters");
            } else {
                System.out.println("⚠️  PARTIAL: Some checks failed.");
                long formatPassCount = formatChecks.values().stream().mapToLong(b -> b ? 1 : 0).sum();
                long templatePassCount = templateChecks.values().stream().mapToLong(b -> b ? 1 : 0).sum();
                System.out.println("   Format checks: " + formatPassCount + "/" + formatChecks.size());
                System.out.println("   Template checks: " + templatePassCount + "/" + templateChecks.size());
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error during JVM testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
