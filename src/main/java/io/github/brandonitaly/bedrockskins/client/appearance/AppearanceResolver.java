package io.github.brandonitaly.bedrockskins.client.appearance;

import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.MojangSkinManager;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockModelManager;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;
import io.github.brandonitaly.bedrockskins.client.render.state.BedrockRenderStateStore;

import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerModelType;

import java.util.UUID;

/** Resolves the effective Bedrock appearance associated with an entity render state. */
public final class AppearanceResolver {
    private AppearanceResolver() {
    }

    public static UUID uuid(Object renderState) {
        return BedrockRenderStateStore.getUniqueId(renderState);
    }

    public static SkinId skinId(Object renderState) {
        SkinId stored = BedrockRenderStateStore.getSkinId(renderState);
        if (stored != null) {
            return stored;
        }
        UUID uuid = uuid(renderState);
        return uuid == null ? null : SkinManager.getSkin(uuid);
    }

    public static boolean isSlim(AvatarRenderState state, SkinId skinId) {
        if (skinId != null) {
            LoadedSkin loaded = SkinPackLoader.getLoadedSkin(skinId);
            if (loaded != null) {
                return MojangSkinManager.isSkinSlim(loaded);
            }
        }
        return state.skin != null && state.skin.model() == PlayerModelType.SLIM;
    }

    public static BedrockPlayerModel baseModel(AvatarRenderState state) {
        SkinId skinId = skinId(state);
        if (skinId != null) {
            return BedrockModelManager.getModel(skinId);
        }

        UUID uuid = uuid(state);
        if (uuid == null || PersonaManager.equipped(uuid).isEmpty()) {
            return null;
        }
        return BedrockModelManager.getVanillaPlayerModel(isSlim(state, null));
    }
}
