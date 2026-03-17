package com.brandonitaly.bedrockskins.pack;

import net.minecraft.resources.Identifier;

public sealed interface AssetSource permits 
    AssetSource.File, AssetSource.Resource, AssetSource.Remote, AssetSource.Zip, AssetSource.Bytes {

    record File(String path) implements AssetSource {}
    
    record Resource(Identifier id) implements AssetSource {}
    
    final class Remote implements AssetSource {
        public static final Remote INSTANCE = new Remote();
        private Remote() {}
    }
    
    record Zip(String zipPath, String internalPath) implements AssetSource {}
    
    final class Bytes implements AssetSource {
        private final byte[] data;
        private final String debugName;

        public Bytes(byte[] data, String debugName) {
            this.data = data == null ? new byte[0] : data;
            this.debugName = debugName == null ? "" : debugName;
        }

        public byte[] getData() { return data; }
        public String getDebugName() { return debugName; }
    }
}