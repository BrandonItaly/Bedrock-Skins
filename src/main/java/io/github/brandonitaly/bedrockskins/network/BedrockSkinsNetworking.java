package io.github.brandonitaly.bedrockskins.network;

import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class BedrockSkinsNetworking {
    private BedrockSkinsNetworking() {}
    public static final String STOP_EMOTE_ID = "bedrockskins:stop";

    private static final HexFormat HEX = HexFormat.of();
    private static final byte[] EMPTY_BYTES = new byte[0];

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static byte[] nullToEmpty(byte[] b) { return b == null ? EMPTY_BYTES : b; }

    private static final StreamCodec<RegistryFriendlyByteBuf, SkinId> OPTIONAL_SKIN_ID_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, SkinId skinId) {
            if (skinId == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                buf.writeUtf(skinId.toString(), 256);
            }
        }

        @Override
        public SkinId decode(RegistryFriendlyByteBuf buf) {
            return buf.readBoolean() ? SkinId.parse(buf.readUtf(256)) : null;
        }
    };

    // GZIP compress the JSON geometry string
    private static final StreamCodec<RegistryFriendlyByteBuf, String> COMPRESSED_GEOMETRY_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, String string) {
            if (string == null || string.isEmpty()) {
                buf.writeByteArray(new byte[0]);
                return;
            }
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                    gzip.write(string.getBytes(StandardCharsets.UTF_8));
                }
                buf.writeByteArray(baos.toByteArray());
            } catch (IOException e) {
                buf.writeByteArray(new byte[0]);
            }
        }

        @Override
        public String decode(RegistryFriendlyByteBuf buf) {
            // Read max 50KB of compressed data
            byte[] bytes = buf.readByteArray(50_000); 
            if (bytes.length == 0) return "";
            
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                // Cap decompressed output at 150KB
                byte[] uncompressed = gzip.readNBytes(150_000);
                if (gzip.read() != -1) {
                    throw new DecoderException("Geometry JSON exceeded maximum safe length!");
                }
                return new String(uncompressed, StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "";
            }
        }
    };

    // Skin Update Payload (Server -> Client)
    public record SkinUpdatePayload(UUID uuid, SkinId skinId, String geometry, byte[] textureData, byte[] capeData) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SkinUpdatePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "skin_update"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, SkinUpdatePayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SkinUpdatePayload::uuid,
            OPTIONAL_SKIN_ID_CODEC, SkinUpdatePayload::skinId,
            COMPRESSED_GEOMETRY_CODEC, SkinUpdatePayload::geometry,
            ByteBufCodecs.byteArray(1048576), SkinUpdatePayload::textureData,
            ByteBufCodecs.byteArray(262144), SkinUpdatePayload::capeData,
            SkinUpdatePayload::new
        );

        public SkinUpdatePayload(UUID uuid, SkinId skinId, String geometry, byte[] textureData, byte[] capeData) {
            this.uuid = uuid;
            this.skinId = skinId;
            this.geometry = nullToEmpty(geometry);
            this.textureData = nullToEmpty(textureData);
            this.capeData = nullToEmpty(capeData);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    // Set Skin Payload (Client -> Server)
    public record SetSkinPayload(SkinId skinId, String geometry, byte[] textureData, byte[] capeData) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetSkinPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "set_skin"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, SetSkinPayload> CODEC = StreamCodec.composite(
            OPTIONAL_SKIN_ID_CODEC, SetSkinPayload::skinId,
            COMPRESSED_GEOMETRY_CODEC, SetSkinPayload::geometry,
            ByteBufCodecs.byteArray(1048576), SetSkinPayload::textureData,
            ByteBufCodecs.byteArray(262144), SetSkinPayload::capeData,
            SetSkinPayload::new
        );

        public SetSkinPayload(SkinId skinId, String geometry, byte[] textureData, byte[] capeData) {
            this.skinId = skinId;
            this.geometry = nullToEmpty(geometry);
            this.textureData = nullToEmpty(textureData);
            this.capeData = nullToEmpty(capeData);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, String> HASH_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, String hash) {
            buf.writeUtf(hash == null ? "" : hash, 256);
        }

        @Override
        public String decode(RegistryFriendlyByteBuf buf) {
            return buf.readUtf(256);
        }
    };

    public static String computeHash(String geometry, byte[] textureData) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            if (geometry != null) digest.update(geometry.getBytes(StandardCharsets.UTF_8));
            if (textureData != null) digest.update(textureData);
            return HEX.formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // Skin Announce Payload (Server -> Client)
    public record SkinAnnouncePayload(UUID uuid, SkinId skinId, String hash) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SkinAnnouncePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "skin_announce"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, SkinAnnouncePayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SkinAnnouncePayload::uuid,
            OPTIONAL_SKIN_ID_CODEC, SkinAnnouncePayload::skinId,
            HASH_CODEC, SkinAnnouncePayload::hash,
            SkinAnnouncePayload::new
        );

        public SkinAnnouncePayload(UUID uuid, SkinId skinId, String hash) {
            this.uuid = uuid;
            this.skinId = skinId;
            this.hash = nullToEmpty(hash);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    // Request Skin Data Payload (Client -> Server)
    public record RequestSkinDataPayload(String hash) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestSkinDataPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "request_skin_data"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestSkinDataPayload> CODEC = StreamCodec.composite(
            HASH_CODEC, RequestSkinDataPayload::hash,
            RequestSkinDataPayload::new
        );

        public RequestSkinDataPayload(String hash) {
            this.hash = nullToEmpty(hash);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record CosmeticData(String id, String type, String name, Set<String> zones,
                               String geometry, String slimGeometry, byte[] textureData, int tintColor, String side) {
        public CosmeticData {
            id = nullToEmpty(id);
            type = nullToEmpty(type);
            name = nullToEmpty(name);
            zones = zones == null ? Set.of() : Set.copyOf(zones);
            geometry = nullToEmpty(geometry);
            slimGeometry = slimGeometry == null || slimGeometry.isEmpty() ? geometry : slimGeometry;
            textureData = nullToEmpty(textureData);
            tintColor &= 0xFFFFFF;
            side = nullToEmpty(side);
        }
    }

    private static void writeCosmetics(RegistryFriendlyByteBuf buf, List<CosmeticData> cosmetics) {
        List<CosmeticData> safe = cosmetics == null ? List.of() : cosmetics;
        buf.writeVarInt(Math.min(safe.size(), 8));
        for (int i = 0; i < Math.min(safe.size(), 8); i++) {
            CosmeticData cosmetic = safe.get(i);
            buf.writeUtf(cosmetic.id(), 128);
            buf.writeUtf(cosmetic.type(), 64);
            buf.writeUtf(cosmetic.name(), 128);
            List<String> zones = cosmetic.zones().stream().limit(32).toList();
            buf.writeVarInt(zones.size());
            for (String zone : zones) buf.writeUtf(zone, 64);
            COMPRESSED_GEOMETRY_CODEC.encode(buf, cosmetic.geometry());
            COMPRESSED_GEOMETRY_CODEC.encode(buf, cosmetic.slimGeometry());
            buf.writeByteArray(cosmetic.textureData());
            buf.writeInt(cosmetic.tintColor());
            buf.writeUtf(cosmetic.side(), 8);
        }
    }

    private static List<CosmeticData> readCosmetics(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > 8) throw new DecoderException("Invalid cosmetic count: " + size);
        List<CosmeticData> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf(128);
            String type = buf.readUtf(64);
            String name = buf.readUtf(128);
            int zoneCount = buf.readVarInt();
            if (zoneCount < 0 || zoneCount > 32) throw new DecoderException("Invalid Persona zone count: " + zoneCount);
            java.util.LinkedHashSet<String> zones = new java.util.LinkedHashSet<>();
            for (int zoneIndex = 0; zoneIndex < zoneCount; zoneIndex++) zones.add(buf.readUtf(64));
            result.add(new CosmeticData(id, type, name, zones,
                COMPRESSED_GEOMETRY_CODEC.decode(buf), COMPRESSED_GEOMETRY_CODEC.decode(buf),
                buf.readByteArray(1_048_576), buf.readInt(), buf.readUtf(8)));
        }
        return List.copyOf(result);
    }

    public record SetCosmeticsPayload(List<CosmeticData> cosmetics) implements CustomPacketPayload {
        public static final Type<SetCosmeticsPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "set_cosmetics"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetCosmeticsPayload> CODEC = new StreamCodec<>() {
            @Override public void encode(RegistryFriendlyByteBuf buf, SetCosmeticsPayload value) { writeCosmetics(buf, value.cosmetics()); }
            @Override public SetCosmeticsPayload decode(RegistryFriendlyByteBuf buf) { return new SetCosmeticsPayload(readCosmetics(buf)); }
        };
        public SetCosmeticsPayload { cosmetics = cosmetics == null ? List.of() : List.copyOf(cosmetics); }
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record CosmeticsUpdatePayload(UUID uuid, List<CosmeticData> cosmetics) implements CustomPacketPayload {
        public static final Type<CosmeticsUpdatePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "cosmetics_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticsUpdatePayload> CODEC = new StreamCodec<>() {
            @Override public void encode(RegistryFriendlyByteBuf buf, CosmeticsUpdatePayload value) {
                UUIDUtil.STREAM_CODEC.encode(buf, value.uuid());
                writeCosmetics(buf, value.cosmetics());
            }
            @Override public CosmeticsUpdatePayload decode(RegistryFriendlyByteBuf buf) {
                return new CosmeticsUpdatePayload(UUIDUtil.STREAM_CODEC.decode(buf), readCosmetics(buf));
            }
        };
        public CosmeticsUpdatePayload { cosmetics = cosmetics == null ? List.of() : List.copyOf(cosmetics); }
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record PlayEmotePayload(String id, String name, String animationName,
                                   String animation, float duration) implements CustomPacketPayload {
        public static final Type<PlayEmotePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "play_emote"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayEmotePayload> CODEC = new StreamCodec<>() {
            @Override public void encode(RegistryFriendlyByteBuf buf, PlayEmotePayload value) {
                buf.writeUtf(nullToEmpty(value.id()), 128); buf.writeUtf(nullToEmpty(value.name()), 128);
                buf.writeUtf(nullToEmpty(value.animationName()), 128);
                COMPRESSED_GEOMETRY_CODEC.encode(buf, value.animation()); buf.writeFloat(value.duration());
            }
            @Override public PlayEmotePayload decode(RegistryFriendlyByteBuf buf) {
                return new PlayEmotePayload(buf.readUtf(128), buf.readUtf(128), buf.readUtf(128),
                    COMPRESSED_GEOMETRY_CODEC.decode(buf), buf.readFloat());
            }
        };
        public boolean isStop() { return STOP_EMOTE_ID.equals(id); }
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record EmoteUpdatePayload(UUID uuid, String id, String name, String animationName,
                                     String animation, float duration) implements CustomPacketPayload {
        public static final Type<EmoteUpdatePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "emote_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EmoteUpdatePayload> CODEC = new StreamCodec<>() {
            @Override public void encode(RegistryFriendlyByteBuf buf, EmoteUpdatePayload value) {
                UUIDUtil.STREAM_CODEC.encode(buf, value.uuid());
                PlayEmotePayload.CODEC.encode(buf, new PlayEmotePayload(value.id(), value.name(), value.animationName(), value.animation(), value.duration()));
            }
            @Override public EmoteUpdatePayload decode(RegistryFriendlyByteBuf buf) {
                UUID uuid = UUIDUtil.STREAM_CODEC.decode(buf); PlayEmotePayload value = PlayEmotePayload.CODEC.decode(buf);
                return new EmoteUpdatePayload(uuid, value.id(), value.name(), value.animationName(), value.animation(), value.duration());
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }
}
