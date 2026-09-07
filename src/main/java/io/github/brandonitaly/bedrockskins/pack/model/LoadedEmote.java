package io.github.brandonitaly.bedrockskins.pack.model;

import com.google.gson.JsonObject;

/** A Bedrock Character Creator emote animation. */
public record LoadedEmote(String id, String displayName, String animationName,
                          JsonObject animation, float duration, byte[] thumbnailData) {
    public LoadedEmote {
        thumbnailData = thumbnailData == null ? new byte[0] : thumbnailData.clone();
    }

    public LoadedEmote(String id, String displayName, String animationName,
                       JsonObject animation, float duration) {
        this(id, displayName, animationName, animation, duration, new byte[0]);
    }

    @Override
    public byte[] thumbnailData() {
        return thumbnailData.clone();
    }
}
