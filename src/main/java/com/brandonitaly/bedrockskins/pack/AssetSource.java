package com.brandonitaly.bedrockskins.pack;

import net.minecraft.util.Identifier;

public abstract class AssetSource {
    private AssetSource() {}

    public static final class File extends AssetSource {
        private final String path;
        public File(String path) { this.path = path; }
        public String getPath() { return path; }
    }

    public static final class Resource extends AssetSource {
        private final Identifier id;
        public Resource(Identifier id) { this.id = id; }
        public Identifier getId() { return id; }
    }

    public static final class Remote extends AssetSource {
        public static final Remote INSTANCE = new Remote();
        private Remote() {}
    }
}
