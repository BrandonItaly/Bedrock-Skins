package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GuiUtils {
    private GuiUtils() {}

    /**
     * Standard rendering for basic preview boxes.
     * Locks the head to the body rotation.
     */
    public static void renderEntityInRect(GuiGraphics gui, LivingEntity entity, float yawOffset, int left, int top, int right, int bottom, int sizeCap) {
        renderEntityInRect(gui, entity, yawOffset, 0.0F, left, top, right, bottom, sizeCap);
    }

    /**
     * Advanced rendering for full customization screens.
     * Allows an independent headYawOffset.
     */
    public static void renderEntityInRect(GuiGraphics gui, LivingEntity entity, float yawOffset, float headYawOffset, int left, int top, int right, int bottom, int sizeCap) {
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
        
        // Apply the independent head offset
        entity.yHeadRot = entity.getYRot() + headYawOffset;
        entity.yHeadRotO = entity.yHeadRot;

        // Get renderer and state
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var entityRenderer = entityRenderDispatcher.getRenderer(entity);

        // Prevent crash if renderers haven't finished loading
        if (entityRenderer == null) return;
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

        //~ if >=26.0 '.submitEntityRenderState' -> '.entity' {
        gui.submitEntityRenderState(entityRenderState, renderScale, vector3f, quat, quat2, left, top, right, bottom);
        //~}

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

    public static void playButtonClickSound() {
        try {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        } catch (Exception ignored) {
        }
    }

    private static boolean isUpsideDown(LivingEntity entity) {
        SkinId id = SkinManager.getSkin(entity.getUUID());
        if (id == null) return false;
        LoadedSkin skin = SkinPackLoader.getLoadedSkin(id);
        return skin != null && skin.upsideDown;
    }

    public static void drawPanelChrome(GuiGraphics gui, int x, int y, int w, int h, Component title, Font font) {
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.PANEL_SPRITE, x-1, y-1, w+2, h+2);
        gui.drawCenteredString(font, title, x + (w / 2), y + 8, 0xFFFFFFFF);
    }
}