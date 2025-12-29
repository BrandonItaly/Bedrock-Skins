package com.brandonitaly.bedrockskins;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.network.RegistryByteBuf;

import java.util.Arrays;

public final class BedrockSkinsNetworking {
    private BedrockSkinsNetworking() {}

    public static final class SkinUpdatePayload implements CustomPayload {
        public static final CustomPayload.Id<SkinUpdatePayload> ID = new CustomPayload.Id<>(Identifier.of("bedrockskins", "skin_update"));
        public static final PacketCodec<RegistryByteBuf, SkinUpdatePayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, SkinUpdatePayload::getUuid,
            PacketCodecs.string(32767), SkinUpdatePayload::getSkinKey,
            PacketCodecs.string(262144), SkinUpdatePayload::getGeometry,
            PacketCodecs.byteArray(1048576), SkinUpdatePayload::getTextureData,
            SkinUpdatePayload::new
        );

        private final java.util.UUID uuid;
        private final String skinKey;
        private final String geometry;
        private final byte[] textureData;

        public SkinUpdatePayload(java.util.UUID uuid, String skinKey, String geometry, byte[] textureData) {
            this.uuid = uuid;
            this.skinKey = skinKey;
            this.geometry = geometry;
            this.textureData = textureData == null ? new byte[0] : textureData;
        }

        @Override
        public CustomPayload.Id<?> getId() {
            return ID;
        }

        public java.util.UUID getUuid() { return uuid; }
        public String getSkinKey() { return skinKey; }
        public String getGeometry() { return geometry; }
        public byte[] getTextureData() { return textureData; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SkinUpdatePayload that = (SkinUpdatePayload) other;
            if (!uuid.equals(that.uuid)) return false;
            if (!skinKey.equals(that.skinKey)) return false;
            if (!geometry.equals(that.geometry)) return false;
            return Arrays.equals(textureData, that.textureData);
        }

        @Override
        public int hashCode() {
            int result = uuid.hashCode();
            result = 31 * result + skinKey.hashCode();
            result = 31 * result + geometry.hashCode();
            result = 31 * result + Arrays.hashCode(textureData);
            return result;
        }
    }

    public static final class SetSkinPayload implements CustomPayload {
        public static final CustomPayload.Id<SetSkinPayload> ID = new CustomPayload.Id<>(Identifier.of("bedrockskins", "set_skin"));
        public static final PacketCodec<RegistryByteBuf, SetSkinPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(32767), SetSkinPayload::getSkinKey,
            PacketCodecs.string(262144), SetSkinPayload::getGeometry,
            PacketCodecs.byteArray(1048576), SetSkinPayload::getTextureData,
            SetSkinPayload::new
        );

        private final String skinKey;
        private final String geometry;
        private final byte[] textureData;

        public SetSkinPayload(String skinKey, String geometry, byte[] textureData) {
            this.skinKey = skinKey;
            this.geometry = geometry;
            this.textureData = textureData == null ? new byte[0] : textureData;
        }

        @Override
        public CustomPayload.Id<?> getId() { return ID; }

        public String getSkinKey() { return skinKey; }
        public String getGeometry() { return geometry; }
        public byte[] getTextureData() { return textureData; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SetSkinPayload that = (SetSkinPayload) other;
            if (!skinKey.equals(that.skinKey)) return false;
            if (!geometry.equals(that.geometry)) return false;
            return Arrays.equals(textureData, that.textureData);
        }

        @Override
        public int hashCode() {
            int result = skinKey.hashCode();
            result = 31 * result + geometry.hashCode();
            result = 31 * result + Arrays.hashCode(textureData);
            return result;
        }
    }
}
