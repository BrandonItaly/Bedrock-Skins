package com.brandonitaly.bedrockskins.client.gui.legacy;

import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.client.gui.GuiSkinUtils;
import com.brandonitaly.bedrockskins.client.gui.PreviewPlayer;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

public class PlayerSkinWidget extends AbstractWidget {
    private static final float VISUAL_SCALE_MULTIPLIER = 1.1F; 
    private static final float ROTATION_SENSITIVITY = 2.5F;
    private static final float ROTATION_X_LIMIT = 50.0F;
    private static final float LEGACY_WALK_SPEED = 0.3F;
    private static final float LEGACY_WALK_DISTANCE = 1.0F;
    private static final long WALK_STEP_MS = 16L;
    private static final float WALK_STEP_SPEED = LEGACY_WALK_SPEED * (WALK_STEP_MS / 50.0F);
    private static final long PREVIEW_TICK_MS = 50L;
    
    // Global sync variables
    private static long walkSyncEpochMs = Util.getMillis();
    private static long lastRenderTickMs = Util.getMillis();
    
    private final PreviewPlayer dummyPlayer;
    private final UUID dummyUuid = UUID.randomUUID();
    final Supplier<SkinReference> skinRef;
    final Supplier<LoadedSkin> skin;
    private final int originalWidth;
    private final int originalHeight;
    public boolean interactable = true;
    public boolean visible = true;
    
    // Animation & State
    private float rotationX = 0.0F, rotationY = 0.0F;
    private float targetRotationX = Float.NEGATIVE_INFINITY, targetRotationY = Float.NEGATIVE_INFINITY;
    private float targetPosX = Float.NEGATIVE_INFINITY, targetPosY = Float.NEGATIVE_INFINITY;
    private float prevPosX = 0, prevPosY = 0, prevRotationX = 0, prevRotationY = 0;
    private float scale = 1, targetScale = Float.NEGATIVE_INFINITY, prevScale = 0;
    float progress = 0; 
    boolean wasHidden = true;
    private long start = 0;

    // Pose State
    private Integer snapX = null, snapY = null;
    private PreviewPose pendingPose = null;
    private PreviewPose previewPose = PreviewPose.STANDING;
    private long lastPreviewTickMs = Util.getMillis();
    private boolean skinSetupComplete = false;

    public enum PreviewPose { STANDING, SNEAKING, PUNCHING }

