package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class BedrockSkinsOptionsScreen extends OptionsSubScreen {
    public BedrockSkinsOptionsScreen(Screen previous) {
        super(previous, Minecraft.getInstance().options, Component.translatable("bedrockskins.gui.mod_options"));
    }

    @Override
    protected void addOptions() {
        if (this.list != null) {
            this.list.addSmall(BedrockSkinsConfig.asOptions());
        }
    }
}
