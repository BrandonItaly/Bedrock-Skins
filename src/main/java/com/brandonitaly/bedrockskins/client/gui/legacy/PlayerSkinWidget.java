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
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Widget that displays a 3D player skin model with rotation and animation.
 */
public class PlayerSkinWidget extends AbstractWidget {
    private static final float ROTATION_SENSITIVITY = 2.5F;
    private static final float DEFAULT_ROTATION_X = -5.0F;
    private static final float DEFAULT_ROTATION_Y = 30.0F;
    private static final float ROTATION_X_LIMIT = 50.0F;
    
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

    // Pose state
    private boolean crouchPose = false;
    
    public PlayerSkinWidget(int width, int height, EntityModelSet entityModelSet, Supplier<SkinReference> supplier) {
        super(-9999, -9999, width, height, CommonComponents.EMPTY);
        originalWidth = width;
        originalHeight = height;
        this.skinRef = supplier;
        this.skin = () -> {
            SkinReference ref = this.skinRef.get();
            if (ref == null) return null;
            SkinPackAdapter pack = SkinPackAdapter.getPack(ref.packId());
            return pack.getSkin(ref.ordinal());
        };
        
        // Initialize preview player
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
            GameProfile profile = new GameProfile(dummyUuid, name);
            dummyPlayer = PreviewPlayer.PreviewPlayerPool.get(minecraft.level, profile);
        }
    }

    public boolean isInterpolating() {
        return !(targetRotationX == Float.NEGATIVE_INFINITY && targetRotationY == targetRotationX);
    }

    public void beginInterpolation(float targetRotationX, float targetRotationY, float targetPosX, float targetPosY, float targetScale) {
        this.progress = 0;
        this.start = System.currentTimeMillis();
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
        progress = (System.currentTimeMillis() - start) / 200f;
        
        if (dummyPlayer != null) {
            // Update the preview skin before rendering
            LoadedSkin loadedSkin = this.skin.get();
            if (loadedSkin != null) {
                var id = loadedSkin.getSkinId();
                if (id != null) {
                    SkinManager.setPreviewSkin(dummyUuid.toString(), id.getPack(), id.getName());
                    SkinPackLoader.registerTextureFor(id);
                }
                // Set cape if provided
                dummyPlayer.setForcedCape(loadedSkin.capeIdentifier);
            }
            
            // Update tick count for animations
            //? if >=1.21.11 {
            dummyPlayer.tickCount = (int)(net.minecraft.util.Util.getMillis() / 50L);
            //?} else {
            /*dummyPlayer.tickCount = (int)(net.minecraft.Util.getMillis() / 50L);*/
            //?}

            // Apply pose
            dummyPlayer.setShiftKeyDown(crouchPose);
            dummyPlayer.setPose(crouchPose ? Pose.CROUCHING : Pose.STANDING);
            
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
        crouchPose = !crouchPose;
    }

    public void resetPose() {
        crouchPose = false;
    }

    public void cleanup() {
        SkinManager.resetPreviewSkin(dummyUuid.toString());
        PreviewPlayer.PreviewPlayerPool.remove(dummyUuid);
    }
}