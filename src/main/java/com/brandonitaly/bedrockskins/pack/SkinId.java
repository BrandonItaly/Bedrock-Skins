package com.brandonitaly.bedrockskins.pack;

import com.mojang.serialization.Codec;

public record SkinId(String pack, String name, String safePackName, String safeSkinName) {
    public static final Codec<SkinId> CODEC = Codec.STRING.xmap(SkinId::parse, SkinId::toString);

    public SkinId(String pack, String name) {
        this(
            pack == null ? "" : pack,
            name == null ? "" : name,
            StringUtils.sanitize("skinpack." + (pack == null ? "" : pack)),
            StringUtils.sanitize("skin." + (pack == null ? "" : pack) + "." + (name == null ? "" : name))
        );
    }

    public static SkinId of(String pack, String name) {
        return new SkinId(pack, name);
    }

    public static SkinId parse(String key) {
        if (key == null || key.isEmpty()) return null;
        String[] parts = key.split(":", 2);
        return new SkinId(parts[0], parts.length > 1 ? parts[1] : "");
    }

    // Backwards-compatible getters
    public String getPack() { return pack; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return pack + ":" + name;
    }
}