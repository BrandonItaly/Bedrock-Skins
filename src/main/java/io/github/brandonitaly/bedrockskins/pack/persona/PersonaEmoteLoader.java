package io.github.brandonitaly.bedrockskins.pack.persona;

import io.github.brandonitaly.bedrockskins.pack.model.LoadedEmote;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Optional;

/** Loads the animation payload from a Persona emote directory. */
public final class PersonaEmoteLoader {
    private PersonaEmoteLoader() {}

    public static Optional<LoadedEmote> load(File directory) {
        File[] metadataFiles = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".meta.json"));
        if (metadataFiles == null || metadataFiles.length == 0) return Optional.empty();
        try {
            JsonObject metadata = read(metadataFiles[0]);
            if (!"persona_emote".equals(string(metadata, "piece_type"))) return Optional.empty();
            var sources = metadata.getAsJsonArray("animation_sources");
            if (sources == null || sources.isEmpty()) return Optional.empty();
            JsonObject source = sources.get(0).getAsJsonObject();
            String animationName = string(source, "name");
            String filename = string(source, "animationFile");
            if (filename == null) filename = string(source, "animation_file");
            if (filename == null) return Optional.empty();
            File animationFile = new File(directory, filename);
            if (!animationFile.isFile()) return Optional.empty();
            JsonObject root = read(animationFile);
            JsonObject animations = root.getAsJsonObject("animations");
            if (animations == null || animations.isEmpty()) return Optional.empty();
            if (animationName == null || !animations.has(animationName)) animationName = animations.keySet().iterator().next();
            JsonObject animation = animations.getAsJsonObject(animationName);
            float duration = animation.has("animation_length") ? animation.get("animation_length").getAsFloat() : maxTime(animation);
            if (duration <= 0.0f) return Optional.empty();
            String id = string(metadata, "piece_id");
            String pieceName = string(metadata, "piece_name");
            if (id == null || id.isBlank()) id = pieceName;
            String displayName = PersonaLocalization.displayName(directory, pieceName);
            return Optional.of(new LoadedEmote(id, displayName, animationName, animation.deepCopy(), duration,
                loadThumbnail(directory)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static byte[] loadThumbnail(File directory) {
        try {
            File directThumbnail = new File(directory, "thumbnail.png");
            if (directThumbnail.isFile()) {
                return Files.readAllBytes(directThumbnail.toPath());
            }

            File[] caseInsensitive = directory.listFiles((dir, name) -> name.equalsIgnoreCase("thumbnail.png"));
            if (caseInsensitive != null && caseInsensitive.length > 0 && caseInsensitive[0].isFile()) {
                return Files.readAllBytes(caseInsensitive[0].toPath());
            }
        } catch (Exception ignored) {
        }
        return new byte[0];
    }

    private static float maxTime(JsonElement value) {
        float max = 0.0f;
        if (value == null) return max;
        if (value.isJsonObject()) {
            for (var entry : value.getAsJsonObject().entrySet()) {
                try { max = Math.max(max, Float.parseFloat(entry.getKey())); } catch (NumberFormatException ignored) {}
                max = Math.max(max, maxTime(entry.getValue()));
            }
        } else if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) max = Math.max(max, maxTime(child));
        }
        return max;
    }

    private static JsonObject read(File file) throws Exception {
        return JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }
}
