package com.brandonitaly.bedrockskins.client.gui.legacy;

//? if legacy4j {
/*
import com.brandonitaly.bedrockskins.client.ContentManager;
import com.brandonitaly.bedrockskins.client.pack.ContentPack;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;
import java.io.File;
import java.util.List;

public class Legacy4JContentListScreen extends PanelVListScreen implements ControlTooltip.Event {
    
    protected final Component title;
    protected final List<ContentPack> packs;
    protected ContentPack hoveredPack; 
    private final LogoRenderer logoRenderer = new LogoRenderer(false);
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, 250);
    
    private boolean needsReload = false;

    public Legacy4JContentListScreen(Screen parent, Component title, List<ContentPack> packs) {
        super(s -> Panel.createPanel(s,
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
            // Open the Confirmation Popup Screen
            minecraft.setScreen(new PackActionScreen(this, pack));
        }) {
            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                if (focused) hoveredPack = pack; 
            }
        }); 
    }

    @Override
    public void onClose() {
        if (this.needsReload) {
            SkinPackLoader.loadPacks();
            if (minecraft != null) {
                minecraft.reloadResourcePacks();
            }
        }
        super.onClose();
    }

    private void deletePack(ContentPack pack) {
        File packDir = new File(minecraft.gameDirectory, "skin_packs/" + pack.id());
        if (packDir.exists()) {
            deleteDirectoryRecursively(packDir);
        }
        needsReload = true;
    }

    private void deleteDirectoryRecursively(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectoryRecursively(file);
            }
        }
        directory.delete();
    }

    @Override
    public void renderableVListInit() {
        tooltipBox.init();
        initRenderableVListHeight(36);
        
        addRenderableOnly((guiGraphics, i, j, f) -> {
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

            String name = hoveredPack.name() != null ? hoveredPack.name() : I18n.get("bedrockskins.pack.unknown");
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

    // --- Inner class for the Custom Confirmation Screen ---

    private class PackActionScreen extends ConfirmationScreen {
        private final ContentPack pack;
        private final boolean isInstalled;

        public PackActionScreen(Screen parent, ContentPack pack) {
            super(parent, 
                  ConfirmationScreen::getPanelWidth, 
                  () -> 95, // Height of the panel
                  Component.literal(pack.name()), 
                  Component.translatable(ContentManager.isPackInstalled(pack) ? "bedrockskins.menu.delete_pack" : "bedrockskins.menu.download_pack"), 
                  (b) -> {} // Overriding addButtons allows us to ignore this default OK action
            );
            this.pack = pack;
            this.isInstalled = ContentManager.isPackInstalled(pack);
        }

        @Override
        protected void addButtons() {
            // Add Download/Delete Button
            Component actionText = isInstalled ? Component.translatable("bedrockskins.button.delete") : Component.translatable("bedrockskins.button.download");
            renderableVList.addRenderable(Button.builder(actionText, b -> {
                if (isInstalled) {
                    deletePack(pack);
                    minecraft.setScreen(parent);
                } else {
                    ContentManager.downloadPack(pack, () -> {
                        needsReload = true;
                    });
                    minecraft.setScreen(parent);
                }
            }).bounds(panel.x + 15, panel.getRectangle().bottom() - 52, 200, 20).build());

            // Add Cancel Button
            renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b -> minecraft.setScreen(parent))
                .bounds(panel.x + 15, panel.getRectangle().bottom() - 30, 200, 20).build());
        }
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