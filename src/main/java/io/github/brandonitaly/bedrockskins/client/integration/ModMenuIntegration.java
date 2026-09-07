package io.github.brandonitaly.bedrockskins.client.integration;

//? if modmenu {
import io.github.brandonitaly.bedrockskins.gui.screen.BedrockSkinsConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BedrockSkinsConfigScreen::new;
    }
}
//?}
