# Phase 3: Code Migration (Native)

## Overview

This is a **native phase** that runs Java code directly (without AI agent) to perform automated source code transformations.

## Cartridge Path

Cartridge Path: [CARTRIDGE_PATH]
Cartridge Name: [CARTRIDGE_NAME]

## What This Phase Does

The `CodeMigrator` class recursively processes all `.java` files in the cartridge and performs:

### 1. javax → jakarta Migrations

- `javax.inject.*` → `jakarta.inject.*`
- `javax.ws.rs.*` → `jakarta.ws.rs.*`
- `javax.xml.bind.*` → `jakarta.xml.bind.*`
- `javax.annotation.*` → `jakarta.annotation.*`
- `javax.servlet.*` → `jakarta.servlet.*`

### 2. JUnit 4 → JUnit 5

- `@Test` (org.junit) → `@Test` (org.junit.jupiter.api)
- `@Before` → `@BeforeEach`
- `@After` → `@AfterEach`
- `@BeforeClass` → `@BeforeAll`
- `@AfterClass` → `@AfterAll`
- `@Ignore` → `@Disabled`
- `@RunWith` → `@ExtendWith`
- `Assert.*` → `Assertions.*`
- `Assume.*` → `Assumptions.*`

### 3. Mockito Upgrades

- `MockitoJUnitRunner` → `MockitoExtension`
- `verifyZeroInteractions()` → `verifyNoInteractions()`
- `MockitoAnnotations.initMocks()` → `MockitoAnnotations.openMocks()`

### 4. Apache Commons Package Changes

- `org.apache.commons.lang.*` → `org.apache.commons.lang3.*`
- `org.apache.commons.collections.*` → `org.apache.commons.collections4.*`

### 5. REST Assured

- `com.jayway.restassured.*` → `io.restassured.*`

### 6. Intershop-Specific Changes

- `com.intershop.beehive.objectgraph.guice.test.*` → `com.intershop.platform.objectgraph.testrule.*`
- `com.intershop.sellside.rest.common.patch.PATCH` → `jakarta.ws.rs.PATCH`
- `com.intershop.sellside.rest.common.v1.capi.resourceobject.common.MoneyRO` → `com.intershop.component.rest.resources.v1.capi.resourceobject.MoneyRO`
- `com.intershop.soennecken.sellside.rest.basket.v1.capi.request.basket.BasketItemGetRequest` → `com.intershop.sellside.rest.basket.v1.capi.request.basket.BasketItemGetRequest`
- `com.intershop.beehive.orm.internal.jdbc.JDBCConnection` → `com.intershop.beehive.orm.capi.jdbc.JDBCConnection`
- `com.intershop.beehive.core.internal.process.xml.Chain` → `com.intershop.xsd.processchain.v1.Chain`

### 7. Import Cleanup

- Removes unused imports
- Organizes imports alphabetically by category:
  - Static imports
  - Java standard library
  - Jakarta EE
  - Third-party (org.*)
  - Third-party (com.*)
  - Other

## Execution

This phase is executed natively by the `Migrator` class:

```java
CodeMigrator migrator = new CodeMigrator(cartridge.getPath());
migrator.migrate();
```

## Output

```text
For each Java file processed, the phase will:

- Show a ✓ if changes were made
- Show a - if no changes were needed
- Show ✗ if an error occurred

Statistics are logged at the end:

- Files processed
- Errors encountered

```
