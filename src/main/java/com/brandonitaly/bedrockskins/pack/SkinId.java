package com.brandonitaly.bedrockskins.pack;

import com.mojang.serialization.Codec;
import java.util.Objects;

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
        int idx = key.indexOf(':');
        if (idx < 0) return new SkinId(key, "");
        return new SkinId(key.substring(0, idx), key.substring(idx + 1));
    }

    // Backwards-compatible getters
    public String getPack() { return pack; }
    public String getName() { return name; }
    public String getSafePackName() { return safePackName; }
    public String getSafeSkinName() { return safeSkinName; }

    @Override
    public String toString() {
        return pack + ":" + name;
    }
}