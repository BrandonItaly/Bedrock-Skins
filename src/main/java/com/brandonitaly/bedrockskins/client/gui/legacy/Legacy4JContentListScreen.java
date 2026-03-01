package com.brandonitaly.bedrockskins.client.gui.legacy;

//? if legacy4j {
/*
import com.brandonitaly.bedrockskins.client.ContentManager;
import com.brandonitaly.bedrockskins.client.pack.ContentPack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;
import java.util.List;

public class Legacy4JContentListScreen extends PanelVListScreen implements ControlTooltip.Event {
    
    protected final Component title;
    protected final List<ContentPack> packs;
    protected ContentPack hoveredPack; 
    private final LogoRenderer logoRenderer = new LogoRenderer(false);
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, 250);

    public Legacy4JContentListScreen(Screen parent, Component title, List<ContentPack> packs) {
        super(s -> Panel.createPanel(s, 
                // Increased width slightly to maintain button size with smaller padding
                p -> p.appearance(286, 249), 
                p -> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s) + 17)), 
                Component.empty()
        );
        this.parent = parent;
        this.title = title;
        this.packs = packs;
        
        renderableVList.layoutSpacing(l -> 0);
        
        for (ContentPack pack : packs) {
            addMenuButton(pack);
        }
    }

    private void addMenuButton(ContentPack pack) {
        renderableVList.addRenderable(new LeftAlignedButton(246, 36, pack, b -> {
            ContentManager.downloadPack(pack, () -> {});
        }) {
            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                if (focused) hoveredPack = pack; 
            }
        }); 
    }

    @Override
    public void renderableVListInit() {
        tooltipBox.init();
        initRenderableVListHeight(36);
        
        addRenderableOnly((guiGraphics, i, j, f) -> {
            // FIX: Recessed panel now uses 10px padding on ALL sides (X, Y, Width, Height)
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.PANEL_RECESS, 
                panel.getX() + 10, panel.getY() + 10, panel.getWidth() - 20, panel.getHeight() - 20);
            
            float textScale = 1.2f;
            int scaledTextWidth = (int)(font.width(title) * textScale);
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(panel.getX() + (panel.getWidth() - scaledTextWidth) / 2, panel.getY() + 18);
            guiGraphics.pose().scale(textScale, textScale);
            guiGraphics.drawString(font, title, 0, 0, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            guiGraphics.pose().popMatrix();
        });

        getRenderableVList().init("renderableVList", panel.getX() + 20, panel.getY() + 32, 246, 200);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), guiGraphics, false);
        logoRenderer.renderLogo(guiGraphics, this.width, 1.0f);

        tooltipBox.render(guiGraphics, mouseX, mouseY, partialTick);
        
        if (hoveredPack != null) {
            int tx = tooltipBox.getX() + 10;
            int ty = tooltipBox.getY() + 10;
            int wrapWidth = tooltipBox.getWidth() - 20;

            String name = hoveredPack.name() != null ? hoveredPack.name() : "Unknown Pack";
            String desc = hoveredPack.description() != null ? hoveredPack.description() : "";

            guiGraphics.drawString(font, name, tx, ty, 0xFFFFFFFF);
            
            if (!desc.isEmpty()) {
                List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(desc), wrapWidth);
                for (int i = 0; i < lines.size() && i < 10; i++) {
                    guiGraphics.drawString(font, lines.get(i), tx, ty + 15 + (i * 10), 0xFFCCCCCC);
                }
            }
        }

        panel.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static class LeftAlignedButton extends Button {
        private final ContentPack pack;

        public LeftAlignedButton(int width, int height, ContentPack pack, OnPress onPress) {
            super(0, 0, width, height, Component.literal(pack.name()), onPress, DEFAULT_NARRATION);
            this.pack = pack;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            
            if (ContentManager.isPackInstalled(pack)) {
                int spriteSize = 20;
                int sx = this.getX() + this.width - spriteSize - 10;
                int sy = this.getY() + (this.height - spriteSize) / 2;
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.BEACON_CONFIRM, sx, sy, spriteSize, spriteSize);
            }
        }

        @Override
        public void renderString(GuiGraphics guiGraphics, Font font, int color) {
            int textY = this.getY() + (this.getHeight() - font.lineHeight) / 2 + 1;
            guiGraphics.drawString(font, this.getMessage(), this.getX() + 12, textY, color, true);
        }
    }
}
*/
//?}