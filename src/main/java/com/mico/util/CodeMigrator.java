package com.mico.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Custom CodeMigrator that performs Java source code transformations
 * without using OpenRewrite. Handles:
 * - javax → jakarta migrations
 * - JUnit 4 → JUnit 5
 * - Mockito upgrades
 * - Apache Commons package changes
 * - REST Assured package changes
 * - Intershop-specific class changes
 * - Import cleanup and organization
 */
public class CodeMigrator {

    // ============================================================================
    // PACKAGE MIGRATIONS
    // ============================================================================

    private static final Map<Pattern, String> PACKAGE_MIGRATIONS = Map.ofEntries(
        // javax → jakarta (most common EE packages)
        Map.entry(Pattern.compile("import javax\\.inject\\."), "import jakarta.inject."),
        Map.entry(Pattern.compile("import javax\\.ws\\.rs\\."), "import jakarta.ws.rs."),
        Map.entry(Pattern.compile("import javax\\.xml\\.bind\\."), "import jakarta.xml.bind."),
        Map.entry(Pattern.compile("import javax\\.annotation\\."), "import jakarta.annotation."),
        Map.entry(Pattern.compile("import javax\\.servlet\\."), "import jakarta.servlet."),

        // Apache Commons
        Map.entry(Pattern.compile("import org\\.apache\\.commons\\.lang\\."), "import org.apache.commons.lang3."),
        Map.entry(Pattern.compile("import org\\.apache\\.commons\\.collections\\."), "import org.apache.commons.collections4."),

        // REST Assured
        Map.entry(Pattern.compile("import com\\.jayway\\.restassured\\."), "import io.restassured."),

        // Intershop ObjectGraph
        Map.entry(Pattern.compile("import com\\.intershop\\.beehive\\.objectgraph\\.guice\\.test\\."),
                  "import com.intershop.platform.objectgraph.testrule.")
    );

    // ============================================================================
    // FULLY QUALIFIED CLASS NAME MIGRATIONS
    // ============================================================================

    private static final Map<String, String> CLASS_MIGRATIONS = Map.ofEntries(
        // JUnit 5 → JUnit 4 - Annotations
        Map.entry("org.junit.jupiter.api.Test", "org.junit.Test"),
        Map.entry("org.junit.jupiter.api.BeforeEach", "org.junit.Before"),
        Map.entry("org.junit.jupiter.api.AfterEach", "org.junit.After"),
        Map.entry("org.junit.jupiter.api.BeforeAll", "org.junit.BeforeClass"),
        Map.entry("org.junit.jupiter.api.AfterAll", "org.junit.AfterClass"),
        Map.entry("org.junit.jupiter.api.Disabled", "org.junit.Ignore"),
        Map.entry("org.junit.jupiter.api.extension.ExtendWith", "org.junit.runner.RunWith"),
        Map.entry("org.junit.jupiter.api.extension.RegisterExtension", "org.junit.rules.TestName"),

        // JUnit 5 → JUnit 4 - Assertions & Assumptions
        Map.entry("org.junit.jupiter.api.Assertions", "org.junit.Assert"),
        Map.entry("org.junit.jupiter.api.Assumptions", "org.junit.Assume"),
        Map.entry("org.hamcrest.MatcherAssert.assertThat", "org.junit.Assert.assertThat"),

        // Mockito
        Map.entry("org.mockito.runners.MockitoJUnitRunner", "org.mockito.junit.jupiter.MockitoExtension"),
        Map.entry("org.mockito.junit.MockitoJUnitRunner", "org.mockito.junit.jupiter.MockitoExtension"),
        Map.entry("org.mockito.MockitoAnnotations", "org.mockito.MockitoAnnotations"), // Keep as-is but usage changes
        Map.entry("org.mockito.Mock", "org.mockito.Mock"),
        Map.entry("org.mockito.InjectMocks", "org.mockito.InjectMocks"),

        // Intershop-specific
        Map.entry("com.intershop.sellside.rest.common.patch.PATCH", "jakarta.ws.rs.PATCH"),
        Map.entry("com.intershop.sellside.rest.common.v1.capi.resourceobject.common.MoneyRO",
                  "com.intershop.component.rest.resources.v1.capi.resourceobject.MoneyRO"),
        Map.entry("com.intershop.soennecken.sellside.rest.basket.v1.capi.request.basket.BasketItemGetRequest",
                  "com.intershop.sellside.rest.basket.v1.capi.request.basket.BasketItemGetRequest"),
        Map.entry("com.intershop.beehive.orm.internal.jdbc.JDBCConnection",
                  "com.intershop.beehive.orm.capi.jdbc.JDBCConnection"),
        Map.entry("com.intershop.beehive.core.internal.process.xml.Chain",
                  "com.intershop.xsd.processchain.v1.Chain")
    );

