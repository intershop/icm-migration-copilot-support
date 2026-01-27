package com.mico;

import com.mico.agent.Agent;
import com.mico.models.Cartridge;
import com.mico.models.Phase;
import com.mico.repo.CartridgeRepository;
import com.mico.repo.PhaseRepository;
import com.mico.util.CodeMigrator;
import com.mico.util.JavaImportScanner;
import com.mico.util.MigrationLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Migrator {

    private final CartridgeRepository cartridgeRepository;
    private final PhaseRepository phaseRepository;
    private final Agent agent;
    private final MigrationLogger logger;

    public Migrator(CartridgeRepository cartridgeRepository, PhaseRepository phaseRepository, Agent agent) {
        this.cartridgeRepository = cartridgeRepository;
        this.phaseRepository = phaseRepository;
        this.agent = agent;
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

                    // Check if this is a native phase (runs Java code directly, not through AI)
                    boolean isNativePhase = "code_migration".equals(phase.getId());

                    if (isNativePhase) {
                        runNativePhase(cartridge, phase, logFile);
                    } else {
                        // Run through AI agent
                        String instructionTemplate = phaseRepository.getPhaseInstructions(phase);
                        String finalPrompt = preparePrompt(instructionTemplate, phase.getInputs(), cartridge);

                        agent.setPrompt(finalPrompt);

                        ProcessBuilder pb = agent.execute();
                        Process process = logger.executeWithLogging(pb, logFile);
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
        logger.createSummaryReport(cartridges.size(), phases.size(), duration);

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
            default -> "";
        };
    }

    private String generateDependenciesList(Cartridge cartridge) {
        Set<String> exclusions = Set.of("com.intershop.");
        LinkedList<String> imports = JavaImportScanner.scanImports(cartridge, exclusions);

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
}
