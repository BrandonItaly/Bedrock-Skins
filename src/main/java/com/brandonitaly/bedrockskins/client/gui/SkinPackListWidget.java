package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;

public class SkinPackListWidget extends AlwaysSelectedEntryListWidget<SkinPackListWidget.SkinPackEntry> {
    public SkinPackListWidget(MinecraftClient client, int width, int height, int y, int itemHeight,
                              java.util.function.Consumer<String> onSelect,
                              java.util.function.Predicate<String> isSelected,
                              TextRenderer textRenderer) {
        super(client, width, height, y, itemHeight);
        this.onSelect = onSelect;
        this.isSelected = isSelected;
        this.textRenderer = textRenderer;
    }

    private final java.util.function.Consumer<String> onSelect;
    private final java.util.function.Predicate<String> isSelected;
    private final TextRenderer textRenderer;

    @Override
    public int getRowWidth() { return getWidth() - 10; }

    @Override
    protected int getScrollbarX() { return getX() + getWidth() - 6; }

    public void addEntryPublic(SkinPackEntry entry) { super.addEntry(entry); }
    public void clear() { super.clearEntries(); }

    public static class SkinPackEntry extends AlwaysSelectedEntryListWidget.Entry<SkinPackEntry> {
        private final String packId;
        private final String translationKey;
        private final String fallbackName;
        private final java.util.function.Consumer<String> onSelect;
        private final java.util.function.Supplier<Boolean> isSelectedFn;
        private final TextRenderer textRenderer;

        public SkinPackEntry(String packId, String translationKey, String fallbackName,
                             java.util.function.Consumer<String> onSelect,
                             java.util.function.Supplier<Boolean> isSelectedFn,
                             TextRenderer textRenderer) {
            this.packId = packId;
            this.translationKey = translationKey;
            this.fallbackName = fallbackName;
            this.onSelect = onSelect;
            this.isSelectedFn = isSelectedFn;
            this.textRenderer = textRenderer;
        }

        private void renderCommon(DrawContext context, int x, int y, boolean hovered) {
            boolean isSelected = Boolean.TRUE.equals(isSelectedFn.get());
            int color = isSelected ? 0xFFFFFF00 : (hovered ? 0xFFFFFFA0 : 0xFFFFFFFF);
            String translated = SkinPackLoader.getTranslation(translationKey);
            if (translated == null) translated = fallbackName;
            context.drawTextWithShadow(textRenderer, Text.literal(translated), x + 2, y + 6, color);
        }

        private boolean clickCommon() {
            onSelect.accept(packId);
            try {
                MinecraftClient.getInstance().getSoundManager().play(
                    net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f)
                );
            } catch (Exception ignored) {}
            return true;
        }

        //? if <=1.21.8 {
        /*
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            renderCommon(context, x, y, hovered);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return clickCommon();
        }
        */
        //?} else {
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            renderCommon(context, getX(), getY(), hovered);
        }

        public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
            return clickCommon();
        }
        //?}

        public Text getNarration() { return Text.literal(packId); }
    }
}
