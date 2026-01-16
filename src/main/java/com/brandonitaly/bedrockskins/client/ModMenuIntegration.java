package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen;
//? if fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
//? } else if neoforge {
//? }
import net.minecraft.client.gui.screens.Screen;

//? if fabric {
public class ModMenuIntegration implements ModMenuApi {
    private static final boolean LEGACY4J_LOADED = FabricLoader.getInstance().isModLoaded("legacy");
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // return parent -> BedrockSkinsClient.getAppropriateSkinScreen((Screen) parent);
        // Open the normal skin selection screen from mod menu for access to mod options
        return parent -> new SkinSelectionScreen((Screen) parent);
    }
}
//? } else if neoforge {
//? }
