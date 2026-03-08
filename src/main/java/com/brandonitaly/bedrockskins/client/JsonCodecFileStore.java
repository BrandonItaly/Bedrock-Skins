package com.brandonitaly.bedrockskins.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class JsonCodecFileStore {
    private JsonCodecFileStore() {}

    private static final Gson GSON = new Gson();

    public static <T> T read(Path path, Codec<T> codec, T defaults, String name) {
        try {
            if (!Files.exists(path)) return defaults;
            try (Reader reader = Files.newBufferedReader(path)) {
                JsonElement element = JsonParser.parseReader(reader);
                return codec.parse(JsonOps.INSTANCE, element)
                    .resultOrPartial(msg -> System.out.println(name + ": decode warning: " + msg))
                    .orElse(defaults);
            }
        } catch (Exception e) {
            BedrockSkinsLog.error(name + ": failed to load", e);
            return defaults;
        }
    }

    public static <T> void write(Path path, Codec<T> codec, T value, String name) {
        try {
            createDir(path, name, codec.encodeStart(JsonOps.INSTANCE, value), Files.newBufferedWriter(path));
        } catch (Exception e) {
            BedrockSkinsLog.error(name + ": failed to save", e);
        }
    }

    private static void createDir(Path path, String name, DataResult<JsonElement> jsonElementDataResult, BufferedWriter bufferedWriter) throws IOException {
        Files.createDirectories(path.getParent());
        JsonElement element = jsonElementDataResult
            .resultOrPartial(msg -> System.out.println(name + ": encode warning: " + msg))
            .orElseGet(JsonObject::new);
        try (Writer writer = bufferedWriter) {
            GSON.toJson(element, writer);
        }
    }

    public static <T> void writeAtomic(Path path, Codec<T> codec, T value, String name) {
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            createDir(path, name, codec.encodeStart(JsonOps.INSTANCE, value), Files.newBufferedWriter(tmp));
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ignored) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            BedrockSkinsLog.error(name + ": failed to save", e);
        }
    }
}
