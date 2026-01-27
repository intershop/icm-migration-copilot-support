package com.mico.agent.agents;

import com.mico.agent.Agent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CopilotAgent implements Agent {

    private final List<String> command = new ArrayList<>();
    private String executionCommand = null;
    private final File workingDirectory;
    private String model = null;

    public CopilotAgent(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        command.add("copilot");
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
    public Agent allowAllTools() {
        command.add("--allow-all-tools");
        return this;
    }

    @Override
    public Agent setPrompt(String text) {
        command.add("--prompt");
        command.add(text);
        return this;
    }

    @Override
    public ProcessBuilder execute() {
        if(model == null) {
            setDefaultModel();
        }
        command.add("--model");
        command.add(model);
        ProcessBuilder pb = new ProcessBuilder(command).inheritIO();
        pb.directory(workingDirectory);
        executionCommand = pb.command().toString();
        return pb;
    }

    private void setDefaultModel(){
        this.model = "gpt-4.1";
    }
}
