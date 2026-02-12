package com.intershop.mico.models;

public class Cartridge {

    private String name;
    private String path;
    private String currentPhase;

    public Cartridge(String name, String path, String currentPhase) {
        this.name = name;
        this.path = path;
        this.currentPhase = currentPhase;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }
}
