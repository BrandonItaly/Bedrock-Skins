package com.brandonitaly.bedrockskins.client.gui;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

public final class PreviewPlayer {

    private final GameProfile profile;
    private boolean showNameTag = false;
    private Component displayName;
    private ClientAsset.Texture forcedCapeTexture = null;
    private boolean hasForcedCapeOverride = false;
    private ClientAsset.Texture forcedBody = null;
    private PlayerSkin forcedProfileSkin = null;
    private boolean useLocalPlayerModel = false;

    public PreviewPlayer(GameProfile profile) {
        this.profile = profile;
        this.displayName = Component.literal(profile.name() == null ? "Preview" : profile.name());
    }

    public UUID getUuid() {
        return profile.id();
    }

    public GameProfile getProfile() {
        return profile;
    }

    public boolean shouldShowName() {
        return showNameTag;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public void setShowNameTag(boolean showNameTag) {
        this.showNameTag = showNameTag;
    }

    public void setDisplayName(Component displayName) {
        this.displayName = displayName;
    }

    // Sets a cape to be forced on the player preview
    public void setForcedCape(Identifier cape) {
        this.hasForcedCapeOverride = true;
        this.forcedCapeTexture = cape != null ? new ResourceTexture(cape, cape) : null;
    }

    public void clearForcedCape() {
        this.hasForcedCapeOverride = false;
        this.forcedCapeTexture = null;
    }

    public void clearForcedBody() {
        this.forcedBody = null;
    }

    public void setForcedBody(Identifier body) {
        this.forcedBody = body != null ? new ResourceTexture(body, body) : null;
    }

    public void setForcedProfileSkin(PlayerSkin skin) {
        this.forcedProfileSkin = skin;
    }

    public void clearForcedProfileSkin() {
        this.forcedProfileSkin = null;
    }

    public void setUseLocalPlayerModel(boolean useLocalPlayerModel) {
        this.useLocalPlayerModel = useLocalPlayerModel;
    }

    public PlayerSkin getSkin(Minecraft minecraft) {
        PlayerSkin original;

        if (forcedProfileSkin != null) {
            original = forcedProfileSkin;
        } else {
            PlayerSkin sessionSkin = BedrockSessionSkin.getSessionPlayerSkin();
            if (sessionSkin != null) {
                original = sessionSkin;
            } else if (minecraft.player != null) {
                original = minecraft.player.getSkin();
            } else {
                original = DefaultPlayerSkin.get(profile.id());
            }
        }

        ClientAsset.Texture finalBody = forcedBody != null ? forcedBody : original.body();
        ClientAsset.Texture finalCape = hasForcedCapeOverride ? forcedCapeTexture : original.cape();

        var finalModel = (useLocalPlayerModel && minecraft.player != null)
            ? minecraft.player.getSkin().model()
            : original.model();

        // Only allocate a new PlayerSkin if something actually changed.
        if (finalBody == original.body() && finalCape == original.cape() && finalModel == original.model()) {
            return original;
        }

        return new PlayerSkin(finalBody, finalCape, original.elytra(), finalModel, original.secure());
    }

    public static final class PreviewPlayerPool {
        private static final Map<UUID, PreviewPlayer> pool = new ConcurrentHashMap<>();

        public static PreviewPlayer get(GameProfile profile) {
            UUID id = profile.id() != null ? profile.id() : UUID.randomUUID();
            return pool.computeIfAbsent(id, ignored -> new PreviewPlayer(profile));
        }

        public static void remove(UUID id) { pool.remove(id); }
    }
}