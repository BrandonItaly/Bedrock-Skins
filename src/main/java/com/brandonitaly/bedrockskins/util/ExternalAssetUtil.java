package com.brandonitaly.bedrockskins.util;

import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Pattern SERIALIZE_NAME_PATTERN = Pattern.compile("\"serialize_name\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * Deletes a skin pack from disk and unloads it from memory.
     */
    public static boolean deletePack(String packId) {
        if (packId == null || packId.isBlank()) return false;

        String cleanId = packId.replace("skinpack.", "").toLowerCase(Locale.ROOT);
        File skinPacksDir = SkinPackLoader.getSkinPacksDir();
        if (skinPacksDir == null || !skinPacksDir.exists()) return false;

        String folderName = resolveFromLoadedSkins(packId, cleanId, skinPacksDir);

        if (folderName == null) {
            folderName = resolveByScanning(packId, cleanId, skinPacksDir);
        }

        boolean deleted = deleteTarget(skinPacksDir, folderName);
        unloadPackSkins(packId, cleanId);

        return deleted;
    }

    private static String resolveFromLoadedSkins(String packId, String cleanId, File skinPacksDir) {
        Path packsRoot = skinPacksDir.toPath().toAbsolutePath();
        synchronized (SkinPackLoader.loadedSkins) {
            for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
                if (!matchesPackId(skin.packId, packId, cleanId)) continue;
                if (skin.texture instanceof AssetSource.File(String path)) {
                    Path texturePath = Path.of(path).toAbsolutePath();
                    if (texturePath.startsWith(packsRoot)) {
                        Path relative = packsRoot.relativize(texturePath);
                        if (relative.getNameCount() > 0) {
                            return relative.getName(0).toString();
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String resolveByScanning(String packId, String cleanId, File skinPacksDir) {
        File[] children = skinPacksDir.listFiles();
        if (children == null) return null;

        for (File child : children) {
            String name = child.getName().toLowerCase(Locale.ROOT);
            if (name.equals(cleanId) || name.equals(packId.toLowerCase(Locale.ROOT))) {
                return child.getName();
            }
            if (child.isDirectory()) {
                File skinsJson = new File(child, "skins.json");
                if (skinsJson.exists()) {
                    try {
                        String content = Files.readString(skinsJson.toPath(), StandardCharsets.UTF_8);
                        Matcher matcher = SERIALIZE_NAME_PATTERN.matcher(content);
                        if (matcher.find()) {
                            String serializeName = matcher.group(1);
                            if (matchesPackId(serializeName, packId, cleanId)) {
                                return child.getName();
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }

    private static boolean deleteTarget(File skinPacksDir, String folderName) {
        if (folderName == null) return false;
        File target = new File(skinPacksDir, folderName);
        if (!target.exists()) return false;

        if (target.isDirectory()) {
            deleteDirectoryRecursively(target);
            return true;
        }
        try {
            return Files.deleteIfExists(target.toPath());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void unloadPackSkins(String packId, String cleanId) {
        synchronized (SkinPackLoader.loadedSkins) {
            List<SkinId> toRemove = new ArrayList<>();
            SkinPackLoader.loadedSkins.forEach((id, skin) -> {
                if (matchesPackId(skin.packId, packId, cleanId)) {
                    toRemove.add(id);
                }
            });
            for (SkinId id : toRemove) {
                SkinPackLoader.releaseSkinAssets(id);
                SkinPackLoader.loadedSkins.remove(id);
            }
        }
    }

    private static boolean matchesPackId(String candidate, String packId, String cleanId) {
        return packId.equals(candidate) || cleanId.equalsIgnoreCase(candidate);
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