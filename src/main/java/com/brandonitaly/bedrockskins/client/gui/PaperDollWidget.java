package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.BedrockModelManager;
import com.brandonitaly.bedrockskins.client.BedrockPlayerModel;
import com.brandonitaly.bedrockskins.client.BedrockSkinsClient;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}
//? if <26.3-snapshot-5 {
import org.lwjgl.glfw.GLFW;
//?}
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class PaperDollWidget extends AbstractWidget {
    public static final int PREVIEW_W = 78;
    public static final int PREVIEW_H = 112;
    public static final int BTN_SIZE = 20;
    private static final int RENDER_PADDING_TOP = 14;
    private static final int SIZE_CAP = 56;
    private static final double Y_TRANSLATION = 0.9;
    private static final double PLAYER_HEIGHT_BLOCKS = 1.8;
    private static final int SPACER = 8;

    private PreviewPlayer previewPlayer;
    private final UUID previewUuid = UUID.randomUUID();
    private final SpriteIconButton openSkinButton;
    private final Screen parentScreen;

    private float previewYaw = 0.0F;
    private boolean draggingPreview = false;
    private boolean leftMouseDown = false;
    private double lastRotMouseX = 0.0;

    public PaperDollWidget(int x, int y, Screen parentScreen, boolean isTitleScreen) {
        super(x, y, PREVIEW_W, PREVIEW_H + BTN_SIZE + SPACER, Component.translatable("bedrockskins.button.change_skin.tooltip"));
        this.parentScreen = parentScreen;

        Minecraft minecraft = Minecraft.getInstance();
        String name = minecraft.getGameProfile() != null ? minecraft.getGameProfile().name() : "Preview";
        this.previewPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(this.previewUuid, name));
        this.previewPlayer.setShowNameTag(true);
        this.previewPlayer.setDisplayName(Component.literal(name));
        updatePreviewSkin(minecraft);

        this.openSkinButton = SpriteIconButton.builder(
            Component.empty(),
            b -> minecraft.gui.setScreen(BedrockSkinsClient.getAppropriateSkinScreen(parentScreen)),
            true
        )
        .size(BTN_SIZE, BTN_SIZE)
        .sprite(BedrockSkinsSprites.MY_CHARACTERS_ICON, 16, 16)
        .build();
        this.openSkinButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.change_skin.tooltip")));
    }

    public static AbstractWidget findMenuTargetButton(Screen parentScreen) {
        AbstractWidget target = null;
        for (var child : parentScreen.children()) {
            if (!(child instanceof Button widget) || !widget.visible || child instanceof PaperDollWidget) continue;
            if (widget instanceof PlainTextButton || widget instanceof SpriteIconButton) continue;
            if (target == null || widget.getY() > target.getY() || (widget.getY() == target.getY() && widget.getX() > target.getX())) {
                target = widget;
            }
        }
        return target;
    }

    public static int getDefaultLeft(Screen parentScreen, int width) {
        AbstractWidget target = findMenuTargetButton(parentScreen);
        if (target != null) {
            return target.getX() + target.getWidth() + 20;
        }
        return Math.round((float) ((width * 5 / 6.0) - (PREVIEW_W / 2.0)));
    }

    public static int getDefaultTop(Screen parentScreen, int height) {
        AbstractWidget target = findMenuTargetButton(parentScreen);
        if (target != null) {
            return target.getY() + target.getHeight() - 115;
        }
        return Math.round((float) ((height / 2.0) - (PREVIEW_H / 2.0)));
    }

    private double getModelFeet(int top) {
        int size = Math.min((PREVIEW_H + RENDER_PADDING_TOP) / 3, SIZE_CAP);
        double centerY = (top - RENDER_PADDING_TOP + top + PREVIEW_H) / 2.0;
        return centerY + Y_TRANSLATION * size;
    }

    private double getModelTop(int top) {
        float heightMultiplier = 1.0f;
        SkinId selected = SkinManager.getLocalSelectedKey();
        if (selected != null) {
            BedrockPlayerModel model = BedrockModelManager.getModel(selected);
            if (model != null) heightMultiplier = model.heightMultiplier;
        }
        int size = Math.min((PREVIEW_H + RENDER_PADDING_TOP) / 3, SIZE_CAP);
        return getModelFeet(top) - (PLAYER_HEIGHT_BLOCKS * size * heightMultiplier);
    }

    private void updatePreviewSkin(Minecraft minecraft) {
        SkinId selected = SkinManager.getLocalSelectedKey();
        if (selected != null) {
            SkinManager.setPreviewSkin(previewUuid, selected.pack(), selected.name());
            SkinPackLoader.registerTextureFor(selected);
            previewPlayer.clearForcedProfileSkin();
        } else {
            SkinManager.resetPreviewSkin(previewUuid);
            var profile = minecraft.getGameProfile();
            if (profile != null) previewPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(profile, false).get());
        }
        previewPlayer.clearForcedBody();

        LoadedSkin loaded = selected != null ? SkinPackLoader.getLoadedSkin(selected) : null;
        SkinManager.ResolvedCape resolved = SkinManager.resolveCape(previewUuid, loaded, true);
        if (resolved != null) {
            previewPlayer.setForcedCape(resolved.capeId.equals(SkinManager.CAPE_NONE) ? null : resolved.capeId);
        } else {
            previewPlayer.clearForcedCape();
        }
        previewPlayer.setUseLocalPlayerModel(false);
    }

    private boolean isMouseOverModel(int mouseX, int mouseY) {
        int left = this.getX();
        int top = this.getY();
        return mouseX >= left && mouseX <= left + PREVIEW_W
            && mouseY >= top && mouseY <= top + PREVIEW_H;
    }

    //~ if >=26.1 'renderWidget' -> 'extractWidgetRenderState' {
    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) { //~}
        if (this.previewPlayer == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (SkinManager.getLocalSelectedKey() == null) {
            this.previewPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false).get());
        }

        //? if >=26.3-snapshot-5 {
        /*boolean leftDown = (org.lwjgl.sdl.SDLMouse.SDL_GetMouseState((java.nio.FloatBuffer) null, (java.nio.FloatBuffer) null) & org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LMASK) != 0;*/
        //?} else {
        long window = minecraft.getWindow().handle();
        boolean leftDown = window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        //?}

        if (leftDown && !this.leftMouseDown && isMouseOverModel(mouseX, mouseY)) {
            this.draggingPreview = true;
            this.lastRotMouseX = mouseX;
        }
        this.leftMouseDown = leftDown;
        if (!leftDown) this.draggingPreview = false;

        if (this.draggingPreview) {
            this.previewYaw -= (float) (mouseX - this.lastRotMouseX) * 1.6F;
            this.lastRotMouseX = mouseX;
        }

        int left = this.getX();
        int top = this.getY();

        GuiUtils.renderEntityInRect(guiGraphics, this.previewPlayer, this.previewYaw, left - 40, top - RENDER_PADDING_TOP, left + PREVIEW_W + 40, top + PREVIEW_H, SIZE_CAP);

        if (this.previewPlayer.shouldShowName()) {
            Font font = minecraft.font;
            int nametagTopY = (int) Math.round(getModelTop(top) - SPACER - font.lineHeight);
            GuiUtils.renderNameTag(guiGraphics, font, this.previewPlayer.getDisplayName(), left + (PREVIEW_W / 2), Math.max(2, nametagTopY));
        }

        this.openSkinButton.setX(left + (PREVIEW_W - BTN_SIZE) / 2);
        this.openSkinButton.setY((int) Math.round(getModelFeet(top) + SPACER));
        //~ if >=26.1 'render' -> 'extractRenderState' {
        this.openSkinButton.extractRenderState(guiGraphics, mouseX, mouseY, delta); //~}
    }

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (!this.visible || !this.active) return false;
        return this.openSkinButton.mouseClicked(click, doubled) || this.isMouseOver(click.x(), click.y());
    }
    //?} else {
    /*
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) return false;
        return this.openSkinButton.mouseClicked(mouseX, mouseY, button) || this.isMouseOver(mouseX, mouseY);
    }
    */
    //?}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("bedrockskins.button.change_skin.tooltip"));
    }

    public void removed() {
        PreviewPlayer.PreviewPlayerPool.remove(this.previewUuid);
        SkinManager.resetPreviewSkin(this.previewUuid);
        this.previewPlayer = null;
    }
}
