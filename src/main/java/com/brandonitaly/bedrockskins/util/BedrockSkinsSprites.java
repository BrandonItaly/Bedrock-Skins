package com.brandonitaly.bedrockskins.util;

import net.minecraft.resources.Identifier;

public class BedrockSkinsSprites {
    public static final Identifier SKIN_BOX = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/skin_box");
    public static final Identifier SKIN_PANEL = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/skin_panel");
    public static final Identifier PANEL_FILLER = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/panel_filler");
    public static final Identifier PACK_NAME_BOX = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/pack_name_box");
    public static final Identifier PANEL_SPRITE = Identifier.fromNamespaceAndPath("bedrockskins", "container/skin_panel");
    public static final Identifier HANGAR_ICON = Identifier.fromNamespaceAndPath("bedrockskins", "container/icon_hangar");
    public static final Identifier ROTATE_SPRITE = Identifier.fromNamespaceAndPath("bedrockskins", "container/rotate");
    public static final Identifier CARD_IDLE = Identifier.fromNamespaceAndPath("bedrockskins", "container/card");
    public static final Identifier CARD_HOVER = Identifier.fromNamespaceAndPath("bedrockskins", "container/card_hover");
    public static final Identifier CARD_SELECTED = Identifier.fromNamespaceAndPath("bedrockskins", "container/card_selected");
    public static final Identifier SKIN_DENY = Identifier.fromNamespaceAndPath("bedrockskins", "container/deny");

    // Legacy4J textures
    public static final Identifier SQUARE_RECESSED_PANEL = Identifier.fromNamespaceAndPath("legacy", "tiles/square_recessed_panel");
    public static final Identifier ICON_HOLDER = Identifier.fromNamespaceAndPath("legacy", "container/icon_holder");
    public static final Identifier BEACON_CONFIRM = Identifier.fromNamespaceAndPath("legacy", "container/beacon_check");

    // Vanilla textures
    public static final Identifier TAB_HEADER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/tab_header_background.png");
    public static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    public static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
}