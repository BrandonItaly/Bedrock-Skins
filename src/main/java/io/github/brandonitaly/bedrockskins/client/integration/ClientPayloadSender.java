package io.github.brandonitaly.bedrockskins.client.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if fabric {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//?}

/** Loader-specific client networking boundary. */
public final class ClientPayloadSender {
    private ClientPayloadSender() {}

    public static void send(CustomPacketPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (payload == null || client == null || client.player == null || client.getConnection() == null) {
            return;
        }

        //? if fabric {
        ClientPlayNetworking.send(payload);
        //?} else if neoforge {
        /*net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);*/
        //?}
    }
}
