package com.mico.util;


import com.mico.models.Cartridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class JavaImportScanner {

    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+);");

    /**
     * Scans all Java files in the cartridge and collects unique import statements.
     * No filtering is applied by default.
     *
     * @param cartridge the cartridge to scan
     * @return a linked list of unique import statements
     */
    public static LinkedList<String> scanImports(Cartridge cartridge) {
        return scanImports(cartridge, Collections.emptySet());
    }

    /**
     * Scans all Java files in the cartridge and collects unique import statements.
     * Filters out imports that start with any of the provided exclusion prefixes.
     *
     * @param cartridge the cartridge to scan
     * @param exclusionPrefixes a set of package prefixes to exclude (e.g., "com.intershop.", "java.lang.")
     * @return a linked list of unique import statements (excluding those matching the prefixes)
     */
    public static LinkedList<String> scanImports(Cartridge cartridge, Set<String> exclusionPrefixes) {
        Set<String> importSet = new LinkedHashSet<>();
        Path cartridgePath = Path.of(cartridge.getPath());
        int fileCount = 0;

        try (Stream<Path> paths = Files.walk(cartridgePath)) {
            for (Path javaFile : (Iterable<Path>) paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))::iterator) {
                fileCount++;
                try {
                    extractImportsFromFile(javaFile, importSet, exclusionPrefixes);
                } catch (IOException e) {
                    System.err.println("Error reading file: " + javaFile + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error walking through cartridge path: " + cartridgePath + " - " + e.getMessage());
        }

        System.out.println("Scanned " + fileCount + " Java files, found " + importSet.size() + " unique imports");
        return new LinkedList<>(importSet);
    }

    /**
     * Extracts import statements from a single Java file and adds them to the set.
     * Filters out imports matching any of the exclusion prefixes.
     *
     * @param javaFile the Java file to read
     * @param importSet the set to add imports to
     * @param exclusionPrefixes prefixes to exclude from the results
     * @throws IOException if the file cannot be read
     */
    private static void extractImportsFromFile(Path javaFile, Set<String> importSet, Set<String> exclusionPrefixes) throws IOException {
        try (Stream<String> lines = Files.lines(javaFile)) {
            lines.forEach(line -> {
                String trimmedLine = line.trim();
                Matcher matcher = IMPORT_PATTERN.matcher(trimmedLine);
                if (matcher.find()) {
                    String importStatement = matcher.group(1);
                    if (!isExcluded(importStatement, exclusionPrefixes)) {
                        importSet.add(importStatement);
                    }
                }
            });
        }
    }

    /**
     * Checks if an import statement starts with any of the exclusion prefixes.
     *
     * @param importStatement the import statement to check
     * @param exclusionPrefixes the set of prefixes to check against
     * @return true if the import should be excluded, false otherwise
     */
    private static boolean isExcluded(String importStatement, Set<String> exclusionPrefixes) {
        for (String prefix : exclusionPrefixes) {
            if (importStatement.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
