package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.client.gui.BedrockSkinsOptionsScreen;
//? if fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
//? } else if neoforge {
//? }
import net.minecraft.client.gui.screens.Screen;

//? if fabric {
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new BedrockSkinsOptionsScreen((Screen) parent);
    }
}
//? } else if neoforge {
//? }
