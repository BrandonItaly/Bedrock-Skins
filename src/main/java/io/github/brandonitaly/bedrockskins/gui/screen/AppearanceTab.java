package io.github.brandonitaly.bedrockskins.gui.screen;

import io.github.brandonitaly.bedrockskins.gui.preview.*;
import io.github.brandonitaly.bedrockskins.gui.widget.*;

public enum AppearanceTab {
    SKINS("bedrockskins.gui.skins"),
    COSMETICS("bedrockskins.gui.cosmetics"),
    EMOTES("bedrockskins.gui.emotes"),
    CAPES("bedrockskins.gui.capes");

    private final String translationKey;

    AppearanceTab(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
