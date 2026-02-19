package com.brandonitaly.bedrockskins.pack;

import com.mojang.serialization.Codec;
import java.util.Objects;

public final class SkinId {
    public static final Codec<SkinId> CODEC = Codec.STRING.xmap(SkinId::parse, SkinId::toString);

    private final String pack;
    private final String name;

    private SkinId(String pack, String name) {
        this.pack = pack == null ? "" : pack;
        this.name = name == null ? "" : name;
    }

    public static SkinId of(String pack, String name) {
        return new SkinId(pack, name);
    }

    public static SkinId parse(String key) {
        if (key == null) return null;
        int idx = key.indexOf(':');
        if (idx < 0) return new SkinId(key, "");
        return new SkinId(key.substring(0, idx), key.substring(idx + 1));
    }

    public String getPack() { return pack; }
    public String getName() { return name; }

    public String getSafePackName() { return StringUtils.sanitize("skinpack." + pack); }
    public String getSafeSkinName() { return StringUtils.sanitize("skin." + pack + "." + name); }

    @Override
    public String toString() { return pack + ":" + name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkinId skinId = (SkinId) o;
        return pack.equals(skinId.pack) && name.equals(skinId.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pack, name);
    }
}