    // ============================================================================
    // ANNOTATION MIGRATIONS (simple name replacements)
    // ============================================================================

    private static final Map<String, String> ANNOTATION_MIGRATIONS = Map.ofEntries(
        Map.entry("@BeforeEach", "@Before"),
        Map.entry("@AfterEach", "@After"),
        Map.entry("@BeforeAll", "@BeforeClass"),
        Map.entry("@AfterAll", "@AfterClass"),
        Map.entry("@Disabled", "@Ignore"),
        Map.entry("@ExtendWith", "@RunWith"),
        Map.entry("@RegisterExtension", "@Rule")
    );

    // ============================================================================
    // STATIC IMPORT MIGRATIONS
    // ============================================================================

    private static final Map<Pattern, String> STATIC_IMPORT_MIGRATIONS = Map.ofEntries(
        // JUnit 5 Assertions static imports → JUnit 4 Assert
        Map.entry(Pattern.compile("^import static\\s+org\\.junit\\.jupiter\\.api\\.Assertions\\."), "import static org.junit.Assert."),
        Map.entry(Pattern.compile("^import static\\s+org\\.junit\\.jupiter\\.api\\.Assumptions\\."), "import static org.junit.Assume.")
    );

    // ============================================================================
    // METHOD CALL MIGRATIONS
    // ============================================================================

    private static final Map<Pattern, String> METHOD_MIGRATIONS = Map.ofEntries(
        // Mockito (keeping these as-is since Mockito 1→4 is different from JUnit 5→4)
        Map.entry(Pattern.compile("\\bverifyZeroInteractions\\s*\\("), "verifyNoInteractions("),
        Map.entry(Pattern.compile("\\bMockitoAnnotations\\.initMocks\\("), "MockitoAnnotations.openMocks("),

        // JUnit 5 → JUnit 4 method calls
        Map.entry(Pattern.compile("\\bAssertions\\.assert"), "Assert.assert"),
        Map.entry(Pattern.compile("\\bAssumptions\\.assume"), "Assume.assume")
    );

    // ============================================================================
    // PATTERNS TO EXCLUDE FROM IMPORTS (standard Java/Jakarta that shouldn't be explicitly imported)
    // ============================================================================

    private static final Set<String> EXCLUDED_IMPORTS = Set.of(
        "java.lang.",
        "java.util.",
        "java.io.",
        "java.nio.",
        "java.net.",
        "java.time.",
        "java.math.",
        "java.text.",
        "java.util.function.",
        "java.util.stream.",
        "jakarta.annotation.", // Generated annotations
        "jakarta.inject.",     // Generated annotations
        "org.junit.jupiter.api.Assertions.", // Static imports handled separately
        "org.junit.jupiter.api.Assumptions."
    );

    private final Path cartridgePath;
    private final List<String> processedFiles;
    private final List<String> errors;

