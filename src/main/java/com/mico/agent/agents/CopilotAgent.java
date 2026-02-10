package com.mico.agent.agents;

import java.io.File;
import com.mico.agent.Agent;
import com.mico.agent.BaseAgent;

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
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDirectory);
        executionCommand = pb.command().toString();
        return pb;
    }

    private void setDefaultModel(){
        this.model = "gpt-4.1";
    }
}
