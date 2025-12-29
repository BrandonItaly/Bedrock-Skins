package com.brandonitaly.bedrockskins.client;

import com.brandonitaly.bedrockskins.client.gui.SkinSelectionScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new SkinSelectionScreen((Screen) parent);
    }
}
