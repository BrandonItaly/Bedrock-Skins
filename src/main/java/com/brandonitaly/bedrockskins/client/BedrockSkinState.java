package com.brandonitaly.bedrockskins.client;

import java.util.UUID;

public interface BedrockSkinState {
    String getBedrockSkinKey();
    void setBedrockSkinKey(String key);

    UUID getUniqueId();
    void setUniqueId(UUID id);
}
