# Phase 6: Build Verification (Native)

## Overview

This is a **native phase** that runs directly in Java (without an AI agent). It
compiles the migrated cartridge with Gradle to confirm the migration produced
code that actually builds, turning "the agent exited successfully" into a real
pass/fail signal.

## Cartridge Path

Cartridge Path: [CARTRIDGE_PATH]
Cartridge Name: [CARTRIDGE_NAME]

## What This Phase Does

The `BuildVerifier` class:

1. Locates a Gradle wrapper (`gradlew` / `gradlew.bat`) by searching the
   cartridge directory and its ancestor directories. Running the wrapper from
   within the cartridge directory lets Gradle discover the surrounding
   multi-project / composite build, so cartridge dependencies are resolved and
   only the affected cartridge is compiled.
2. Executes the configured Gradle task (default: `compileJava`) with
   `--console=plain -s`.
3. Records the outcome per cartridge:
   - **PASSED** — the Gradle task completed with exit code `0`.
   - **FAILED** — the Gradle task returned a non-zero exit code (or could not be
     executed). This is recorded, but does **not** abort the overall session.
   - **SKIPPED** — no Gradle wrapper was found in the cartridge or any ancestor
     directory, so no build environment is available to verify against.

## Configuration

The Gradle task can be changed via the `build_task` input in
`phases/config.json`:

```json
{
  "name": "Build Verification",
  "instructions": "Phase_6.md",
  "id": "build_verify",
  "order": 6,
  "inputs": {
    "cartridge_path": "path/to/cartridge",
    "cartridge_name": "name of the cartridge",
    "build_task": "compileJava"
  }
}
```

Use a heavier task (e.g. `build`) for stricter verification that also runs
tests, or a lighter one (e.g. `compileJava`) for a fast compile check.

## Output

The per-cartridge status (PASSED / FAILED / SKIPPED) and aggregate counts are
written to `SUMMARY.txt` in the session log directory, and full Gradle output is
captured in the phase log file for the cartridge.
