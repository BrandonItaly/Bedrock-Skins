package com.brandonitaly.bedrockskins.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class JsonCodecFileStore {
    private JsonCodecFileStore() {}

    private static final Logger LOGGER = LogUtils.getLogger();   
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static <T> T read(Path path, Codec<T> codec, T defaults, String name) {
        if (!Files.exists(path)) return defaults;
        
        try (Reader reader = Files.newBufferedReader(path)) {
            return codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                .resultOrPartial(msg -> LOGGER.warn("{}: decode warning: {}", name, msg))
                .orElse(defaults);
        } catch (Exception e) {
            LOGGER.error("{}: failed to load", name, e);
            return defaults;
        }
    }

    public static <T> void write(Path path, Codec<T> codec, T value, String name) {
        try {
            encodeAndWrite(path, codec, value, name);
        } catch (Exception e) {
            LOGGER.error("{}: failed to save", name, e);
        }
    }

    public static <T> void writeAtomic(Path path, Codec<T> codec, T value, String name) {
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            encodeAndWrite(tmp, codec, value, name);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ignored) {
                // Fallback if atomic move is not supported by the OS filesystem
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LOGGER.error("{}: failed to save", name, e);
        }
    }

    private static <T> void encodeAndWrite(Path path, Codec<T> codec, T value, String name) throws Exception {
        // 1. Ensure directories exist BEFORE creating the writer
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        
        // 2. Encode the data
        JsonElement element = codec.encodeStart(JsonOps.INSTANCE, value)
            .resultOrPartial(msg -> LOGGER.warn("{}: encode warning: {}", name, msg))
            .orElseGet(JsonObject::new);
            
        // 3. Safely write to the file
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(element, writer);
        }
    }
}