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
        return this;
    }

    @Override
    public ProcessBuilder execute() {
        if (model != null && !model.isEmpty()) {
            command.add("--model");
            command.add(model);
        }
        // Removed inheritIO() to allow BaseAgent to write prompt to stdin
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDirectory);
        executionCommand = pb.command().toString();
        return pb;
    }
}
