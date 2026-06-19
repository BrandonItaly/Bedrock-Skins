package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.BedrockRenderStateStore;
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
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
//? if >=26.2
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

        BedrockRenderStateStore.setUniqueId(state, preview.getUuid());
        BedrockRenderStateStore.setSkinId(state, skinId);

        state.nameTag = preview.shouldShowName() ? preview.getDisplayName() : null;

        state.id = -0x5D011;
        //~ if >=26.2 'EntityType.' -> 'EntityTypes.' {
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

    private static final Vector3f TEMP_TRANSLATE = new Vector3f();
    private static final Quaternionf TEMP_BODY_ROT = new Quaternionf();
    private static final Quaternionf TEMP_CAM_ROT = new Quaternionf();
    private static final Vector3f TEMP_TRANSLATE_CAPE = new Vector3f();
    private static final Quaternionf TEMP_BODY_ROT_CAPE = new Quaternionf();
    private static final Quaternionf TEMP_CAM_ROT_CAPE = new Quaternionf();
    public static final Identifier EQUIPPED_BORDER = Identifier.fromNamespaceAndPath("bedrockskins", "container/equipped_item_border");

    public static void renderEntityInRect(GuiGraphicsExtractor gui, PreviewPlayer preview, float yawOffset, int left, int top, int right, int bottom, int sizeCap) {
        renderEntityInRect(gui, preview, yawOffset, left, top, right, bottom, sizeCap, 200.0F);
    }

    public static void renderEntityInRect(GuiGraphicsExtractor gui, PreviewPlayer preview, float yawOffset, int left, int top, int right, int bottom, int sizeCap, float baseYaw) {
        AvatarRenderState state = new AvatarRenderState();
        setupAvatarRenderState(state, preview, SkinManager.getSkin(preview.getUuid()), baseYaw + yawOffset, false, 0.0F);
        
        state.yRot = 0.0F;
        state.xRot = 0.0F;

        int size = Math.min((bottom - top) / 3, sizeCap);
        float centerY = isUpsideDown(preview.getUuid()) ? -0.9F : 0.9F;
        TEMP_TRANSLATE.set(0.0F, centerY, 0.0F);
        TEMP_BODY_ROT.identity().rotationZ((float) Math.PI).rotateX(-0.1F);
        TEMP_CAM_ROT.identity(); 

        //~ if >=26.1 '.submitEntityRenderState' -> '.entity' {
        gui.entity(state, size, TEMP_TRANSLATE, TEMP_BODY_ROT, TEMP_CAM_ROT, left, top, right, bottom);
        //~}
    }

    public static void renderCapeInRect(GuiGraphicsExtractor gui, PreviewPlayer preview, float hoverYaw, int left, int top, int right, int bottom) {
        AvatarRenderState state = new AvatarRenderState();
        float finalYaw = -35.0F + hoverYaw;
        setupAvatarRenderState(state, preview, SkinManager.getSkin(preview.getUuid()), finalYaw, false, 0.0F);
        state.ageInTicks = 0.0F;
        
        state.yRot = 0.0F;
        state.xRot = 0.0F;

        int height = bottom - top;
        int size = (int) (height * 0.78F);
        float centerY = isUpsideDown(preview.getUuid()) ? -0.9F : 0.9F;
        TEMP_TRANSLATE_CAPE.set(0.0F, centerY, 0.0F);
        TEMP_BODY_ROT_CAPE.identity().rotationZ((float) Math.PI).rotateX(-0.22F);
        TEMP_CAM_ROT_CAPE.identity(); 

        //~ if >=26.1 '.submitEntityRenderState' -> '.entity' {
        gui.entity(state, size, TEMP_TRANSLATE_CAPE, TEMP_BODY_ROT_CAPE, TEMP_CAM_ROT_CAPE, left, top, right, bottom);
        //~}
    }

    public static void renderActionCard(GuiGraphicsExtractor gui, Font font, Component tooltipText, int x, int y, int w, int h, boolean hovered, int mouseX, int mouseY) {
        var cardSprite = hovered ? BedrockSkinsSprites.CARD_HOVER : BedrockSkinsSprites.CARD_IDLE;
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, cardSprite, x, y, w, h);

        int plusCenterX = x + (w / 2);
        int plusCenterY = y + (h / 2) - 2;
        int arm = 13;
        int thickness = 4;
        gui.fill(plusCenterX - arm, plusCenterY - (thickness / 2), plusCenterX + arm, plusCenterY + (thickness / 2) + 1, 0xFFFFFFFF);
        gui.fill(plusCenterX - (thickness / 2), plusCenterY - arm, plusCenterX + (thickness / 2) + 1, plusCenterY + arm, 0xFFFFFFFF);

        if (hovered && tooltipText != null) {
            gui.setTooltipForNextFrame(font, tooltipText, mouseX, mouseY);
        }
    }

    public static void renderSkinCard(GuiGraphicsExtractor gui, Font font, Component tooltipText, int x, int y, int w, int h, boolean hovered, boolean selected, boolean equipped, PreviewPlayer player, float hoverYaw, int mouseX, int mouseY) {
        var cardSprite = selected ? BedrockSkinsSprites.CARD_SELECTED : (hovered ? BedrockSkinsSprites.CARD_HOVER : BedrockSkinsSprites.CARD_IDLE);
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, cardSprite, x, y, w, h);

        if (player != null) {
            renderEntityInRect(gui, player, hoverYaw, x, y, x + w, y + h, 72, 180.0F);
        }

        if (equipped) {
            gui.blitSprite(RenderPipelines.GUI_TEXTURED, EQUIPPED_BORDER, x, y, w, h);
        }

        if (hovered && tooltipText != null) {
            gui.setTooltipForNextFrame(font, tooltipText, mouseX, mouseY);
        }
    }

    public static void renderPackCard(GuiGraphicsExtractor gui, Font font, String text, int x, int y, int w, int h, boolean hovered, boolean selected, int mouseX, int mouseY) {
        var cardSprite = selected ? BedrockSkinsSprites.CARD_SELECTED : (hovered ? BedrockSkinsSprites.CARD_HOVER : BedrockSkinsSprites.CARD_IDLE);
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, cardSprite, x, y, w, h);

        int textColor = selected ? 0xFFFFFFF0 : hovered ? 0xFFFFFFFF : 0xFFD7D7D7;
        int textX = x + 6;
        int textY = y + (h - font.lineHeight) / 2;
        int maxTextWidth = Math.max(20, w - 12);

        boolean truncated = font.width(text) > maxTextWidth;
        String shown = truncated
                ? font.plainSubstrByWidth(text, Math.max(0, maxTextWidth - font.width("..."))) + "..."
                : text;

        gui.text(font, Component.literal(shown), textX, textY, textColor, false);

        if (hovered && truncated) {
            gui.setTooltipForNextFrame(font, Component.literal(text), mouseX, mouseY);
        }
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