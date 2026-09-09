package io.github.brandonitaly.bedrockskins.pack.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Read-modify-write operations for a Bedrock skin pack's skins.json file. */
public final class SkinManifestEditor {
    private static final String SKINS = "skins";
    private static final String LOCALIZATION_NAME = "localization_name";
    private SkinManifestEditor() {}

    public static void add(Path file, JsonObject skin) throws IOException {
        JsonObject root = read(file);
        skins(root).add(skin);
        write(file, root);
    }

    public static boolean update(Path file, String localizationName, Consumer<JsonObject> update) throws IOException {
        if (!Files.isRegularFile(file)) return false;
        JsonObject root = read(file);
        for (JsonElement element : skins(root)) {
            JsonObject skin = element.getAsJsonObject();
            if (!matches(skin, localizationName)) continue;
            update.accept(skin);
            write(file, root);
            return true;
        }
        return false;
    }

    public static List<JsonObject> remove(Path file, String localizationName) throws IOException {
        if (!Files.isRegularFile(file)) return List.of();
        JsonObject root = read(file);
        JsonArray retained = new JsonArray();
        List<JsonObject> removed = new ArrayList<>();
        for (JsonElement element : skins(root)) {
            JsonObject skin = element.getAsJsonObject();
            if (matches(skin, localizationName)) removed.add(skin.deepCopy());
            else retained.add(skin);
        }
        if (!removed.isEmpty()) {
            root.add(SKINS, retained);
            write(file, root);
        }
        return List.copyOf(removed);
    }

    private static boolean matches(JsonObject skin, String localizationName) {
        return skin.has(LOCALIZATION_NAME) && localizationName.equals(skin.get(LOCALIZATION_NAME).getAsString());
    }

    private static JsonObject read(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return new JsonObject();
        try (var reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonArray skins(JsonObject root) {
        if (!root.has(SKINS) || !root.get(SKINS).isJsonArray()) root.add(SKINS, new JsonArray());
        return root.getAsJsonArray(SKINS);
    }

    private static void write(Path file, JsonObject root) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, root.toString());
    }
}
