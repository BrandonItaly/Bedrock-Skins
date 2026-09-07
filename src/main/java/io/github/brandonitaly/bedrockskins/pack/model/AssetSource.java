package io.github.brandonitaly.bedrockskins.pack.model;

import net.minecraft.resources.Identifier;

public sealed interface AssetSource permits 
    AssetSource.File, AssetSource.Resource, AssetSource.Memory, AssetSource.Remote {

    record File(String path) implements AssetSource {}
    
    record Resource(Identifier id) implements AssetSource {}

    /** In-memory assets produced while converting formats such as Persona pieces. */
    record Memory(byte[] data) implements AssetSource {
        public Memory {
            data = data == null ? new byte[0] : data.clone();
        }

        @Override
        public byte[] data() {
            return data.clone();
        }
    }
    
    final class Remote implements AssetSource {
        public static final Remote INSTANCE = new Remote();
        private Remote() {}
    }
}
