package com.brandonitaly.bedrockskins.util;

import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ExternalAssetUtil {

    /**
     * Extracts the pack_icon.png from an external folder or ZIP file and converts it to a NativeImage.
     */
    public static NativeImage loadPackIcon(AssetSource src) {
        try {
            if (src instanceof AssetSource.File fSrc) {
                Path file = Paths.get(fSrc.path());
                Path iconFile = file.getParent().resolve("pack_icon.png");
                if (Files.exists(iconFile)) {
                    try (InputStream in = Files.newInputStream(iconFile)) {
                        return NativeImage.read(in);
                    }
                }
            } else if (src instanceof AssetSource.Zip z) {
                try (ZipFile zip = new ZipFile(z.zipPath())) {
                    String entryPath = z.internalPath();
                    int lastSlash = entryPath.lastIndexOf('/');
                    String iconPath = (lastSlash != -1 ? entryPath.substring(0, lastSlash) + "/" : "") + "pack_icon.png";
                    
                    ZipEntry entry = zip.getEntry(iconPath);
                    if (entry != null) {
                        try (InputStream in = zip.getInputStream(entry)) {
                            return NativeImage.read(in);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

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
            } else if (src instanceof AssetSource.Zip z) {
                try (ZipFile zip = new ZipFile(z.zipPath())) {
                    ZipEntry entry = zip.getEntry(z.internalPath());
                    if (entry != null) {
                        try (InputStream is = zip.getInputStream(entry)) { 
                            return is.readAllBytes(); 
                        }
                    }
                }
            } else if (src instanceof AssetSource.Bytes bytes) {
                return bytes.getData();
            }
        } catch (Exception ignored) {}
        return new byte[0];
    }

    public static boolean deletePack(String packId, String storeFolderName) {
        boolean deleted = false;
        File storeDir = new File(Minecraft.getInstance().gameDirectory, storeFolderName);

        // Try deleting via loaded AssetSource
        LoadedSkin firstSkin = null;
        for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
            if (packId.equals(skin.packId)) {
                firstSkin = skin;
                break;
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
            } else if (firstSkin.texture instanceof AssetSource.Bytes bytesSource) {
                String debugName = bytesSource.getDebugName();
                if (debugName != null && debugName.toLowerCase().contains(".pck:")) {
                    String pckFileName = debugName.substring(0, debugName.toLowerCase().indexOf(".pck:") + 4);
                    File pckFile = new File(storeDir, pckFileName);
                    if (pckFile.exists() && pckFile.isFile()) {
                        deleted = pckFile.delete();
                    }
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