package com.brandonitaly.bedrockskins.pack;

import net.minecraft.resources.Identifier;

public sealed interface AssetSource permits 
    AssetSource.File, AssetSource.Resource, AssetSource.Remote {

    record File(String path) implements AssetSource {}
    
    record Resource(Identifier id) implements AssetSource {}
    
    final class Remote implements AssetSource {
        public static final Remote INSTANCE = new Remote();
        private Remote() {}
    }
}