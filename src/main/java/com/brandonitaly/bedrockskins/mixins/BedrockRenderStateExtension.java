package com.brandonitaly.bedrockskins.mixins;

import java.util.UUID;

public interface BedrockRenderStateExtension {
    UUID getUniqueId();
    void setUniqueId(UUID uuid);
}
