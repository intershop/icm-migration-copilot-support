package com.intershop.mico.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * Verifies that a migrated cartridge still compiles by invoking a Gradle task
 * (default: {@code compileJava}).
 *
 * <p>The verifier locates a Gradle wrapper ({@code gradlew}/{@code gradlew.bat})
 * by searching the cartridge directory and its ancestors. Running the wrapper
 * from within the cartridge directory lets Gradle discover the surrounding
 * (composite/multi-project) build, so cartridge dependencies are resolved and
 * only the affected cartridge is compiled.</p>
 *
 * <p>If no wrapper can be found the verification is reported as
 * {@link Status#SKIPPED} rather than {@link Status#FAILED}, so partners without
 * a local build environment are not shown false negatives.</p>
 */
public class BuildVerifier {

    private static final int MAX_PARENT_LOOKUP = 4;

    public enum Status {
        PASSED, FAILED, SKIPPED
    }

    public record Result(Status status, int exitCode, String detail) {
    }

    private final Path cartridgePath;
    private final String task;

    public BuildVerifier(String cartridgePath, String task) {
        this.cartridgePath = Paths.get(cartridgePath).toAbsolutePath().normalize();
        this.task = (task == null || task.isBlank()) ? "compileJava" : task.trim();
    }

    /**
     * Runs the verification and appends all Gradle output to the given log file.
     */
    public Result verify(Path logFile) {
        Path wrapper = findGradleWrapper();
        if (wrapper == null) {
            String detail = "No Gradle wrapper (gradlew) found in cartridge or ancestor directories - build verification skipped.";
            appendToLog(logFile, "[SKIPPED] " + detail);
            return new Result(Status.SKIPPED, -1, detail);
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String wrapperCommand = isWindows ? wrapper.toString() : "./" + wrapper.getFileName();

        ProcessBuilder pb = new ProcessBuilder(wrapperCommand, task, "--console=plain -s");
        pb.directory(cartridgePath.toFile());
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
        pb.redirectError(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

        appendToLog(logFile, "[BUILD] Running: " + wrapperCommand + " " + task + " (working dir: " + cartridgePath + ")");

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                appendToLog(logFile, "[PASSED] Gradle task '" + task + "' completed successfully.");
                return new Result(Status.PASSED, exitCode, "Gradle task '" + task + "' succeeded.");
            }
            appendToLog(logFile, "[FAILED] Gradle task '" + task + "' exited with code " + exitCode + ".");
            return new Result(Status.FAILED, exitCode, "Gradle task '" + task + "' failed with exit code " + exitCode + ".");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String detail = "Could not execute Gradle verification: " + e.getMessage();
            appendToLog(logFile, "[FAILED] " + detail);
            return new Result(Status.FAILED, -1, detail);
        }
    }

    /**
     * Searches the cartridge directory and up to {@value #MAX_PARENT_LOOKUP}
     * ancestor directories for a Gradle wrapper script.
     */
    private Path findGradleWrapper() {
        String wrapperName = System.getProperty("os.name").toLowerCase().contains("win") ? "gradlew.bat" : "gradlew";

        Path current = cartridgePath;
        for (int i = 0; i <= MAX_PARENT_LOOKUP && current != null; i++) {
            Path candidate = current.resolve(wrapperName);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private void appendToLog(Path logFile, String message) {
        String line = "[" + LocalDateTime.now() + "] " + message + System.lineSeparator();
        try {
            Files.writeString(logFile, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write build verification log: " + e.getMessage());
        }
    }
}
