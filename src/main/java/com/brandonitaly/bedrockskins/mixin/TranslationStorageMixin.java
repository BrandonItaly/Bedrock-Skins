package com.brandonitaly.bedrockskins.mixin;

import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public class TranslationStorageMixin {
    @Inject(method = "getOrDefault", at = @At("HEAD"), cancellable = true)
    private void onGet(String key, String fallback, CallbackInfoReturnable<String> cir) {
        String custom = SkinPackLoader.getTranslation(key);
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
