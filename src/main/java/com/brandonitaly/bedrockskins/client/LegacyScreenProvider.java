package com.brandonitaly.bedrockskins.client;

import net.minecraft.client.gui.screens.Screen;
import com.brandonitaly.bedrockskins.client.gui.legacy.Legacy4JChangeSkinScreen;

public class LegacyScreenProvider {
    public static Screen createLegacyScreen(Screen parent) {
        return new Legacy4JChangeSkinScreen(parent);
    }
}