    public CodeMigrator(String cartridgePath) {
        this.cartridgePath = Paths.get(cartridgePath);
        this.processedFiles = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    /**
     * Main entry point - migrates all Java files in the cartridge
     */
    public void migrate() {
        System.out.println("Starting code migration for: " + cartridgePath);

        try (Stream<Path> paths = Files.walk(cartridgePath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(this::migrateFile);
        } catch (IOException e) {
            errors.add("Failed to walk directory tree: " + e.getMessage());
        }

        System.out.println("Migration complete. Processed " + processedFiles.size() + " files.");
        if (!errors.isEmpty()) {
            System.err.println("Errors encountered: " + errors.size());
            errors.forEach(System.err::println);
        }
    }

    /**
     * Migrates a single Java file
     */
    private void migrateFile(Path javaFile) {
        try {
            String originalContent = Files.readString(javaFile);
            String migratedContent = migrateContent(originalContent);

            // Only write if content changed
            if (!originalContent.equals(migratedContent)) {
                Files.writeString(javaFile, migratedContent);
                System.out.println("  ✓ Migrated: " + cartridgePath.relativize(javaFile));
            } else {
                System.out.println("  - No changes: " + cartridgePath.relativize(javaFile));
            }

            processedFiles.add(javaFile.toString());

        } catch (IOException e) {
            String error = "Failed to migrate " + javaFile + ": " + e.getMessage();
            errors.add(error);
            System.err.println("  ✗ " + error);
        }
    }

    /**
     * Performs all transformations on file content
     */
    private String migrateContent(String content) {
        String result = content;

        // 1. Package migrations
        result = migratePackages(result);

        // 1.5. Static import migrations
        result = migrateStaticImports(result);

        // 2. Class name migrations in imports
        result = migrateImportedClasses(result);

        // 3. Annotation replacements (outside comments/strings)
        result = migrateAnnotations(result);

        // 4. Method call migrations
        result = migrateMethodCalls(result);

        // 5. Remove unused imports and organize
        result = cleanupImports(result);

        return result;
    }

    /**
     * Migrates package declarations in imports
     */
    private String migratePackages(String content) {
        String result = content;

        for (Map.Entry<Pattern, String> entry : PACKAGE_MIGRATIONS.entrySet()) {
            result = entry.getKey().matcher(result).replaceAll(entry.getValue());
        }

        return result;
    }

    /**
     * Migrates static imports
     */
    private String migrateStaticImports(String content) {
        String result = content;

        for (Map.Entry<Pattern, String> entry : STATIC_IMPORT_MIGRATIONS.entrySet()) {
            Pattern pattern = entry.getKey();
            String replacement = entry.getValue();

            // Only apply the pattern if it's for JUnit 5 → JUnit 4 migration
            // Skip if the line already contains JUnit 4 (plain org.junit, not jupiter)
            Matcher lineMatcher = Pattern.compile("^import.*$", Pattern.MULTILINE).matcher(result);
            StringBuffer sb = new StringBuffer();

            while (lineMatcher.find()) {
                String line = lineMatcher.group(0);
                // Only replace if this is a JUnit 5 import (jupiter), not JUnit 4
                if (line.contains("org.junit.jupiter")) {
                    lineMatcher.appendReplacement(sb, pattern.matcher(line).replaceAll(replacement));
                } else {
                    lineMatcher.appendReplacement(sb, line);
                }
            }
            lineMatcher.appendTail(sb);
            result = sb.toString();
        }

        return result;
    }

    /**
     * Migrates fully qualified class names in import statements
     */
    private String migrateImportedClasses(String content) {
        // Extract import statements
        Pattern importPattern = Pattern.compile("^import\\s+([^;]+);", Pattern.MULTILINE);
        Matcher matcher = importPattern.matcher(content);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String fullClassName = matcher.group(1).trim();
            String migrated = fullClassName;

            // Skip if already using JUnit 4 (plain org.junit) - don't migrate JUnit 4 to JUnit 4
            // Also skip jakarta imports
            if ((fullClassName.startsWith("org.junit.") && !fullClassName.startsWith("org.junit.jupiter")) ||
                fullClassName.startsWith("jakarta.")) {
                matcher.appendReplacement(sb, "import " + fullClassName + ";");
                continue;
            }

            // Check if this import needs migration (JUnit 5 → JUnit 4)
            // Use exact match first, then prefix match for inner classes
            for (Map.Entry<String, String> entry : CLASS_MIGRATIONS.entrySet()) {
                String key = entry.getKey();

                // Exact match
                if (fullClassName.equals(key)) {
                    migrated = entry.getValue();
                    break;
                }
                // Prefix match for inner classes (e.g., org.junit.jupiter.api.Assertions.* → org.junit.Assert.*)
                else if (fullClassName.startsWith(key + ".")) {
                    migrated = entry.getValue() + fullClassName.substring(key.length());
                    break;
                }
            }

            matcher.appendReplacement(sb, "import " + migrated + ";");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Migrates annotation names
     */
    private String migrateAnnotations(String content) {
        String result = content;

        for (Map.Entry<String, String> entry : ANNOTATION_MIGRATIONS.entrySet()) {
            // Only replace annotations at the beginning of lines or after whitespace
            Pattern pattern = Pattern.compile("(\\s)" + Pattern.quote(entry.getKey()) + "(\\s|\\()");
            result = pattern.matcher(result).replaceAll("$1" + entry.getValue() + "$2");
        }

        return result;
    }

    /**
     * Migrates method calls
     */
    private String migrateMethodCalls(String content) {
        String result = content;

        for (Map.Entry<Pattern, String> entry : METHOD_MIGRATIONS.entrySet()) {
            result = entry.getKey().matcher(result).replaceAll(entry.getValue());
        }

        return result;
    }

    /**
     * Removes unused imports and organizes them alphabetically
     */
    private String cleanupImports(String content) {
        // Extract all imports
        List<String> imports = new ArrayList<>();
        Pattern importPattern = Pattern.compile("^import\\s+([^;]+);", Pattern.MULTILINE);
        Matcher matcher = importPattern.matcher(content);

        while (matcher.find()) {
            imports.add(matcher.group(0));
        }

        if (imports.isEmpty()) {
            return content;
        }

        // Remove import statements and surrounding blank lines from content
        String withoutImports = content.replaceAll("(?m)^import\\s+[^;]+;\\s*", "");

        // Sort imports
        imports.sort(String::compareTo);

        // Group by category
        List<String> staticImports = new ArrayList<>();
        List<String> javaImports = new ArrayList<>();
        List<String> jakartaImports = new ArrayList<>();
        List<String> orgImports = new ArrayList<>();
        List<String> comImports = new ArrayList<>();
        List<String> otherImports = new ArrayList<>();

        for (String imp : imports) {
            if (imp.contains("static ")) {
                staticImports.add(imp);
            } else if (imp.startsWith("import java.")) {
                javaImports.add(imp);
            } else if (imp.startsWith("import jakarta.")) {
                jakartaImports.add(imp);
            } else if (imp.startsWith("import org.")) {
                orgImports.add(imp);
            } else if (imp.startsWith("import com.")) {
                comImports.add(imp);
            } else {
                otherImports.add(imp);
            }
        }

        // Rebuild the import section with proper spacing
        List<String> importLines = new ArrayList<>(staticImports);
        isEmpty(staticImports, javaImports, jakartaImports, importLines);
        isEmpty(jakartaImports, orgImports, comImports, importLines);
        if (!comImports.isEmpty() && !otherImports.isEmpty()) {
            importLines.add(""); // blank line between categories
        }
        importLines.addAll(otherImports);

        String importBlock = String.join("\n", importLines);

        // Find the package declaration and insert imports after it
        Pattern packagePattern = Pattern.compile("^(package\\s+[^;]+;)\\s*", Pattern.MULTILINE);
        Matcher packageMatcher = packagePattern.matcher(withoutImports);

        if (packageMatcher.find()) {
            return packageMatcher.replaceFirst("$1\n\n" + importBlock + "\n");
        }

        return importBlock + "\n" + withoutImports;
    }

    public void isEmpty(List<String> staticImports, List<String> javaImports, List<String> jakartaImports, List<String> importLines) {
        if (!staticImports.isEmpty() && !javaImports.isEmpty()) {
            importLines.add(""); // blank line between categories
        }
        importLines.addAll(javaImports);
        if (!javaImports.isEmpty() && !jakartaImports.isEmpty()) {
            importLines.add(""); // blank line between categories
        }
        importLines.addAll(jakartaImports);
    }

    /**
     * Get migration statistics
     */
    public MigrationStats getStats() {
        return new MigrationStats(processedFiles.size(), errors.size());
    }

    public record MigrationStats(int filesProcessed, int errorCount) {}
}