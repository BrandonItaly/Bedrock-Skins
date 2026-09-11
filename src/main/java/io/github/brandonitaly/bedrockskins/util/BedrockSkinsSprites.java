package io.github.brandonitaly.bedrockskins.util;

import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.FontDescription;

public class BedrockSkinsSprites {
    public static final FontDescription TAB_ICONS_FONT = new FontDescription.Resource(
        Identifier.fromNamespaceAndPath("bedrockskins", "tab_icons"));
    public static final Identifier PANEL_SPRITE = Identifier.fromNamespaceAndPath("bedrockskins", "container/skin_panel");
    public static final Identifier WARDROBE_ICON = Identifier.fromNamespaceAndPath("bedrockskins", "container/icon_hangar");
    public static final Identifier ROTATE_SPRITE = Identifier.fromNamespaceAndPath("bedrockskins", "container/rotate");
    public static final Identifier CARD_IDLE = Identifier.fromNamespaceAndPath("bedrockskins", "container/card");
    public static final Identifier CARD_HOVER = Identifier.fromNamespaceAndPath("bedrockskins", "container/card_hover");
    public static final Identifier CARD_SELECTED = Identifier.fromNamespaceAndPath("bedrockskins", "container/card_selected");
    public static final Identifier UPLOAD_ICON = Identifier.fromNamespaceAndPath("bedrockskins", "container/upload");
    public static final Identifier TRASH_ICON = Identifier.fromNamespaceAndPath("bedrockskins", "container/icon_trash");
    public static final Identifier COLOR_PICKER_ICON = Identifier.fromNamespaceAndPath("bedrockskins", "container/color_picker");
    public static final Identifier PREVIEW_ICON = Identifier.fromNamespaceAndPath("bedrockskins", "container/icon_preview");
    public static final Identifier NONE_ICON = Identifier.fromNamespaceAndPath("bedrockskins", "container/icon_none");
    public static final Identifier KEYBOARD_TOOLTIP_ICON = Identifier.fromNamespaceAndPath("bedrockskins", "container/keyboard_tooltip_icon");
    public static final Identifier EMOTE_WHEEL_BASE = Identifier.fromNamespaceAndPath("bedrockskins", "container/emote_wheel_base");
    public static final Identifier[] EMOTE_WHEEL_SELECTIONS = {
        Identifier.fromNamespaceAndPath("bedrockskins", "container/emote_wheel_select_0"),
        Identifier.fromNamespaceAndPath("bedrockskins", "container/emote_wheel_select_1"),
        Identifier.fromNamespaceAndPath("bedrockskins", "container/emote_wheel_select_2"),
        Identifier.fromNamespaceAndPath("bedrockskins", "container/emote_wheel_select_3"),
        Identifier.fromNamespaceAndPath("bedrockskins", "container/emote_wheel_select_4"),
        Identifier.fromNamespaceAndPath("bedrockskins", "container/emote_wheel_select_5")
    };

    // Vanilla textures
    public static final Identifier TAB_HEADER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/tab_header_background.png");
    public static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    public static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
}
