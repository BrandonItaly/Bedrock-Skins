package io.github.brandonitaly.bedrockskins.gui.preview;

import io.github.brandonitaly.bedrockskins.client.render.state.BedrockRenderStateStore;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import io.github.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GuiUtils {
    public static final int PANEL_HEADER_HEIGHT = 26;
    private static final float COSMETIC_PREVIEW_SCALE = 0.7F;
    
    private GuiUtils() {}

    public static void setupAvatarRenderState(AvatarRenderState state, PreviewPlayer preview,
                                              float yaw, boolean crouch, float attackTime) {
        Minecraft minecraft = Minecraft.getInstance();
        var options = minecraft.options;

        BedrockRenderStateStore.setUniqueId(state, preview.getUuid());
        BedrockRenderStateStore.setSkinId(state, SkinManager.getSkin(preview.getUuid()));
        BedrockRenderStateStore.setGuiRender(state, true);

        state.nameTag = preview.shouldShowName() ? preview.getDisplayName() : null;

        state.id = -0x5D011;
        //~ if >=26.2 'EntityType.' -> 'EntityTypes.' {
        state.entityType = EntityTypes.PLAYER;
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
        state.isCrouching = crouch;
        state.skin = preview.getSkin(minecraft);
    }

    private static final Vector3f TEMP_TRANSLATE = new Vector3f();
    private static final Quaternionf TEMP_BODY_ROT = new Quaternionf();
    private static final Quaternionf TEMP_CAM_ROT = new Quaternionf();
    private static final Vector3f TEMP_TRANSLATE_CAPE = new Vector3f();
    private static final Quaternionf TEMP_BODY_ROT_CAPE = new Quaternionf();
    private static final Quaternionf TEMP_CAM_ROT_CAPE = new Quaternionf();
    private static final Quaternionf COSMETIC_BODY_ROT = new Quaternionf()
        .rotationZ((float) Math.PI).rotateX(-0.22F);
    private static final Quaternionf COSMETIC_CAM_ROT = new Quaternionf();
    private static final CosmeticFrame WHOLE_BODY_FRAME = new CosmeticFrame(0.9F);
    private static final CosmeticFrame HEAD_FRAME = new CosmeticFrame(1.48F);
    private static final CosmeticFrame HOOD_FRAME = new CosmeticFrame(1.34F);
    private static final CosmeticFrame ARMS_FRAME = new CosmeticFrame(1.02F);
    private static final CosmeticFrame TORSO_FRAME = new CosmeticFrame(1.14F);
    private static final CosmeticFrame LEGS_FRAME = new CosmeticFrame(0.58F);
    private static final CosmeticFrame BACK_FRAME = new CosmeticFrame(1.08F);
    public static final Identifier EQUIPPED_BORDER = Identifier.fromNamespaceAndPath("bedrockskins", "container/equipped_item_border");

    public static void renderEntityInRect(GuiGraphicsExtractor gui, PreviewPlayer preview, float yawOffset, int left, int top, int right, int bottom, int sizeCap) {
        renderEntityInRect(gui, preview, yawOffset, left, top, right, bottom, sizeCap, 200.0F);
    }

    public static void renderEntityInRect(GuiGraphicsExtractor gui, PreviewPlayer preview, float yawOffset, int left, int top, int right, int bottom, int sizeCap, float baseYaw) {
        renderEntityInRect(gui, preview, yawOffset, left, top, right, bottom, sizeCap, baseYaw, 1.0F / 3.0F);
    }

    private static void renderEntityInRect(GuiGraphicsExtractor gui, PreviewPlayer preview, float yawOffset,
                                           int left, int top, int right, int bottom, int sizeCap, float baseYaw,
                                           float heightScale) {
        AvatarRenderState state = createAvatarRenderState(preview, baseYaw + yawOffset, false);

        int height = bottom - top;
        int size = Math.max(1, Math.min(Math.round(height * heightScale), sizeCap));
        float centerY = isUpsideDown(preview) ? -0.9F : 0.9F;
        TEMP_TRANSLATE.set(0.0F, centerY, 0.0F);
        TEMP_BODY_ROT.identity().rotationZ((float) Math.PI).rotateX(-0.1F);
        TEMP_CAM_ROT.identity(); 

        //~ if >=26.1 '.submitEntityRenderState' -> '.entity' {
        gui.entity(state, size, TEMP_TRANSLATE, TEMP_BODY_ROT, TEMP_CAM_ROT, left, top, right, bottom);
        //~}
    }

    public static void renderGridEntityInRect(GuiGraphicsExtractor gui, PreviewPlayer preview, float hoverYaw,
                                              int left, int top, int right, int bottom) {
        renderEntityInRect(gui, preview, hoverYaw, left, top, right, bottom,
            72, 180.0F, 0.4F);
    }

    public static void renderCapeInRect(GuiGraphicsExtractor gui, PreviewPlayer preview, float hoverYaw, int left, int top, int right, int bottom) {
        float finalYaw = -35.0F + hoverYaw;
        AvatarRenderState state = createAvatarRenderState(preview, finalYaw, true);

        int height = bottom - top;
        int size = (int) (height * 0.78F);
        float centerY = isUpsideDown(preview) ? -0.9F : 0.9F;
        TEMP_TRANSLATE_CAPE.set(0.0F, centerY, 0.0F);
        TEMP_BODY_ROT_CAPE.identity().rotationZ((float) Math.PI).rotateX(-0.22F);
        TEMP_CAM_ROT_CAPE.identity(); 

        //~ if >=26.1 '.submitEntityRenderState' -> '.entity' {
        gui.entity(state, size, TEMP_TRANSLATE_CAPE, TEMP_BODY_ROT_CAPE, TEMP_CAM_ROT_CAPE, left, top, right, bottom);
        //~}
    }

    /** Renders a Persona preview cropped around the body region affected by its piece type. */
    public static void renderCosmeticInRect(GuiGraphicsExtractor gui, PreviewPlayer preview,
                                            String type, int left, int top, int right, int bottom) {
        boolean back = "persona_back".equals(type);
        AvatarRenderState state = createAvatarRenderState(preview, back ? -35.0F : 135.0F, true);

        CosmeticFrame frame = cosmeticFrame(type);
        int size = Math.max(1, Math.round((bottom - top) * COSMETIC_PREVIEW_SCALE));
        Vector3f translation = frame.translation(isUpsideDown(preview));

        //~ if >=26.1 '.submitEntityRenderState' -> '.entity' {
        gui.entity(state, size, translation, COSMETIC_BODY_ROT,
            COSMETIC_CAM_ROT, left, top, right, bottom);
        //~}
    }

    private static CosmeticFrame cosmeticFrame(String type) {
        if (type == null) return WHOLE_BODY_FRAME;
        return switch (type) {
            case "persona_hair", "persona_facial_hair", "persona_head", "persona_eyes",
                 "persona_mouth", "persona_face_accessory" -> HEAD_FRAME;
            case "persona_hood" -> HOOD_FRAME;
            case "persona_arms", "persona_hand" -> ARMS_FRAME;
            case "persona_top", "persona_outerwear" -> TORSO_FRAME;
            case "persona_bottom", "persona_high_pants", "persona_legs", "persona_feet" -> LEGS_FRAME;
            case "persona_back" -> BACK_FRAME;
            default -> WHOLE_BODY_FRAME;
        };
    }

    private static AvatarRenderState createAvatarRenderState(PreviewPlayer preview, float yaw, boolean freezeAnimation) {
        AvatarRenderState state = new AvatarRenderState();
        setupAvatarRenderState(state, preview, yaw, false, 0.0F);
        if (freezeAnimation) state.ageInTicks = 0.0F;
        state.yRot = 0.0F;
        state.xRot = 0.0F;
        return state;
    }

    private record CosmeticFrame(Vector3f normalTranslation, Vector3f upsideDownTranslation) {
        private CosmeticFrame(float centerY) {
            this(new Vector3f(0.0F, centerY, 0.0F), new Vector3f(0.0F, -centerY, 0.0F));
        }

        private Vector3f translation(boolean upsideDown) {
            return upsideDown ? upsideDownTranslation : normalTranslation;
        }
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
            renderGridEntityInRect(gui, player, hoverYaw, x + 1, y + 1, x + w - 1, y + h - 1);
        }

        if (equipped) {
            gui.blitSprite(RenderPipelines.GUI_TEXTURED, EQUIPPED_BORDER, x, y, w, h);
        }

        if (hovered && tooltipText != null) {
            gui.setTooltipForNextFrame(font, tooltipText, mouseX, mouseY);
        }
    }

    public static void renderGeometryCard(GuiGraphicsExtractor gui, Font font, PreviewPlayer player, Component label,
                                          int x, int y, int width, int height, boolean selected, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        Identifier sprite = selected ? BedrockSkinsSprites.CARD_SELECTED
            : hovered ? BedrockSkinsSprites.CARD_HOVER : BedrockSkinsSprites.CARD_IDLE;
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);

        if (player != null) {
            renderEntityInRect(gui, player, 0.0F, x + 2, y - 10, x + width - 2, y + 130, 112);
        }
        gui.centeredText(font, label, x + width / 2, y + height - 14, 0xFFFFFFFF);
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

    public static void renderPackCard(GuiGraphicsExtractor gui, Font font, Component text, int x, int y, int w, int h, boolean hovered, boolean selected, int mouseX, int mouseY) {
        renderPackCard(gui, font, text, x, y, w, h, hovered, selected, mouseX, mouseY, 1.0F);
    }

    public static void renderPackCard(GuiGraphicsExtractor gui, Font font, Component text, int x, int y, int w, int h, boolean hovered, boolean selected, int mouseX, int mouseY, float textScale) {
        var cardSprite = selected ? BedrockSkinsSprites.CARD_SELECTED : (hovered ? BedrockSkinsSprites.CARD_HOVER : BedrockSkinsSprites.CARD_IDLE);
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, cardSprite, x, y, w, h);
        int textX = x + 8;
        int textY = y + (h - (int) (font.lineHeight * textScale)) / 2;
        gui.pose().pushMatrix();
        gui.pose().translate(textX, textY);
        gui.pose().scale(textScale, textScale);
        gui.text(font, text, 0, 0, selected || hovered ? 0xFFFFFFFF : 0xFFD7D7D7, false);
        gui.pose().popMatrix();
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

    private static boolean isUpsideDown(PreviewPlayer preview) {
        SkinId id = SkinManager.getSkin(preview.getUuid());
        if (id == null) return false;
        
        LoadedSkin skin = SkinPackLoader.getLoadedSkin(id);
        return skin != null && skin.upsideDown;
    }

    public static void drawPanelChrome(GuiGraphicsExtractor gui, int x, int y, int w, int h, Component title, Font font) {
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.PANEL_SPRITE, x - 1, y - 1, w + 2, h + 2);
        gui.text(font, title, x + 8,
            y + Math.max(0, (PANEL_HEADER_HEIGHT - font.lineHeight) / 2), 0xFFFFFFFF, false);
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
