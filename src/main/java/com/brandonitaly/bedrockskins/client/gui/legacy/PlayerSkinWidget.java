package com.brandonitaly.bedrockskins.client.gui.legacy;

import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.gui.PreviewPlayer;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

public class PlayerSkinWidget extends AbstractWidget {
    private static final String AUTO_SELECTED_INTERNAL_NAME = "__auto_selected__";

    private static final float ROTATION_SENSITIVITY = 2.5F;
    private static final float ROTATION_X_LIMIT = 50.0F;
    private static final float LEGACY_WALK_SPEED = 0.3F;
    private static final float LEGACY_WALK_DISTANCE = 1.0F;
    private static final long WALK_SYNC_EPOCH_MS = currentTimeMillis();
    private static final long WALK_STEP_MS = 16L;
    private static final int WALK_BOOTSTRAP_MOD_STEPS = 384;
    private static final float WALK_STEP_SPEED = LEGACY_WALK_SPEED * (WALK_STEP_MS / 50.0F);
    private static final long PREVIEW_TICK_MS = 50L;
    
    private final PreviewPlayer dummyPlayer;
    private final UUID dummyUuid = UUID.randomUUID();
    final Supplier<SkinReference> skinRef;
    final Supplier<LoadedSkin> skin;
    private final int originalWidth;
    private final int originalHeight;
    private float rotationX = 0.0F;
    private float rotationY = 0.0F;
    public boolean interactable = true;
    public boolean visible = true;
    
    // Animation state
    private float targetRotationX = Float.NEGATIVE_INFINITY;
    private float targetRotationY = Float.NEGATIVE_INFINITY;
    private float targetPosX = Float.NEGATIVE_INFINITY;
    private float targetPosY = Float.NEGATIVE_INFINITY;
    private float prevPosX = 0;
    private float prevPosY = 0;
    private float prevRotationX = 0;
    private float prevRotationY = 0;
    float progress = 0;
    private float scale = 1;
    private float targetScale = Float.NEGATIVE_INFINITY;
    private float prevScale = 0;
    boolean wasHidden = true;
    private long start = 0;

    // Snap & Pose state
    private Integer snapX = null;
    private Integer snapY = null;
    private PreviewPose pendingPose = null;
    private PreviewPose previewPose = PreviewPose.STANDING;
    private long lastWalkUpdateMs = currentTimeMillis();
    private long lastPreviewTickMs = currentTimeMillis();
    
    private boolean skinSetupComplete = false;

    public enum PreviewPose {
        STANDING,
        SNEAKING,
        PUNCHING
    }

    public PlayerSkinWidget(int width, int height, EntityModelSet entityModelSet, Supplier<SkinReference> supplier) {
        this(width, height, entityModelSet, supplier, null);
    }

