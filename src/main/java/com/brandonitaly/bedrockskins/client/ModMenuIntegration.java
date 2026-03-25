package com.brandonitaly.bedrockskins.client;

//? if fabric {
import com.brandonitaly.bedrockskins.client.gui.BedrockSkinsConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BedrockSkinsConfigScreen::new;
    }
}
//?}