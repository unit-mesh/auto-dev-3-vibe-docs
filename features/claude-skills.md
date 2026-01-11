# Claude Skills Guide

Claude Skills are reusable prompt templates that help you perform specific tasks efficiently. They're like custom commands you can create and share across projects.

## What are Claude Skills?

A Claude Skill is a directory containing a `SKILL.md` file with:
- **Frontmatter**: Metadata (name, description, variables)
- **Template**: Instructions for the AI with variable placeholders

## Quick Start

### Using a Skill

```bash
# In any AutoDev interface (IDEA, VSCode, CLI)
/skill.pdf Extract all tables from report.pdf
/skill.code-review Review src/main.ts for security issues
/skill.test-gen Create tests for UserService.kt
```

### Creating Your First Skill

1. **Create a directory** in your project root or `~/.claude/skills/`:
   ```bash
   mkdir -p ~/.claude/skills/my-skill
   ```

2. **Create SKILL.md**:
   ```markdown
   ---
   name: my-skill
   description: Brief description of what this does
   ---
   
   # Instructions for AI
   
   $ARGUMENTS
   
   Project: $PROJECT_NAME
   ```

3. **Use it**:
   ```bash
   /skill.my-skill Do something amazing
   ```

## SKILL.md Format

### Basic Structure

```markdown
---
name: skill-name
description: What this skill does
variables:
  OPTIONAL_VAR: "default value"
---

# Skill Instructions

Your prompt template here.

Use $ARGUMENTS for user input.
Use $PROJECT_NAME for project name.
```

### Frontmatter (YAML)

```yaml
---
name: pdf              # Skill name (required)
description: Handle PDF operations  # Description (required)
variables:             # Optional variables
  OUTPUT_FORMAT: "markdown"
  TEMPLATE_FILE: "templates/pdf.md"
---
```

### Built-in Variables

Always available:
- `$ARGUMENTS` - User-provided arguments
- `$COMMAND` - The command name
- `$INPUT` - Raw input text
- `$PROJECT_PATH` - Project root path
- `$PROJECT_NAME` - Project name

### Custom Variables

Define in frontmatter:
```yaml
variables:
  OUTPUT_FORMAT: "json"
  MAX_ITEMS: "10"
```

Use in template:
```markdown
Format the output as $OUTPUT_FORMAT with max $MAX_ITEMS items.
```

### File Content Loading

Variables that look like file paths are automatically loaded:

```yaml
variables:
  SPEC: "docs/api-spec.md"
  TEMPLATE: "templates/review.md"
```

The file content replaces the variable in the template.

## Skill Locations

Skills are discovered from two locations:

### 1. Project Skills
Any directory in your project root with a `SKILL.md`:
```
my-project/
├── pdf-skill/
│   └── SKILL.md
├── custom-review/
│   └── SKILL.md
└── src/
```

### 2. User Skills
Global skills in `~/.claude/skills/`:
```
~/.claude/skills/
├── pdf/
│   └── SKILL.md
├── code-review/
│   └── SKILL.md
└── my-custom-skill/
    └── SKILL.md
```

## Platform Support

Claude Skills work across all AutoDev platforms:

### IntelliJ IDEA
```
# In DevIns console
/skill.code-review Review this file
```

### VSCode
```
# In AutoDev chat
/skill.pdf Extract data from report.pdf
```

### CLI
```bash
$ xiuper chat
> /skill.test-gen Generate tests for UserService
```

### Desktop App
Skills work automatically in the Compose desktop app.

## Example Skills

See `examples/skills/` for ready-to-use skills:

- **pdf**: Extract information from PDF documents
- **code-review**: Comprehensive code review
- **test-gen**: Generate unit tests
- **doc-gen**: Generate documentation
- **refactor**: Suggest refactoring improvements

## Best Practices

### 1. Clear Descriptions
```yaml
# Good
description: Extract tables and data from PDF documents

# Bad
description: PDF stuff
```

### 2. Structured Templates
```markdown
# Task
$ARGUMENTS

## Instructions
1. Step one
2. Step two

## Output Format
- Use markdown
- Include examples
```

### 3. Helpful Defaults
```yaml
variables:
  OUTPUT_FORMAT: "markdown"  # Sensible default
  DETAIL_LEVEL: "medium"     # Not too verbose
```

### 4. Documentation
Include usage examples in comments:
```markdown
---
name: api-test
description: Generate API tests
---

<!-- Usage: /skill.api-test Test the /users endpoint -->

# API Test Generation
...
```

## Advanced Features

### Multi-line Templates

Use standard markdown:
```markdown
---
name: complex
---

# Part 1
Instructions...

# Part 2
More instructions...

$ARGUMENTS
```

### Conditional Content

Use AI instructions:
```markdown
If $ARGUMENTS contains "detailed":
  Provide comprehensive analysis
Otherwise:
  Provide summary only
```

### Combining Skills

Reference other skills:
```markdown
First, use the code-review skill approach.
Then, apply refactoring suggestions.
```

## Troubleshooting

### Skill Not Found

```
Error: Skill not found: my-skill
Available skills: pdf, code-review, test-gen
```

**Solution**: Check skill name and location:
```bash
# List skills
ls ~/.claude/skills/
ls -d */SKILL.md  # In project root
```

### Variable Not Replaced

If `$MY_VAR` appears in output:

1. Check frontmatter defines it:
   ```yaml
   variables:
     MY_VAR: "value"
   ```

2. Check variable name matches exactly (case-sensitive)

### File Not Loaded

If file content variable doesn't work:

1. Check file path is relative to project root
2. Verify file exists
3. Check file permissions

## Tips

1. **Start Simple**: Begin with basic templates, add complexity as needed
2. **Test Thoroughly**: Try skills with different inputs
3. **Share Skills**: Commit useful skills to your project
4. **Iterate**: Improve skills based on results
5. **Document**: Add comments explaining complex templates

## Contributing

To contribute example skills:

1. Create skill in `examples/skills/your-skill/`
2. Add `SKILL.md` with clear frontmatter
3. Update `examples/skills/README.md`
4. Test on at least one platform
5. Submit pull request

## License

Skills you create are yours. Example skills in this repository are under MPL 2.0.

