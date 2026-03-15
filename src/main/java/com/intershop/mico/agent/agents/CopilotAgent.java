package com.intershop.mico.agent.agents;

import java.io.File;

import com.intershop.mico.agent.Agent;
import com.intershop.mico.agent.BaseAgent;

public class CopilotAgent extends BaseAgent {

    public CopilotAgent(File workingDirectory) {
        super(workingDirectory);
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            command.add("copilot.bat");
        } else {
            command.add("copilot");
        }
    }

    @Override
    public Agent allowAllTools() {
        command.add("--allow-all-tools");
        return this;
    }

    @Override
    public ProcessBuilder execute() {
        if(model == null) {
            setDefaultModel();
        }
        command.add("--model");
        command.add(model);
        
        String systemPrompt = "You are a headless automated enterprise Java migration tool. "
            + "Execute the instructions immediately. Do not ask for user input. "
            + "Do not output pleasantries or conversational filler.";
            
        if (phaseId != null) {
            switch (phaseId) {
                case "gradle_build_migration":
                    systemPrompt += " Your specialization is Gradle Build Migration. Update build scripts without removing unrelated settings.";
                    break;
                case "dep_res":
                    systemPrompt += " Your specialization is Java Dependency Resolution in cross-modular spaces.";
                    break;
                case "code_fixing":
                    systemPrompt += " Your specialization is Java Syntax and Imports. Post-migration compile fixing only, do not change business logic.";
                    break;
                case "resource_processing":
                    systemPrompt += " Your specialization is Resource and Properties Migration.";
                    break;
            }
        }
        
        if (promptText != null && !promptText.isEmpty()) {
            promptText = systemPrompt + "\n\n" + promptText;
        }

        command.add("--prompt");
        command.add(promptText != null ? promptText : systemPrompt);
        command.add("--stream");
        command.add("off");
        command.add("--no-ask-user");
        command.add("--silent");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDirectory);
        executionCommand = String.join(" ", command);
        return pb;
    }
    
    @Override
    public void postExecution(Process process) throws java.io.IOException {
        // Since we pass the prompt as an argument in -p mode,
        // we override this to avoid writing to stdin which may hang or be ignored.
    }

    private void setDefaultModel(){
        this.model = "gpt-5.3-codex"; 
    }
}
