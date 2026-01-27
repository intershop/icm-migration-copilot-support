package com.mico.util;

import com.mico.models.Cartridge;
import com.mico.models.Phase;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MigrationLogger {
    private static final Path LOGS_DIR = Paths.get("logs");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final String sessionId;
    private final Path sessionLogDir;

    public MigrationLogger() {
        this.sessionId = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        this.sessionLogDir = LOGS_DIR.resolve("session_" + sessionId);

        try {
            Files.createDirectories(sessionLogDir);
            System.out.println("📝 Logging to: " + sessionLogDir.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
    }

    /**
     * Gets the log file path for a specific cartridge and phase
     */
    public Path getLogFile(Cartridge cartridge, Phase phase) {
        String sanitizedName = sanitizeFileName(cartridge.getName());
        String fileName = String.format("%s_phase_%d_%s.log",
            sanitizedName,
            phase.getOrder(),
            phase.getId());
        return sessionLogDir.resolve(fileName);
    }

    /**
     * Gets the summary log file path for a cartridge (all phases combined)
     */
    public Path getCartridgeSummaryLog(Cartridge cartridge) {
        String sanitizedName = sanitizeFileName(cartridge.getName());
        return sessionLogDir.resolve(sanitizedName + "_summary.log");
    }

    /**
     * Gets the master log file for the entire migration session
     */
    public Path getMasterLogFile() {
        return sessionLogDir.resolve("migration_master.log");
    }

    /**
     * Creates a process that redirects output to a log file while also displaying to console
     */
    public Process executeWithLogging(ProcessBuilder pb, Path logFile) throws IOException {
        // Create log file parent directories if needed
        Files.createDirectories(logFile.getParent());

        // Create the log file writer
        FileOutputStream logStream = new FileOutputStream(logFile.toFile());

        // Instead of inheritIO, we'll redirect to the log file
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
        pb.redirectError(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

        return pb.start();
    }

    /**
     * Writes a header to the log file
     */
    public void writeLogHeader(Path logFile, Cartridge cartridge, Phase phase) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(logFile)) {
            writer.write("=".repeat(80) + "\n");
            writer.write("Migration Log\n");
            writer.write("=".repeat(80) + "\n");
            writer.write("Cartridge: " + cartridge.getName() + "\n");
            writer.write("Path: " + cartridge.getPath() + "\n");
            writer.write("Phase: " + phase.getOrder() + " - " + phase.getName() + "\n");
            writer.write("Phase ID: " + phase.getId() + "\n");
            writer.write("Timestamp: " + LocalDateTime.now() + "\n");
            writer.write("=".repeat(80) + "\n\n");
        }
    }

    /**
     * Appends a message to the master log
     */
    public void logToMaster(String message) {
        try {
            Path masterLog = getMasterLogFile();
            String timestampedMessage = String.format("[%s] %s\n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                message);
            Files.writeString(masterLog, timestampedMessage,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write to master log: " + e.getMessage());
        }
    }

    /**
     * Appends a message to the cartridge summary log
     */
    public void logToCartridgeSummary(Cartridge cartridge, String message) {
        try {
            Path summaryLog = getCartridgeSummaryLog(cartridge);
            String timestampedMessage = String.format("[%s] %s\n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                message);
            Files.writeString(summaryLog, timestampedMessage,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write to cartridge summary log: " + e.getMessage());
        }
    }

    /**
     * Reads and returns the content of a log file
     */
    public String readLog(Path logFile) throws IOException {
        if (Files.exists(logFile)) {
            return Files.readString(logFile);
        }
        return "";
    }

    /**
     * Sanitizes a file name to remove invalid characters
     */
    private String sanitizeFileName(String name) {
        // Remove path separators and keep only safe characters
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Creates a summary report at the end of migration
     */
    public void createSummaryReport(int totalCartridges, int totalPhases, long durationMillis) {
        try {
            Path summaryFile = sessionLogDir.resolve("SUMMARY.txt");
            try (BufferedWriter writer = Files.newBufferedWriter(summaryFile)) {
                writer.write("=".repeat(80) + "\n");
                writer.write("MIGRATION SESSION SUMMARY\n");
                writer.write("=".repeat(80) + "\n");
                writer.write("Session ID: " + sessionId + "\n");
                writer.write("Total Cartridges: " + totalCartridges + "\n");
                writer.write("Total Phases: " + totalPhases + "\n");
                writer.write("Duration: " + formatDuration(durationMillis) + "\n");
                writer.write("Completed: " + LocalDateTime.now() + "\n");
                writer.write("=".repeat(80) + "\n");
                writer.write("\nDetailed logs available in: " + sessionLogDir.toAbsolutePath() + "\n");
            }
            System.out.println("\n📊 Summary report created: " + summaryFile.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to create summary report: " + e.getMessage());
        }
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%d min %d sec", minutes, seconds);
        } else {
            return String.format("%d sec", seconds);
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public Path getSessionLogDir() {
        return sessionLogDir;
    }
}
