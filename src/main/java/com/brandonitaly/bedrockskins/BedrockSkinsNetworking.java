package com.brandonitaly.bedrockskins;

import com.brandonitaly.bedrockskins.pack.SkinId;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public final class BedrockSkinsNetworking {
    private BedrockSkinsNetworking() {}

    private static final StreamCodec<RegistryFriendlyByteBuf, SkinId> OPTIONAL_SKIN_ID_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, skinId -> skinId != null,
        ByteBufCodecs.stringUtf8(32767), skinId -> skinId == null ? "" : skinId.toString(),
        (present, skinId) -> present ? SkinId.parse(skinId) : null
    );

    // Skin Update Payload (Server -> Client)
    public record SkinUpdatePayload(UUID uuid, SkinId skinId, String geometry, byte[] textureData) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SkinUpdatePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "skin_update"));
        
        public static final StreamCodec<RegistryFriendlyByteBuf, SkinUpdatePayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SkinUpdatePayload::uuid,
            OPTIONAL_SKIN_ID_CODEC, SkinUpdatePayload::skinId,
            ByteBufCodecs.stringUtf8(262144), SkinUpdatePayload::geometry,
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
            ByteBufCodecs.stringUtf8(262144), SetSkinPayload::geometry,
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