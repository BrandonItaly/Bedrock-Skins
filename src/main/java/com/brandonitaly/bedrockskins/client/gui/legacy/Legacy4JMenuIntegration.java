package com.brandonitaly.bedrockskins.client.gui.legacy;

//? if legacy4j {
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.screen.HelpAndOptionsScreen;
import wily.legacy.client.screen.ScreenSection;

public final class Legacy4JMenuIntegration {
    private static final ScreenSection<?> CHANGE_SKIN = new ScreenSection<>() {
        @Override
        public Component title() {
            return Component.translatable("legacy.menu.change_skin");
        }

        @Override
        public Screen build(Screen parent) {
            return new BedrockChangeSkinSource().create(parent);
        }
    };

    private Legacy4JMenuIntegration() {
    }

    public static void init() {
        if (HelpAndOptionsScreen.CHANGE_SKIN != CHANGE_SKIN) HelpAndOptionsScreen.CHANGE_SKIN = CHANGE_SKIN;
    }

    public static Screen createScreen(Screen parent) {
        init();
        return new BedrockChangeSkinSource().create(parent);
    }

    public static void openScreen(Minecraft minecraft, Screen parent) {
        if (minecraft == null) return;
        init();
        minecraft.gui.setScreen(createScreen(parent));
    }
}
//? }