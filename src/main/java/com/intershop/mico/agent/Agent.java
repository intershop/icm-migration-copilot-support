package com.intershop.mico.agent;

public interface Agent {

    String getExecutionCommand();
    Agent setModel(String model);
    Agent setDirectory(String directory);
    Agent allowAllTools();
    Agent setPrompt(String text);
    Agent setPhaseId(String phaseId);
    public ProcessBuilder execute();

    default void postExecution(Process process) throws java.io.IOException {
        // Default implementation does nothing
    }
}
