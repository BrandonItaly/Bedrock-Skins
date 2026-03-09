package com.brandonitaly.bedrockskins.client;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ContentManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CATEGORIES_FILE = "store_categories.json";
    public static final List<Category> CATEGORIES = new ArrayList<>();

    public record Category(
        String id,
        String indexUrl,
        String targetDirectoryName,
        boolean requiresResourceReload
    ) {}

    public record Pack(
        String id,
        String name,
        String description,
        String downloadURI,
        String imageUrl,
        Optional<String> checkSum
    ) {
        public static final Codec<Pack> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Pack::id),
            Codec.STRING.fieldOf("name").forGetter(Pack::name),
            Codec.STRING.optionalFieldOf("description", "").forGetter(Pack::description),
            Codec.STRING.fieldOf("downloadURI").forGetter(Pack::downloadURI),
            Codec.STRING.optionalFieldOf("imageUrl", "").forGetter(Pack::imageUrl),
            Codec.STRING.optionalFieldOf("checkSum").forGetter(Pack::checkSum)
        ).apply(i, Pack::new));

        public static final Codec<List<Pack>> LIST_CODEC = CODEC.listOf();
    }

    public static CompletableFuture<List<Pack>> fetchIndex(String indexUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStreamReader reader = new InputStreamReader(new URL(indexUrl).openStream())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                return Pack.LIST_CODEC.parse(JsonOps.INSTANCE, json.get("packs"))
                        .resultOrPartial(LOGGER::warn)
                        .orElseGet(ArrayList::new);
            } catch (Exception e) {
                LOGGER.warn("Failed to fetch content index from {}: {}", indexUrl, e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    public static void reloadCategories(ResourceManager resourceManager) {
        CATEGORIES.clear();
        List<String> namespaces = new ArrayList<>(resourceManager.getNamespaces());
        Collections.sort(namespaces);

        for (String namespace : namespaces) {
            resourceManager.getResource(Identifier.fromNamespaceAndPath(namespace, CATEGORIES_FILE)).ifPresent(resource -> {
                try (BufferedReader bufferedReader = resource.openAsReader()) {
                    JsonElement parsed = JsonParser.parseReader(bufferedReader);
                    if (!parsed.isJsonArray()) {
                        LOGGER.warn("{} in namespace {} is not a JSON array", CATEGORIES_FILE, namespace);
                        return;
                    }

                    JsonArray categories = parsed.getAsJsonArray();
                    for (JsonElement element : categories) {
                        if (!element.isJsonObject()) continue;
                        JsonObject category = element.getAsJsonObject();
                        String id = getString(category, "id", "");
                        String indexUrl = getString(category, "indexUrl", "");
                        String targetDirectoryName = getString(category, "targetDirectoryName", "");
                        boolean requiresResourceReload = category.has("requiresResourceReload") && category.get("requiresResourceReload").getAsBoolean();

                        if (!id.isBlank() && !indexUrl.isBlank()) {
                            CATEGORIES.add(new Category(id, indexUrl, targetDirectoryName, requiresResourceReload));
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to load store categories from namespace {}: {}", namespace, e.getMessage());
                }
            });
        }
    }

    public static Optional<Category> getCategory(String id) {
        return CATEGORIES.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    private static String getString(JsonObject object, String key, String defaultValue) {
        return object.has(key) ? object.get(key).getAsString() : defaultValue;
    }

    public static Path getContentDir(String folderName) {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve(folderName);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                LOGGER.warn("Failed to create content directory: {}", e.getMessage());
            }
        }
        return dir;
    }

    public static String readFileCheckSum(Path path) {
        try {
            ByteSource byteSource = com.google.common.io.Files.asByteSource(path.toFile());
            return byteSource.hash(Hashing.md5()).toString();
        } catch (IOException e) {
            LOGGER.warn("Error when reading existing checksum from {}: {}", path, e.getMessage());
            return null;
        }
    }

    public static boolean isPackInstalled(Pack pack, String folderName) {
        Path path = getContentDir(folderName).resolve(pack.id());
        if (!Files.exists(path) || !Files.isDirectory(path)) return false;

        if (pack.checkSum().isPresent()) {
            Path checksumFile = path.resolve(".md5");
            if (Files.exists(checksumFile)) {
                try {
                    String existingChecksum = Files.readString(checksumFile).trim();
                    return pack.checkSum().get().equals(existingChecksum);
                } catch (IOException e) {
                    return false;
                }
            }
            return false; 
        }

        return true;
    }

    public static void downloadPack(Pack pack, String folderName, Runnable onFinished) {
        if (isPackInstalled(pack, folderName)) {
            Minecraft.getInstance().execute(onFinished);
            return;
        }

        Path contentDir = getContentDir(folderName);
        CompletableFuture.runAsync(() -> {
            try {
                Path downloadedTempFile = Files.createTempFile("legacy_pack_", ".zip");
                
                try (InputStream stream = new URL(pack.downloadURI()).openStream()) {
                    Files.copy(stream, downloadedTempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                if (pack.checkSum().isPresent()) {
                    String fileHash = readFileCheckSum(downloadedTempFile);
                    if (!pack.checkSum().get().equals(fileHash)) {
                        LOGGER.warn("Checksum mismatch for pack {}. Expected {}, got {}", pack.id(), pack.checkSum().get(), fileHash);
                        Files.deleteIfExists(downloadedTempFile);
                        return; 
                    }
                }

                Path targetFolder = contentDir.resolve(pack.id());
                if (Files.exists(targetFolder)) {
                    deleteDirectoryRecursively(targetFolder.toFile());
                }
                Files.createDirectories(targetFolder);
                
                extractZip(downloadedTempFile, targetFolder);
                Files.deleteIfExists(downloadedTempFile);
                
                if (pack.checkSum().isPresent()) {
                    Files.writeString(targetFolder.resolve(".md5"), pack.checkSum().get());
                }

                Minecraft.getInstance().execute(onFinished);
            } catch (Exception e) {
                LOGGER.warn("Error when downloading content pack to {}: {}", contentDir.resolve(pack.id()), e.getMessage());
            }
        });
    }

    private static void deleteDirectoryRecursively(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectoryRecursively(file);
            }
        }
        directory.delete();
    }

    private static void extractZip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String normalizedName = entry.getName().replace('\\', '/');
                Path entryPath = targetDir.resolve(normalizedName);
                
                if (!entryPath.normalize().startsWith(targetDir.normalize())) {
                    throw new IOException("Zip entry is outside of the target directory: " + normalizedName);
                }

                if (entry.isDirectory() || normalizedName.endsWith("/")) {
                    Files.createDirectories(entryPath);
                } else {
                    if (entryPath.getParent() != null) {
                        Files.createDirectories(entryPath.getParent());
                    }
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
}