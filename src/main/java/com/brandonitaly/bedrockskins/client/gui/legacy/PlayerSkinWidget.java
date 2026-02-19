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

/**
 * Widget that displays a 3D player skin model with rotation and animation.
 */
public class PlayerSkinWidget extends AbstractWidget {
    private static final String AUTO_SELECTED_INTERNAL_NAME = "__auto_selected__";

    private static final float ROTATION_SENSITIVITY = 2.5F;
    private static final float DEFAULT_ROTATION_X = -5.0F;
    private static final float DEFAULT_ROTATION_Y = 30.0F;
    private static final float ROTATION_X_LIMIT = 50.0F;
    private static final float LEGACY_WALK_SPEED = 0.3F;
    private static final float LEGACY_WALK_DISTANCE = 1.0F;
    private static final long WALK_SYNC_EPOCH_MS = currentTimeMillis();
    private static final long WALK_STEP_MS = 10L;
    private static final int WALK_BOOTSTRAP_MOD_STEPS = 384;
    private static final int WALK_MAX_CATCHUP_STEPS = 12;
    private static final float WALK_STEP_SPEED = LEGACY_WALK_SPEED * (WALK_STEP_MS / 50.0F);
    private static final long SWING_REPEAT_MS = 260L;
    private static final long PREVIEW_TICK_MS = 50L;
    
    private PreviewPlayer dummyPlayer;
    private UUID dummyUuid = UUID.randomUUID();
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
    private boolean overrideVisible = true;
    boolean wasHidden = true;
    private long start = 0;

    // Snap state for wrapping
    private Integer snapX = null;
    private Integer snapY = null;

    private enum PreviewPose {
        STANDING,
        SNEAKING,
        PUNCHING
    }

    // Pose state
    private PreviewPose previewPose = PreviewPose.STANDING;
    private long lastSwingPoseTriggerMs = 0L;
    private long lastWalkUpdateMs = currentTimeMillis();
    private long lastPreviewTickMs = currentTimeMillis();
    
    public PlayerSkinWidget(int width, int height, EntityModelSet entityModelSet, Supplier<SkinReference> supplier) {
        this(width, height, entityModelSet, supplier, null);
    }

    public PlayerSkinWidget(int width, int height, EntityModelSet entityModelSet, Supplier<SkinReference> supplier, @Nullable Supplier<LoadedSkin> skinSupplier) {
        super(-9999, -9999, width, height, CommonComponents.EMPTY);
        originalWidth = width;
        originalHeight = height;
        this.skinRef = supplier;
        this.skin = skinSupplier != null ? skinSupplier : () -> {
            SkinReference ref = this.skinRef.get();
            if (ref == null) return null;
            SkinPackAdapter pack = SkinPackAdapter.getPack(ref.packId());
            return pack.getSkin(ref.ordinal());
        };
        
        // Initialize preview player
        Minecraft minecraft = Minecraft.getInstance();
        String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
        GameProfile profile = new GameProfile(dummyUuid, name);
        dummyPlayer = PreviewPlayer.PreviewPlayerPool.get(profile);
        initializeWalkAnimationPhase(dummyPlayer);
    }

    public boolean isInterpolating() {
        return !(targetRotationX == Float.NEGATIVE_INFINITY && targetRotationY == targetRotationX);
    }

    public void beginInterpolation(float targetRotationX, float targetRotationY, float targetPosX, float targetPosY, float targetScale) {
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
        
        // Reset snap state on new interpolation
        this.snapX = null;
        this.snapY = null;
        
        if(!this.visible || this.wasHidden) {
            this.rotationX = this.targetRotationX;
            this.rotationY = this.targetRotationY;
            this.setX((int) this.targetPosX);
            this.setY((int) this.targetPosY);
            this.scale = targetScale;
            setWidth((int) (this.originalWidth * scale));
            setHeight((int) (this.originalHeight * scale));
            this.progress = 2;
            if (this.visible) this.wasHidden = false;
        }
    }
    
    public void snapTo(int x, int y) {
        this.snapX = x;
        this.snapY = y;
    }

    public void visible() {
        this.visible = true;
    }

    public void overrideVisible(boolean overrideVisible) {
        this.overrideVisible = overrideVisible;
    }

