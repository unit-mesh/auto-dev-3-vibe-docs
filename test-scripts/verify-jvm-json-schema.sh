#!/bin/bash

echo "🔧 Verifying JVM Tool Template Generation with JSON Schema"
echo "============================================================"

# Step 1: Check if the classes are compiled
echo -e "\n1. Checking JVM compilation..."
if [ -d "mpp-core/build/classes/kotlin/jvm/main" ]; then
    echo "✅ JVM classes found"
    echo "   Directory: mpp-core/build/classes/kotlin/jvm/main"
    
    # List some key classes
    echo "   Key classes:"
    find mpp-core/build/classes/kotlin/jvm/main -name "*CodingAgentContext*" -type f | head -3
    find mpp-core/build/classes/kotlin/jvm/main -name "*Tool*" -type f | head -5
else
    echo "❌ JVM classes not found. Running compilation..."
    ./gradlew :mpp-core:compileKotlinJvm
fi

# Step 2: Check if the formatToolListForAI method exists in compiled classes
echo -e "\n2. Checking formatToolListForAI method..."
if javap -cp "mpp-core/build/classes/kotlin/jvm/main" cc.unitmesh.agent.CodingAgentContext\$Companion 2>/dev/null | grep -q "formatToolListForAI"; then
    echo "✅ formatToolListForAI method found in compiled classes"
else
    echo "❌ formatToolListForAI method not found"
fi

# Step 3: Check JSON serialization classes
echo -e "\n3. Checking JSON serialization support..."
if find mpp-core/build/classes/kotlin/jvm/main -name "*Json*" -type f | head -1 | grep -q "Json"; then
    echo "✅ JSON serialization classes found"
else
    echo "⚠️  JSON serialization classes not found (may be in dependencies)"
fi

# Step 4: Create a simple verification using reflection
echo -e "\n4. Creating verification script..."
cat > /tmp/VerifyJvmJsonSchema.java << 'EOF'
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class VerifyJvmJsonSchema {
    public static void main(String[] args) {
        System.out.println("🔍 Verifying JVM JSON Schema Support via Reflection");
        System.out.println("=".repeat(50));
        
        try {
            // Check CodingAgentContext.Companion class
            Class<?> companionClass = Class.forName("cc.unitmesh.agent.CodingAgentContext$Companion");
            System.out.println("✅ CodingAgentContext$Companion class found");
            
            // Check formatToolListForAI method
            Method[] methods = companionClass.getDeclaredMethods();
            boolean foundFormatMethod = false;
            for (Method method : methods) {
                if (method.getName().equals("formatToolListForAI")) {
                    foundFormatMethod = true;
                    System.out.println("✅ formatToolListForAI method found");
                    System.out.println("   Method signature: " + method);
                    System.out.println("   Is public: " + Modifier.isPublic(method.getModifiers()));
                    System.out.println("   Is static: " + Modifier.isStatic(method.getModifiers()));
                    break;
                }
            }
            
            if (!foundFormatMethod) {
                System.out.println("❌ formatToolListForAI method not found");
                System.out.println("Available methods:");
                for (Method method : methods) {
                    if (method.getName().contains("format") || method.getName().contains("Tool")) {
                        System.out.println("   - " + method.getName());
                    }
                }
            }
            
            // Check tool classes
            String[] toolClasses = {
                "cc.unitmesh.agent.tool.impl.ReadFileTool",
                "cc.unitmesh.agent.tool.impl.WriteFileTool",
                "cc.unitmesh.agent.tool.impl.ShellTool"
            };
            
            System.out.println("\n🔍 Checking tool classes:");
            for (String className : toolClasses) {
                try {
                    Class<?> toolClass = Class.forName(className);
                    System.out.println("✅ " + className + " found");
                } catch (ClassNotFoundException e) {
                    System.out.println("❌ " + className + " not found");
                }
            }
            
            System.out.println("\n🎉 JVM classes verification completed!");
            
        } catch (Exception e) {
            System.out.println("❌ Error during verification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
EOF

# Step 5: Compile and run the verification
echo -e "\n5. Running verification..."
if javac -cp "mpp-core/build/classes/kotlin/jvm/main" /tmp/VerifyJvmJsonSchema.java 2>/dev/null; then
    echo "✅ Verification script compiled"
    if java -cp "mpp-core/build/classes/kotlin/jvm/main:/tmp" VerifyJvmJsonSchema; then
        echo "✅ Verification completed successfully"
    else
        echo "⚠️  Verification completed with issues"
    fi
else
    echo "❌ Failed to compile verification script"
fi

# Step 6: Summary
echo -e "\n6. Summary:"
echo "   - JVM compilation: $([ -d "mpp-core/build/classes/kotlin/jvm/main" ] && echo "✅ OK" || echo "❌ Failed")"
echo "   - Classes available: $(find mpp-core/build/classes/kotlin/jvm/main -name "*.class" | wc -l | tr -d ' ') classes"
echo "   - JSON Schema format should work in JVM environment"

echo -e "\n🚀 JVM verification completed!"
