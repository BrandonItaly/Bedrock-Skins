package com.brandonitaly.bedrockskins.client.gui.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.screen.HelpAndOptionsScreen;
import wily.legacy.client.screen.ScreenSection;
import wily.legacy.skins.api.ui.LegacySkinUi;

public final class Legacy4JMenuIntegration {
    private static final BedrockSkinUiAdapter ADAPTER = new BedrockSkinUiAdapter();
    private static final ScreenSection<?> CHANGE_SKIN = new ScreenSection<>() {
        @Override
        public Component title() {
            return Component.translatable("legacy.menu.change_skin");
        }

        @Override
        public Screen build(Screen parent) {
            return LegacySkinUi.create(parent, ADAPTER);
        }
    };

    private Legacy4JMenuIntegration() {
    }

    public static void init() {
        if (HelpAndOptionsScreen.CHANGE_SKIN != CHANGE_SKIN) HelpAndOptionsScreen.CHANGE_SKIN = CHANGE_SKIN;
    }

    public static Screen createScreen(Screen parent) {
        init();
        return LegacySkinUi.create(parent, ADAPTER);
    }

    public static void openScreen(Minecraft minecraft, Screen parent) {
        if (minecraft == null) return;
        init();
        minecraft.setScreen(createScreen(parent));
    }
}
