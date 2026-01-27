# ICM Cartridge Dependency Resolution - Phase 2

## Your Role
You are an AI agent that ensures all dependencies are present in the `build.gradle.kts` file. You have received a list of all Java imports used in the cartridge. Your job is to add any missing dependencies so the code compiles successfully.

---

## INPUT

You will receive:
1. **Cartridge path** - The directory containing the cartridge
2. **List of all Java imports** - All unique import statements found in .java files

Cartridge Path: [CARTRIDGE_PATH]
Dependencies List: [DEPENDENCIES_LIST]

Example import list:
```
com.intershop.beehive.core.capi.naming.NamingMgr
com.intershop.component.customer.capi.CustomerBO
com.google.inject.Inject
java.util.List
org.slf4j.Logger
jakarta.ws.rs.GET
io.swagger.v3.oas.annotations.Operation
```

---

## YOUR WORKFLOW

### Step 1: READ the current build.gradle.kts
Read: `[CARTRIDGE_PATH]/build.gradle.kts`

### Step 2: ANALYZE existing dependencies
- Extract ALL dependencies that are already in the file
- Create a set of existing `group:artifact` combinations
- Note which configuration each uses (cartridge, implementation, testImplementation)

### Step 3: ANALYZE the import list
For each import, determine the required dependency. **ONLY add dependencies for imports that are actually present.**

### Step 4: CHECK for each dependency
- For each required dependency from the import list:
  - **Check if it already exists** in the build.gradle.kts
  - **Only add if NOT already present**
  - **Use correct configuration:**
    - Test dependencies (junit, mockito, hamcrest, pf_core_test, pf_unit_test) → `testImplementation()`
    - Platform dependencies (except pf_core_test, pf_unit_test) → `cartridge()`
    - Everything else → `implementation()`
  - **Be conservative:** If you only see `com.fasterxml.jackson.annotation.*`, ONLY add `jackson-annotations`. Don't add `jackson-core` or `jackson-databind`.
  - **Be thorough:** Make sure EVERY import in the list has a corresponding dependency. Don't skip any.

### Step 5: REMOVE duplicates and FIX configurations
- Scan the entire `dependencies {}` block
- Remove ANY duplicate dependency entries (same `group:artifact`)
- **FIX incorrect configurations:**
  - If `pf_core_test` or `pf_unit_test` use `cartridge()` → **MOVE to testImplementation()**
  - If any test library uses `implementation()` → **MOVE to testImplementation()**
- Ensure there is only ONE section of each type (e.g., only one "External dependencies" section)

### Step 6: PRESERVE existing dependencies
**CRITICAL:** Do NOT remove dependencies that were already present from Phase 1!
- **KEEP all `com.intershop:*` dependencies** - These were intentionally added during migration
- **KEEP all business dependencies** like `common-webinterface`, even if no imports match
- Only remove duplicates, not legitimate dependencies

### Step 7: SORT alphabetically
- Sort ALL dependencies alphabetically within each section
- This includes External dependencies and Test dependencies

### Step 8: SAVE the updated file
Overwrite: `[CARTRIDGE_PATH]/build.gradle.kts`

---

## CRITICAL RULES

