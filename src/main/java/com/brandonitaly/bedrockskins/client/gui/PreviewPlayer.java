package com.brandonitaly.bedrockskins.client.gui;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.ClientAsset;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.core.ClientAsset.ResourceTexture;

public class PreviewPlayer extends RemotePlayer {

    @Override
    public boolean shouldShowName() {
        return false;
    }

    //? if >=1.21.11 {
    private Identifier forcedCape = null;
    //?} else {
    /*private ResourceLocation forcedCape = null;*/
    //?}
    private ClientAsset.Texture forcedCapeTexture = null;
    private ClientAsset.Texture forcedBody = null;
    private boolean useLocalPlayerModel = false;

    public PreviewPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    // Sets a cape to be forced on the player preview
    //? if >=1.21.11 {
    public void setForcedCape(Identifier cape) {
        this.forcedCape = cape;
        this.forcedCapeTexture = null;
    }
    //?} else {
    /*public void setForcedCape(ResourceLocation cape) {
        this.forcedCape = cape;
        this.forcedCapeTexture = null;
    }*/
    //?}

    public void setForcedCapeTexture(ClientAsset.Texture capeTexture) {
        this.forcedCapeTexture = capeTexture;
        this.forcedCape = null;
    }

    public void clearForcedCape() {
        this.forcedCape = null;
        this.forcedCapeTexture = null;
    }

    public void setForcedBody(ClientAsset.Texture body) {
        this.forcedBody = body;
    }

    public void clearForcedBody() {
        this.forcedBody = null;
    }

    public void setUseLocalPlayerModel(boolean useLocalPlayerModel) {
        this.useLocalPlayerModel = useLocalPlayerModel;
    }

    @Override
    public PlayerSkin getSkin() {
        PlayerSkin original = super.getSkin();
        ClientAsset.Texture body = forcedBody != null ? forcedBody : original.body();
        var model = original.model();
        if (useLocalPlayerModel && Minecraft.getInstance().player != null) {
            model = Minecraft.getInstance().player.getSkin().model();
        }

        ClientAsset.Texture cape = original.cape();
        if (forcedCapeTexture != null) {
            cape = forcedCapeTexture;
        }
        if (forcedCape != null) {
            // Create a new PlayerSkin with the forced cape
            ResourceTexture capeAsset = new ResourceTexture(forcedCape, forcedCape);
            return new PlayerSkin(
                body,
                capeAsset,
                original.elytra(),
                model,
                original.secure()
            );
        }
        if (forcedBody != null || useLocalPlayerModel) {
            return new PlayerSkin(
                body,
                cape,
                original.elytra(),
                model,
                original.secure()
            );
        }
        return original;
    }

    // Forces outer skin layers to render
    @Override
    public boolean isModelPartShown(PlayerModelPart part) {
        // Respect the client's options so changes update the preview instantly
        try {
            return Minecraft.getInstance().options.isModelPartEnabled(part);
        } catch (Exception e) {
            return true;
        }
    }

    public static final class PreviewPlayerPool {
        private static final Map<UUID, PreviewPlayer> pool = new ConcurrentHashMap<>();

        public static PreviewPlayer get(ClientLevel world, GameProfile profile) {
            UUID id = profile.id();
            if (id == null) id = UUID.randomUUID();

            return pool.computeIfAbsent(id, k -> new PreviewPlayer(world, profile));
        }

        public static void remove(UUID id) { pool.remove(id); }
        public static void clear() { pool.clear(); }
    }
}