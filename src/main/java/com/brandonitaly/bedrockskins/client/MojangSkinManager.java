package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.util.ExternalAssetUtil;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MojangSkinManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MojangSkinManager() {}

    public static boolean isSkinSlim(LoadedSkin skin) {
        if (skin == null) return false;
        if (skin.geometryData != null) {
            String geo = skin.geometryData.toString().toLowerCase();
            return geo.contains("slim");
        }
        return false;
    }

    public static CompletableFuture<Void> uploadSkin(String accessToken, byte[] textureBytes, String variant) {
        if (textureBytes == null || textureBytes.length == 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Skin texture data is empty."));
        }

        String boundary = "----BedrockSkinsBoundary" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            // Part 1: variant
            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write("Content-Disposition: form-data; name=\"variant\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write((variant + "\r\n").getBytes(StandardCharsets.UTF_8));

            // Part 2: file
            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write("Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write(textureBytes);
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));

            // End boundary
            baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/skins"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                .build();

        return ContentManager.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 401 || response.statusCode() == 403) {
                        throw new RuntimeException("Unauthorized: Invalid or expired Minecraft session.");
                    }
                    if (response.statusCode() != 200 && response.statusCode() != 204) {
                        throw new RuntimeException("Failed to upload skin: HTTP " + response.statusCode());
                    }
                });
    }

    public static CompletableFuture<Void> resetSkin(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/skins/active"))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();

        return ContentManager.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200 && response.statusCode() != 204) {
                        throw new RuntimeException("Failed to reset skin: HTTP " + response.statusCode());
                    }
                });
    }
}
