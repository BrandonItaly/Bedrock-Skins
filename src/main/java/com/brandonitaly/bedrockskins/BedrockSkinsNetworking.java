package com.brandonitaly.bedrockskins;

import java.util.Arrays;
import java.util.Objects;
import com.brandonitaly.bedrockskins.pack.SkinId;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}

public final class BedrockSkinsNetworking {
    private BedrockSkinsNetworking() {}

    private static final StreamCodec<RegistryFriendlyByteBuf, SkinId> OPTIONAL_SKIN_ID_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, skinId -> skinId != null,
        ByteBufCodecs.stringUtf8(32767), skinId -> skinId == null ? "" : skinId.toString(),
        (present, skinId) -> present ? SkinId.parse(skinId) : null
    );

    public static final class SkinUpdatePayload implements CustomPacketPayload {
        //? if >=1.21.11 {
        public static final CustomPacketPayload.Type<SkinUpdatePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "skin_update"));
        //?} else {
        /*public static final CustomPacketPayload.Type<SkinUpdatePayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("bedrockskins", "skin_update"));*/
        //?}
        public static final StreamCodec<RegistryFriendlyByteBuf, SkinUpdatePayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SkinUpdatePayload::getUuid,
            OPTIONAL_SKIN_ID_CODEC, SkinUpdatePayload::getSkinId,
            ByteBufCodecs.stringUtf8(262144), SkinUpdatePayload::getGeometry,
            ByteBufCodecs.byteArray(1048576), SkinUpdatePayload::getTextureData,
            SkinUpdatePayload::new
        );

        private final java.util.UUID uuid;
        private final SkinId skinId;
        private final String geometry;
        private final byte[] textureData;

        public SkinUpdatePayload(java.util.UUID uuid, SkinId skinId, String geometry, byte[] textureData) {
            this.uuid = uuid;
            this.skinId = skinId;
            this.geometry = geometry == null ? "" : geometry;
            this.textureData = textureData == null ? new byte[0] : textureData;
        }

        @Override
        public CustomPacketPayload.Type<?> type() {
            return ID;
        }

        public java.util.UUID getUuid() { return uuid; }
        public SkinId getSkinId() { return skinId; }
        public String getGeometry() { return geometry; }
        public byte[] getTextureData() { return textureData; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SkinUpdatePayload that = (SkinUpdatePayload) other;
            if (!uuid.equals(that.uuid)) return false;
            if (!Objects.equals(skinId, that.skinId)) return false;
            if (!geometry.equals(that.geometry)) return false;
            return Arrays.equals(textureData, that.textureData);
        }

        @Override
        public int hashCode() {
            int result = uuid.hashCode();
            result = 31 * result + (skinId == null ? 0 : skinId.hashCode());
            result = 31 * result + geometry.hashCode();
            result = 31 * result + Arrays.hashCode(textureData);
            return result;
        }
    }

    public static final class SetSkinPayload implements CustomPacketPayload {
        //? if >=1.21.11 {
        public static final CustomPacketPayload.Type<SetSkinPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bedrockskins", "set_skin"));
        //?} else {
        /*public static final CustomPacketPayload.Type<SetSkinPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("bedrockskins", "set_skin"));*/
        //?}
        public static final StreamCodec<RegistryFriendlyByteBuf, SetSkinPayload> CODEC = StreamCodec.composite(
            OPTIONAL_SKIN_ID_CODEC, SetSkinPayload::getSkinId,
            ByteBufCodecs.stringUtf8(262144), SetSkinPayload::getGeometry,
            ByteBufCodecs.byteArray(1048576), SetSkinPayload::getTextureData,
            SetSkinPayload::new
        );

        private final SkinId skinId;
        private final String geometry;
        private final byte[] textureData;

        public SetSkinPayload(SkinId skinId, String geometry, byte[] textureData) {
            this.skinId = skinId;
            this.geometry = geometry == null ? "" : geometry;
            this.textureData = textureData == null ? new byte[0] : textureData;
        }

        @Override
        public CustomPacketPayload.Type<?> type() { return ID; }

        public SkinId getSkinId() { return skinId; }
        public String getGeometry() { return geometry; }
        public byte[] getTextureData() { return textureData; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SetSkinPayload that = (SetSkinPayload) other;
            if (!Objects.equals(skinId, that.skinId)) return false;
            if (!geometry.equals(that.geometry)) return false;
            return Arrays.equals(textureData, that.textureData);
        }

        @Override
        public int hashCode() {
            int result = (skinId == null ? 0 : skinId.hashCode());
            result = 31 * result + geometry.hashCode();
            result = 31 * result + Arrays.hashCode(textureData);
            return result;
        }
    }
}
