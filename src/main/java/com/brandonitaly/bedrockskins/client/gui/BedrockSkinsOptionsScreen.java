package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.util.PlatformUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.OptionInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BedrockSkinsOptionsScreen extends OptionsSubScreen {
    public BedrockSkinsOptionsScreen(Screen previous) {
        super(previous, Minecraft.getInstance().options, Component.translatable("bedrockskins.gui.mod_options"));
    }

    @Override
    protected void addOptions() {
        if (this.list != null) {
            List<OptionInstance<?>> options = new ArrayList<>(Arrays.asList(BedrockSkinsConfig.asOptions()));
            
            if (PlatformUtil.isModLoaded("legacy")) {
                options.add(BedrockSkinsConfig.SMOOTH_INTERPOLATION);
            }
            
            this.list.addSmall(options.toArray(new OptionInstance[0]));
        }
    }
}