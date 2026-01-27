package com.mico.agent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface Agent {

    String getExecutionCommand();
    Agent setModel(String model);
    Agent setDirectory(String directory);
    Agent allowAllTools();
    Agent setPrompt(String text);
    public ProcessBuilder execute();
}
