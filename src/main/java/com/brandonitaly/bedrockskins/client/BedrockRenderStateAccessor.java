package com.brandonitaly.bedrockskins.client;

import java.util.UUID;
import com.brandonitaly.bedrockskins.pack.SkinId;

public interface BedrockRenderStateAccessor {
    SkinId bedrockSkins$getBedrockSkinId();
    void bedrockSkins$setBedrockSkinId(SkinId id);

    UUID bedrockSkins$getUniqueId();
    void bedrockSkins$setUniqueId(UUID id);
} 
