package com.mico;

import com.mico.agent.Agent;
import com.mico.agent.agents.AgentType;
import com.mico.agent.agents.CopilotAgent;
import com.mico.agent.agents.ClaudeCodeAgent;
import com.mico.repo.CartridgeRepository;
import com.mico.repo.PhaseRepository;

import java.io.File;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        // Flags setup
        String path = null;
        boolean isSingleCartridge = false;
        AgentType agentType = null;
        String model = null;

        for (int i = 0; i < args.length; i++) {
            if ("-p".equals(args[i]) && i + 1 < args.length) {
                path = args[i + 1];
                i++;
            } else if ("-s".equals(args[i])) {
                isSingleCartridge = true;
            } else if ("-a".equals(args[i]) && i + 1 < args.length){
                String agentArg = args[i + 1];
                i++;
                switch (agentArg) {
                    case "copilot":
                        agentType = AgentType.COPILOT;
                        break;
                    case "claude_code":
                        agentType = AgentType.CLAUDE_CODE;
                        break;
                }
            } else if("-m".equals(args[i]) && i + 1 < args.length){
                model = args[i + 1];
                i++;
            }
        }

        // Validate required arguments
        if (path == null || agentType == null) {
            System.err.println("Usage: MiCo -p <path> -a <agent> [-m <model>] [-s]");
            System.err.println("  -p <path>    : Path to cartridge(s)");
            System.err.println("  -a <agent>   : Agent type (copilot or claude_code)");
            System.err.println("  -m <model>   : Model to use (optional)");
            System.err.println("  -s           : Single cartridge mode (optional)");
            System.exit(1);
        }

        // Create the agent
        Agent agent = switch (agentType) {
            case COPILOT -> new CopilotAgent(new File(path));
            case CLAUDE_CODE -> new ClaudeCodeAgent(new File(path));
        };

        agent.setModel(model);
        agent.allowAllTools();
        agent.setDirectory(path);

        // Initialize repositories
        CartridgeRepository cartridgeRepository = new CartridgeRepository(path, isSingleCartridge);
        PhaseRepository phaseRepository = new PhaseRepository(
            Paths.get("phases/config.json"),
            Paths.get("phases/instructions")
        );

        // Create and run migrator
        Migrator migrator = new Migrator(cartridgeRepository, phaseRepository, agent);
        migrator.migrate();
    }
}