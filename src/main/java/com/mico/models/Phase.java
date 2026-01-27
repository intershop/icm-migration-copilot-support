package com.mico.models;

import java.util.Map;

public class Phase {
    private String name;
    private String instructions;
    private String id;
    private int order;
    private Map<String, String> inputs;

    public Phase(String name, String instructions, String id, int order, Map<String, String> inputs) {
        this.name = name;
        this.instructions = instructions;
        this.id = id;
        this.order = order;
        this.inputs = inputs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Map<String, String> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, String> inputs) {
        this.inputs = inputs;
    }
}
