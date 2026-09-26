package io.github.brandonitaly.bedrockskins.api;

import io.github.brandonitaly.bedrockskins.network.BedrockSkinsNetworking;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-thread notifications for companion mods, after appearance validation. */
public final class ServerAppearanceEvents {
    public interface Listener {
        default void tick(MinecraftServer server) {}
        default void skinChanged(UUID player) {}
        default void cosmeticsChanged(UUID player, List<BedrockSkinsNetworking.CosmeticData> pieces) {}
        default void emote(ServerPlayer player, BedrockSkinsNetworking.PlayEmotePayload payload) {}
        default void disconnect(UUID player) {}
        default void stop() {}
    }

    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private ServerAppearanceEvents() {}

    /** Register once during mod initialization. Close the handle to unsubscribe. */
    public static AutoCloseable register(Listener listener) {
        java.util.Objects.requireNonNull(listener);
        LISTENERS.addIfAbsent(listener);
        return () -> LISTENERS.remove(listener);
    }
    public static void tick(MinecraftServer server) { LISTENERS.forEach(listener -> listener.tick(server)); }
    public static void skinChanged(UUID player) { LISTENERS.forEach(listener -> listener.skinChanged(player)); }
    public static void cosmeticsChanged(UUID player, List<BedrockSkinsNetworking.CosmeticData> pieces) {
        var snapshot = List.copyOf(pieces);
        LISTENERS.forEach(listener -> listener.cosmeticsChanged(player, snapshot));
    }
    public static void emote(ServerPlayer player, BedrockSkinsNetworking.PlayEmotePayload payload) {
        LISTENERS.forEach(listener -> listener.emote(player, payload));
    }
    public static void disconnect(UUID player) { LISTENERS.forEach(listener -> listener.disconnect(player)); }
    public static void stop() { LISTENERS.forEach(Listener::stop); }
}
