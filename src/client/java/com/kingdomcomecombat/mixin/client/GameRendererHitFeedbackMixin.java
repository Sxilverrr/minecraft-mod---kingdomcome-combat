package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.animation.GeckoLikeAnimationLibrary;
import com.kingdomcomecombat.client.feedback.CombatHitFeedbackClient;
import com.kingdomcomecombat.client.feedback.CustomHurtOverlaySuppressor;
import com.kingdomcomecombat.client.render.FirstPersonHeadPoseTracker;
import com.kingdomcomecombat.riding.KccHorseRidingData;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererHitFeedbackMixin {
    private static final float PIXEL_TO_BLOCK = 1.0F / 16.0F;

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$suppressHurtCameraTilt(
            MatrixStack matrices,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (CombatClientConfig.firstPersonCameraHeadBindingEnabled()
                && MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {
            ci.cancel();
            return;
        }
        if (!CombatClientConfig.hurtCameraMovementEnabled()) {
            ci.cancel();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && CustomHurtOverlaySuppressor.shouldSuppress(client.player.getId())) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$suppressCameraMovementWhenBound(
            MatrixStack matrices,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (CombatClientConfig.firstPersonCameraHeadBindingEnabled()
                && MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("TAIL"))
    private void kingdomcomecombat$applyHitFeedbackRoll(
            MatrixStack matrices,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (CombatClientConfig.firstPersonCameraHeadBindingEnabled()
                && MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                FirstPersonHeadPoseTracker.get(client.player.getId()).ifPresent(pose ->
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pose.roll()))
                );
            }
            return;
        }
        boolean hurtCameraEnabled = CombatClientConfig.hurtCameraMovementEnabled();
        float hitXOffset = hurtCameraEnabled ? CombatHitFeedbackClient.getHitCameraXOffset(tickDelta) : 0.0F;
        float hitYOffset = hurtCameraEnabled ? CombatHitFeedbackClient.getHitCameraYOffset(tickDelta) : 0.0F;
        float hitZOffset = hurtCameraEnabled ? CombatHitFeedbackClient.getHitCameraZOffset(tickDelta) : 0.0F;
        if (hitXOffset != 0.0F || hitYOffset != 0.0F || hitZOffset != 0.0F) {
            matrices.translate(hitXOffset, hitYOffset, hitZOffset);
        }

        float hitYaw = hurtCameraEnabled ? CombatHitFeedbackClient.getCameraYawDegrees(tickDelta) : 0.0F;
        if (hitYaw != 0.0F) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(hitYaw));
        }

        float hitPitch = hurtCameraEnabled ? CombatHitFeedbackClient.getCameraPitchDegrees(tickDelta) : 0.0F;
        if (hitPitch != 0.0F) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(hitPitch));
        }

        float yOffset = CombatHitFeedbackClient.getDodgeCameraYOffset(tickDelta);
        if (yOffset != 0.0F) {
            matrices.translate(0.0F, -yOffset, 0.0F);
        }

        float roll = (hurtCameraEnabled ? CombatHitFeedbackClient.getCameraRollDegrees(tickDelta) : 0.0F)
                + CombatHitFeedbackClient.getDodgeCameraRollDegrees(tickDelta);
        if (roll != 0.0F) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        }

        applyHorseSprintBob(matrices, tickDelta);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.getPerspective().isFirstPerson()) {
            CombatAnimationClient.getLocalPlayerCameraTransform().ifPresent(transform -> {
                transform.position().ifPresent(position ->
                        matrices.translate(
                                position.x() * PIXEL_TO_BLOCK,
                                -position.y() * PIXEL_TO_BLOCK,
                                -position.z() * PIXEL_TO_BLOCK
                        )
                );

                transform.rotation().ifPresent(rotation -> applyCameraRotation(matrices, rotation));
            });
        }
    }

    private static void applyHorseSprintBob(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null
                || !(client.player.getVehicle() instanceof AbstractHorseEntity horse)
                || !((Object) horse instanceof KccHorseRidingData horseData)) {
            return;
        }

        double speed = horseData.kingdomcomecombat$getHorseCurrentSpeed();
        if (speed <= 0.001) {
            return;
        }

        float speedProgress = (float) MathHelper.clamp(speed, 0.0, 1.0);
        float phase = (client.player.age + tickDelta) * (0.28F + speedProgress * 0.18F);
        float vertical = MathHelper.sin(phase) * 0.018F * speedProgress * 1.2F;

        matrices.translate(0.0F, vertical, 0.0F);
    }

    private static void applyCameraRotation(
            MatrixStack matrices,
            GeckoLikeAnimationLibrary.BonePose rotation
    ) {
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation.z()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation.y()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation.x()));
    }
}
