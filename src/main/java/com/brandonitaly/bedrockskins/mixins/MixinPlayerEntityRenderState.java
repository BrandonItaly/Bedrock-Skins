package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.client.BedrockRenderStateAccessor;
import com.brandonitaly.bedrockskins.pack.SkinId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.UUID;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

@Mixin(AvatarRenderState.class)
public class MixinPlayerEntityRenderState implements BedrockRenderStateAccessor {
    @Unique
    private SkinId bedrockSkinId;
    @Unique
    private UUID uniqueId;

    @Override
    public SkinId getBedrockSkinId() {
        return bedrockSkinId;
    }

    @Override
    public void setBedrockSkinId(SkinId id) {
        this.bedrockSkinId = id;
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
