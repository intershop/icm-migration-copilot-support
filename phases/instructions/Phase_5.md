# ICM Move cartridge files — Phase 5

## Role

You are an AI agent that migrates files and folders using former locations to new locations so they become java resources and bundles.

---

## Inputs

| Parameter | Description |
|-----------|-------------|

| `[CARTRIDGE_PATH]` | Directory containing the cartridge |
| `[CARTRIDGE_NAME]` | Name of the cartridge |

---

## Workflow

### Step 1: Move cartridge descriptor and cartridge resources like pipelines, queries, templates, or other

- if exists, move file `staticfiles/share/system/config/cartridges/[CARTRIDGE_NAME].properties` to `src/main/resources/cartridges/[CARTRIDGE_NAME].properties
- if exists, move all files and folders recursively in `edl/*` `src/main/resources/resources/[CARTRIDGE_NAME]/*`.
- if exists, move all files and folders recursively in `staticfiles/cartridge/templates/*` to `src/main/[CARTRIDGE_NAME]/*`
- if exists, move all files and folders recursively in `staticfiles/cartridge/*` to `src/main/resources/resources/[CARTRIDGE_NAME]/*`.

Examples

| Old location | New location |
|--------------|--------------|

| `staticfiles/share/system/config/cartridges/[CARTRIDGE_NAME].properties` | `src/main/resources/cartridges/[CARTRIDGE_NAME].properties` |
| `staticfiles/cartridge/dbinit.properties` | `src/main/resources/resources/[CARTRIDGE_NAME]/dbinit.properties` |
| `staticfiles/cartridge/migration.properties` | `src/main/resources/resources/[CARTRIDGE_NAME]/migration.properties` |
| `staticfiles/cartridge/migration.properties` | `src/main/resources/resources/[CARTRIDGE_NAME]/migration.properties` |
| `staticfiles/cartridge/pipelines/Pipeline.pipeline` | `src/main/resources/resources/[CARTRIDGE_NAME]/pipelines/APipeline.pipeline` |
| `staticfiles/cartridge/queries/Query.pipeline` | `src/main/resources/resources/[CARTRIDGE_NAME]/queries/Query.pipeline` |
| `staticfiles/cartridge/templates/default/t1.isml` | `src/main/isml/[CARTRIDGE_NAME]/default/t1.isml` |
| `edl/com/test/MyType.edl` | `src/main/resources/resources/[CARTRIDGE_NAME]/edl/com/test/MyType.edl` |

**Additional step for templates** If at least one template existed in `staticfiles/cartridge/templates/` modify the build file `build.gradle.kts`:

- Plugin "java" and "com.intershop.gradle.isml" have to be present

```kotlin
plugins {
    java
    // ..
    id("com.intershop.gradle.isml")
    // ..
}
```

After this step, folder `staticfiles/cartridge` should be empty. If the whole folder `staticfiles` is empty or only contains empty folders, it has to be removed.

### Step 2: Move site files and apply preparation steps

- if exists, recursively move folder `staticfiles/share/sites/` to `src/main/resources/resources/[CARTRIDGE_NAME]/`
- if sites folder exists, create preparation step ´pre.Class0=com.intershop.site.dbinit.SiteContentPreparer´

A preparation step is registered in file ´src/main/resources/resources/[CARTRIDGE_NAME]/dbinit.properties´.
If no file ´src/main/resources/resources/[CARTRIDGE_NAME]/dbinit.properties´ exists yet, create a new one, otherwise edit the existing one.
Add the step as new line in the properties file.

### Step 3: Move process chains

- if exists, recursively move folder `staticfiles/share/processchain/` to `src/main/resources/resources/[CARTRIDGE_NAME]/sites/processchain/`

### Step 4: Move configuration

- if exists, recursively move folder `staticfiles/share/stystem/config/` to `src/main/resources/resources/[CARTRIDGE_NAME]/config/`

## Output

After processing all files:

```text
✅ Cartridge resource file processing complete: [CARTRIDGE_NAME]

Files processed: [NUMBER]
Files modified: [NUMBER]
Sites register: [NUMBER]
Files created: [NUMBER]
Templates moved: [NUMBER]

Status: ✅ Cartridge ready to build
```
