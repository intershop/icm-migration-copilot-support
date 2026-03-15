package com.intershop.mico.agent.agents;

import java.io.File;

import com.intershop.mico.agent.Agent;
import com.intershop.mico.agent.BaseAgent;

public class ClaudeCodeAgent extends BaseAgent {

    public ClaudeCodeAgent(File workingDirectory) {
        super(workingDirectory);
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            command.add("claude.cmd");
        } else {
            command.add("claude");
        }
    }

    @Override
    public Agent allowAllTools() {
        command.add("--dangerously-skip-permissions");
        
        // Additionally ensure the permission mode explicitly bypasses the standard prompt block
        command.add("--permission-mode");
        command.add("bypassPermissions");
        
        return this;
    }

    @Override
    public ProcessBuilder execute() {
        if (model != null && !model.isEmpty()) {
            command.add("--model");
            command.add(model);
        }
        
        String agentsJson = "{"
                + "\"gradle-expert\": {"
                + "\"description\": \"Gradle Build Migration Specialist\","
                + "\"prompt\": \"You are an expert in Gradle build scripts and dependency migrations. Your task is to update build files securely and accurately without removing non-related settings.\""
                + "},"
                + "\"dependency-analyst\": {"
                + "\"description\": \"Java Dependency Resolution Specialist\","
                + "\"prompt\": \"You are a specialist in resolving Java dependencies and fixing import issues across module boundaries. Analyze dependencies and make the minimal necessary changes to fix compilation.\""
                + "},"
                + "\"java-fixer\": {"
                + "\"description\": \"Java Code Fixer\","
                + "\"prompt\": \"You are an expert Java developer. Your task is to perform post-migration compilation fixes, correct syntax errors, and resolve missing imports. Do not change underlying business logic.\""
                + "},"
                + "\"resource-manager\": {"
                + "\"description\": \"Resource Processing Specialist\","
                + "\"prompt\": \"You are an expert configuration config migration specialist. Update properties, XML, and other configuration files accurately.\""
                + "},"
                + "\"java-migrator\": {"
                + "\"description\": \"Java Code Migration Specialist\","
                + "\"prompt\": \"You are a professional enterprise Java migration specialist. "
                + "Your task is to apply precise refactoring to seamlessly modernize legacy code. "
                + "You must deeply understand the instructions and use your available tools appropriately "
                + "without unprompted creative deviations. Answer concisely.\""
                + "}"
                + "}";
        command.add("--agents");
        command.add(agentsJson);
        
        String activeAgent = "java-migrator"; // default fallback
        if (phaseId != null) {
            switch (phaseId) {
                case "gradle_build_migration":
                    activeAgent = "gradle-expert";
                    break;
                case "dep_res":
                    activeAgent = "dependency-analyst";
                    break;
                case "code_fixing":
                    activeAgent = "java-fixer";
                    break;
                case "resource_processing":
                    activeAgent = "resource-manager";
                    break;
            }
        }
        command.add("--agent");
        command.add(activeAgent);
        
        command.add("--print");
        command.add("--no-session-persistence");
        
        command.add("--output-format");
        command.add("text");
        command.add("--system-prompt");
        command.add("You are a headless automated enterprise Java migration tool. Execute the instructions immediately. Do not ask for user input. Do not output pleasantries or conversational filler.");

        if (promptText != null && !promptText.isEmpty()) {
            command.add(promptText);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDirectory);
        executionCommand = String.join(" ", command);
        return pb;
    }
    
    @Override
    public void postExecution(Process process) throws java.io.IOException {
        // Since we pass the prompt as an argument in --print mode,
        // we override this to avoid writing to stdin which may hang or be ignored.
    }
}
