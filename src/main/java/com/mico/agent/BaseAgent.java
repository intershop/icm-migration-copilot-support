package com.mico.agent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseAgent implements Agent {
    protected final List<String> command = new ArrayList<>();
    protected File workingDirectory;
    protected String model;
    protected String promptText;
    protected String executionCommand;

    public BaseAgent(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String getExecutionCommand() {
        return executionCommand;
    }

    @Override
    public Agent setModel(String model) {
        this.model = model;
        return this;
    }

    @Override
    public Agent setDirectory(String directory) {
        command.add("--add-dir");
        command.add(directory);
        return this;
    }

    @Override
    public Agent setPrompt(String text) {
        this.promptText = text;
        return this;
    }
    
    @Override
    public void postExecution(Process process) throws IOException {
        if (promptText != null) {
            if (!process.isAlive()) {
                reportProcessFailure(process);
            }
            
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(promptText);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                if (!process.isAlive()) {
                    reportProcessFailure(process);
                }
                throw e;
            }
        }
    }

    protected void reportProcessFailure(Process process) throws IOException {
        String errorMessage = "Process exited prematurely with code " + process.exitValue();
        try {
            try (InputStream err = process.getErrorStream()) {
                String errContent = new String(err.readAllBytes(), StandardCharsets.UTF_8).trim();
                if (!errContent.isEmpty()) {
                    errorMessage += "\nSTDERR: " + errContent;
                }
            }
            
            try (InputStream out = process.getInputStream()) {
                String outContent = new String(out.readAllBytes(), StandardCharsets.UTF_8).trim();
                if (!outContent.isEmpty()) {
                    errorMessage += "\nSTDOUT: " + outContent;
                }
            }
        } catch (Exception ignored) {
            // Ignore errors reading streams during crash reporting
        }
        throw new IOException(errorMessage);
    }
}
