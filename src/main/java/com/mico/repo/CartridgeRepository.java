package com.mico.repo;

import com.mico.models.Cartridge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class CartridgeRepository {

    private final String fileToLookFor = "build.gradle";

    private ArrayList<Cartridge> cartridges = new ArrayList<>();

    public CartridgeRepository(String pathToCartridges, boolean isSingleCartridge) {
        if (isSingleCartridge) {
            cartridges.add(new Cartridge(pathToCartridges, pathToCartridges, "to_do"));
        }else{
            extractAllCartridges(pathToCartridges);
        }
        System.out.println("Workspace mode: Found " + cartridges.size() + " cartridges");
    }

    public ArrayList<Cartridge> getCartridges() {
        return cartridges;
    }
    public void setCartridges(ArrayList<Cartridge> cartridges) {
        this.cartridges = cartridges;
    }
    public void extractAllCartridges(String pathToCartridges) {
        try (var paths = Files.list(Paths.get(pathToCartridges))) {
            paths.filter(java.nio.file.Files::isDirectory)
                    .filter(dirPath -> Files.exists(dirPath.resolve(fileToLookFor)))
                    .map(Path::toString)
                    .forEach(dir -> {
                        cartridges.add(new Cartridge(dir, dir, "to_do"));
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
