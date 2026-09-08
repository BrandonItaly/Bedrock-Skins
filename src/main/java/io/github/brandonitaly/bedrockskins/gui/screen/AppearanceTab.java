package io.github.brandonitaly.bedrockskins.gui.screen;

import io.github.brandonitaly.bedrockskins.gui.preview.*;
import io.github.brandonitaly.bedrockskins.gui.widget.*;
import io.github.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import net.minecraft.network.chat.Component;

public enum AppearanceTab {
    SKINS("bedrockskins.gui.skins", "\uE000"),
    COSMETICS("bedrockskins.gui.cosmetics", "\uE001"),
    EMOTES("bedrockskins.gui.emotes", "\uE002"),
    CAPES("bedrockskins.gui.capes", "\uE003");

    private final String translationKey;
    private final String icon;

    AppearanceTab(String translationKey, String icon) {
        this.translationKey = translationKey;
        this.icon = icon;
    }

    public String translationKey() {
        return translationKey;
    }

    public Component title() {
        return Component.empty()
            .append(Component.literal(icon).withStyle(style -> style.withFont(BedrockSkinsSprites.TAB_ICONS_FONT)))
            .append(Component.literal(" "))
            .append(Component.translatable(translationKey));
    }
}
