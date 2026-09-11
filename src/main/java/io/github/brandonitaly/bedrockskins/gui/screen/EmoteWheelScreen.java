package io.github.brandonitaly.bedrockskins.gui.screen;

import io.github.brandonitaly.bedrockskins.gui.preview.*;
import io.github.brandonitaly.bedrockskins.gui.widget.*;

import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.client.BedrockSkinsClient;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedEmote;
import io.github.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Bedrock-style six-slot radial selector for Persona emotes. */
public final class EmoteWheelScreen extends Screen {
    private static final int SLOTS = 6;
    private static final int WHEEL_WIDTH = 185;
    private static final int WHEEL_HEIGHT = 172;
    private static final int[][] SLOT_CENTERS = {
        {61, 34}, {123, 34}, {154, 86}, {123, 138}, {61, 138}, {30, 86}
    };

    private final Screen parent;
    private final LoadedEmote emoteToEquip;
    private int hoveredSlot = -1;

    public EmoteWheelScreen(Screen parent) {
        this(parent, null);
    }

    /** Creates a wheel that assigns one selected emote instead of playing a slot. */
    public EmoteWheelScreen(Screen parent, LoadedEmote emoteToEquip) {
        super(Component.translatable("bedrockskins.emote_wheel.title"));
        this.parent = parent;
        this.emoteToEquip = emoteToEquip;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 120;
        Component buttonText = emoteToEquip == null
            ? Component.translatable("bedrockskins.button.change_emotes")
            : CommonComponents.GUI_CANCEL;
        addRenderableWidget(Button.builder(buttonText,
            button -> {
                if (emoteToEquip == null) {
                    minecraft.gui.setScreen(new SkinSelectionScreen(parent, AppearanceTab.EMOTES));
                } else {
                    onClose();
                }
            })
            .bounds((width - buttonWidth) / 2, changeButtonY(), buttonWidth, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        int wheelX = (width - WHEEL_WIDTH) / 2;
        int wheelY = wheelY();
        hoveredSlot = slotAt(mouseX, mouseY, wheelX, wheelY);
        if (emoteToEquip == null && emoteAt(hoveredSlot) == null) hoveredSlot = -1;

        int selectedSlot = emoteToEquip == null ? -1 : EmoteManager.slotOf(emoteToEquip);
        int highlightedSlot = hoveredSlot >= 0 ? hoveredSlot : selectedSlot;

        gui.centeredText(font, title, width / 2, wheelY - 17, 0xFFFFFFFF);
        gui.blitSprite(RenderPipelines.GUI_TEXTURED,
            highlightedSlot < 0 ? BedrockSkinsSprites.EMOTE_WHEEL_BASE
                : BedrockSkinsSprites.EMOTE_WHEEL_SELECTIONS[highlightedSlot],
            wheelX, wheelY, WHEEL_WIDTH, WHEEL_HEIGHT);

        for (int slot = 0; slot < SLOTS; slot++) {
            LoadedEmote emote = emoteAt(slot);
            int centerX = wheelX + SLOT_CENTERS[slot][0];
            int centerY = wheelY + SLOT_CENTERS[slot][1];
            if (emote == null) {
                int iconSize = 12;
                gui.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.NONE_ICON,
                    centerX - iconSize / 2, centerY - iconSize / 2, iconSize, iconSize);
                continue;
            }
            EmoteManager.Thumbnail thumbnail = EmoteManager.thumbnail(emote);
            if (thumbnail != null) {
                int maxSize = 42;
                float scale = Math.min(maxSize / (float) thumbnail.width(), maxSize / (float) thumbnail.height());
                int drawWidth = Math.max(1, Math.round(thumbnail.width() * scale));
                int drawHeight = Math.max(1, Math.round(thumbnail.height() * scale));
                gui.blit(RenderPipelines.GUI_TEXTURED, thumbnail.texture(),
                    centerX - drawWidth / 2, centerY - drawHeight / 2,
                    0.0F, 0.0F,
                    drawWidth, drawHeight,
                    thumbnail.width(), thumbnail.height(),
                    thumbnail.width(), thumbnail.height());
            } else {
                int color = slot == hoveredSlot ? 0xFFFFFFFF : 0xFFE8E8E8;
                gui.centeredText(font, Component.literal(shortName(emote.displayName(), 48)),
                    centerX, centerY - font.lineHeight / 2, color);
            }
        }

        LoadedEmote centerEmote = emoteToEquip != null ? emoteToEquip : emoteAt(hoveredSlot);
        if (centerEmote != null) {
            renderCenterLabel(gui, centerEmote.displayName(),
                wheelX + WHEEL_WIDTH / 2, wheelY + WHEEL_HEIGHT / 2);
        }

        if (EmoteManager.all().isEmpty()) {
            gui.centeredText(font, Component.translatable("bedrockskins.emotes.none"),
                width / 2, wheelY + WHEEL_HEIGHT + 5, 0xFFAAAAAA);
        } else if (emoteToEquip != null) {
            Component footer;
            if (hoveredSlot < 0) {
                footer = Component.translatable("bedrockskins.emote_wheel.choose_slot",
                    emoteToEquip.displayName());
            } else if (hoveredSlot == selectedSlot) {
                footer = Component.translatable("bedrockskins.emote_wheel.unequip_slot", hoveredSlot + 1);
            } else {
                footer = Component.translatable("bedrockskins.emote_wheel.equip_slot", hoveredSlot + 1);
            }
            gui.centeredText(font, footer, width / 2, wheelY + WHEEL_HEIGHT + 5, 0xFFFFFFFF);
        }

        super.extractRenderState(gui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (isLeftButton(event.button())) {
            int slot = slotAt(event.x(), event.y(), (width - WHEEL_WIDTH) / 2,
                wheelY());
            if (choose(slot)) return true;
        }
        return super.mouseClicked(event, doubled);
    }

    private static boolean isLeftButton(int button) {
        return button == 0 || button == InputConstants.MOUSE_BUTTON_LEFT;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (BedrockSkinsClient.playEmoteKey != null
                && BedrockSkinsClient.playEmoteKey.matches(event)) {
            BedrockSkinsClient.markEmoteKeyHandledByScreen();
            onClose();
            return true;
        }
        int slot = event.key() - 49;
        if (slot >= 0 && slot < SLOTS && choose(slot)) return true;
        return super.keyPressed(event);
    }

    private int wheelY() {
        return Math.max(18, (height - WHEEL_HEIGHT) / 2 - 10);
    }

    private int changeButtonY() {
        return Math.min(height - 24, wheelY() + WHEEL_HEIGHT + 19);
    }

    private boolean choose(int slot) {
        if (slot < 0 || slot >= SLOTS) return false;
        if (emoteToEquip != null) {
            int equippedSlot = EmoteManager.slotOf(emoteToEquip);
            if (slot == equippedSlot) EmoteManager.unequip(slot);
            else EmoteManager.equip(slot, emoteToEquip);
            GuiUtils.playButtonClickSound();
            onClose();
            return true;
        }

        LoadedEmote emote = emoteAt(slot);
        if (emote == null) return false;
        EmoteManager.playLocal(emote);
        GuiUtils.playButtonClickSound();
        onClose();
        return true;
    }

    private LoadedEmote emoteAt(int slot) {
        return EmoteManager.slot(slot);
    }

    private static int slotAt(double mouseX, double mouseY, int wheelX, int wheelY) {
        double dx = mouseX - (wheelX + WHEEL_WIDTH / 2.0);
        double dy = mouseY - (wheelY + WHEEL_HEIGHT / 2.0);
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 27.0 || distance > 105.0) return -1;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        return Math.floorMod((int) Math.round((angle + 120.0) / 60.0), SLOTS);
    }

    private String shortName(String name, int maxWidth) {
        if (name == null) return "";
        if (font.width(name) <= maxWidth) return name;
        String suffix = "…";
        String shortened = name;
        while (!shortened.isEmpty() && font.width(shortened + suffix) > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + suffix;
    }

    private void renderCenterLabel(GuiGraphicsExtractor gui, String name, int centerX, int centerY) {
        List<FormattedCharSequence> wrapped = font.split(Component.literal(name), 48);
        int lineCount = Math.min(3, wrapped.size());
        int firstY = centerY - lineCount * font.lineHeight / 2;
        for (int i = 0; i < lineCount; i++) {
            FormattedCharSequence line = wrapped.get(i);
            gui.text(font, line, centerX - font.width(line) / 2,
                firstY + i * font.lineHeight, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (parent instanceof SkinSelectionScreen wardrobe) {
            wardrobe.restorePreviewAfterChildScreen();
        }
        minecraft.gui.setScreen(parent);
    }
}
