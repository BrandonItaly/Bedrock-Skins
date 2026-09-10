package io.github.brandonitaly.bedrockskins.pack.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Creates the writable overlay used when skins are added to any pack, including built-in packs. */
public final class WritableSkinPack {
    private static final String PACK_PREFIX = "skinpack.";

    private WritableSkinPack() {}

    public static Path ensure(String packId) throws IOException {
        String packName = packId != null && packId.startsWith(PACK_PREFIX)
            ? packId.substring(PACK_PREFIX.length()) : packId;
        if (packName == null || packName.isBlank()) {
            throw new IOException("Invalid skin pack id: " + packId);
        }

        Path directory = SkinPackLoader.getSkinPacksDir().toPath().resolve(packName);
        Files.createDirectories(directory);

        Path manifestFile = directory.resolve("skins.json");
        if (!Files.isRegularFile(manifestFile)) {
            JsonObject manifest = new JsonObject();
            manifest.add("skins", new JsonArray());
            manifest.addProperty("serialize_name", packName);
            manifest.addProperty("localization_name", packName);
            manifest.addProperty("pack_type", "custom");
            Files.writeString(manifestFile, manifest.toString());
        }

        Path languageFile = directory.resolve("texts").resolve("en_us.lang");
        if (!Files.isRegularFile(languageFile)) {
            LangFileEditor.put(languageFile, PACK_PREFIX + packName, packName);
        }
        return directory;
    }
}
