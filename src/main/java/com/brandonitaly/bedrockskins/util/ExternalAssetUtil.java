package com.brandonitaly.bedrockskins.util;

import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;

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
                Path file = Paths.get(fSrc.getPath());
                Path iconFile = file.getParent().resolve("pack_icon.png");
                if (Files.exists(iconFile)) {
                    try (InputStream in = Files.newInputStream(iconFile)) {
                        return NativeImage.read(in);
                    }
                }
            } else if (src instanceof AssetSource.Zip z) {
                try (ZipFile zip = new ZipFile(z.getZipPath())) {
                    String entryPath = z.getInternalPath();
                    int lastSlash = entryPath.lastIndexOf('/');
                    String iconPath = (lastSlash != -1 ? entryPath.substring(0, lastSlash) : "") + "/pack_icon.png";
                    
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
        if (skin == null || skin.getTexture() == null) return new byte[0];
        return loadTextureData(skin.getTexture(), minecraft);
    }

    /**
     * Reads the raw byte array of a skin texture, supporting built-in, folder, and ZIP assets.
     */
    public static byte[] loadTextureData(AssetSource src, Minecraft minecraft) {
        try {
            if (src instanceof AssetSource.Resource res) {
                return minecraft.getResourceManager().getResource(res.getId()).map(r -> {
                    try (InputStream in = r.open()) { 
                        return in.readAllBytes(); 
                    } catch (Exception e) { return new byte[0]; }
                }).orElse(new byte[0]);
            } else if (src instanceof AssetSource.File fSrc) {
                return Files.readAllBytes(Paths.get(fSrc.getPath()));
            } else if (src instanceof AssetSource.Zip z) {
                try (ZipFile zip = new ZipFile(z.getZipPath())) {
                    ZipEntry entry = zip.getEntry(z.getInternalPath());
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
}