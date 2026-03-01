package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.client.pack.ContentPack;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.util.HttpUtil;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ContentManager {
    private static final Gson GSON = new Gson();
    private static final String INDEX_URL = "https://raw.githubusercontent.com/BrandonItaly/LCE-Resources/refs/heads/skin-packs/skin_packs.json";

    public static CompletableFuture<List<ContentPack>> fetchIndex() {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStreamReader reader = new InputStreamReader(new URL(INDEX_URL).openStream())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                return GSON.fromJson(json.get("packs"), new TypeToken<List<ContentPack>>(){}.getType());
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    public static Path getSkinPacksDir() {
        File dir = new File(Minecraft.getInstance().gameDirectory, "skin_packs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.toPath();
    }

    public static boolean isPackInstalled(ContentPack pack) {
        Path path = getSkinPacksDir().resolve(pack.id());
        return Files.exists(path) && Files.isDirectory(path);
    }

    public static void downloadPack(ContentPack pack, Runnable onFinished) {
        if (isPackInstalled(pack)) return;

        Path skinPacksDir = getSkinPacksDir();
        CompletableFuture.runAsync(() -> {
            try {
                Path downloadedTempFile = HttpUtil.downloadFile(
                    skinPacksDir,
                    new URL(pack.downloadURI()),
                    new java.util.HashMap<>(),
                    com.google.common.hash.Hashing.sha256(),
                    null,
                    50 * 1024 * 1024,
                    java.net.Proxy.NO_PROXY,
                    new HttpUtil.DownloadProgressListener() {
                        public void requestStart() {}
                        public void downloadStart(java.util.OptionalLong size) {}
                        public void downloadedBytes(long bytes) {}
                        public void requestFinished(boolean success) {}
                    }
                );

                if (downloadedTempFile != null && Files.exists(downloadedTempFile)) {
                    Path targetFolder = skinPacksDir.resolve(pack.id());
                    
                    // Extract the zip to the target folder
                    extractZip(downloadedTempFile, targetFolder);
                    
                    // Clean up the temporary download file
                    Files.deleteIfExists(downloadedTempFile);

                    Minecraft.getInstance().execute(() -> {
                        SkinPackLoader.loadPacks();
                        Minecraft.getInstance().reloadResourcePacks().thenRun(() -> {
                            Minecraft.getInstance().execute(onFinished);
                        });
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void extractZip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName());
                
                // Security check
                if (!entryPath.normalize().startsWith(targetDir.normalize())) {
                    throw new IOException("Zip entry is outside of the target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
}