    public PlayerSkinWidget(int width, int height, EntityModelSet entityModelSet, Supplier<SkinReference> supplier, @Nullable Supplier<LoadedSkin> skinSupplier) {
        super(-9999, -9999, width, height, CommonComponents.EMPTY);
        this.originalWidth = width;
        this.originalHeight = height;
        this.skinRef = supplier;
        this.skin = skinSupplier != null ? skinSupplier : () -> {
            SkinReference ref = this.skinRef.get();
            if (ref == null) return null;
            return SkinPackAdapter.getPack(ref.packId()).getSkin(ref.ordinal());
        };
        
        Minecraft minecraft = Minecraft.getInstance();
        String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
        this.dummyPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(dummyUuid, name));
        initializeWalkAnimationPhase(this.dummyPlayer);
    }

    public boolean isInterpolating() {
        return targetRotationX != Float.NEGATIVE_INFINITY;
    }

    public void beginInterpolation(float targetRotationX, float targetRotationY, float targetPosX, float targetPosY, float targetScale) {
        if (this.snapX != null && this.snapY != null) {
            this.setX(this.snapX);
            this.setY(this.snapY);
        }

        this.progress = 0;
        this.start = currentTimeMillis();
        this.prevRotationX = rotationX;
        this.prevRotationY = rotationY;
        this.targetRotationX = targetRotationX;
        this.targetRotationY = targetRotationY;        
        this.prevPosX = getX();
        this.prevPosY = getY();
        this.targetPosX = targetPosX;
        this.targetPosY = targetPosY;
        this.prevScale = scale;
        this.targetScale = targetScale;
        
        this.snapX = null;
        this.snapY = null;
        
        if (!this.visible || this.wasHidden) {
            this.rotationX = targetRotationX;
            this.rotationY = targetRotationY;
            this.setX((int) targetPosX);
            this.setY((int) targetPosY);
            this.scale = targetScale;
            updateBounds();
            
            // Overwrite the previous coordinates
            this.prevPosX = targetPosX;
            this.prevPosY = targetPosY;
            this.prevRotationX = targetRotationX;
            this.prevRotationY = targetRotationY;
            this.prevScale = targetScale;
            
            this.start = currentTimeMillis() - 200L;
            this.progress = 2;
            if (this.visible) this.wasHidden = false;
        }
    }
    
    public void snapTo(int x, int y) {
        this.snapX = x;
        this.snapY = y;
    }

    public void visible() { this.visible = true; }

    public void invisible() {
        this.wasHidden = true;
        this.visible = false;
        this.progress = 2;
        if (progress >= 1) finishInterpolation();
    }
    
    private void finishInterpolation() {
        if (this.targetRotationX != Float.NEGATIVE_INFINITY) {
            this.rotationX = this.targetRotationX;
            this.rotationY = this.targetRotationY;
        }
        this.targetRotationX = Float.NEGATIVE_INFINITY;
        this.targetRotationY = Float.NEGATIVE_INFINITY;
        
        if (snapX != null && snapY != null) {
            this.setX(snapX);
            this.setY(snapY);
            snapX = null;
            snapY = null;
        } else if (this.targetPosX != Float.NEGATIVE_INFINITY) {
            this.setX((int) this.targetPosX);
            this.setY((int) targetPosY);
        }
        
        this.targetPosX = Float.NEGATIVE_INFINITY;
        this.targetPosY = Float.NEGATIVE_INFINITY;
        
        if (this.targetScale != Float.NEGATIVE_INFINITY) {
            this.scale = targetScale;
            updateBounds();
        }
        this.targetScale = Float.NEGATIVE_INFINITY;

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
        Minecraft minecraft = Minecraft.getInstance();

        dummyPlayer.clearForcedProfileSkin();
        dummyPlayer.clearForcedBody();
        dummyPlayer.clearForcedCape();

        if (loadedSkin != null && isAutoSelectedSkin(loadedSkin)) {
            SkinManager.resetPreviewSkin(dummyUuid);
            var profile = minecraft.getGameProfile();
            if (profile != null) {
                dummyPlayer.setForcedProfileSkin(minecraft.getSkinManager().createLookup(profile, false).get());
            }
            dummyPlayer.setUseLocalPlayerModel(false);
        } else if (loadedSkin != null) {
            dummyPlayer.setUseLocalPlayerModel(false);
            var id = loadedSkin.getSkinId();
            if (id != null) {
                SkinManager.setPreviewSkin(dummyUuid, id.pack(), id.name());
                SkinPackLoader.registerTextureFor(id);
            }
            dummyPlayer.setForcedCape(loadedSkin.capeIdentifier);
        } else {
            SkinManager.resetPreviewSkin(dummyUuid);
            dummyPlayer.setUseLocalPlayerModel(false);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        progress = (currentTimeMillis() - start) / 200f;
        interpolate(progress);
        
        if (dummyPlayer != null) {
            if (!skinSetupComplete) {
                setupDummyPlayerSkin();
                skinSetupComplete = true;
            }

            LoadedSkin loadedSkin = this.skin.get();
            Minecraft mc = Minecraft.getInstance();
            if (loadedSkin != null && isAutoSelectedSkin(loadedSkin) && mc.getGameProfile() != null) {
                dummyPlayer.setForcedProfileSkin(mc.getSkinManager().createLookup(mc.getGameProfile(), false).get());
            }
            
            advancePreviewSimulation(dummyPlayer);

            boolean crouching = previewPose == PreviewPose.SNEAKING;
            dummyPlayer.setShiftKeyDown(crouching);
            dummyPlayer.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
            applyLegacyWalkAnimation(dummyPlayer);
            applySwingPose(dummyPlayer);
            
            float headYawOffset = crouching ? 5.0F : 0.0F;
            com.brandonitaly.bedrockskins.client.gui.GuiUtils.renderEntityInRect(
                guiGraphics, dummyPlayer, this.rotationY, headYawOffset,
                this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 110
            );
        }
    }

    private void applyLegacyWalkAnimation(PreviewPlayer player) {
        long now = currentTimeMillis();
        int totalSteps = (int) ((now - WALK_SYNC_EPOCH_MS) / WALK_STEP_MS);

        player.walkAnimation.stop();
        for (int i = 0; i < totalSteps; i++) {
            player.walkAnimation.update(WALK_STEP_SPEED, 1.0F, LEGACY_WALK_DISTANCE);
        }
        lastWalkUpdateMs = now;
    }

    private void initializeWalkAnimationPhase(PreviewPlayer player) {
        long now = currentTimeMillis();
        int bootstrapSteps = (int) (((now - WALK_SYNC_EPOCH_MS) / WALK_STEP_MS) % WALK_BOOTSTRAP_MOD_STEPS);
        if (bootstrapSteps < 0) bootstrapSteps += WALK_BOOTSTRAP_MOD_STEPS;

        player.walkAnimation.stop();
        for (int i = 0; i < bootstrapSteps; i++) {
            player.walkAnimation.update(WALK_STEP_SPEED, 1.0F, LEGACY_WALK_DISTANCE);
        }
        lastWalkUpdateMs = now;
    }

    private void advancePreviewSimulation(PreviewPlayer player) {
        long now = currentTimeMillis();
        long elapsed = now - lastPreviewTickMs;
        if (elapsed < PREVIEW_TICK_MS) return;

        int ticks = Math.min((int) (elapsed / PREVIEW_TICK_MS), 5);
        for (int i = 0; i < ticks; i++) {
            player.tick();
        }
        lastPreviewTickMs += ticks * PREVIEW_TICK_MS;
    }

    private void applySwingPose(PreviewPlayer player) {
        if (previewPose != PreviewPose.PUNCHING) return;

        float swingDuration = 6.0F;
        float swingTimeMs = swingDuration * 50.0F;
        float timeSinceStart = (currentTimeMillis() - WALK_SYNC_EPOCH_MS) % (int)swingTimeMs;
        float swingProgress = timeSinceStart / swingTimeMs;

        player.swinging = true;
        player.swingTime = (int)(swingProgress * swingDuration);
        player.swingingArm = InteractionHand.MAIN_HAND;
        player.attackAnim = swingProgress;
    }

    private static long currentTimeMillis() {
        //? if >=1.21.11 {
        return net.minecraft.util.Util.getMillis();
        //?} else {
        /*return net.minecraft.Util.getMillis();*/
        //?}
    }

    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (isInterpolating() || !interactable) return;
        this.rotationX = Mth.clamp(this.rotationX - (float)deltaY * 2.5F, -ROTATION_X_LIMIT, ROTATION_X_LIMIT);
        this.rotationY += (float)deltaX * ROTATION_SENSITIVITY;
        while (this.rotationY < 0) this.rotationY += 360;
        this.rotationY = (this.rotationY + 180) % 360 - 180;
    }

    public void playDownSound(SoundManager soundManager) {}

    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    public boolean isActive() { return false; }

    public void toggleCrouchPose() {
        previewPose = previewPose == PreviewPose.SNEAKING ? PreviewPose.STANDING : PreviewPose.SNEAKING;
    }

    public void toggleSwingPose() {
        previewPose = previewPose == PreviewPose.PUNCHING ? PreviewPose.STANDING : PreviewPose.PUNCHING;
    }

    public void cyclePose(boolean forward) {
        previewPose = switch (previewPose) {
            case STANDING -> forward ? PreviewPose.SNEAKING : PreviewPose.PUNCHING;
            case SNEAKING -> forward ? PreviewPose.PUNCHING : PreviewPose.STANDING;
            case PUNCHING -> forward ? PreviewPose.STANDING : PreviewPose.SNEAKING;
        };
    }

    public void resetPose() {
        previewPose = PreviewPose.STANDING;
        pendingPose = null;
    }

    public PreviewPose getPreviewPose() { 
        return this.pendingPose != null ? this.pendingPose : this.previewPose; 
    }

    public void setPreviewPose(PreviewPose pose) {
        this.previewPose = pose != null ? pose : PreviewPose.STANDING;
    }

    public void setPendingPose(PreviewPose pose) { this.pendingPose = pose; }

    public float getRotationX() { return this.rotationX; }
    public float getRotationY() { return this.rotationY; }

    public void setRotation(float x, float y) {
        this.rotationX = x;
        this.rotationY = y;
        this.targetRotationX = Float.NEGATIVE_INFINITY;
        this.targetRotationY = Float.NEGATIVE_INFINITY;
    }

    public void cleanup() {
        SkinManager.resetPreviewSkin(dummyUuid);
        PreviewPlayer.PreviewPlayerPool.remove(dummyUuid);
    }

    public @Nullable LoadedSkin getCurrentSkin() {
        return this.skin.get();
    }

    private boolean isAutoSelectedSkin(LoadedSkin skin) {
        return skin != null
            && "Standard".equals(skin.getSerializeName())
            && AUTO_SELECTED_INTERNAL_NAME.equals(skin.getSkinDisplayName());
    }
}