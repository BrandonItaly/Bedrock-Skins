package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockSkinState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.UUID;

@Mixin(PlayerEntityRenderState.class)
public class MixinPlayerEntityRenderState implements BedrockSkinState {
    @Unique
    private String bedrockSkinKey;
    @Unique
    private UUID uniqueId;

    @Override
    public String getBedrockSkinKey() {
        return bedrockSkinKey;
    }

    @Override
    public void setBedrockSkinKey(String key) {
        this.bedrockSkinKey = key;
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(UUID uuid) {
        this.uniqueId = uuid;
    }
}
