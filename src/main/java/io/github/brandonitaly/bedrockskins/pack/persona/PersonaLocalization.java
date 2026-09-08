package io.github.brandonitaly.bedrockskins.pack.persona;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves per-piece and shared Bedrock Persona language entries. */
final class PersonaLocalization {
    private static final Map<String, Map<String, String>> LANGUAGE_CACHE = new ConcurrentHashMap<>();

    private PersonaLocalization() {}

    static void clearCache() {
        LANGUAGE_CACHE.clear();
    }

    static String displayName(File pieceDirectory, String fallback) {
        List<String> keys = new ArrayList<>();
        String storeKey = storeNameKey(pieceDirectory);
        if (storeKey != null && !storeKey.isBlank()) keys.add(storeKey);
        keys.add("persona.offer.title");
        if (fallback != null && !fallback.isBlank()) keys.add("persona." + fallback + ".title");

        for (File root = pieceDirectory, previous = null; root != null && root != previous; previous = root, root = root.getParentFile()) {
            File language = languageFile(root);
            if (language != null) {
                String translated = find(language, keys);
                if (translated != null) return translated;
            }
            if ("persona".equalsIgnoreCase(root.getName())) break;
        }
        return fallback == null ? pieceDirectory.getName() : fallback.replace('_', ' ');
    }

    private static String storeNameKey(File directory) {
        File[] files = directory.listFiles((dir, name) ->
            name.toLowerCase(Locale.ROOT).endsWith(".store_info.json"));
        if (files == null || files.length == 0) return null;
        try {
            JsonObject json = JsonParser.parseString(
                Files.readString(files[0].toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            return json.has("piece_store_name") ? json.get("piece_store_name").getAsString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static File languageFile(File root) {
        File texts = new File(root, "texts");
        File[] files = texts.listFiles((dir, name) -> name.equalsIgnoreCase("en_US.lang"));
        return files != null && files.length > 0 ? files[0] : null;
    }

    private static String find(File language, List<String> keys) {
        Map<String, String> entries = LANGUAGE_CACHE.computeIfAbsent(language.getAbsolutePath(), ignored -> {
            Map<String, String> parsed = new HashMap<>();
            try {
                for (String line : Files.readAllLines(language.toPath(), StandardCharsets.UTF_8)) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    int separator = line.indexOf('=');
                    if (separator <= 0) continue;
                    String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
                    parsed.putIfAbsent(key, line.substring(separator + 1).split("\\t#", 2)[0].trim());
                }
            } catch (Exception ignoredRead) {
            }
            return Map.copyOf(parsed);
        });
        for (String key : keys) {
            String translated = entries.get(key.toLowerCase(Locale.ROOT));
            if (translated != null) return translated;
        }
        return null;
    }
}
