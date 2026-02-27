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

        PlayerSkinWidget.PreviewPose currentPose = PlayerSkinWidget.PreviewPose.STANDING;
        if (this.element3 != null) {
            currentPose = this.element3.getPreviewPose();
        }

        int n = widgets.size();
        
        int oldIndex = this.index;
        int targetIndex = ((newIndex % n) + n) % n;
        
        int delta = targetIndex - oldIndex;
        if (delta > n / 2) delta -= n;
        if (delta < -n / 2) delta += n;
        
        this.index = targetIndex;
        this.element3 = widgets.get(this.index);
        
        Set<PlayerSkinWidget> usedWidgets = new HashSet<>();
        int[] offsets = {0, -1, 1, -2, 2, -3, 3, -4, 4};
        
        for (int offset : offsets) {
            PlayerSkinWidget w = getWrapped(this.index + offset);
            if (w == null) continue;
            if (usedWidgets.contains(w)) continue; 
            
            usedWidgets.add(w);
            
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
            
            if (offset == 0) this.element3 = w;
            else if (offset == -1) this.element2 = w;
            else if (offset == -2) this.element1 = w;
            else if (offset == -3) this.element0 = w;
            else if (offset == 1) this.element4 = w;
            else if (offset == 2) this.element5 = w;
            else if (offset == 3) this.element6 = w;
        }

        for (PlayerSkinWidget w : widgets) {
            if (!usedWidgets.contains(w)) {
                w.invisible();
                w.resetPose();
            }
        }
    }

    private PlayerSkinWidget getWrapped(int i) {
         if (widgets.isEmpty()) return null;
         int n = widgets.size();
         int wrapped = (i % n);
         if (wrapped < 0) wrapped += n;
         return widgets.get(wrapped);
    }

    private void setupSlot(PlayerSkinWidget w, int offset, int delta, Float forcedRotX, Float forcedRotY) {
        float rotX = 0;
        float rotY = 0;
        int targetPosX = x;
        int targetPosY = y;
        float scale = 1.0f;
        
        switch (offset) {
            case 0: // Center
                w.interactable = true;
                // Explicitly inject the forced rotation if it exists, otherwise snap to 0
                if (forcedRotX != null && forcedRotY != null) {
                    rotX = forcedRotX;
                    rotY = forcedRotY;
                } else {
                    rotX = 0;
                    rotY = 0;
                }
                targetPosX = x + 8;
                targetPosY = y + 20;
                scale = 0.85f;
                break;
            case -1: // Left 1
                w.interactable = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET + 18;
                targetPosY = y + VERTICAL_OFFSET + 17;
                scale = 0.7f;
                break;
            case 1: // Right 1
                w.interactable = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET + 20;
                targetPosY = y + VERTICAL_OFFSET + 17;
                scale = 0.7f;
                break;
            case -2: // Left 2
                w.interactable = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET - 45;
                targetPosY = y + VERTICAL_OFFSET + 25;
                scale = 0.55f;
                break;
            case 2: // Right 2
                w.interactable = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET * 2 + 18;
                targetPosY = y + VERTICAL_OFFSET + 25;
                scale = 0.55f;
                break;
            case -3: // Left 3
                w.interactable = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET * 3;
                targetPosY = y + VERTICAL_OFFSET + 33;
                scale = 0.4f;
                break;
            case 3: // Right 3
                w.interactable = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET * 3 + 35;
                targetPosY = y + VERTICAL_OFFSET + 33;
                scale = 0.4f;
                break;
            case -4: // Left 4
                w.interactable = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET * 4 + 80;
                targetPosY = y + VERTICAL_OFFSET + 10;
                scale = 0.4f;
                break;
            case 4: // Right 4
                w.interactable = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET * 4;
                targetPosY = y + VERTICAL_OFFSET * 4 + 20;
                scale = 0.4f;
                break;
            default:
                w.invisible();
                return;
        }
        
        int currentX = w.getX();
        
        boolean isWrap = false;
        if (w.visible && delta != 0) {
            if (delta > 0 && targetPosX > currentX + 50) { 
                isWrap = true;
            } else if (delta < 0 && targetPosX < currentX - 50) { 
                isWrap = true;
            }
        }

        if (isWrap) {
            int virtualTargetX;
            if (delta > 0) {
                virtualTargetX = currentX - OFFSET * Math.abs(delta);
            } else {
                virtualTargetX = currentX + OFFSET * Math.abs(delta);
            }
            w.visible();
            w.beginInterpolation(rotX, rotY, virtualTargetX, targetPosY, scale);
            w.snapTo(targetPosX, targetPosY);
        } else {
            w.visible();
            w.beginInterpolation(rotX, rotY, targetPosX, targetPosY, scale);
        }
    }
}