    public PlayerSkinWidget(int width, int height, EntityModelSet entityModelSet, Supplier<SkinReference> supplier, @Nullable Supplier<LoadedSkin> skinSupplier) {
        super(-9999, -9999, width, height, CommonComponents.EMPTY);
        this.originalWidth = width;
        this.originalHeight = height;
        this.skinRef = supplier;
        
        this.skin = skinSupplier != null ? skinSupplier : () -> {
            SkinReference ref = this.skinRef.get();
            return ref != null ? SkinPackAdapter.getPack(ref.packId()).getSkin(ref.ordinal()) : null;
        };
        
        Minecraft mc = Minecraft.getInstance();
        String name = mc.player != null ? mc.player.getName().getString() : "Preview";
        this.dummyPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(dummyUuid, name));
    }

    public boolean isInterpolating() { return targetRotationX != Float.NEGATIVE_INFINITY; }

    public void beginInterpolation(float targetRotX, float targetRotY, float targetPosX, float targetPosY, float targetScale) {
        if (this.snapX != null && this.snapY != null) {
            this.setX(this.snapX);
            this.setY(this.snapY);
            this.snapX = this.snapY = null;
        }

        this.progress = 0;
        this.start = Util.getMillis();
        
        this.prevRotationX = rotationX; this.prevRotationY = rotationY;
        this.prevPosX = getX(); this.prevPosY = getY(); this.prevScale = scale;
        
        this.targetRotationX = targetRotX; this.targetRotationY = targetRotY;
        this.targetPosX = targetPosX; this.targetPosY = targetPosY; this.targetScale = targetScale;
        
        if (!this.visible || this.wasHidden) {
            this.rotationX = targetRotX; this.rotationY = targetRotY;
            this.setX((int) targetPosX); this.setY((int) targetPosY);
            this.scale = targetScale;
            updateBounds();
            
            this.prevPosX = targetPosX; this.prevPosY = targetPosY;
            this.prevRotationX = targetRotX; this.prevRotationY = targetRotY; this.prevScale = targetScale;
            
            this.start = Util.getMillis() - 200L;
            this.progress = 2;
            if (this.visible) this.wasHidden = false;
        }
    }
    
    public void snapTo(int x, int y) { this.snapX = x; this.snapY = y; }
    public void visible() { this.visible = true; }
    
    public void invisible() {
        this.wasHidden = true; this.visible = false; this.progress = 2;
        finishInterpolation();
    }
    
    private void finishInterpolation() {
        if (this.targetRotationX != Float.NEGATIVE_INFINITY) {
            this.rotationX = this.targetRotationX; this.rotationY = this.targetRotationY;
        }
        
        if (snapX != null && snapY != null) {
            this.setX(snapX); this.setY(snapY);
            snapX = snapY = null;
        } else if (this.targetPosX != Float.NEGATIVE_INFINITY) {
            this.setX((int) this.targetPosX); this.setY((int) targetPosY);
        }
        
        if (this.targetScale != Float.NEGATIVE_INFINITY) {
            this.scale = targetScale; updateBounds();
        }

        this.targetRotationX = this.targetRotationY = Float.NEGATIVE_INFINITY;
        this.targetPosX = this.targetPosY = this.targetScale = Float.NEGATIVE_INFINITY;

        if (this.pendingPose != null) {
            setPreviewPose(this.pendingPose);
            this.pendingPose = null;
        }
    }

    public void interpolate(float progress) {
        if (!isInterpolating()) return;
        if (progress >= 1) {
            finishInterpolation();
            return;
        }
        
        this.rotationX = Mth.lerp(progress, prevRotationX, targetRotationX);
        this.rotationY = Mth.lerp(progress, prevRotationY, targetRotationY);
        this.setX((int) Mth.lerp(progress, prevPosX, targetPosX));
        this.setY((int) Mth.lerp(progress, prevPosY, targetPosY));
        this.scale = Mth.lerp(progress, prevScale, targetScale);
        updateBounds();
    }
    
    private void updateBounds() {
        setWidth((int) (this.originalWidth * scale));
        setHeight((int) (this.originalHeight * scale));
    }

    private void setupDummyPlayerSkin() {
        LoadedSkin loadedSkin = this.skin.get();
        dummyPlayer.clearForcedProfileSkin();
        dummyPlayer.clearForcedBody();
        dummyPlayer.clearForcedCape();

        if (GuiSkinUtils.isAutoSelectedSkin(loadedSkin)) {
            GuiSkinUtils.applyAutoSelectedPreview(Minecraft.getInstance(), dummyPlayer, dummyUuid);
        } else {
            GuiSkinUtils.applyLoadedSkinPreview(dummyPlayer, dummyUuid, loadedSkin);
        }
    }

    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        long elapsed = Util.getMillis() - start;
        if (!BedrockSkinsConfig.isSmoothInterpolationEnabled()) elapsed = (elapsed / 33L) * 33L;

        progress = elapsed / 200f;
        interpolate(progress);
        
        if (dummyPlayer != null) {
            if (!skinSetupComplete) {
                setupDummyPlayerSkin();
                skinSetupComplete = true;
            }

            if (GuiSkinUtils.isAutoSelectedSkin(this.skin.get())) {
                GuiSkinUtils.refreshAutoSelectedProfileSkin(Minecraft.getInstance(), dummyPlayer);
            }
            
            advancePreviewSimulation(dummyPlayer);

            boolean crouching = previewPose == PreviewPose.SNEAKING;
            dummyPlayer.setShiftKeyDown(crouching);
            dummyPlayer.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
            applyLegacyWalkAnimation(dummyPlayer);
            applySwingPose(dummyPlayer);
            
            // Scaled Render Bounds
            int w = (int)(this.getWidth() * VISUAL_SCALE_MULTIPLIER);
            int h = (int)(this.getHeight() * VISUAL_SCALE_MULTIPLIER);
            int rx = this.getX() - (w - this.getWidth()) / 2;
            int ry = this.getY() - (h - this.getHeight());
            
            com.brandonitaly.bedrockskins.client.gui.GuiUtils.renderEntityInRect(
                guiGraphics, dummyPlayer, this.rotationY, crouching ? 5.0F : 0.0F,
                rx, ry, rx + w, ry + h, 110
            );
        }
    }

    private void applyLegacyWalkAnimation(PreviewPlayer player) {
        long now = Util.getMillis();
        if (now - lastRenderTickMs > 2000L || now < lastRenderTickMs) walkSyncEpochMs = now;
        lastRenderTickMs = now;

        int totalSteps = Math.max(0, (int) ((now - walkSyncEpochMs) / WALK_STEP_MS));
        if (totalSteps > 30000) {
            walkSyncEpochMs = now; totalSteps = 0;
        }

        player.walkAnimation.stop();
        for (int i = 0; i < totalSteps; i++) {
            player.walkAnimation.update(WALK_STEP_SPEED, 1.0F, LEGACY_WALK_DISTANCE);
        }
    }

    private void advancePreviewSimulation(PreviewPlayer player) {
        int ticks = Math.min((int) ((Util.getMillis() - lastPreviewTickMs) / PREVIEW_TICK_MS), 5);
        if (ticks > 0) {
            for (int i = 0; i < ticks; i++) player.tick();
            lastPreviewTickMs += ticks * PREVIEW_TICK_MS;
        }
    }

    private void applySwingPose(PreviewPlayer player) {
        if (previewPose != PreviewPose.PUNCHING) return;
        
        float swingProgress = ((Util.getMillis() - walkSyncEpochMs) % 300L) / 300.0F;
        player.swinging = true;
        player.swingTime = (int)(swingProgress * 6.0F);
        player.swingingArm = InteractionHand.MAIN_HAND;
        player.attackAnim = swingProgress;
    }

    public void onDrag(double deltaX) {
        if (isInterpolating() || !interactable) return;
        this.rotationX = Mth.clamp(this.rotationX, -ROTATION_X_LIMIT, ROTATION_X_LIMIT);
        
        this.rotationY = Mth.wrapDegrees(this.rotationY + (float)deltaX * ROTATION_SENSITIVITY);
    }

    public void playDownSound(SoundManager soundManager) {}
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
    public boolean isActive() { return false; }

    public void cyclePose(boolean forward) {
        previewPose = switch (previewPose) {
            case STANDING -> forward ? PreviewPose.SNEAKING : PreviewPose.PUNCHING;
            case SNEAKING -> forward ? PreviewPose.PUNCHING : PreviewPose.STANDING;
            case PUNCHING -> forward ? PreviewPose.STANDING : PreviewPose.SNEAKING;
        };
    }

    public void resetPose() { previewPose = PreviewPose.STANDING; pendingPose = null; }
    public PreviewPose getPreviewPose() { return this.pendingPose != null ? this.pendingPose : this.previewPose; }
    public void setPreviewPose(PreviewPose pose) { this.previewPose = pose != null ? pose : PreviewPose.STANDING; }
    public void setPendingPose(PreviewPose pose) { this.pendingPose = pose; }
    public float getRotationX() { return this.rotationX; }
    public float getRotationY() { return this.rotationY; }
    public void cleanup() { GuiSkinUtils.cleanupPreview(dummyUuid); }
    public @Nullable LoadedSkin getCurrentSkin() { return this.skin.get(); }
}