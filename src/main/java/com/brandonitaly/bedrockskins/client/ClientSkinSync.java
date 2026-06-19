package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.BedrockSkinsNetworking;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
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
                byte[] textureData = com.brandonitaly.bedrockskins.util.ExternalAssetUtil.loadTextureData(skin, minecraft);
                
                byte[] capeData = new byte[0];
                SkinManager.ResolvedCape resolved = SkinManager.resolveCape(minecraft.player.getUUID(), skin, true);
                if (resolved != null && !resolved.capeId.equals(SkinManager.CAPE_NONE)) {
                    SkinId capeOverrideId = SkinManager.getLocalCapeOverride();
                    LoadedSkin capeSkin = null;
                    if (capeOverrideId != null && !capeOverrideId.equals(SkinManager.CAPE_NONE_SKIN_ID)) {
                        capeSkin = SkinPackLoader.getLoadedSkin(capeOverrideId);
                    } else if (capeOverrideId == null) {
                        capeSkin = skin;
                    }
                    if (capeSkin != null && capeSkin.cape != null) {
                        capeData = com.brandonitaly.bedrockskins.util.ExternalAssetUtil.loadTextureData(capeSkin.cape, minecraft);
                    }
                }
                sendSetSkinPayload(skinId, skin.geometryData.toString(), textureData, capeData);
            } catch (Exception e) {
                com.mojang.logging.LogUtils.getLogger().error("Failed to sync skin and cape", e);
            }
        }
    }
}