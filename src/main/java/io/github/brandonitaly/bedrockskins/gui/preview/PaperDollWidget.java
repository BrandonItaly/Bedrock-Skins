package io.github.brandonitaly.bedrockskins.gui.preview;

import io.github.brandonitaly.bedrockskins.client.render.model.BedrockModelManager;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.gui.screen.SkinSelectionScreen;
import io.github.brandonitaly.bedrockskins.gui.WidgetVisibility;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import io.github.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
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
import net.minecraft.client.input.MouseButtonEvent;
//? if <=26.2 {
import org.lwjgl.glfw.GLFW;
//?}
import net.minecraft.network.chat.Component;

public class PaperDollWidget extends AbstractWidget {
    public static final int PREVIEW_W = 78;
    public static final int PREVIEW_H = 112;
    public static final int BTN_SIZE = 20;
    private static final int RENDER_PADDING_TOP = 14;
    private static final int SIZE_CAP = 56;
    private static final double Y_TRANSLATION = 0.9;
    private static final double PLAYER_HEIGHT_BLOCKS = 1.8;
    private static final int SPACER = 8;
    private static final int SCREEN_MARGIN = 4;
    private static final int MENU_GAP = 20;
    private static final int SCALED_CONTENT_HEIGHT = 96;

    private PreviewPlayer previewPlayer;
    private final SpriteIconButton openSkinButton;
    private double layoutScale;

    private float previewYaw = 0.0F;
    private boolean draggingPreview = false;
    private boolean leftMouseDown = false;
    private double lastRotMouseX = 0.0;

    public PaperDollWidget(Screen parentScreen) {
        super(0, 0, PREVIEW_W, PREVIEW_H + BTN_SIZE + SPACER, Component.translatable("bedrockskins.button.wardrobe.tooltip"));
        positionBesideMenu(parentScreen);

        Minecraft minecraft = Minecraft.getInstance();
        String name = minecraft.getGameProfile() != null ? minecraft.getGameProfile().name() : "Preview";
        this.previewPlayer = new PreviewPlayer(name);
        this.previewPlayer.setShowNameTag(true);
        updatePreviewSkin(minecraft);
        PersonaManager.setPreviewFromLocal(this.previewPlayer.getUuid());

        this.openSkinButton = SpriteIconButton.builder(
            Component.empty(),
            b -> minecraft.gui.setScreen(new SkinSelectionScreen(parentScreen)),
            true
        )
        .size(BTN_SIZE, BTN_SIZE)
        .sprite(BedrockSkinsSprites.WARDROBE_ICON, 16, 16)
        .build();
        this.openSkinButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.wardrobe.tooltip")));
    }

    private void positionBesideMenu(Screen parentScreen) {
        int menuLeft = Integer.MAX_VALUE;
        int menuRight = Integer.MIN_VALUE;
        int menuBottom = 0;
        for (var child : parentScreen.children()) {
            if (!(child instanceof Button widget) || widget instanceof PlainTextButton || !WidgetVisibility.isVisible(widget)) continue;
            menuLeft = Math.min(menuLeft, widget.getX());
            menuRight = Math.max(menuRight, widget.getX() + widget.getWidth());
            menuBottom = Math.max(menuBottom, widget.getY() + widget.getHeight());
        }
        if (menuLeft == Integer.MAX_VALUE) {
            menuLeft = menuRight = parentScreen.width / 2;
            menuBottom = parentScreen.height / 2;
        }
        int rightSpace = Math.max(0, parentScreen.width - SCREEN_MARGIN - menuRight);
        int leftSpace = Math.max(0, menuLeft - SCREEN_MARGIN);
        boolean useRight = rightSpace >= PREVIEW_W + MENU_GAP || rightSpace >= leftSpace;
        int space = useRight ? rightSpace : leftSpace;
        int gap = space >= PREVIEW_W + MENU_GAP ? MENU_GAP : SPACER;
        double horizontalScale = (space - gap) / (double) PREVIEW_W;
        double verticalScale = (parentScreen.height - 2 * SCREEN_MARGIN - BTN_SIZE) / (double) SCALED_CONTENT_HEIGHT;
        this.layoutScale = Math.max(0, Math.min(1.0, Math.min(horizontalScale, verticalScale)));

        int width = Math.max(BTN_SIZE, (int) Math.floor(PREVIEW_W * this.layoutScale));
        int height = Math.max(scaled(PREVIEW_H), (int) Math.round(getModelFeet(0) + scaled(SPACER)) + BTN_SIZE);
        int left = useRight ? menuRight + gap : menuLeft - gap - width;
        left = Math.max(SCREEN_MARGIN, Math.min(left, parentScreen.width - SCREEN_MARGIN - width));
        int top = Math.max(SCREEN_MARGIN, Math.min(menuBottom - height, parentScreen.height - SCREEN_MARGIN - height));
        this.setPosition(left, top);
        this.setWidth(width);
        this.setHeight(height);
    }

