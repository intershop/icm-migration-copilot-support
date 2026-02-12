package com.intershop.mico.repo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intershop.mico.models.Phase;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PhaseRepository {
    private final Path configPath;
    private final Path instructionsPath;
    private final Gson gson;

    public PhaseRepository(Path configPath, Path instructionsPath) {
        this.configPath = configPath;
        this.instructionsPath = instructionsPath;
        this.gson = new Gson();
    }

    public List<Phase> getPhases() {
        try {
            String json = Files.readString(configPath);
            Type listType = new TypeToken<List<Phase>>(){}.getType();
            List<Phase> phases = gson.fromJson(json, listType);

            // Sort phases by order
            return phases.stream()
                    .sorted(Comparator.comparingInt(Phase::getOrder))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load phases configuration", e);
        }
    }

    public String getPhaseInstructions(Phase phase) {
        try {
            Path instructionFile = instructionsPath.resolve(phase.getInstructions());
            return Files.readString(instructionFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load instructions for phase: " + phase.getName(), e);
        }
    }
}
