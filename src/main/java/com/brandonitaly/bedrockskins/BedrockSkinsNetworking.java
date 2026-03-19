package com.brandonitaly.bedrockskins;

import com.brandonitaly.bedrockskins.pack.SkinId;
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
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class BedrockSkinsNetworking {
    private BedrockSkinsNetworking() {}

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
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                    gzip.write(string.getBytes(StandardCharsets.UTF_8));
                }
                buf.writeByteArray(baos.toByteArray());
            } catch (IOException e) {
                buf.writeByteArray(new byte[0]); // Safe fallback
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
    public record SkinUpdatePayload(UUID uuid, SkinId skinId, String geometry, byte[] textureData) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SkinUpdatePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "skin_update"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, SkinUpdatePayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SkinUpdatePayload::uuid,
            OPTIONAL_SKIN_ID_CODEC, SkinUpdatePayload::skinId,
            COMPRESSED_GEOMETRY_CODEC, SkinUpdatePayload::geometry,
            ByteBufCodecs.byteArray(1048576), SkinUpdatePayload::textureData,
            SkinUpdatePayload::new
        );

        public SkinUpdatePayload(UUID uuid, SkinId skinId, String geometry, byte[] textureData) {
            this.uuid = uuid;
            this.skinId = skinId;
            this.geometry = geometry == null ? "" : geometry;
            this.textureData = textureData == null ? new byte[0] : textureData;
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    // Set Skin Payload (Client -> Server)
    public record SetSkinPayload(SkinId skinId, String geometry, byte[] textureData) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetSkinPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "set_skin"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, SetSkinPayload> CODEC = StreamCodec.composite(
            OPTIONAL_SKIN_ID_CODEC, SetSkinPayload::skinId,
            COMPRESSED_GEOMETRY_CODEC, SetSkinPayload::geometry,
            ByteBufCodecs.byteArray(1048576), SetSkinPayload::textureData,
            SetSkinPayload::new
        );

        public SetSkinPayload(SkinId skinId, String geometry, byte[] textureData) {
            this.skinId = skinId;
            this.geometry = geometry == null ? "" : geometry;
            this.textureData = textureData == null ? new byte[0] : textureData;
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }
}