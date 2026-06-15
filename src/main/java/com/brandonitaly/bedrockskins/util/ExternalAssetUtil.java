package com.brandonitaly.bedrockskins.util;

import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class ExternalAssetUtil {

    /**
     * Helper to load texture data directly from a LoadedSkin object.
     */
    public static byte[] loadTextureData(LoadedSkin skin, Minecraft minecraft) {
        if (skin == null || skin.texture == null) return new byte[0];
        return loadTextureData(skin.texture, minecraft);
    }

    /**
     * Reads the raw byte array of a skin texture, supporting built-in, folder, and ZIP assets.
     */
    public static byte[] loadTextureData(AssetSource src, Minecraft minecraft) {
        if (src == null) return new byte[0];

        try {
            return switch (src) {
                case AssetSource.Resource(Identifier id) -> minecraft.getResourceManager().getResource(id).map(r -> {
                    try (InputStream in = r.open()) { 
                        return in.readAllBytes(); 
                    } catch (Exception e) { return new byte[0]; }
                }).orElse(new byte[0]);
                case AssetSource.File(String path) -> Files.readAllBytes(Path.of(path));
                case AssetSource.Remote ignored -> new byte[0];
            };
        } catch (Exception ignored) {}
        return new byte[0];
    }

    public static boolean deletePack(String packId, String storeFolderName) {
        boolean deleted = false;

        // Try deleting via loaded AssetSource
        LoadedSkin firstSkin;
        synchronized (SkinPackLoader.loadedSkins) {
            firstSkin = SkinPackLoader.loadedSkins.values().stream()
                    .filter(skin -> packId.equals(skin.packId))
                    .findFirst()
                    .orElse(null);
        }

        if (firstSkin != null) {
            if (firstSkin.texture instanceof AssetSource.File(String path)) {
                Path textureFile = Path.of(path);
                Path targetToDelete = textureFile.getFileName().toString().endsWith(".png") ? textureFile.getParent() : null;
                if (targetToDelete != null && Files.exists(targetToDelete)) {
                    deleteDirectoryRecursively(targetToDelete.toFile());
                    deleted = true;
                }
            }
        }
        
        return deleted;
    }

    public static void deleteDirectoryRecursively(File directory) {
        if (directory == null || !directory.exists()) return;
        Path root = directory.toPath();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {
            directory.delete();
        }
    }
}