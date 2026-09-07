package io.github.brandonitaly.bedrockskins.client.appearance.skin;

import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;

import io.github.brandonitaly.bedrockskins.network.BedrockSkinsNetworking;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedEmote;
//? if fabric {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//?}

public final class ClientSkinSync {
    private ClientSkinSync() {}

    public static void sendSetSkinPayload(SkinId skinId, String geometry, byte[] textureData, byte[] capeData) {
        var payload = new BedrockSkinsNetworking.SetSkinPayload(skinId, geometry, textureData, capeData);
        //? if fabric {
        ClientPlayNetworking.send(payload);
        //?} else if neoforge {
        /*net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);*/
        //?}
    }

    public static void sendRequestSkinDataPayload(String hash) {
        var payload = new BedrockSkinsNetworking.RequestSkinDataPayload(hash);
        //? if fabric {
        ClientPlayNetworking.send(payload);
        //?} else if neoforge {
        /*net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);*/
        //?}
    }

    private static final byte[] EMPTY_TEXTURE = new byte[0];

    public static void sendResetSkinPayload() {
        sendSetSkinPayload(null, "", EMPTY_TEXTURE, EMPTY_TEXTURE);
    }

    public static void syncCurrentCosmetics() {
        var payload = new BedrockSkinsNetworking.SetCosmeticsPayload(PersonaManager.localPayload());
        //? if fabric {
        ClientPlayNetworking.send(payload);
        //?} else if neoforge {
        /*net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);*/
        //?}
    }

    public static void sendEmote(LoadedEmote emote) {
        if (emote == null) return;
        var payload = new BedrockSkinsNetworking.PlayEmotePayload(emote.id(), emote.displayName(),
            emote.animationName(), emote.animation().toString(), emote.duration());
        //? if fabric {
        ClientPlayNetworking.send(payload);
        //?} else if neoforge {
        /*net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);*/
        //?}
    }

    public static void sendStopEmote() {
        var payload = new BedrockSkinsNetworking.PlayEmotePayload(
            BedrockSkinsNetworking.STOP_EMOTE_ID, "", "", "", 0.0f);
        //? if fabric {
        ClientPlayNetworking.send(payload);
        //?} else if neoforge {
        /*net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);*/
        //?}
    }

    public static void syncCurrentSkin(net.minecraft.client.Minecraft minecraft) {
        if (minecraft.player == null) return;
        SkinId skinId = SkinManager.getLocalSelectedKey();
        if (skinId == null) {
            sendResetSkinPayload();
            return;
        }
        LoadedSkin skin = SkinPackLoader.getLoadedSkin(skinId);
        if (skin != null) {
            try {
                byte[] textureData = io.github.brandonitaly.bedrockskins.util.ExternalAssetUtil.loadTextureData(skin, minecraft);
                
                byte[] capeData = new byte[0];
                var resolved = SkinManager.resolveCape(skin, true);
                if (resolved != null && !resolved.equals(SkinManager.CAPE_NONE)) {
                    SkinId capeOverrideId = SkinManager.getLocalCapeOverride();
                    LoadedSkin capeSkin = null;
                    if (capeOverrideId != null && !capeOverrideId.equals(SkinManager.CAPE_NONE_SKIN_ID)) {
                        capeSkin = SkinPackLoader.getLoadedSkin(capeOverrideId);
                    } else if (capeOverrideId == null) {
                        capeSkin = skin;
                    }
                    if (capeSkin != null && capeSkin.cape != null) {
                        capeData = io.github.brandonitaly.bedrockskins.util.ExternalAssetUtil.loadTextureData(capeSkin.cape, minecraft);
                    }
                }
                sendSetSkinPayload(skinId, skin.geometryData.toString(), textureData, capeData);
            } catch (Exception e) {
                com.mojang.logging.LogUtils.getLogger().error("Failed to sync skin and cape", e);
            }
        }
    }
}
