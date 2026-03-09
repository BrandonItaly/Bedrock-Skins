package com.brandonitaly.bedrockskins.client.gui;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import com.brandonitaly.bedrockskins.client.dummy.DummyClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.GameType;
import net.minecraft.core.ClientAsset.ResourceTexture;

public class PreviewPlayer extends RemotePlayer {

    private boolean showNameTag = false;
    private ClientAsset.Texture forcedCapeTexture = null;
    private ClientAsset.Texture forcedBody = null;
    private PlayerSkin forcedProfileSkin = null;
    private boolean useLocalPlayerModel = false;

    public PreviewPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Override
    public boolean shouldShowName() {
        return showNameTag || this.isCustomNameVisible();
    }

    public void setShowNameTag(boolean showNameTag) {
        this.showNameTag = showNameTag;
    }

    // Sets a cape to be forced on the player preview
    public void setForcedCape(Identifier cape) {
        this.forcedCapeTexture = cape != null ? new ResourceTexture(cape, cape) : null;
    }

    public void clearForcedCape() {
        this.forcedCapeTexture = null;
    }

    public void clearForcedBody() {
        this.forcedBody = null;
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

    @Override
    public GameType gameMode() {
        return GameType.SURVIVAL;
    }

    @Override
    public PlayerSkin getSkin() {
        Minecraft mc = Minecraft.getInstance();
        PlayerSkin original;

        if (forcedProfileSkin != null) {
            original = forcedProfileSkin;
        } else if (mc.getConnection() != null) {
            original = super.getSkin();
        } else if (mc.player != null) {
            original = mc.player.getSkin();
        } else {
            original = DefaultPlayerSkin.get(this.getUUID());
        }

        ClientAsset.Texture finalBody = forcedBody != null ? forcedBody : original.body();
        ClientAsset.Texture finalCape = forcedCapeTexture != null ? forcedCapeTexture : original.cape();
        
        var finalModel = (useLocalPlayerModel && mc.player != null) 
            ? mc.player.getSkin().model() 
            : original.model();

        // Only allocate a new PlayerSkin if something actually changed.
        if (finalBody == original.body() && finalCape == original.cape() && finalModel == original.model()) {
            return original;
        }

        return new PlayerSkin(finalBody, finalCape, original.elytra(), finalModel, original.secure());
    }

    // Forces outer skin layers to render
    @Override
    public boolean isModelPartShown(PlayerModelPart part) {
        // Respect the client's options so changes update the preview instantly
        Minecraft mc = Minecraft.getInstance();
        return mc.options.isModelPartEnabled(part);
    }

    public static final class PreviewPlayerPool {
        private static final Map<UUID, PreviewPlayer> pool = new ConcurrentHashMap<>();

        public static PreviewPlayer get(ClientLevel world, GameProfile profile) {
            UUID id = profile.id() != null ? profile.id() : UUID.randomUUID();

            return pool.compute(id, (k, existing) -> {
                if (existing != null && existing.level() == world) {
                    return existing;
                }
                return new PreviewPlayer(world, profile);
            });
        }

        public static PreviewPlayer get(GameProfile profile) {
            return get(DummyClientLevel.getPreviewLevel(), profile);
        }

        public static void remove(UUID id) { pool.remove(id); }
    }
}