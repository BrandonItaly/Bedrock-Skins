package io.github.brandonitaly.bedrockskins.server;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//? if fabric {
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//?} else if neoforge {
/*import net.neoforged.neoforge.network.PacketDistributor;*/
//?}

/** Bedrock and unmodded Java connections must not receive the mod's custom payloads. */
public final class ServerAppearanceNetworking {
    private ServerAppearanceNetworking() {}

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        //? if fabric {
        if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
        //?} else if neoforge {
        /*if (player.connection.hasChannel(payload.type().id())) PacketDistributor.sendToPlayer(player, payload);*/
        //?}
    }

    public static void broadcast(MinecraftServer server, CustomPacketPayload payload) {
        server.getPlayerList().getPlayers().forEach(player -> send(player, payload));
    }
}
