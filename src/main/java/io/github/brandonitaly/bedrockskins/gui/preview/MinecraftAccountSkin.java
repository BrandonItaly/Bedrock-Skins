package io.github.brandonitaly.bedrockskins.gui.preview;

import io.github.brandonitaly.bedrockskins.pack.model.AssetSource;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;

/** Virtual wardrobe entry for the signed-in player's Mojang skin. */
public final class MinecraftAccountSkin extends LoadedSkin {
    public static final String PACK_ID = "skinpack.Imports";
    public static final MinecraftAccountSkin INSTANCE = new MinecraftAccountSkin();

    private MinecraftAccountSkin() {
        super("Imports", "Imports", "Minecraft Account Skin", null, AssetSource.Remote.INSTANCE);
    }

    public static boolean is(LoadedSkin skin) {
        return skin instanceof MinecraftAccountSkin;
    }
}
