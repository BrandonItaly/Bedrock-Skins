package io.github.brandonitaly.bedrockskins.gui;

import net.minecraft.client.gui.components.AbstractWidget;

/** Widget visibility across the field and accessor versions of Minecraft's GUI API. */
public final class WidgetVisibility {
    private WidgetVisibility() {}

    public static boolean isVisible(AbstractWidget widget) {
        //? if >=26.4-snapshot-2 {
        /*return widget.isVisible();
        *///?} else {
        return widget.visible;
        //?}
    }

    public static void setVisible(AbstractWidget widget, boolean visible) {
        //? if >=26.4-snapshot-2 {
        /*widget.setVisible(visible);
        *///?} else {
        widget.visible = visible;
        //?}
    }
}
