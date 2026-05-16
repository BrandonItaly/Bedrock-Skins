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
//? if >=26.2-snapshot-7
// import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.UUID;

public final class GuiUtils {
    
    private GuiUtils() {}

    public static void setupAvatarRenderState(AvatarRenderState state, PreviewPlayer preview, SkinId skinId, float yaw, boolean crouch, float attackTime) {
        Minecraft minecraft = Minecraft.getInstance();
        var options = minecraft.options;

        if (state instanceof BedrockRenderStateAccessor accessor) {
            accessor.bedrockSkins$setUniqueId(preview.getUuid());
            accessor.bedrockSkins$setBedrockSkinId(skinId);
        }

        if (preview.shouldShowName()) state.nameTag = preview.getDisplayName();

        state.id = -0x5D011;
        //~ if >=26.2-snapshot-7 'EntityType.' -> 'EntityTypes.' {
        state.entityType = EntityType.PLAYER;
        //~}
        state.lightCoords = 15728880;
        state.boundingBoxHeight = 1.8F;
        state.boundingBoxWidth = 0.6F;
        state.bodyRot = yaw;
        state.pose = crouch ? Pose.CROUCHING : Pose.STANDING;
        state.isBaby = false;
        state.scale = 1.0F;
        state.ageInTicks = (float) Util.getMillis() / 50.0F;
        state.ageScale = 1.0F;

        state.showHat = options.isModelPartEnabled(PlayerModelPart.HAT);
        state.showJacket = options.isModelPartEnabled(PlayerModelPart.JACKET);
        state.showLeftSleeve = options.isModelPartEnabled(PlayerModelPart.LEFT_SLEEVE);
        state.showRightSleeve = options.isModelPartEnabled(PlayerModelPart.RIGHT_SLEEVE);
        state.showLeftPants = options.isModelPartEnabled(PlayerModelPart.LEFT_PANTS_LEG);
        state.showRightPants = options.isModelPartEnabled(PlayerModelPart.RIGHT_PANTS_LEG);
        state.showCape = options.isModelPartEnabled(PlayerModelPart.CAPE);

        state.attackArm = HumanoidArm.RIGHT;
        state.attackTime = attackTime;
        state.isCrouching = crouch;
        state.skin = preview.getSkin(minecraft);
    }

    public static void renderEntityInRect(GuiGraphicsExtractor gui, PreviewPlayer preview, float yawOffset, int left, int top, int right, int bottom, int sizeCap) {
        AvatarRenderState state = new AvatarRenderState();
        setupAvatarRenderState(state, preview, SkinManager.getSkin(preview.getUuid()), 180.0F + yawOffset, false, 0.0F);
        
        state.yRot = 0.0F;
        state.xRot = 0.0F;

        int size = Math.min((bottom - top) / 3, sizeCap);
        float centerY = isUpsideDown(preview.getUuid()) ? -0.9F : 0.9F;
        Vector3f translate = new Vector3f(0.0F, centerY, 0.0F);
        Quaternionf bodyRotation = new Quaternionf().rotationZ((float) Math.PI);
        Quaternionf cameraRotation = new Quaternionf(); 

        //~ if >=26.1 '.submitEntityRenderState' -> '.entity' {
        gui.entity(state, size, translate, bodyRotation, cameraRotation, left, top, right, bottom);
        //~}
    }

    public static void safeRegisterTexture(String key) { 
        try { 
            SkinId id = SkinId.parse(key); 
            if (id != null) SkinPackLoader.registerTextureFor(id); 
        } catch (Exception ignored) {} 
    }

    public static void playButtonClickSound() {
        try {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        } catch (Exception ignored) {}
    }

    private static boolean isUpsideDown(UUID uuid) {
        SkinId id = SkinManager.getSkin(uuid);
        if (id == null) return false;
        
        LoadedSkin skin = SkinPackLoader.getLoadedSkin(id);
        return skin != null && skin.upsideDown;
    }

    public static void drawPanelChrome(GuiGraphicsExtractor gui, int x, int y, int w, int h, Component title, Font font) {
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.PANEL_SPRITE, x - 1, y - 1, w + 2, h + 2);
        gui.centeredText(font, title, x + (w / 2), y + 8, 0xFFFFFFFF);
    }

    public static void renderNameTag(GuiGraphicsExtractor gui, Font font, Component text, int centerX, int topY) {
        if (text == null) return;
        
        int halfWidth = font.width(text) / 2;
        int padding = 2;
        int left = centerX - halfWidth - padding;
        int right = centerX + halfWidth + padding;
        int bottom = topY + font.lineHeight;

        gui.fill(left, topY - 1, right, bottom, 0x55000000);
        gui.text(font, text, centerX - halfWidth, topY, 0xFFFFFFFF, false);
    }
}