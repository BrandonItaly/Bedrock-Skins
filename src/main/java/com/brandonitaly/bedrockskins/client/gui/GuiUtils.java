package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.BedrockRenderStateAccessor;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.UUID;

public final class GuiUtils {
    private GuiUtils() {}

    /**
     * Standard rendering for basic preview boxes using PreviewPlayer data.
     */
    public static void renderEntityInRect(GuiGraphicsExtractor gui, PreviewPlayer preview, float yawOffset, int left, int top, int right, int bottom, int sizeCap) {
        float yaw = 180.0F + yawOffset;
        Minecraft minecraft = Minecraft.getInstance();

        // Build an avatar render state for UI rendering.
        AvatarRenderState entityRenderState = new AvatarRenderState();
        if (entityRenderState instanceof BedrockRenderStateAccessor accessor) {
            accessor.bedrockSkins$setUniqueId(preview.getUuid());
            accessor.bedrockSkins$setBedrockSkinId(SkinManager.getSkin(preview.getUuid()));
        }
        if (preview.shouldShowName()) {
            entityRenderState.nameTag = preview.getDisplayName();
        }
        entityRenderState.id = -0x5D011;
        entityRenderState.entityType = EntityType.PLAYER;
        entityRenderState.lightCoords = 15728880;
        entityRenderState.boundingBoxHeight = 1.8F;
        entityRenderState.boundingBoxWidth = 0.6F;
        entityRenderState.bodyRot = yaw;
        entityRenderState.yRot = 0.0F;
        entityRenderState.xRot = 0.0F;
        entityRenderState.pose = Pose.STANDING;
        entityRenderState.isBaby = false;
        entityRenderState.scale = 1.0F;
        entityRenderState.ageInTicks = (float) Util.getMillis() / 50.0F;
        entityRenderState.ageScale = entityRenderState.scale;
        entityRenderState.showHat = isModelPartEnabled(PlayerModelPart.HAT);
        entityRenderState.showJacket = isModelPartEnabled(PlayerModelPart.JACKET);
        entityRenderState.showLeftSleeve = isModelPartEnabled(PlayerModelPart.LEFT_SLEEVE);
        entityRenderState.showRightSleeve = isModelPartEnabled(PlayerModelPart.RIGHT_SLEEVE);
        entityRenderState.showLeftPants = isModelPartEnabled(PlayerModelPart.LEFT_PANTS_LEG);
        entityRenderState.showRightPants = isModelPartEnabled(PlayerModelPart.RIGHT_PANTS_LEG);
        entityRenderState.showCape = isModelPartEnabled(PlayerModelPart.CAPE);
        entityRenderState.attackArm = HumanoidArm.RIGHT;
        entityRenderState.attackTime = 0.0F;
        entityRenderState.isCrouching = false;
        entityRenderState.skin = preview.getSkin(minecraft);

        // Calculate size/scale
        int height = bottom - top;
        int size = Math.min((int) (height / 3.0), sizeCap);
        float scale = entityRenderState.scale;
        float centerY = 1.8F / 2.0F;
        if (isUpsideDown(preview.getUuid())) {
            centerY -= 1.8F;
        }
        Vector3f vector3f = new Vector3f(0.0F, centerY, 0.0F);
        float renderScale = (float) size / scale;

        // Quaternions
        Quaternionf quat = new Quaternionf().rotationZ((float) Math.toRadians(180.0F));
        Quaternionf quat2 = new Quaternionf().rotationX(0);
        quat.mul(quat2);
        quat2.conjugate();

        //~ if >=26.0 '.submitEntityRenderState' -> '.entity' {
        gui.entity(entityRenderState, renderScale, vector3f, quat, quat2, left, top, right, bottom);
        //~}
    }

    public static void safeRegisterTexture(String key) { try { var id = SkinId.parse(key); if (id != null) SkinPackLoader.registerTextureFor(id); } catch (Exception ignored) {} }

    public static void playButtonClickSound() {
        try {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        } catch (Exception ignored) {
        }
    }

    private static boolean isUpsideDown(UUID uuid) {
        SkinId id = SkinManager.getSkin(uuid);
        if (id == null) return false;
        LoadedSkin skin = SkinPackLoader.getLoadedSkin(id);
        return skin != null && skin.upsideDown;
    }

    public static void drawPanelChrome(GuiGraphicsExtractor gui, int x, int y, int w, int h, Component title, Font font) {
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.PANEL_SPRITE, x-1, y-1, w+2, h+2);
        gui.centeredText(font, title, x + (w / 2), y + 8, 0xFFFFFFFF);
    }

    public static void renderNameTag(GuiGraphicsExtractor gui, Font font, Component text, int centerX, int topY) {
        if (text == null) return;
        int textWidth = font.width(text);
        int padding = 2;
        int left = centerX - (textWidth / 2) - padding;
        int right = centerX + (textWidth / 2) + padding;
        int bottom = topY + font.lineHeight;
        int background = 0x55000000;

        gui.fill(left, topY - 1, right, bottom, background);
        gui.text(font, text, centerX - (textWidth / 2), topY, 0xFFFFFFFF, false);
    }

    private static boolean isModelPartEnabled(PlayerModelPart part) {
        return Minecraft.getInstance().options.isModelPartEnabled(part);
    }

}