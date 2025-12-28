package com.brandonitaly.bedrockskins.mixins;

import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TranslationStorage.class)
public class MixinTranslationStorage {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void onGet(String key, String fallback, CallbackInfoReturnable<String> cir) {
        String custom = SkinPackLoader.getTranslation(key);
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