    public void invisible() {
        this.wasHidden = true;
        this.visible = false;
        this.progress = 2;
        if (progress >= 1) {
            finishInterpolation();
        }
    }
    
    private void finishInterpolation() {
        if (this.targetRotationX != Float.NEGATIVE_INFINITY) {
            this.rotationX = this.targetRotationX;
            this.rotationY = this.targetRotationY;
        }
        this.targetRotationX = Float.NEGATIVE_INFINITY;
        this.targetRotationY = Float.NEGATIVE_INFINITY;
        
        // Apply snap if pending
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
            setWidth((int) (this.originalWidth * scale));
            setHeight((int) (this.originalHeight * scale));
        }
        this.targetScale = Float.NEGATIVE_INFINITY;
    }

    public void interpolate(float progress) {
        if (targetRotationX == Float.NEGATIVE_INFINITY && targetRotationY == targetRotationX) return;
        if (progress >= 1) {
            finishInterpolation();
            return;
        }
        
        float delta = progress;
        float nX = prevRotationX * (1 - delta) + targetRotationX * delta;
        float nY = prevRotationY * (1 - delta) + targetRotationY * delta;
        float nX2 = prevPosX * (1 - delta) + targetPosX * delta;
        float nY2 = prevPosY * (1 - delta) + targetPosY * delta;
        float nS = prevScale * (1 - delta) + targetScale * delta;
        
        this.rotationX = nX;
        this.rotationY = nY;
        this.setX((int) nX2);
        this.setY((int) nY2);
        this.scale = nS;
        setWidth((int) (this.originalWidth * scale));
        setHeight((int) (this.originalHeight * scale));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Ensure visibility logic
        if (!visible) return;

        interpolate(progress);
        progress = (currentTimeMillis() - start) / 200f;
        
        if (dummyPlayer != null) {
            // Update the preview skin before rendering
            LoadedSkin loadedSkin = this.skin.get();
            if (loadedSkin != null && isAutoSelectedSkin(loadedSkin)) {
                SkinManager.resetPreviewSkin(dummyUuid.toString());
                if (Minecraft.getInstance().player != null) {
                    dummyPlayer.clearForcedProfileSkin();
                    dummyPlayer.setForcedBody(Minecraft.getInstance().player.getSkin().body());
                    dummyPlayer.setForcedCapeTexture(Minecraft.getInstance().player.getSkin().cape());
                    dummyPlayer.setUseLocalPlayerModel(true);
                } else {
                    dummyPlayer.clearForcedBody();
                    dummyPlayer.clearForcedCape();
                    var profile = Minecraft.getInstance().getGameProfile();
                    if (profile != null) {
                        dummyPlayer.setForcedProfileSkin(
                            Minecraft.getInstance().getSkinManager().createLookup(profile, false).get()
                        );
                    } else {
                        dummyPlayer.clearForcedProfileSkin();
                    }
                    dummyPlayer.setUseLocalPlayerModel(false);
                }
            } else if (loadedSkin != null) {
                dummyPlayer.clearForcedProfileSkin();
                dummyPlayer.clearForcedBody();
                dummyPlayer.clearForcedCape();
                dummyPlayer.setUseLocalPlayerModel(false);
                var id = loadedSkin.getSkinId();
                if (id != null) {
                    SkinManager.setPreviewSkin(dummyUuid.toString(), id.getPack(), id.getName());
                    SkinPackLoader.registerTextureFor(id);
                }
                // Set cape if provided
                dummyPlayer.setForcedCape(loadedSkin.capeIdentifier);
            } else {
                dummyPlayer.clearForcedProfileSkin();
                SkinManager.resetPreviewSkin(dummyUuid.toString());
                dummyPlayer.clearForcedBody();
                dummyPlayer.setUseLocalPlayerModel(false);
                dummyPlayer.clearForcedCape();
            }
            
            // Advance entity simulation so swing animation progresses in preview
            advancePreviewSimulation(dummyPlayer);

            // Keep age-based idle/walk arm bob active every frame
            dummyPlayer.tickCount = (int)(currentTimeMillis() / 50L);

            // Apply pose
            boolean crouching = previewPose == PreviewPose.SNEAKING;
            dummyPlayer.setShiftKeyDown(crouching);
            dummyPlayer.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
            applyLegacyWalkAnimation(dummyPlayer);
            applySwingPose(dummyPlayer);
            
            // Use the same rendering approach as SkinSelectionScreen/SkinPreviewPanel
            float yawOffset = this.rotationY;
            int left = this.getX();
            int top = this.getY();
            int right = this.getX() + this.getWidth();
            int bottom = this.getY() + this.getHeight();
            int sizeCap = 110; // Increased for larger preview models
            
            com.brandonitaly.bedrockskins.client.gui.GuiUtils.renderEntityInRect(
                guiGraphics, dummyPlayer, yawOffset, left, top, right, bottom, sizeCap
            );
        }
    }

    private void applyLegacyWalkAnimation(PreviewPlayer player) {
        if (player == null) return;

        long now = currentTimeMillis();
        long elapsed = now - lastWalkUpdateMs;
        if (elapsed < WALK_STEP_MS) return;

        int steps = (int) (elapsed / WALK_STEP_MS);
        if (steps > WALK_MAX_CATCHUP_STEPS) {
            steps = WALK_MAX_CATCHUP_STEPS;
        }

        for (int i = 0; i < steps; i++) {
            player.walkAnimation.update(WALK_STEP_SPEED, 1.0F, LEGACY_WALK_DISTANCE);
        }

        lastWalkUpdateMs += (long) steps * WALK_STEP_MS;
    }

    private void initializeWalkAnimationPhase(PreviewPlayer player) {
        if (player == null) return;

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

        int ticks = (int) (elapsed / PREVIEW_TICK_MS);
        if (ticks > 5) ticks = 5;
        for (int i = 0; i < ticks; i++) {
            player.tick();
        }
        lastPreviewTickMs += ticks * PREVIEW_TICK_MS;
    }

    private void applySwingPose(PreviewPlayer player) {
        if (player == null || previewPose != PreviewPose.PUNCHING) return;

        long now = currentTimeMillis();
        if (lastSwingPoseTriggerMs == 0L || (now - lastSwingPoseTriggerMs) >= SWING_REPEAT_MS) {
            player.swing(InteractionHand.MAIN_HAND);
            lastSwingPoseTriggerMs = now;
        }
    }

    private static long currentTimeMillis() {
        //? if >=1.21.11 {
        return net.minecraft.util.Util.getMillis();
        //?} else {
        /*return net.minecraft.Util.getMillis();*/
        //?}
    }

    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (isInterpolating()) return;
        if (!interactable) return;
        this.rotationX = Mth.clamp(this.rotationX - (float)deltaY * 2.5F, -ROTATION_X_LIMIT, ROTATION_X_LIMIT);
        this.rotationY += (float)deltaX * ROTATION_SENSITIVITY;
        while (this.rotationY < 0) this.rotationY += 360;
        this.rotationY = (this.rotationY + 180) % 360 - 180;
    }

    public void playDownSound(SoundManager soundManager) {
    }

    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public boolean isActive() {
        return false;
    }

    public void togglePose() {
        cyclePose();
    }

    public void toggleCrouchPose() {
        previewPose = previewPose == PreviewPose.SNEAKING ? PreviewPose.STANDING : PreviewPose.SNEAKING;
    }

    public void toggleSwingPose() {
        previewPose = previewPose == PreviewPose.PUNCHING ? PreviewPose.STANDING : PreviewPose.PUNCHING;
        if (previewPose == PreviewPose.PUNCHING) {
            lastSwingPoseTriggerMs = 0L;
        }
    }

    public void cyclePose() {
        previewPose = switch (previewPose) {
            case STANDING -> PreviewPose.SNEAKING;
            case SNEAKING -> PreviewPose.PUNCHING;
            case PUNCHING -> PreviewPose.STANDING;
        };
        if (previewPose == PreviewPose.PUNCHING) {
            lastSwingPoseTriggerMs = 0L;
        }
    }

    public void resetPose() {
        previewPose = PreviewPose.STANDING;
        lastSwingPoseTriggerMs = 0L;
    }

    public void cleanup() {
        SkinManager.resetPreviewSkin(dummyUuid.toString());
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