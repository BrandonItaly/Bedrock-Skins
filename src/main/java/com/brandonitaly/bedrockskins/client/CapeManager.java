package com.brandonitaly.bedrockskins.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CapeManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<String, Boolean> downloadingCapes = new ConcurrentHashMap<>();
    private static final java.util.Set<Identifier> registeredCapes = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static boolean isCapeRegistered(Identifier id) {
        return registeredCapes.contains(id);
    }

    public static class MinecraftCape {
        public final String id;
        public final String state;
        public final String url;
        public final String alias;
        public final Identifier textureIdentifier;

        public MinecraftCape(String id, String state, String url, String alias) {
            this.id = id;
            this.state = state;
            this.url = url;
            this.alias = alias;
            this.textureIdentifier = Identifier.fromNamespaceAndPath("bedrockskins", "capes/mojang/" + id.toLowerCase().replace("-", "_").replace(" ", "_"));
        }

        public MinecraftCape(String id, String state, String url, String alias, Identifier textureIdentifier) {
            this.id = id;
            this.state = state;
            this.url = url;
            this.alias = alias;
            this.textureIdentifier = textureIdentifier;
        }
    }

    private CapeManager() {}

    public static CompletableFuture<List<MinecraftCape>> fetchOwnedCapes(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return ContentManager.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 401 || response.statusCode() == 403) {
                        throw new RuntimeException("Unauthorized: Invalid or expired Minecraft session.");
                    }
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Failed to fetch profile: HTTP " + response.statusCode());
                    }
                    List<MinecraftCape> capes = new ArrayList<>();
                    JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
                    if (json.has("capes")) {
                        JsonArray capesArray = json.getAsJsonArray("capes");
                        for (JsonElement element : capesArray) {
                            JsonObject capeObj = element.getAsJsonObject();
                            String id = capeObj.get("id").getAsString();
                            String state = capeObj.get("state").getAsString();
                            String url = capeObj.get("url").getAsString();
                            String alias = capeObj.has("alias") ? capeObj.get("alias").getAsString() : id;
                            capes.add(new MinecraftCape(id, state, url, alias));
                        }
                    }
                    return capes;
                });
    }

    public static CompletableFuture<Void> equipCape(String accessToken, String capeId) {
        String jsonBody = "{\"capeId\":\"" + capeId + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/capes/active"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return ContentManager.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200 && response.statusCode() != 204) {
                        throw new RuntimeException("Failed to equip cape: HTTP " + response.statusCode());
                    }
                });
    }

    public static CompletableFuture<Void> unequipCape(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/capes/active"))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();
        return ContentManager.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200 && response.statusCode() != 204) {
                        throw new RuntimeException("Failed to unequip cape: HTTP " + response.statusCode());
                    }
                });
    }

    public static void downloadAndRegisterCape(MinecraftCape cape, Runnable onLoad) {
        if (cape.url == null || cape.url.isBlank()) {
            return;
        }

        Identifier texIdx = cape.textureIdentifier;

        if (isCapeRegistered(texIdx)) {
            return;
        }

        if (downloadingCapes.putIfAbsent(cape.id, true) == null) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cape.url))
                    .GET()
                    .build();
            ContentManager.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new UncheckedIOException(new java.io.IOException("HTTP error status code: " + response.statusCode()));
                    }
                    try (InputStream stream = response.body()) {
                        return NativeImage.read(stream);
                    } catch (java.io.IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .thenAccept(img -> {
                    if (img != null) {
                        Minecraft.getInstance().execute(() -> {
                            Minecraft.getInstance().getTextureManager().register(texIdx, new DynamicTexture(() -> "cape_mojang_" + cape.id, img));
                            registeredCapes.add(texIdx);
                            downloadingCapes.remove(cape.id);
                            if (onLoad != null) {
                                onLoad.run();
                            }
                        });
                    }
                })
                .exceptionally(e -> {
                    LOGGER.warn("Failed to download cape " + cape.id + " from " + cape.url + ": " + e.getMessage());
                    downloadingCapes.remove(cape.id);
                    return null;
                });
        }
    }
}
