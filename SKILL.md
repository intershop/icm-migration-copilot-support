---
name: icm-cartridge-migration
description: >-
  Migrate a single Intershop Commerce Management (ICM) cartridge from ICM 7.10 to
  ICM 11+. Converts the legacy Groovy build.gradle to build.gradle.kts (Kotlin
  DSL), resolves cartridge dependencies from Java imports, applies deterministic
  source-code transformations (javax→jakarta, JUnit 4→5, Mockito, Apache Commons,
  RestAssured, ICM-specific relocations), processes resources and verifies the
  cartridge compiles. Use when a user asks to migrate/upgrade an ICM 7.10
  cartridge to ICM 11+, or mentions ICM 11 cartridge migration.
  Trigger: /icm-cartridge-migration, or "run icm-cartridge-migration".
---

# ICM 7.10 → 11+ Cartridge Migration Skill

This skill packages the same migration knowledge used by the **MiCo** batch
runner so that **any AI agent (GitHub Copilot, Claude Code, …) can run the
migration for a single cartridge directly in an IDE / chat session**, without
launching the Java CLI.

- Use **this skill** for interactive, one-cartridge migration.
- Use the **MiCo CLI** (see [README.md](README.md)) for unattended batch
  migration of many cartridges.

Both share one source of truth: the phase instruction files under
[`phases/instructions/`](phases/instructions). Always read the referenced file
for the authoritative, up-to-date rules and mapping tables — do not rely on
memory for the dependency mappings.

## ⚠️ Safety

You will create, overwrite and delete files in the target cartridge (the old
`build.gradle` is deleted; Java sources are rewritten in place).

- Only operate on a cartridge that is under version control (Git) or backed up.
- Let the user review the changes before they commit or ship.

## Inputs

- `CARTRIDGE_PATH` — absolute path to the single cartridge directory.
- `CARTRIDGE_NAME` — the cartridge (directory) name.

## Workflow

Run the phases in order for the one cartridge. For each phase, open the linked
instruction file and follow it exactly, substituting `[CARTRIDGE_PATH]` and
`[CARTRIDGE_NAME]`.

### Phase 1 — Gradle build file migration
Follow [`phases/instructions/Phase_1.md`](phases/instructions/Phase_1.md):
read the old `build.gradle`, scan Java imports, generate `build.gradle.kts`
(Kotlin DSL), then delete the old `build.gradle`.

### Phase 2 — Dependency resolution
Follow [`phases/instructions/Phase_2.md`](phases/instructions/Phase_2.md):
map the cartridge's Java imports to ICM 11+ cartridge dependencies using the
mapping tables in that file (e.g. `com.intershop.beehive.*` →
`cartridge("com.intershop.platform:…")`), add them to `build.gradle.kts`,
de-duplicate and sort. If the cartridge has no Java code, this phase can be
skipped.

To collect the imports:
```bash
find [CARTRIDGE_PATH] -name "*.java" -exec grep -h "^import" {} + | sort | uniq
```

### Phase 3 — Deterministic code migration
Follow [`phases/instructions/Phase_3.md`](phases/instructions/Phase_3.md). These
are **mechanical, rule-based** transforms (javax→jakarta, JUnit 4→5, Mockito,
Apache Commons, RestAssured, ICM-specific relocations). Apply them exactly as
specified — do not invent additional rewrites.

> Tip: this phase is implemented deterministically in `CodeMigrator`. For large
> or many cartridges, prefer running the MiCo CLI (which executes this phase as
> native Java) instead of rewriting by hand.

### Phase 4 — Code fix
Follow [`phases/instructions/Phase_4.md`](phases/instructions/Phase_4.md) to
resolve remaining compile-affecting issues introduced by the migration.

### Phase 5 — Resource processing
Follow [`phases/instructions/Phase_5.md`](phases/instructions/Phase_5.md) for
resource/config adjustments.

### Phase 6 — Build verification
Compile the cartridge to confirm the migration succeeded. From a directory that
has (or whose ancestor has) a Gradle wrapper:
```bash
cd [CARTRIDGE_PATH] && ./gradlew compileJava --console=plain
```
Running the wrapper from within the cartridge directory lets Gradle resolve the
cartridge's dependencies against the surrounding multi-project build. If there
is no Gradle wrapper available, report build verification as **skipped**.

## Definition of done

- `build.gradle` is gone; a valid `build.gradle.kts` exists with correctly
  mapped cartridge dependencies.
- Source transformations from Phase 3 are applied.
- `./gradlew compileJava` passes (or is explicitly reported as skipped with the
  reason).
