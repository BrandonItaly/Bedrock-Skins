package com.brandonitaly.bedrockskins.client.gui.legacy;

import java.util.*;

public class PlayerSkinWidgetList {
    public final int x, y;
    public final List<PlayerSkinWidget> widgets;
    public int index;
    
    // Fields accessed by screen or logic
    public PlayerSkinWidget element3; // Center
    public PlayerSkinWidget element0;
    public PlayerSkinWidget element1;
    public PlayerSkinWidget element2;
    public PlayerSkinWidget element4;
    public PlayerSkinWidget element5;
    public PlayerSkinWidget element6;

    private static final int VERTICAL_OFFSET = 10;
    private static final int OFFSET = 80;
    private static final float FACING_FROM_LEFT = -45f;
    private static final float FACING_FROM_RIGHT = 45f;

    private static final int[] OFFSETS = {0, -1, 1, -2, 2, -3, 3, -4, 4};
    private static final SlotConfig[] SLOT_CONFIGS = new SlotConfig[9];

    static {
        // Mapped by (offset + 4) -> index 0 to 8
        SLOT_CONFIGS[0] = new SlotConfig(-OFFSET * 4 + 80, VERTICAL_OFFSET + 10, 0.4f, FACING_FROM_LEFT, false); // Offset -4
        SLOT_CONFIGS[1] = new SlotConfig(-OFFSET * 3, VERTICAL_OFFSET + 33, 0.4f, FACING_FROM_LEFT, false);      // Offset -3
        SLOT_CONFIGS[2] = new SlotConfig(-OFFSET - 45, VERTICAL_OFFSET + 25, 0.55f, FACING_FROM_LEFT, false);    // Offset -2
        SLOT_CONFIGS[3] = new SlotConfig(-OFFSET + 18, VERTICAL_OFFSET + 17, 0.7f, FACING_FROM_LEFT, false);     // Offset -1
        SLOT_CONFIGS[4] = new SlotConfig(8, 20, 0.85f, 0f, true);                                                // Offset  0 (Center)
        SLOT_CONFIGS[5] = new SlotConfig(OFFSET + 20, VERTICAL_OFFSET + 17, 0.7f, FACING_FROM_RIGHT, false);     // Offset  1
        SLOT_CONFIGS[6] = new SlotConfig(OFFSET * 2 + 18, VERTICAL_OFFSET + 25, 0.55f, FACING_FROM_RIGHT, false);// Offset  2
        SLOT_CONFIGS[7] = new SlotConfig(OFFSET * 3 + 35, VERTICAL_OFFSET + 33, 0.4f, FACING_FROM_RIGHT, false); // Offset  3
        SLOT_CONFIGS[8] = new SlotConfig(OFFSET * 4, VERTICAL_OFFSET * 4 + 20, 0.4f, FACING_FROM_RIGHT, false);  // Offset  4
    }

    private record SlotConfig(int dx, int dy, float scale, float rotY, boolean interactable) {}

    private PlayerSkinWidgetList(int x, int y, PlayerSkinWidget[] widgets) {
        this.x = x;
        this.y = y;
        this.widgets = new ArrayList<>(Arrays.asList(widgets));
    }

    public static PlayerSkinWidgetList of(int x, int y, PlayerSkinWidget... widgets) {
        return new PlayerSkinWidgetList(x, y, widgets);
    }

    public void sortForIndex(int newIndex) {
        sortForIndex(newIndex, null, null);
    }

    public void sortForIndex(int newIndex, Float forcedRotX, Float forcedRotY) {
        if (widgets.isEmpty()) {
            this.index = 0;
            return;
        }

        int n = widgets.size();
        
        PlayerSkinWidget.PreviewPose currentPose = PlayerSkinWidget.PreviewPose.STANDING;
        if (this.element3 != null) {
            currentPose = this.element3.getPreviewPose();
        }

        int oldIndex = this.index;
        int targetIndex = getWrappedIndex(newIndex, n);
        
        int delta = targetIndex - oldIndex;
        if (delta > n / 2) delta -= n;
        if (delta < -n / 2) delta += n;
        
        this.index = targetIndex;
        this.element3 = widgets.get(this.index);
        
        boolean[] usedWidgets = new boolean[n];
        
        for (int offset : OFFSETS) {
            int wIndex = getWrappedIndex(this.index + offset, n);
            
            if (usedWidgets[wIndex]) continue;
            usedWidgets[wIndex] = true;
            
            PlayerSkinWidget w = widgets.get(wIndex);
            
            if (offset == 0) {
                if (delta != 0) {
                    w.setPendingPose(currentPose);
                    w.setPreviewPose(PlayerSkinWidget.PreviewPose.STANDING);
                } else {
                    w.setPreviewPose(currentPose);
                }
            } else {
                w.resetPose();
            }

            setupSlot(w, offset, delta, forcedRotX, forcedRotY);
            
            switch (offset) {
                case  0 -> this.element3 = w;
                case -1 -> this.element2 = w;
                case -2 -> this.element1 = w;
                case -3 -> this.element0 = w;
                case  1 -> this.element4 = w;
                case  2 -> this.element5 = w;
                case  3 -> this.element6 = w;
            }
        }

        // Hide and reset any widgets that weren't placed in the visible offsets
        for (int i = 0; i < n; i++) {
            if (!usedWidgets[i]) {
                PlayerSkinWidget w = widgets.get(i);
                w.invisible();
                w.resetPose();
            }
        }
    }

    private int getWrappedIndex(int i, int n) {
        return ((i % n) + n) % n;
    }

    private void setupSlot(PlayerSkinWidget w, int offset, int delta, Float forcedRotX, Float forcedRotY) {
        // Map offset (-4 to 4) to array index (0 to 8)
        SlotConfig config = SLOT_CONFIGS[offset + 4];
        
        w.interactable = config.interactable;
        float rotX = 0f;
        float rotY = config.rotY;
        
        if (offset == 0 && forcedRotX != null && forcedRotY != null) {
            rotX = forcedRotX;
            rotY = forcedRotY;
        }

        int targetPosX = this.x + config.dx;
        int targetPosY = this.y + config.dy;
        float scale = config.scale;
        
        int currentX = w.getX();
        
        boolean isWrap = w.visible && (
            (delta > 0 && targetPosX > currentX + 50) || 
            (delta < 0 && targetPosX < currentX - 50)
        );

        if (isWrap) {
            int virtualTargetX = currentX + (delta > 0 ? -OFFSET : OFFSET) * Math.abs(delta);
            w.visible();
            w.beginInterpolation(rotX, rotY, virtualTargetX, targetPosY, scale);
            w.snapTo(targetPosX, targetPosY);
        } else {
            w.visible();
            w.beginInterpolation(rotX, rotY, targetPosX, targetPosY, scale);
        }
    }
}