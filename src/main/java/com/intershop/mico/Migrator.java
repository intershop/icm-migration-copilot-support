package com.intershop.mico;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.intershop.mico.agent.Agent;
import com.intershop.mico.models.Cartridge;
import com.intershop.mico.models.Phase;
import com.intershop.mico.repo.CartridgeRepository;
import com.intershop.mico.repo.PhaseRepository;
import com.intershop.mico.util.BuildVerifier;
import com.intershop.mico.util.CodeMigrator;
import com.intershop.mico.util.JavaImportScanner;
import com.intershop.mico.util.MigrationLogger;

public class Migrator {

    private final CartridgeRepository cartridgeRepository;
    private final PhaseRepository phaseRepository;
    private final Supplier<Agent> agentFactory;
    private final MigrationLogger logger;
    private final Map<String, BuildVerifier.Status> buildResults = new LinkedHashMap<>();

    public Migrator(CartridgeRepository cartridgeRepository, PhaseRepository phaseRepository, Supplier<Agent> agentFactory) {
        this.cartridgeRepository = cartridgeRepository;
        this.phaseRepository = phaseRepository;
        this.agentFactory = agentFactory;
        this.logger = new MigrationLogger();
    }

    public void migrate() {
        long startTime = System.currentTimeMillis();
        List<Phase> phases = phaseRepository.getPhases();
        List<Cartridge> cartridges = cartridgeRepository.getCartridges();

        logger.logToMaster("Migration session started");
        logger.logToMaster("Total cartridges: " + cartridges.size());
        logger.logToMaster("Total phases: " + phases.size());

        for (Cartridge cartridge : cartridges) {
            System.out.println("=== Migrating cartridge: " + cartridge.getName() + " ===");
            logger.logToMaster("Starting cartridge: " + cartridge.getName());
            logger.logToCartridgeSummary(cartridge, "Migration started for: " + cartridge.getName());

            for (Phase phase : phases) {
                System.out.println("  → Phase " + phase.getOrder() + ": " + phase.getName());
                logger.logToMaster("  Phase " + phase.getOrder() + ": " + phase.getName());
                logger.logToCartridgeSummary(cartridge, "Starting Phase " + phase.getOrder() + ": " + phase.getName());

                try {
                    Path logFile = logger.getLogFile(cartridge, phase);
                    logger.writeLogHeader(logFile, cartridge, phase);

                    boolean isNativePhase = "code_migration".equals(phase.getId());
                    boolean isBuildVerifyPhase = "build_verify".equals(phase.getId());

                    if (isBuildVerifyPhase) {
                        runBuildVerifyPhase(cartridge, phase, logFile);
                    } else if (isNativePhase) {
                        runNativePhase(cartridge, phase, logFile);
                    } else {
                        Agent agent = agentFactory.get();
                        String instructionTemplate = phaseRepository.getPhaseInstructions(phase);
                        String finalPrompt = preparePrompt(instructionTemplate, phase.getInputs(), cartridge);

                        agent.setPrompt(finalPrompt);

                        ProcessBuilder pb = agent.execute();
                        Process process = logger.executeWithLogging(pb, logFile);
                        
                        agent.postExecution(process);
                        
                        int exitCode = process.waitFor();

                        if (exitCode != 0) {
                            String errorMsg = "Phase failed with exit code: " + exitCode;
                            System.err.println("    ✗ " + errorMsg);
                            logger.logToMaster("  ✗ " + errorMsg);
                            logger.logToCartridgeSummary(cartridge, "✗ Phase " + phase.getOrder() + " failed with exit code: " + exitCode);
                        } else {
                            String successMsg = "Phase completed successfully";
                            System.out.println("    ✓ " + successMsg);
                            logger.logToMaster("  ✓ " + successMsg);
                            logger.logToCartridgeSummary(cartridge, "✓ Phase " + phase.getOrder() + " completed successfully");
                        }
                    }

                    cartridge.setCurrentPhase(phase.getId());
                    System.out.println("    📄 Log: " + logFile.toAbsolutePath());

                } catch (IOException | InterruptedException e) {
                    String errorMsg = "Error executing phase: " + e.getMessage();
                    System.err.println("    ✗ " + errorMsg);
                    logger.logToMaster("  ✗ " + errorMsg);
                    logger.logToCartridgeSummary(cartridge, "✗ Phase " + phase.getOrder() + " error: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            System.out.println("=== Completed migration for: " + cartridge.getName() + " ===\n");
            logger.logToMaster("Completed cartridge: " + cartridge.getName());
            logger.logToCartridgeSummary(cartridge, "Migration completed for: " + cartridge.getName());
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.logToMaster("Migration session completed");
        logger.createSummaryReport(cartridges.size(), phases.size(), duration, buildResults);

        System.out.println("\n📁 All logs saved to: " + logger.getSessionLogDir().toAbsolutePath());
    }

    private String preparePrompt(String instructionTemplate, Map<String, String> inputs, Cartridge cartridge) {
        String result = instructionTemplate;

        for (Map.Entry<String, String> input : inputs.entrySet()) {
            String placeholder = "[" + input.getKey().toUpperCase() + "]";
            String value = getInputValue(input.getKey(), cartridge);
            result = result.replace(placeholder, value);
        }
        return result;
    }

    private String getInputValue(String inputKey, Cartridge cartridge) {
        return switch (inputKey.toLowerCase()) {
            case "cartridge_path" -> cartridge.getPath();
            case "cartridge_name" -> cartridge.getName();
            case "dependencies_list" -> generateDependenciesList(cartridge);
            case "java_classes_list" -> generateJavaClassesList(cartridge);
            default -> "";
        };
    }

    private String generateJavaClassesList(Cartridge cartridge) {
        java.nio.file.Path cartridgePath = java.nio.file.Paths.get(cartridge.getPath());
        StringBuilder sb = new StringBuilder();

        try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(cartridgePath)) {
            paths.filter(java.nio.file.Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> {
                     String relativePath = cartridgePath.relativize(p).toString();
                     sb.append(relativePath).append("\n");
                 });
        } catch (IOException e) {
            System.err.println("Error scanning Java files: " + e.getMessage());
        }

        return sb.toString();
    }

    private String generateDependenciesList(Cartridge cartridge) {
        Set<String> exclusions = Set.of();
        Set<String> imports = JavaImportScanner.scanImports(cartridge, exclusions);

        StringBuilder sb = new StringBuilder();
        for (String importStatement : imports) {
            sb.append(importStatement).append("\n");
        }

        return sb.toString();
    }

    /**
     * Runs a native phase (Java code) directly without using AI agent
     */
    private void runNativePhase(Cartridge cartridge, Phase phase, Path logFile) {
        try {
            // Redirect System.out and System.err to log file
            var originalOut = System.out;
            var originalErr = System.err;
            CodeMigrator.MigrationStats stats;

            try (var printStream = new java.io.PrintStream(
                    new java.io.FileOutputStream(logFile.toFile(), true))) {
                System.setOut(printStream);
                System.setErr(printStream);

                // Run CodeMigrator
                CodeMigrator migrator = new CodeMigrator(cartridge.getPath());
                migrator.migrate();
                stats = migrator.getStats();

                printStream.println("\n=== Code Migration Statistics ===");
                printStream.println("Files processed: " + stats.filesProcessed());
                printStream.println("Errors: " + stats.errorCount());
                printStream.println("===================================\n");

            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
            }

            String successMsg = "Native phase completed: " + stats.filesProcessed() + " files";
            System.out.println("    ✓ " + successMsg);
            logger.logToMaster("  ✓ " + successMsg);
            logger.logToCartridgeSummary(cartridge, "✓ Phase " + phase.getOrder() + " completed (native)");

        } catch (Exception e) {
            String errorMsg = "Native phase failed: " + e.getMessage();
            System.err.println("    ✗ " + errorMsg);
            logger.logToMaster("  ✗ " + errorMsg);
            logger.logToCartridgeSummary(cartridge, "✗ Phase " + phase.getOrder() + " failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs the build verification phase (native): compiles the migrated cartridge
     * via Gradle and records the outcome. A failed build is recorded as a result
     * rather than aborting the whole migration session.
     */
    private void runBuildVerifyPhase(Cartridge cartridge, Phase phase, Path logFile) {
        String buildTask = phase.getInputs() != null
                ? phase.getInputs().getOrDefault("build_task", "compileJava")
                : "compileJava";

        BuildVerifier verifier = new BuildVerifier(cartridge.getPath(), buildTask);
        BuildVerifier.Result result = verifier.verify(logFile);
        buildResults.put(cartridge.getName(), result.status());

        switch (result.status()) {
            case PASSED -> {
                System.out.println("    ✓ Build verification passed");
                logger.logToMaster("  ✓ Build verification passed");
                logger.logToCartridgeSummary(cartridge, "✓ Phase " + phase.getOrder() + " build verification passed");
            }
            case FAILED -> {
                System.err.println("    ✗ Build verification failed: " + result.detail());
                logger.logToMaster("  ✗ Build verification failed: " + result.detail());
                logger.logToCartridgeSummary(cartridge, "✗ Phase " + phase.getOrder() + " build verification failed: " + result.detail());
            }
            case SKIPPED -> {
                System.out.println("    ⚠ Build verification skipped: " + result.detail());
                logger.logToMaster("  ⚠ Build verification skipped: " + result.detail());
                logger.logToCartridgeSummary(cartridge, "⚠ Phase " + phase.getOrder() + " build verification skipped: " + result.detail());
            }
        }
    }
}
