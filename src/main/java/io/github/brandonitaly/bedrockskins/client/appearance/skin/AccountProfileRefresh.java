package io.github.brandonitaly.bedrockskins.client.appearance.skin;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.github.brandonitaly.bedrockskins.client.ContentManager;
import io.github.brandonitaly.bedrockskins.gui.preview.BedrockSessionSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Refreshes the local appearance from the authenticated account, avoiding stale session textures. */
public final class AccountProfileRefresh {
    private static UUID accountId;
    private static PlayerSkin appearance;
    private static long revision;

    private AccountProfileRefresh() {}

    public static PlayerSkin current(UUID id) {
        return id != null && id.equals(accountId) ? appearance : null;
    }

    public static CompletableFuture<Void> refresh(String token) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID id = minecraft.getUser().getProfileId();
        String name = minecraft.getUser().getName();
        long requestRevision = ++revision;
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
            .header("Authorization", "Bearer " + token).GET().build();
        return ContentManager.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Account changed, but profile refresh failed: HTTP " + response.statusCode());
                }
                return textureProfile(id, name, JsonParser.parseString(response.body()).getAsJsonObject());
            })
            .thenComposeAsync(profile -> minecraft.getSkinManager().get(profile), minecraft)
            .thenAcceptAsync(result -> {
                PlayerSkin skin = result.orElseThrow(() -> new IllegalStateException("Account changed, but refreshed textures could not be loaded."));
                if (requestRevision != revision || !id.equals(minecraft.getUser().getProfileId())) return;
                accountId = id;
                appearance = skin;
                BedrockSessionSkin.clearCache();
            }, minecraft);
    }

    static GameProfile textureProfile(UUID id, String name, JsonObject account) {
        if (!account.get("id").getAsString().replace("-", "").equalsIgnoreCase(id.toString().replace("-", ""))) {
            throw new IllegalStateException("Refreshed profile belongs to a different account.");
        }
        JsonObject textures = new JsonObject();
        for (String kind : new String[]{"skins", "capes"}) {
            if (!account.has(kind)) continue;
            for (var entry : account.getAsJsonArray(kind)) {
                JsonObject asset = entry.getAsJsonObject();
                if (!"ACTIVE".equals(asset.get("state").getAsString())) continue;
                JsonObject texture = new JsonObject();
                texture.addProperty("url", asset.get("url").getAsString());
                if (kind.equals("skins") && asset.has("variant")) {
                    JsonObject metadata = new JsonObject();
                    metadata.addProperty("model", "SLIM".equalsIgnoreCase(asset.get("variant").getAsString()) ? "slim" : "default");
                    texture.add("metadata", metadata);
                }
                textures.add(kind.equals("skins") ? "SKIN" : "CAPE", texture);
                break;
            }
        }
        JsonObject payload = new JsonObject();
        payload.add("textures", textures);
        String encoded = Base64.getEncoder().encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
        // Only used locally; authenticated account data is not a signed multiplayer profile.
        return new GameProfile(id, name, new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", encoded))));
    }
}
