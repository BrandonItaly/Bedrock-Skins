package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityAttachment;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GuiUtils {
    private GuiUtils() {}

    /**
     * Render a living entity inside the given rectangle using submitEntityRenderState.
     * yawOffset is added to the base 180 degrees orientation. sizeCap caps the computed render size.
     */
    public static void renderEntityInRect(GuiGraphics gui, LivingEntity entity, float yawOffset, int left, int top, int right, int bottom, int sizeCap) {
        // Save entity state
        float yBodyRot = entity.yBodyRot;
        float yRot = entity.getYRot();
        float yRotO = entity.yRotO;
        float yBodyRotO = entity.yBodyRotO;
        float xRot = entity.getXRot();
        float xRotO = entity.xRotO;
        float yHeadRotO = entity.yHeadRotO;
        float yHeadRot = entity.yHeadRot;
        var vel = entity.getDeltaMovement();

        // Apply rotation based on yawOffset
        entity.yBodyRot = (180.0F + yawOffset);
        entity.setYRot(180.0F + yawOffset);
        entity.yBodyRotO = entity.yBodyRot;
        entity.yRotO = entity.getYRot();
        entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        entity.setXRot(0);
        entity.xRotO = entity.getXRot();
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        // Get renderer and state
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var entityRenderer = entityRenderDispatcher.getRenderer(entity);
        var entityRenderState = entityRenderer.createRenderState(entity, 1.0F);
        if (entity instanceof PreviewPlayer previewPlayer && previewPlayer.shouldShowName()) {
            entityRenderState.nameTag = entity.getDisplayName();
            entityRenderState.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot());
        }
        entityRenderState.lightCoords = 15728880;
        entityRenderState.boundingBoxHeight = 0;
        entityRenderState.boundingBoxWidth = 0;

        // Calculate size/scale
        int height = bottom - top;
        int size = Math.min((int) (height / 3.0), sizeCap);
        float scale = entity.getScale();
        float centerY = entity.getBbHeight() / 2.0F;
        if (isUpsideDown(entity)) {
            centerY -= entity.getBbHeight();
        }
        Vector3f vector3f = new Vector3f(0.0F, centerY, 0.0F);
        float renderScale = (float) size / scale;

        // Quaternions
        Quaternionf quat = new Quaternionf().rotationZ((float) Math.toRadians(180.0F));
        Quaternionf quat2 = new Quaternionf().rotationX(0);
        quat.mul(quat2);
        quat2.conjugate();

        // Submit
        gui.submitEntityRenderState(entityRenderState, renderScale, vector3f, quat, quat2, left, top, right, bottom);

        // Restore state
        entity.yBodyRot = yBodyRot;
        entity.yBodyRotO = yBodyRotO;
        entity.setYRot(yRot);
        entity.yRotO = yRotO;
        entity.setXRot(xRot);
        entity.xRotO = xRotO;
        entity.yHeadRotO = yHeadRotO;
        entity.yHeadRot = yHeadRot;
        entity.setDeltaMovement(vel);
    }

    public static void safeRegisterTexture(String key) { try { var id = SkinId.parse(key); if (id != null) SkinPackLoader.registerTextureFor(id); } catch (Exception ignored) {} }
    public static void safeResetPreview(String uuid) { try { com.brandonitaly.bedrockskins.client.SkinManager.resetPreviewSkin(uuid); } catch (Exception ignored) {} }

    private static boolean isUpsideDown(LivingEntity entity) {
        SkinId id = SkinManager.getSkin(entity.getUUID().toString());
        if (id == null) return false;
        LoadedSkin skin = SkinPackLoader.getLoadedSkin(id);
        return skin != null && skin.isUpsideDown();
    }

    public static void drawPanelChrome(GuiGraphics gui, int x, int y, int w, int h, Component title, Font font) {
        int PANEL_HEADER_HEIGHT = 24;
        int right = x + w;
        int bottom = y + h;

        int COL_PANEL_BG = 0xE6181818;
        int COL_PANEL_HEADER = 0xFF252525;
        int COL_BORDER_OUTER = 0xFF000000;
        int COL_BORDER_INNER = 0xFF383838;
        int COL_TEXT_TITLE = 0xFFFFFFFF;

        gui.fill(x - 1, y - 1, right + 1, bottom + 1, COL_BORDER_OUTER);
        gui.fill(x, y, right, bottom, COL_PANEL_BG);
        gui.fill(x, y, right, y + PANEL_HEADER_HEIGHT, COL_PANEL_HEADER);
        gui.fill(x, y + PANEL_HEADER_HEIGHT, right, y + PANEL_HEADER_HEIGHT + 1, COL_BORDER_INNER);
        gui.drawCenteredString(font, title, x + (w / 2), y + 8, COL_TEXT_TITLE);

        gui.fill(x, y, right, y + 1, COL_BORDER_INNER);
        gui.fill(x, bottom - 1, right, bottom, COL_BORDER_INNER);
        gui.fill(x, y, x + 1, bottom, COL_BORDER_INNER);
        gui.fill(right - 1, y, right, bottom, COL_BORDER_INNER);
    }
}