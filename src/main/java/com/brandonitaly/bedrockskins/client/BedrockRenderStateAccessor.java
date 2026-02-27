package com.brandonitaly.bedrockskins.client;

import java.util.UUID;
import com.brandonitaly.bedrockskins.pack.SkinId;

public interface BedrockRenderStateAccessor {
    SkinId getBedrockSkinId();
    void setBedrockSkinId(SkinId id);

    UUID getUniqueId();
    void setUniqueId(UUID id);
} 
