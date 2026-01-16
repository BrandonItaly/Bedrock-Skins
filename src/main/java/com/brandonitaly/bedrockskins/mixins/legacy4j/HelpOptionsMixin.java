package com.brandonitaly.bedrockskins.mixins.legacy4j;

//? if legacy4j {
/*
//? if fabric {
import com.brandonitaly.bedrockskins.client.BedrockSkinsClient;
//? }
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import wily.legacy.client.screen.HelpAndOptionsScreen;
import wily.legacy.client.screen.RenderableVList;
import wily.legacy.client.screen.RenderableVListScreen;
import java.util.function.Consumer;

@Mixin(HelpAndOptionsScreen.class)
public abstract class HelpOptionsMixin extends RenderableVListScreen {
    public HelpOptionsMixin(Screen parent, Component component, Consumer<RenderableVList> vListBuild) {
        super(parent, component, vListBuild);
    }

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lwily/legacy/client/screen/RenderableVList;addRenderable(Lnet/minecraft/client/gui/components/Renderable;)Lwily/legacy/client/screen/RenderableVList;",
            ordinal = 0
        )
    )
    private RenderableVList bedrockskins$ChangeSkinButton(RenderableVList instance, Renderable renderable, Operation<RenderableVList> original) {
        original.call(instance, openScreenButton(Component.translatable("legacy.menu.change_skin"), () -> {
            // Works for both Fabric and NeoForge
            return com.brandonitaly.bedrockskins.client.BedrockSkinsClient.getAppropriateSkinScreen(this);
        }).build());
        return instance;
    }
}*/
//?} else {
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class HelpOptionsMixin {

}
//?}