package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.client.BedrockRenderStateAccessor;
import com.brandonitaly.bedrockskins.pack.SkinId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.UUID;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

@Mixin(AvatarRenderState.class)
public class PlayerEntityRenderStateMixin implements BedrockRenderStateAccessor {
    @Unique
    private SkinId bedrockSkinId;
    @Unique
    private UUID uniqueId;

    @Override
    public SkinId bedrockSkins$getBedrockSkinId() {
        return bedrockSkinId;
    }

    @Override
    public void bedrockSkins$setBedrockSkinId(SkinId id) {
        this.bedrockSkinId = id;
    }

    @Override
    public UUID bedrockSkins$getUniqueId() {
        return uniqueId;
    }

    @Override
    public void bedrockSkins$setUniqueId(UUID uuid) {
        this.uniqueId = uuid;
    }
}
