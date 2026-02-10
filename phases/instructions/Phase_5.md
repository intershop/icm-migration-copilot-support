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

### Step 1: Move cartridge descriptor and cartridge resources like pipelines, queries

- if exists, move file `staticfiles/share/system/config/cartridges/[CARTRIDGE_NAME].properties` to `src/main/resources/cartridges/[CARTRIDGE_NAME].properties
- move all files and folders recursively in `staticfiles/cartridge/*` to `src/main/resources/resources/[CARTRIDGE_NAME]/*`.

Examples

| Old location | New location |
|--------------|--------------|

| `staticfiles/share/system/config/cartridges/[CARTRIDGE_NAME].properties` | `src/main/resources/cartridges/[CARTRIDGE_NAME].properties` |
| `staticfiles/cartridge/dbinit.properties` | `src/main/resources/resources/[CARTRIDGE_NAME]/dbinit.properties` |
| `staticfiles/cartridge/migration.properties` | `src/main/resources/resources/[CARTRIDGE_NAME]/migration.properties` |
| `staticfiles/cartridge/migration.properties` | `src/main/resources/resources/[CARTRIDGE_NAME]/migration.properties` |
| `staticfiles/cartridge/cartridge/pipelines/Pipeline.pipeline` | `src/main/resources/resources/[CARTRIDGE_NAME]/pipelines/APipeline.pipeline` |
| `staticfiles/cartridge/cartridge/queries/Query.pipeline` | `src/main/resources/resources/[CARTRIDGE_NAME]/queries/Query.pipeline` |

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

Status: ✅ Cartridge ready to build
```