1. **NEVER add a dependency that already exists**
2. **NEVER create duplicate sections** (e.g., don't have two "External dependencies" sections)
3. **ALWAYS check the entire file** before adding anything
4. **Remove ALL duplicates** before saving
5. **ALL dependencies MUST be alphabetically sorted** within each section
6. **Test platform deps (pf_core_test, pf_unit_test) MUST be testImplementation()**
7. **Preserve the original structure** - only add missing dependencies
8. **One "Test dependencies" section** - includes both platform test deps and regular test deps
9. **ONLY add what you see in imports** - If you only see `jackson-annotation` imports, don't add `jackson-core` or `jackson-databind`
10. **NEVER remove existing Intershop dependencies** - Keep all `com.intershop:*` deps that were added by Phase 1
11. **NEVER remove business dependencies** - Keep deps like `common-webinterface` even if no imports match

---

## IMPORT → DEPENDENCY MAPPINGS

### Standard Java/Jakarta Libraries
| Import Pattern | Dependency |
|----------------|------------|
| `java.*` | (No dependency needed - part of JDK) |
| `javax.inject.*` | `implementation("jakarta.inject:jakarta.inject-api")` |
| `javax.ws.rs.*` | `implementation("jakarta.ws.rs:jakarta.ws.rs-api")` |
| `javax.xml.bind.annotation.*` | `implementation("jakarta.xml.bind:jakarta.xml.bind-api")` |
| `javax.validation.*` | `implementation("jakarta.validation:jakarta.validation-api")` |
| `javax.servlet.*` | `implementation("jakarta.servlet:jakarta.servlet-api")` |
| `javax.annotation.*` | `implementation("jakarta.annotation:jakarta.annotation-api")` |
| `jakarta.inject.*` | `implementation("jakarta.inject:jakarta.inject-api")` |
| `jakarta.ws.rs.*` | `implementation("jakarta.ws.rs:jakarta.ws.rs-api")` |
| `jakarta.xml.bind.*` | `implementation("jakarta.xml.bind:jakarta.xml.bind-api")` |
| `jakarta.validation.*` | `implementation("jakarta.validation:jakarta.validation-api")` |
| `jakarta.servlet.*` | `implementation("jakarta.servlet:jakarta.servlet-api")` |
| `jakarta.annotation.*` | `implementation("jakarta.annotation:jakarta.annotation-api")` |

**CRITICAL:** If you see `javax.xml.bind.annotation.*` imports, you MUST add `implementation("jakarta.xml.bind:jakarta.xml.bind-api")`. This is a common migration dependency.

### Google Libraries
| Import Pattern | Dependency |
|----------------|------------|
| `com.google.inject.*` | `implementation("com.google.inject:guice")` |
| `com.google.common.*` | `implementation("com.google.guava:guava")` |
| `com.google.gson.*` | `implementation("com.google.code.gson:gson")` |

### Logging
| Import Pattern | Dependency |
|----------------|------------|
| `org.slf4j.*` | `implementation("org.slf4j:slf4j-api")` |
| `org.apache.logging.log4j.*` | `implementation("org.apache.logging.log4j:log4j-core")` |

### Apache Commons
| Import Pattern | Dependency |
|----------------|------------|
| `org.apache.commons.lang3.*` | `implementation("org.apache.commons:commons-lang3")` |
| `org.apache.commons.collections4.*` | `implementation("org.apache.commons:commons-collections4")` |
| `org.apache.commons.io.*` | `implementation("commons-io:commons-io")` |

### Jackson (JSON)
| Import Pattern | Dependency |
|----------------|------------|
| `com.fasterxml.jackson.annotation.*` | `implementation("com.fasterxml.jackson.core:jackson-annotations")` |
| `com.fasterxml.jackson.databind.*` | `implementation("com.fasterxml.jackson.core:jackson-databind")` |
| `com.fasterxml.jackson.core.*` | `implementation("com.fasterxml.jackson.core:jackson-core")` |

**IMPORTANT:** Only add Jackson dependencies if the specific imports are present. Don't add all three by default.

### Swagger/OpenAPI
| Import Pattern | Dependency |
|----------------|------------|
| `io.swagger.v3.oas.annotations.*` | `implementation("io.swagger.core.v3:swagger-annotations-jakarta")` |

### Testing
| Import Pattern | Dependency |
|----------------|------------|
| `org.junit.Test` | `testImplementation("junit:junit")` |
| `org.junit.Before` | `testImplementation("junit:junit")` |
| `org.junit.After` | `testImplementation("junit:junit")` |
| `org.junit.Assert.*` | `testImplementation("junit:junit")` |
| `org.junit.jupiter.api.Test` | `testImplementation("org.junit.jupiter:junit-jupiter-api")` |
| `org.junit.jupiter.api.*` | `testImplementation("org.junit.jupiter:junit-jupiter-api")` |
| `org.junit.jupiter.*` | `testImplementation("org.junit.jupiter:junit-jupiter")` |
| `org.mockito.*` | `testImplementation("org.mockito:mockito-core")` |
| `org.hamcrest.*` | `testImplementation("org.hamcrest:hamcrest")` |

**IMPORTANT:** Only add JUnit dependencies based on what's actually imported:
- If you see `org.junit.Test` → add `junit:junit`
- If you see `org.junit.jupiter.api.Test` → add `junit-jupiter-api`
- DO NOT add both - only add what matches the imports!

### Intershop Platform Packages
| Import Pattern | Dependency |
|----------------|------------|
| `com.intershop.beehive.core.*` | `cartridge("com.intershop.platform:core")` |
| `com.intershop.beehive.orm.*` | `cartridge("com.intershop.platform:orm")` |
| `com.intershop.beehivex.core.*` | `cartridge("com.intershop.platform:core")` |
| `com.intershop.component.rest.*` | `cartridge("com.intershop.platform:rest")` |
| `com.intershop.component.service.*` | `cartridge("com.intershop.platform:bc_service")` |
| `com.intershop.component.foundation.*` | `cartridge("com.intershop.platform:bc_foundation")` |
| `com.intershop.component.payment.*` | `cartridge("com.intershop.platform:bc_payment")` |
| `com.intershop.component.order.*` | `cartridge("com.intershop.platform:bc_order")` |
| `com.intershop.tools.eturtle.*` | `cartridge("com.intershop.platform:isml")` |
| `com.intershop.validation.*` | `cartridge("com.intershop.platform:validation")` |

### Intershop Business Packages
| Import Pattern | Dependency |
|----------------|------------|
| `com.intershop.component.product.*` | `cartridge("com.intershop.business:bc_product")` |
| `com.intershop.component.customer.*` | `cartridge("com.intershop.business:bc_customer")` |
| `com.intershop.component.user.*` | `cartridge("com.intershop.business:bc_user")` |
| `com.intershop.component.pricing.*` | `cartridge("com.intershop.business:bc_pricing")` |
| `com.intershop.component.marketing.*` | `cartridge("com.intershop.business:bc_marketing")` |
| `com.intershop.component.catalog.*` | `cartridge("com.intershop.business:bc_catalog")` |
| `com.intershop.component.inventory.*` | `cartridge("com.intershop.business:bc_inventory")` |
| `com.intershop.component.shipping.*` | `cartridge("com.intershop.business:bc_shipping")` |
| `com.intershop.component.tax.*` | `cartridge("com.intershop.business:bc_tax")` |
| `com.intershop.component.address.*` | `cartridge("com.intershop.business:bc_address")` |

### Intershop B2B Packages
| Import Pattern | Dependency |
|----------------|------------|
| `com.intershop.component.costcenter.*` | `cartridge("com.intershop.b2b:bc_costcenter")` |
| `com.intershop.component.quotation.*` | `cartridge("com.intershop.b2b:bc_quotation")` |
| `com.intershop.component.requisition.*` | `cartridge("com.intershop.b2b:bc_requisition")` |

### Intershop Content Packages
| Import Pattern | Dependency |
|----------------|------------|
| `com.intershop.component.content.*` | `cartridge("com.intershop.content:bc_content")` |

### Intershop Application/REST Packages
| Import Pattern | Dependency |
|----------------|------------|
| `com.intershop.business.rest.*` | `cartridge("com.intershop.business:app_sf_rest_customer")` |
| `com.intershop.rest.*` | `cartridge("com.intershop.platform:rest")` |

---

## DEPENDENCY CONFIGURATION RULES

| Group Pattern | Configuration |
|---------------|---------------|
| `com.intershop.platform:*` | `cartridge()` |
| `com.intershop.platform:pf_core_test` | `testImplementation()` |
| `com.intershop.platform:pf_unit_test` | `testImplementation()` |
| `com.intershop.business:*` | `cartridge()` |
| `com.intershop.b2b:*` | `cartridge()` |
| `com.intershop.content:*` | `cartridge()` |
| `com.intershop:*` (no subgroup) | `implementation()` |
| Test libraries (junit, mockito, hamcrest, etc.) | `testImplementation()` |
| Everything else | `implementation()` |

**IMPORTANT:** Test-specific platform dependencies like `pf_core_test` and `pf_unit_test` MUST use `testImplementation()`, NOT `cartridge()`.

---

## DEPENDENCY ORDERING

```kotlin
dependencies {
    // Project dependencies
    cartridge(project(":xxx"))

    // Platform dependencies
    cartridge("com.intershop.platform:xxx")

    // Business dependencies
    cartridge("com.intershop.business:xxx")
    implementation("com.intershop:xxx")

    // B2B dependencies
    cartridge("com.intershop.b2b:xxx")

    // Content dependencies
    cartridge("com.intershop.content:xxx")

    // External dependencies (alphabetically sorted)
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.google.inject:guice")
    implementation("jakarta.inject:jakarta.inject-api")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")

    // Test dependencies (alphabetically sorted)
    testImplementation("com.intershop.platform:pf_core_test")
    testImplementation("com.intershop.platform:pf_unit_test")
    testImplementation("junit:junit")
    testImplementation("org.hamcrest:hamcrest")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
}
```

**CRITICAL:** Dependencies MUST be sorted alphabetically within each section!

---

## IMPORTANT NOTES

1. **Never include versions** - Version numbers are managed centrally
2. **CHECK FOR DUPLICATES FIRST** - Before adding any dependency, scan the entire `dependencies {}` block to check if it already exists
3. **NEVER add duplicates** - If `implementation("com.google.inject:guice")` already exists, DO NOT add it again
4. **ONE section per type** - Do not create multiple "External dependencies" sections. Merge them into one
5. **ALWAYS sort alphabetically** within each section - this is critical!
6. **Test platform dependencies** - `pf_core_test` and `pf_unit_test` MUST be `testImplementation()`, NOT `cartridge()`
7. **Remove empty sections** - Don't include sections that have no dependencies
8. **Keep existing dependencies** - Only add missing ones, don't remove existing ones unless they're duplicates
9. **All test dependencies together** - Test platform deps and regular test deps go in the same "Test dependencies" section
10. **Be conservative with Jackson** - Only add specific Jackson modules that are actually imported. If only `jackson-annotation` imports exist, only add that artifact.
11. **Be conservative with ALL libraries** - Only add what you see in the import list. Don't add "related" dependencies.
12. **PRESERVE Phase 1 dependencies** - NEVER remove Intershop dependencies (like `common-webinterface`) that were added during initial migration. These are legitimate dependencies needed by the cartridge.

---

## COMPLETION

After adding all missing dependencies, removing duplicates, and saving the file, output:

```
✅ Dependency resolution complete: [CARTRIDGE_NAME]

Scanned imports: [NUMBER] unique imports
Dependencies added: [NUMBER]
Dependencies already present: [NUMBER]
Duplicates removed: [NUMBER]

Updated: build.gradle.kts

All imports now have corresponding dependencies with no duplicates.
```

---

## RESOLVE DEPENDENCIES FOR THIS CARTRIDGE:

**Cartridge path:** [CARTRIDGE_PATH]

**All Java imports found:**
[IMPORT_LIST]

