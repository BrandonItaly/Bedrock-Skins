package com.brandonitaly.bedrockskins.client;

//? if fabric {
import com.brandonitaly.bedrockskins.client.gui.BedrockSkinsOptionsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BedrockSkinsOptionsScreen::new;
    }
}
//?}