    private int scaled(int value) {
        return (int) Math.round(value * this.layoutScale);
    }

    private int getModelSize() {
        return Math.max(1, Math.min((scaled(PREVIEW_H) + scaled(RENDER_PADDING_TOP)) / 3, scaled(SIZE_CAP)));
    }

    private double getModelFeet(int top) {
        double centerY = top + (scaled(PREVIEW_H) - scaled(RENDER_PADDING_TOP)) / 2.0;
        return centerY + Y_TRANSLATION * getModelSize();
    }

    private double getModelTop(int top) {
        float heightMultiplier = 1.0f;
        SkinId selected = SkinManager.getLocalSelectedKey();
        if (selected != null) {
            BedrockPlayerModel model = BedrockModelManager.getModel(selected);
            if (model != null) heightMultiplier = model.heightMultiplier;
        }
        return getModelFeet(top) - (PLAYER_HEIGHT_BLOCKS * getModelSize() * heightMultiplier);
    }

    private void updatePreviewSkin(Minecraft minecraft) {
        GuiSkinUtils.applyCurrentEquippedSkin(minecraft, previewPlayer);
    }

    private boolean isMouseOverModel(int mouseX, int mouseY) {
        int left = this.getX();
        int top = this.getY();
        return mouseX >= left && mouseX <= left + this.getWidth()
            && mouseY >= top && mouseY <= top + scaled(PREVIEW_H);
    }

    //~ if >=26.1 'renderWidget' -> 'extractWidgetRenderState' {
    @Override
    //? if >=26.4-snapshot-5 {
    /*protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics) {
        int mouseX = guiGraphics.mouseX();
        int mouseY = guiGraphics.mouseY();
    *///?} else {
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
    //?}
    //~}
        if (this.previewPlayer == null) return;

        // The paper doll uses a synthetic UUID, so keep its Persona preview bound
        // to the persisted local loadout rather than a world-only player entity.
        PersonaManager.setPreviewFromLocal(this.previewPlayer.getUuid());

        Minecraft minecraft = Minecraft.getInstance();
        if (SkinManager.getLocalSelectedKey() == null) {
            GuiSkinUtils.refreshAutoSelectedProfileSkin(minecraft, this.previewPlayer);
        }

        //? if >26.2 {
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

        GuiUtils.renderEntityInRect(guiGraphics, this.previewPlayer, this.previewYaw, left - scaled(40), top - scaled(RENDER_PADDING_TOP), left + this.getWidth() + scaled(40), top + scaled(PREVIEW_H), Math.max(1, scaled(SIZE_CAP)));

        if (this.previewPlayer.shouldShowName()) {
            Font font = minecraft.font;
            int nametagTopY = (int) Math.round(getModelTop(top) - SPACER - font.lineHeight);
            Component displayName = this.previewPlayer.getDisplayName();
            if (this.layoutScale < 1.0) {
                displayName = Component.literal(font.plainSubstrByWidth(displayName.getString(), Math.max(0, this.getWidth() - 4)));
            }
            GuiUtils.renderNameTag(guiGraphics, font, displayName, left + (this.getWidth() / 2), Math.max(2, nametagTopY));
        }

        this.openSkinButton.setX(left + (this.getWidth() - BTN_SIZE) / 2);
        this.openSkinButton.setY((int) Math.round(getModelFeet(top) + scaled(SPACER)));
        //~ if >=26.1 'render' -> 'extractRenderState' {
        //? if >=26.4-snapshot-5 {
        /*this.openSkinButton.extractRenderState(guiGraphics);
        *///?} else {
        this.openSkinButton.extractRenderState(guiGraphics, mouseX, mouseY, delta);
        //?}
        //~}
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (!this.visible || !this.active) return false;
        return this.openSkinButton.mouseClicked(click, doubled) || this.isMouseOver(click.x(), click.y());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("bedrockskins.button.wardrobe.tooltip"));
    }

    public void removed() {
        if (this.previewPlayer != null) {
            this.previewPlayer.close();
            this.previewPlayer = null;
        }
    }
}
