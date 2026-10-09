package io.github.brandonitaly.bedrockskins.mixin.skin;

import io.github.brandonitaly.bedrockskins.client.appearance.skin.HeadIconTextures;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Chat Heads blits faces directly, including heads beside command suggestions. */
@Pseudo
@Mixin(targets = "dzwdz.chat_heads.ChatHeads", remap = false)
public abstract class ChatHeadsHeadMixin {
    @ModifyVariable(target = @Desc(value = "renderChatHead", args = {
        GuiGraphicsExtractor.class, int.class, int.class, PlayerInfo.class, float.class, boolean.class }),
        at = @At("STORE"), ordinal = 0, require = 0)
    private static Identifier bedrockskins$portrait(Identifier source,
            @Local(argsOnly = true) PlayerInfo owner) {
        // Keep Chat Heads' drawing, shadow and flipping; the atlas's hat rectangle is transparent.
        return HeadIconTextures.resolve(source, owner.showHat());
    }
}
