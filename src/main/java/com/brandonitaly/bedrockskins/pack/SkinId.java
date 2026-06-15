package com.brandonitaly.bedrockskins.pack;

import com.mojang.serialization.Codec;

import java.util.Objects;

public record SkinId(String pack, String name) {
    public static final Codec<SkinId> CODEC = Codec.STRING.xmap(SkinId::parse, SkinId::toString);

    public SkinId {
        pack = Objects.requireNonNullElse(pack, "");
        name = Objects.requireNonNullElse(name, "");
    }

    public static SkinId of(String pack, String name) {
        return new SkinId(pack, name);
    }

    public static SkinId parse(String key) {
        if (key == null || key.isEmpty()) return null;
        
        int colonIndex = key.indexOf(':');
        if (colonIndex < 0) return new SkinId(key, "");
        return new SkinId(key.substring(0, colonIndex), key.substring(colonIndex + 1));
    }

    @Override
    public String toString() {
        return pack + ":" + name;
    }
}