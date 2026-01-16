package com.brandonitaly.bedrockskins.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//? }

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class BedrockSkinsConfig {
    private static final Gson GSON = new Gson();
    //? if fabric {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("bedrockskins.json");
    //? }
    //? if neoforge {
    // private static final Path CONFIG_PATH = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("bedrockskins.json");
    //? }

    private static boolean scanResourcePacksForSkins = true;

    static {
        load();
    }

    public static synchronized boolean isScanResourcePacksForSkinsEnabled() {
        return scanResourcePacksForSkins;
    }

    public static synchronized void setScanResourcePacksForSkins(boolean enabled) {
        scanResourcePacksForSkins = enabled;
        save();
    }

    private static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                    JsonObject obj = GSON.fromJson(r, JsonObject.class);
                    if (obj != null && obj.has("scanResourcePacksForSkins")) {
                        scanResourcePacksForSkins = obj.get("scanResourcePacksForSkins").getAsBoolean();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("BedrockSkinsConfig: failed to load config: " + e);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("scanResourcePacksForSkins", scanResourcePacksForSkins);
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(obj, w);
            }
        } catch (IOException e) {
            System.out.println("BedrockSkinsConfig: failed to save config: " + e);
        }
    }
}