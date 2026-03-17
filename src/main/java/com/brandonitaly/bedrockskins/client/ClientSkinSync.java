package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.BedrockSkinsNetworking;
import com.brandonitaly.bedrockskins.pack.SkinId;
//? if fabric {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//?}

public final class ClientSkinSync {
    private ClientSkinSync() {}

    public static void sendSetSkinPayload(SkinId skinId, String geometry, byte[] textureData) {
        var payload = new BedrockSkinsNetworking.SetSkinPayload(skinId, geometry, textureData);
        //? if fabric {
        ClientPlayNetworking.send(payload);
        //?} else if neoforge {
        /*net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);*/
        //?}
    }

    private static final byte[] EMPTY_TEXTURE = new byte[0];

    public static void sendResetSkinPayload() {
        sendSetSkinPayload(null, "", EMPTY_TEXTURE);
    }
}