# MiCo - Migration Coordinator

**MiCo** (Migration Coordinator) is an automated tool for migrating Intershop Commerce Management (ICM) cartridge build scripts from ICM 7.10 to ICM 11, using AI agents to perform the migration tasks.

## Features

✅ **Multi-Phase Migration** - Configurable migration phases defined in JSON
✅ **Dynamic Placeholder Replacement** - Automatic data injection into prompts
✅ **Multiple AI Agents** - Support for GitHub Copilot and Claude Code
✅ **Comprehensive Logging** - Detailed logs for every cartridge and phase
✅ **Batch Processing** - Migrate multiple cartridges in one session
✅ **Import Scanning** - Automatic Java import analysis for dependency resolution

## Prerequisites

- **Java 17+** - Required to run the tool
- **Gradle** - Included via Gradle Wrapper
- **AI Agent** - One of the following:
  - [GitHub Copilot CLI](https://www.npmjs.com/package/@githubnext/github-copilot-cli)
  - [Claude Code CLI](https://code.claude.com/)

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd MiCo
```

### 2. Install an AI Agent

#### Option A: GitHub Copilot CLI

```bash
npm install -g @githubnext/github-copilot-cli
copilot auth
```

#### Option B: Claude Code CLI

```bash
# Follow installation instructions at https://code.claude.com/
claude auth
```

### 3. Build the Project

```bash
./gradlew build
```

## Configuration

### Phase Configuration

Phases are defined in `phases/config.json`:

```json
[
  {
    "name": "Initialization Phase",
    "instructions": "Phase_1.md",
    "id": "init",
    "order": 1,
    "inputs": {
      "cartridge_path": "path/to/cartridge",
      "cartridge_name": "name of the cartridge"
    }
  },
  {
    "name": "Dependency Resolution",
    "instructions": "Phase_2.md",
    "id": "dep_res",
    "order": 2,
    "inputs": {
      "dependencies_list": "path/to/dependencies_list",
      "cartridge_path": "path/to/cartridge"
    }
  }
]
```

### Phase Instructions

Phase instruction files are stored in `phases/instructions/` as Markdown files:
- `Phase_1.md` - Initialization and build script migration
- `Phase_2.md` - Dependency resolution
- `Phase_3.md` - Import fixes (if needed)

Use placeholders in instruction files:
- `[CARTRIDGE_PATH]` - Replaced with actual cartridge path
- `[CARTRIDGE_NAME]` - Replaced with cartridge name
- `[DEPENDENCIES_LIST]` - Auto-generated list of Java imports

## ⚠️ Safety Warning — Read Before Running

MiCo runs AI agents **autonomously and non-interactively** against your
cartridges. To do this it grants the agents full tool access
(`--allow-all-tools` for GitHub Copilot, `--dangerously-skip-permissions` for
Claude Code). During migration the agents (and the native phases) **create,
overwrite and delete files** in the target cartridges, for example, the old
`build.gradle` is deleted and Java sources are rewritten in place.

**Before running MiCo:**
- **Only run it on cartridges under version control (Git)** so every change is
  reviewable and revertible, or keep a separate backup copy.
- **Review the diff** after each session before committing or shipping the
  results.
- **Only run it on code you trust**, in an environment you trust, since the
  agents execute with unrestricted tool permissions.

## Usage

### Basic Command

```bash
./gradlew run --args="-p <path> -a <agent>"
```

### Command-Line Options

| Option       | Required | Description                            | Example                    |
|--------------|----------|----------------------------------------|----------------------------|
| `-p <path>`  | ✅       | Path to cartridge(s)                   | `-p /home/user/cartridges` |
| `-a <agent>` | ✅       | Agent type: `copilot` or `claude_code` | `-a claude_code`           |
| `-m <model>` | ❌       | AI model to use                        | `-m claude-sonnet-4-6`     |
| `-s`         | ❌       | Single cartridge mode                  | `-s`                       |

### Examples

#### Migrate Multiple Cartridges with Claude Code

```bash
./gradlew run --args="-p /home/user/migration/cartridges -a claude_code -m claude-sonnet-4-6"
```

#### Migrate Single Cartridge with GitHub Copilot

```bash
./gradlew run --args="-p /home/user/migration/single_cartridge -a copilot -m gpt-4.1 -s"
```

#### Use Default Model

```bash
./gradlew run --args="-p /home/user/cartridges -a claude_code"
```

## Directory Structure

```
MiCo/
├── phases/
│   ├── config.json                  # Phase configuration
│   └── instructions/
│       ├── Phase_1.md              # Phase instruction files
│       ├── Phase_2.md
│       ├── Phase_3.md
│       ├── Phase_4.md
│       ├── Phase_5.md
│       └── Phase_6.md
├── logs/                           # Generated logs (gitignored)
│   └── session_YYYY-MM-DD_HH-mm-ss/
│       ├── SUMMARY.txt
│       ├── migration_master.log
│       └── cartridge_logs...
├── src/
│   └── main/java/com/intershop/mico/
│       ├── Main.java               # Entry point
│       ├── Migrator.java           # Migration orchestrator
│       ├── agent/                  # AI agent implementations
│       ├── models/                 # Data models
│       ├── repo/                   # Repositories
│       └── util/                   # Utilities
├── build.gradle.kts                # Gradle build file
└── README.md                       # This file
```

## Migration Workflow

### 1. **Session Initialization**
- Creates timestamped log directory
- Loads phase configuration
- Initializes AI agent

### 2. **Cartridge Processing**
For each cartridge:

#### Phase 1: Initialization
- Reads old `build.gradle` (Groovy)
- Scans Java files for imports
- Generates new `build.gradle.kts` (Kotlin DSL)
- Deletes old build file

#### Phase 2: Dependency Resolution
- Scans Java imports (excluding `com.intershop.*`, `java.*`, `javax.*`)
- Analyzes existing dependencies
- Adds missing dependencies
- Removes duplicates
- Sorts alphabetically

Phases 3–5 (code migration, code fix, resource processing) then run, followed by:

#### Phase 6: Build Verification (Native)
- Compiles each migrated cartridge with Gradle (default task: `compileJava`)
- Locates a Gradle wrapper in the cartridge or an ancestor directory so
  dependencies resolve against the surrounding build
- Records a per-cartridge result — **PASSED**, **FAILED**, or **SKIPPED**
  (when no Gradle wrapper is available) — in `SUMMARY.txt`
- A failed build is recorded as a result and does not abort the session

See [phases/instructions/Phase_6.md](phases/instructions/Phase_6.md) for details
and how to change the verification task via the `build_task` input.

### 3. **Logging**
- Creates detailed logs for each phase
- Generates cartridge summaries
- Creates master session log
- Produces final summary report

## Output

### Console Output

```
📝 Logging to: /home/user/MiCo/logs/session_2026-01-27_14-30-45
Workspace mode: Found 3 cartridges
=== Migrating cartridge: cartridge_a ===
  → Phase 1: Initialization Phase
    ✓ Phase completed successfully
    📄 Log: /home/user/MiCo/logs/session_2026-01-27_14-30-45/cartridge_a_phase_1_init.log
  → Phase 2: Dependency Resolution
    ✓ Phase completed successfully
    📄 Log: /home/user/MiCo/logs/session_2026-01-27_14-30-45/cartridge_a_phase_2_dep_res.log
=== Completed migration for: cartridge_a ===

📊 Summary report created: /home/user/MiCo/logs/session_2026-01-27_14-30-45/SUMMARY.txt
📁 All logs saved to: /home/user/MiCo/logs/session_2026-01-27_14-30-45
```

## Cartridge Organization

### Multiple Cartridges (Default)

Place all cartridges in a parent directory:

```
/home/user/cartridges/
├── cartridge_a/
│   ├── build.gradle
│   └── src/
├── cartridge_b/
│   ├── build.gradle
│   └── src/
└── cartridge_c/
    ├── build.gradle
    └── src/
```

Run with:
```bash
./gradlew run --args="-p /home/user/cartridges -a claude_code"
```

### Single Cartridge Mode

For a single cartridge, use the `-s` flag:

```bash
./gradlew run --args="-p /home/user/cartridges/cartridge_a -a claude_code -s"
```

## Customization

### Adding New Phases

1. Create instruction file: `phases/instructions/Phase_X.md`
2. Add phase to `phases/config.json`:

```json
{
  "name": "My Custom Phase",
  "instructions": "Phase_X.md",
  "id": "custom",
  "order": 3,
  "inputs": {
    "cartridge_path": "path/to/cartridge",
    "custom_input": "custom value"
  }
}
```

3. Update `Migrator.getInputValue()` if using custom inputs

### Modifying Exclusion Patterns

Edit `Migrator.generateDependenciesList()`:

```java
Set<String> exclusions = Set.of(
    "com.intershop.", 
    "java.", 
    "javax.",
    "your.custom.exclusion."
);
```

## Troubleshooting

### Issue: "Agent not found"

**Solution:** Ensure the AI agent CLI is installed and in your PATH:
```bash
# Test GitHub Copilot
copilot --version

# Test Claude Code
claude --version
```

### Issue: "Permission denied" errors

**Solution:** The tool uses `--dangerously-skip-permissions` for Claude Code to automate file operations. Ensure you trust the code being migrated.

For Copilot, ensure `--allow-all-tools` is set (done automatically).

### Issue: Phase fails with exit code

**Solution:** 
1. Check the phase-specific log file (path printed in console)
2. Review the instruction file for that phase
3. Verify the AI agent has access to the cartridge directory
4. Check the cartridge summary log for context

### Issue: "No cartridges found"

**Solution:**
- Verify the path is correct
- Check that each cartridge directory contains a `build.gradle` file
- Use `-s` flag if targeting a single cartridge

### Issue: Build verification reports FAILED or SKIPPED

The build verification phase records a per-cartridge result in
`SUMMARY.txt`.

**FAILED:** the cartridge did not compile after migration.
1. Open the cartridge's `*_phase_6_build_verify.log` for the full Gradle output.
2. Fix the reported compiler errors, then re-run `./gradlew compileJava` in the
   cartridge directory to confirm.

**SKIPPED:** no Gradle wrapper (`gradlew`) was found in the cartridge or any
ancestor directory, so there was no build environment to verify against.
- Run MiCo from within your ICM project so a wrapper is discoverable, or verify
  the build manually.
- The Gradle task can be changed via the `build_task` input in
  `phases/config.json` (default `compileJava`; use `build` to also run tests).

## Model Selection

**Note:** The exact set of selectable models depends on the version of the AI
agent CLI you have installed and on your account/subscription entitlements.
Run the agent's own help (e.g. `copilot --help` or `claude --help`) to see the
models currently available to you. The values below are examples.

### GitHub Copilot

If no model is passed with `-m`, MiCo defaults to **`gpt-4.1`**.

Example models:
- `gpt-4.1` (default)
- `gpt-5`
- `claude-sonnet-4.6`

```bash
./gradlew run --args="-p /path -a copilot -m gpt-4.1"
```

### Claude Code

If no model is passed with `-m`, the Claude Code CLI uses its own default model.

Example models:
- `claude-sonnet-4-6` (recommended)
- `opus`
- `sonnet`
- `haiku`

```bash
./gradlew run --args="-p /path -a claude_code -m claude-sonnet-4-6"
```

## Best Practices

✅ **Backup First** - Always backup your cartridges before migration
✅ **Version Control** - Use Git to track changes
✅ **Review Logs** - Check detailed logs after migration
✅ **Test Build** - Run `gradle build` on migrated cartridges
✅ **Incremental Migration** - Start with one cartridge to validate
✅ **Custom Phases** - Add project-specific phases as needed

## Development

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Clean Build

```bash
./gradlew clean build
```

### Create Distribution

```bash
./gradlew installDist
```

Binary will be in `build/install/MiCo/bin/`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

[Add your license information here]

## Support

For issues, questions, or contributions:
- Check existing logs in `logs/` directory
- Create an issue in the repository

## Changelog

### Version 1.1.0
- Added Phase 6: Build Verification — compiles each migrated cartridge with
  Gradle and records a per-cartridge PASSED / FAILED / SKIPPED result in `SUMMARY.txt`
  single-cartridge migration in an IDE, reusing the same phase instructions
- Corrected the model list and documented the default model (`gpt-4.1`)
- Stopped tracking IDE files (`.idea/`) in Git

### Version 1.0.0
- Initial release
- Support for GitHub Copilot and Claude Code
- Dynamic phase configuration
- Comprehensive logging system
- Multi-cartridge batch processing
- Automatic import scanning and dependency resolution
