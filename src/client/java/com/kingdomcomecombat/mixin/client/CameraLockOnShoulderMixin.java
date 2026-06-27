package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.lockon.LockOnCameraController;
import com.kingdomcomecombat.client.render.FirstPersonHeadPoseTracker;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class, priority = 500)
public abstract class CameraLockOnShoulderMixin {
    @Unique private static final double KCC_HEAD_CAMERA_POSITION_SMOOTHING = 0.45;
    @Unique private static final float KCC_HEAD_CAMERA_ROTATION_SMOOTHING = 0.42F;
    @Unique private static int kccSmoothedHeadCameraEntityId = -1;
    @Unique private static Vec3d kccSmoothedHeadCameraPos = null;
    @Unique private static float kccSmoothedHeadCameraYaw = 0.0F;
    @Unique private static float kccSmoothedHeadCameraPitch = 0.0F;

    @Shadow
    public abstract Vec3d getPos();

    @Shadow
    protected abstract void setPos(Vec3d pos);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    public abstract float getYaw();

    @Shadow
    public abstract float getPitch();

    @Inject(method = "update", at = @At("TAIL"))
    private void kingdomcomecombat$offsetHardLockThirdPersonCamera(
            BlockView area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float tickProgress,
        CallbackInfo ci
    ) {
        if (!thirdPerson) {
            var lockRotation = LockOnCameraController.getFirstPersonCameraRotation(
                    MinecraftClient.getInstance(),
                    tickProgress
            );
            lockRotation.ifPresent(rotation -> setRotation(rotation.yaw(), rotation.pitch()));
            var headPose = FirstPersonHeadPoseTracker.get(focusedEntity.getId());
            double configuredOffset = CombatClientConfig.firstPersonCameraForwardOffset();
            if (CombatClientConfig.firstPersonCameraHeadBindingEnabled() && headPose.isPresent()) {
                var pose = headPose.get();
                kingdomcomecombat$setSmoothedHeadBoundCamera(
                        focusedEntity,
                        pose.faceCenter().add(pose.forward().multiply(configuredOffset)),
                        pose.yaw(),
                        pose.pitch()
                );
                return;
            }
            kingdomcomecombat$resetSmoothedHeadBoundCamera();
            if (CombatClientConfig.firstPersonSimpleEyeSimulationEnabled() && headPose.isPresent()) {
                float renderTickProgress = MinecraftClient.getInstance()
                        .getRenderTickCounter().getTickProgress(true);
                Vec3d vanillaCamera = focusedEntity.getCameraPosVec(renderTickProgress);
                Vec3d expectedPivot = vanillaCamera.add(0.0, -2.0 / 16.0, 0.0);
                setPos(getPos().add(headPose.get().pivot().subtract(expectedPivot)));
            }
            double forwardOffset = 0.0;
            if (CombatAnimationClient.isKccSpecialFirstPersonActive()) {
                forwardOffset = configuredOffset;
            }
            if (lockRotation.isPresent()) {
                forwardOffset += CombatClientConfig.lockOnFirstPersonCameraForwardOffset();
            }
            if (Math.abs(forwardOffset) > 0.0001) {
                setPos(getPos().add(Vec3d.fromPolar(getPitch(), getYaw())
                        .normalize().multiply(forwardOffset)));
            }
            return;
        }
        if (!thirdPerson || inverseView) {
            return;
        }

        Vec3d offset = LockOnCameraController.getShoulderCameraOffset(MinecraftClient.getInstance(), tickProgress);
        if (offset.lengthSquared() <= 0.0001) {
            return;
        }

        setPos(getPos().add(offset));
        LockOnCameraController.getShoulderCameraRotation(MinecraftClient.getInstance(), getPos())
                .ifPresent(rotation -> setRotation(rotation.yaw(), rotation.pitch()));
    }

    @Unique
    private void kingdomcomecombat$setSmoothedHeadBoundCamera(
            Entity focusedEntity,
            Vec3d targetPos,
            float targetYaw,
            float targetPitch
    ) {
        if (kccSmoothedHeadCameraEntityId != focusedEntity.getId()
                || kccSmoothedHeadCameraPos == null
                || kccSmoothedHeadCameraPos.squaredDistanceTo(targetPos) > 0.65 * 0.65) {
            kccSmoothedHeadCameraEntityId = focusedEntity.getId();
            kccSmoothedHeadCameraPos = targetPos;
            kccSmoothedHeadCameraYaw = targetYaw;
            kccSmoothedHeadCameraPitch = targetPitch;
        } else {
            kccSmoothedHeadCameraPos = kccSmoothedHeadCameraPos.lerp(
                    targetPos,
                    KCC_HEAD_CAMERA_POSITION_SMOOTHING
            );
            kccSmoothedHeadCameraYaw += MathHelper.wrapDegrees(targetYaw - kccSmoothedHeadCameraYaw)
                    * KCC_HEAD_CAMERA_ROTATION_SMOOTHING;
            kccSmoothedHeadCameraPitch += (targetPitch - kccSmoothedHeadCameraPitch)
                    * KCC_HEAD_CAMERA_ROTATION_SMOOTHING;
        }

        setRotation(kccSmoothedHeadCameraYaw, kccSmoothedHeadCameraPitch);
        setPos(kccSmoothedHeadCameraPos);
    }

    @Unique
    private static void kingdomcomecombat$resetSmoothedHeadBoundCamera() {
        kccSmoothedHeadCameraEntityId = -1;
        kccSmoothedHeadCameraPos = null;
    }
}
