package com.brandonitaly.bedrockskins.util;

import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        try {
            if (src instanceof AssetSource.Resource res) {
                return minecraft.getResourceManager().getResource(res.id()).map(r -> {
                    try (InputStream in = r.open()) { 
                        return in.readAllBytes(); 
                    } catch (Exception e) { return new byte[0]; }
                }).orElse(new byte[0]);
            } else if (src instanceof AssetSource.File fSrc) {
                return Files.readAllBytes(Paths.get(fSrc.path()));
            }
        } catch (Exception ignored) {}
        return new byte[0];
    }

    public static boolean deletePack(String packId, String storeFolderName) {
        boolean deleted = false;

        // Try deleting via loaded AssetSource
        LoadedSkin firstSkin = null;
        synchronized (SkinPackLoader.loadedSkins) {
            for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
                if (packId.equals(skin.packId)) {
                    firstSkin = skin;
                    break;
                }
            }
        }

        if (firstSkin != null) {
            if (firstSkin.texture instanceof AssetSource.File fileSource) {
                File textureFile = new File(fileSource.path());
                File targetToDelete = textureFile.getName().endsWith(".png") ? textureFile.getParentFile() : null;
                if (targetToDelete != null && targetToDelete.exists()) {
                    deleteDirectoryRecursively(targetToDelete);
                    deleted = true;
                }
            }
        }
        
        return deleted;
    }

    public static void deleteDirectoryRecursively(File directory) {
        if (directory == null || !directory.exists()) return;
        if (directory.isDirectory()) {
            File[] allContents = directory.listFiles();
            if (allContents != null) {
                for (File file : allContents) {
                    deleteDirectoryRecursively(file);
                }
            }
        }
        directory.delete();
    }
}