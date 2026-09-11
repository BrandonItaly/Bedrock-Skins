package io.github.brandonitaly.bedrockskins.gui.preview;

import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public final class PreviewPlayer implements AutoCloseable {

    private final UUID uuid = UUID.randomUUID();
    private boolean showNameTag = false;
    private final Component displayName;
    private ClientAsset.Texture forcedCapeTexture = null;
    private boolean hasForcedCapeOverride = false;
    private ClientAsset.Texture forcedBody = null;
    private PlayerSkin forcedProfileSkin = null;
    private PlayerModelType forcedModel = null;

    public PreviewPlayer(String name) {
        this.displayName = Component.literal(name == null ? "Preview" : name);
    }

    public UUID getUuid() {
        return uuid;
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

    public void setForcedModel(PlayerModelType model) {
        this.forcedModel = model;
    }

    public void clearForcedModel() {
        this.forcedModel = null;
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
                original = DefaultPlayerSkin.get(uuid);
            }
        }

        ClientAsset.Texture finalBody = forcedBody != null ? forcedBody : original.body();
        ClientAsset.Texture finalCape = original.cape();
        if (hasForcedCapeOverride) {
            if (forcedCapeTexture == null) {
                finalCape = null;
            } else {
                Identifier path = forcedCapeTexture.texturePath();
                if (path.getNamespace().equals("bedrockskins") && path.getPath().startsWith("capes/mojang/")) {
                    if (io.github.brandonitaly.bedrockskins.client.appearance.cape.CapeManager.isCapeRegistered(path)) {
                        finalCape = forcedCapeTexture;
                    } else {
                        finalCape = null;
                    }
                } else {
                    finalCape = forcedCapeTexture;
                }
            }
        }

        var finalModel = forcedModel != null
            ? forcedModel
            : original.model();

        // Only allocate a new PlayerSkin if something actually changed.
        if (finalBody == original.body() && finalCape == original.cape() && finalModel == original.model()) {
            return original;
        }

        return new PlayerSkin(finalBody, finalCape, original.elytra(), finalModel, original.secure());
    }

    @Override
    public void close() {
        SkinManager.resetPreviewSkin(getUuid());
        PersonaManager.clearPreview(getUuid());
        EmoteManager.stop(getUuid());
    }
}
