package com.kingdomcomecombat.client.lockon;

import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.animation.GeckoLikeAnimationLibrary;
import com.kingdomcomecombat.client.feedback.CombatHitFeedbackClient;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class LockOnCameraController {
    private static final int MAX_BLOCKED_TICKS = 25;
    private static final int DEATH_UNLOCK_DELAY_TICKS = 20;
    private static final double DEATH_RELOCK_MAX_ANGLE_DEGREES = 80.0;
    private static final double ANIMATION_PIXEL_TO_BLOCK = 1.0 / 16.0;
    private static final double MIN_CAMERA_TARGET_FORWARD_GAP = 0.75;

    // 目标点高度：胸口上方 / 脖子附近
    private static final double TARGET_HEIGHT_FACTOR = 0.70;
    private static final double FIRST_PERSON_YAW_FOLLOW_PER_SECOND = 22.0;
    private static final double FIRST_PERSON_PITCH_FOLLOW_PER_SECOND = 10.0;

    // 每秒跟随速度
    private static final float MIN_PITCH = -75.0F;
    private static final float MAX_PITCH = 75.0F;

    private static long lastFrameNanos = -1L;
    private static long lastFirstPersonFrameNanos = -1L;
    private static float currentFirstPersonYaw = 0.0F;
    private static float currentFirstPersonPitch = 0.0F;
    private static long lastShoulderFrameNanos = -1L;
    private static long lastShoulderRotationFrameNanos = -1L;
    private static long shoulderReleaseEndNanos = -1L;
    private static Vec3d currentShoulderCameraOffset = Vec3d.ZERO;
    private static Vec3d currentShoulderBaseOffset = Vec3d.ZERO;
    private static Vec3d currentAnimationCameraOffset = Vec3d.ZERO;
    private static float currentShoulderCameraYaw = 0.0F;
    private static float currentShoulderCameraPitch = 0.0F;

    public static void tickLogic(MinecraftClient client) {
        if (!LockOnState.locked) {
            return;
        }

        if (client.player == null || client.world == null) {
            LockOnState.clear();
            return;
        }

        if (LockOnState.isSoftLocked()) {
            LockOnState.blockedTicks = 0;
            return;
        }

        if (LockOnState.delayedClearPending()) {
            LockOnState.tickDelayedClear();
            return;
        }

        LivingEntity target = getLockedTarget(client);

        if (target != null && !LockOnTargetSelector.isLockableTarget(client, target)) {
            LockOnState.clear();
            return;
        }

        if (target == null || !target.isAlive()) {
            if (tryLockNextTarget(client)) {
                return;
            }

            LockOnState.beginDelayedClear(DEATH_UNLOCK_DELAY_TICKS);
            return;
        }

        double distance = client.player.distanceTo(target);

        if (distance > CombatClientConfig.lockOnReleaseDistance()) {
            LockOnState.clear();
            return;
        }

        if (!LockOnTargetSelector.canSeeLockPoint(client, target)) {
            LockOnState.blockedTicks++;

            if (LockOnState.blockedTicks >= MAX_BLOCKED_TICKS) {
                LockOnState.clear();
            }

            return;
        }

        LockOnState.blockedTicks = 0;
    }

    public static void renderFrame(MinecraftClient client) {
        if (!LockOnState.locked || LockOnState.isSoftLocked()) {
            lastFrameNanos = -1L;
            return;
        }

        if (CombatHitFeedbackClient.isCameraHitFeedbackActive()) {
            lastFrameNanos = -1L;
            return;
        }

        if (client.player == null || client.world == null) {
            LockOnState.clear();
            lastFrameNanos = -1L;
            return;
        }

        if (client.options.getPerspective().isFirstPerson()) {
            lastFrameNanos = -1L;
            return;
        }

        LivingEntity target = getLockedTarget(client);

        if (target == null || !target.isAlive()) {
            if (tryLockNextTarget(client)) {
                return;
            }

            if (!LockOnState.delayedClearPending()) {
                LockOnState.clear();
            }
            lastFrameNanos = -1L;
            return;
        }

        long now = System.nanoTime();

        if (lastFrameNanos < 0L) {
            lastFrameNanos = now;
            return;
        }

        float deltaSeconds = (now - lastFrameNanos) / 1_000_000_000.0F;
        lastFrameNanos = now;

        deltaSeconds = MathHelper.clamp(deltaSeconds, 0.0F, 0.05F);

        Vec3d from = client.player.getEyePos();
        Vec3d to = getTargetLockPoint(target);

        Rotation targetRotation = calculateLookAtRotation(from, to);

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        double yawFollowSpeed = CombatClientConfig.lockOnCameraYawFollowSpeed();
        double pitchFollowSpeed = CombatClientConfig.lockOnCameraPitchFollowSpeed();
        float yawLerp = 1.0F - (float) Math.exp(-yawFollowSpeed * deltaSeconds);
        float pitchLerp = 1.0F - (float) Math.exp(-pitchFollowSpeed * deltaSeconds);
        yawLerp = MathHelper.clamp(yawLerp, 0.0F, 0.55F);
        pitchLerp = MathHelper.clamp(pitchLerp, 0.0F, 0.45F);

        float newYaw = smoothAngle(currentYaw, targetRotation.yaw, yawLerp);
        float newPitch = smoothAngle(currentPitch, targetRotation.pitch, pitchLerp);
        newPitch = MathHelper.clamp(newPitch, MIN_PITCH, MAX_PITCH);

        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
    }

    public static Optional<CameraRotation> getFirstPersonCameraRotation(MinecraftClient client, float tickDelta) {
        if (client.player == null || client.world == null
                || !client.options.getPerspective().isFirstPerson()
                || !LockOnState.isHardLocked()) {
            lastFirstPersonFrameNanos = -1L;
            return Optional.empty();
        }

        LivingEntity target = getLockedTarget(client);
        if (target == null || !target.isAlive()) {
            lastFirstPersonFrameNanos = -1L;
            return Optional.empty();
        }

        Rotation targetRotation = calculateLookAtRotation(
                client.player.getLerpedPos(tickDelta).add(0.0, client.player.getEyeHeight(client.player.getPose()), 0.0),
                getTargetLockPoint(target, tickDelta)
        );
        long now = System.nanoTime();
        if (lastFirstPersonFrameNanos < 0L) {
            currentFirstPersonYaw = client.player.getYaw(tickDelta);
            currentFirstPersonPitch = client.player.getPitch(tickDelta);
            lastFirstPersonFrameNanos = now;
        }

        float deltaSeconds = MathHelper.clamp(
                (now - lastFirstPersonFrameNanos) / 1_000_000_000.0F,
                0.0F,
                0.05F
        );
        lastFirstPersonFrameNanos = now;
        float yawLerp = 1.0F - (float) Math.exp(-FIRST_PERSON_YAW_FOLLOW_PER_SECOND * deltaSeconds);
        float pitchLerp = 1.0F - (float) Math.exp(-FIRST_PERSON_PITCH_FOLLOW_PER_SECOND * deltaSeconds);
        currentFirstPersonYaw = smoothAngle(currentFirstPersonYaw, targetRotation.yaw(), yawLerp);
        currentFirstPersonPitch = MathHelper.clamp(
                smoothAngle(currentFirstPersonPitch, targetRotation.pitch(), pitchLerp),
                MIN_PITCH,
                MAX_PITCH
        );
        client.player.setYaw(currentFirstPersonYaw);
        client.player.setPitch(currentFirstPersonPitch);
        return Optional.of(new CameraRotation(currentFirstPersonYaw, currentFirstPersonPitch));
    }

    public static Vec3d getShoulderCameraOffset(MinecraftClient client, float tickDelta) {
        if (client.player == null || client.world == null) {
            currentShoulderCameraOffset = Vec3d.ZERO;
            currentShoulderBaseOffset = Vec3d.ZERO;
            currentAnimationCameraOffset = Vec3d.ZERO;
            lastShoulderFrameNanos = -1L;
            return Vec3d.ZERO;
        }

        ShoulderCameraOffsets desiredOffsets = desiredShoulderCameraOffsets(client, tickDelta);
        currentShoulderBaseOffset = smoothShoulderOffset(currentShoulderBaseOffset, desiredOffsets.baseOffset());
        currentAnimationCameraOffset = desiredOffsets.animationOffset();
        currentShoulderCameraOffset = currentShoulderBaseOffset.add(currentAnimationCameraOffset);
        if (currentShoulderCameraOffset.lengthSquared() <= 0.0001) {
            currentShoulderCameraOffset = Vec3d.ZERO;
        }

        return currentShoulderCameraOffset;
    }

    public static void clearShoulderCameraOffset() {
        currentShoulderCameraOffset = Vec3d.ZERO;
        currentShoulderBaseOffset = Vec3d.ZERO;
        currentAnimationCameraOffset = Vec3d.ZERO;
        lastShoulderFrameNanos = -1L;
        lastShoulderRotationFrameNanos = -1L;
        shoulderReleaseEndNanos = -1L;
    }

    public static Optional<CameraRotation> getShoulderCameraRotation(MinecraftClient client, Vec3d cameraPos) {
        if (client.player == null || client.world == null || !LockOnState.isHardLocked()) {
            resetShoulderCameraRotation();
            return Optional.empty();
        }

        LivingEntity target = getLockedTarget(client);
        if (target == null || !target.isAlive()) {
            resetShoulderCameraRotation();
            return Optional.empty();
        }

        Vec3d stableAimCameraPos = cameraPos.subtract(currentAnimationCameraOffset);
        Rotation rotation = calculateLookAtRotation(stableAimCameraPos, getTargetLockPoint(target));
        return Optional.of(smoothShoulderCameraRotation(rotation));
    }

    private static ShoulderCameraOffsets desiredShoulderCameraOffsets(MinecraftClient client, float tickDelta) {
        boolean hardLocked = LockOnState.isHardLocked();
        LivingEntity target = hardLocked ? getLockedTarget(client) : null;
        if (!hardLocked && currentShoulderCameraOffset.lengthSquared() <= 0.0001) {
            return ShoulderCameraOffsets.ZERO;
        }

        if (hardLocked) {
            shoulderReleaseEndNanos = -1L;
            if (target == null || !target.isAlive()) {
                return ShoulderCameraOffsets.ZERO;
            }
        } else if (CombatClientState.attacking) {
            shoulderReleaseEndNanos = -1L;
        } else {
            long now = System.nanoTime();
            if (shoulderReleaseEndNanos < 0L) {
                shoulderReleaseEndNanos = now + 200_000_000L;
            }
            if (now >= shoulderReleaseEndNanos) {
                shoulderReleaseEndNanos = -1L;
                return ShoulderCameraOffsets.ZERO;
            }
        }

        if (!hardLocked && client.options.getPerspective().isFirstPerson()) {
            return ShoulderCameraOffsets.ZERO;
        }

        float yaw = stableShoulderOffsetYaw(client, tickDelta);
        Vec3d right = Vec3d.fromPolar(0.0F, yaw + 90.0F).multiply(1.0, 0.0, 1.0);
        Vec3d forward = Vec3d.fromPolar(0.0F, yaw).multiply(1.0, 0.0, 1.0);
        if (right.lengthSquared() <= 0.0001 || forward.lengthSquared() <= 0.0001) {
            return ShoulderCameraOffsets.ZERO;
        }

        Vec3d shoulderOffset = right.normalize()
                .multiply(CombatClientConfig.lockOnCameraShoulderOffset())
                .add(0.0, CombatClientConfig.lockOnCameraHeightOffset(), 0.0);
        double pullIn = safeCameraPullIn(client, target, forward.normalize(), shoulderOffset);
        Vec3d baseOffset = shoulderOffset.add(forward.normalize().multiply(pullIn));

        Rotation cameraRotation = estimatedShoulderCameraRotation(client, baseOffset, tickDelta);
        return new ShoulderCameraOffsets(baseOffset, animationCameraOffset(client, cameraRotation));
    }

    private static float stableShoulderOffsetYaw(MinecraftClient client, float tickDelta) {
        if (LockOnState.isHardLocked()) {
            LivingEntity target = getLockedTarget(client);
            if (target != null && target.isAlive() && client.player != null) {
                Rotation rotation = calculateLookAtRotation(
                        client.player.getLerpedPos(tickDelta),
                        getTargetLockPoint(target, tickDelta)
                );
                return rotation.yaw();
            }
        }

        return client.player.getYaw(tickDelta);
    }

    private static double safeCameraPullIn(
            MinecraftClient client,
            LivingEntity target,
            Vec3d forward,
            Vec3d shoulderOffset
    ) {
        if (client.gameRenderer == null || target == null) {
            return CombatClientConfig.lockOnCameraPullIn();
        }

        Vec3d cameraAfterShoulder = client.gameRenderer.getCamera().getPos().add(shoulderOffset);
        Vec3d toTarget = getTargetLockPoint(target).subtract(cameraAfterShoulder);
        double forwardDistance = toTarget.x * forward.x + toTarget.z * forward.z;
        double safeMaximum = Math.max(0.0, forwardDistance - MIN_CAMERA_TARGET_FORWARD_GAP);
        return Math.min(CombatClientConfig.lockOnCameraPullIn(), safeMaximum);
    }

    private static Rotation estimatedShoulderCameraRotation(MinecraftClient client, Vec3d baseOffset, float tickDelta) {
        if (client.gameRenderer == null || client.player == null) {
            return new Rotation(0.0F, 0.0F);
        }

        LivingEntity target = getLockedTarget(client);
        if (target != null && target.isAlive() && LockOnState.isHardLocked()) {
            Vec3d cameraPos = client.gameRenderer.getCamera().getPos().add(baseOffset);
            return calculateLookAtRotation(cameraPos, getTargetLockPoint(target));
        }

        return new Rotation(client.player.getYaw(tickDelta), client.player.getPitch(tickDelta));
    }

    private static Vec3d animationCameraOffset(MinecraftClient client, Rotation cameraRotation) {
        if (client.player == null) {
            return Vec3d.ZERO;
        }

        return CombatAnimationClient.getLocalPlayerRawCameraTransform()
                .flatMap(GeckoLikeAnimationLibrary.BoneTransform::position)
                .map(position -> {
                    Vec3d forward = Vec3d.fromPolar(cameraRotation.pitch(), cameraRotation.yaw());
                    Vec3d right = Vec3d.fromPolar(0.0F, cameraRotation.yaw() + 90.0F);
                    if (forward.lengthSquared() <= 0.0001 || right.lengthSquared() <= 0.0001) {
                        return Vec3d.ZERO;
                    }

                    forward = forward.normalize();
                    right = right.normalize();
                    Vec3d up = right.crossProduct(forward);
                    if (up.lengthSquared() <= 0.0001) {
                        up = new Vec3d(0.0, 1.0, 0.0);
                    } else {
                        up = up.normalize();
                    }

                    return right.multiply(position.x() * ANIMATION_PIXEL_TO_BLOCK)
                            .add(up.multiply(-position.y() * ANIMATION_PIXEL_TO_BLOCK))
                            .add(forward.multiply(-position.z() * ANIMATION_PIXEL_TO_BLOCK));
                })
                .orElse(Vec3d.ZERO);
    }

    private static Vec3d smoothShoulderOffset(Vec3d current, Vec3d desired) {
        long now = System.nanoTime();
        if (lastShoulderFrameNanos < 0L) {
            lastShoulderFrameNanos = now;
            return desired;
        }

        float deltaSeconds = MathHelper.clamp((now - lastShoulderFrameNanos) / 1_000_000_000.0F, 0.0F, 0.05F);
        lastShoulderFrameNanos = now;
        double amount = 1.0 - Math.exp(-CombatClientConfig.lockOnCameraTransitionSpeed() * deltaSeconds);
        return current.lerp(desired, amount);
    }

    private static CameraRotation smoothShoulderCameraRotation(Rotation target) {
        long now = System.nanoTime();
        if (lastShoulderRotationFrameNanos < 0L) {
            lastShoulderRotationFrameNanos = now;
            currentShoulderCameraYaw = target.yaw();
            currentShoulderCameraPitch = MathHelper.clamp(target.pitch(), MIN_PITCH, MAX_PITCH);
            return new CameraRotation(currentShoulderCameraYaw, currentShoulderCameraPitch);
        }

        float deltaSeconds = MathHelper.clamp(
                (now - lastShoulderRotationFrameNanos) / 1_000_000_000.0F,
                0.0F,
                0.05F
        );
        lastShoulderRotationFrameNanos = now;

        float yawLerp = 1.0F - (float) Math.exp(-CombatClientConfig.lockOnCameraYawFollowSpeed() * deltaSeconds);
        float pitchLerp = 1.0F - (float) Math.exp(-CombatClientConfig.lockOnCameraPitchFollowSpeed() * deltaSeconds);
        yawLerp = MathHelper.clamp(yawLerp, 0.0F, 0.55F);
        pitchLerp = MathHelper.clamp(pitchLerp, 0.0F, 0.45F);

        currentShoulderCameraYaw = smoothAngle(currentShoulderCameraYaw, target.yaw(), yawLerp);
        currentShoulderCameraPitch = MathHelper.clamp(
                smoothAngle(currentShoulderCameraPitch, target.pitch(), pitchLerp),
                MIN_PITCH,
                MAX_PITCH
        );
        return new CameraRotation(currentShoulderCameraYaw, currentShoulderCameraPitch);
    }

    private static void resetShoulderCameraRotation() {
        lastShoulderRotationFrameNanos = -1L;
    }

    private static LivingEntity getLockedTarget(MinecraftClient client) {
        if (client.world == null || LockOnState.targetEntityId < 0) {
            return null;
        }

        Entity entity = client.world.getEntityById(LockOnState.targetEntityId);

        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }

        return null;
    }

    private static Vec3d getTargetLockPoint(LivingEntity target) {
        return LockOnTargetSelector.getTargetLockPoint(target);
    }

    private static Vec3d getTargetLockPoint(LivingEntity target, float tickDelta) {
        return target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * TARGET_HEIGHT_FACTOR, 0.0);
    }

    private static Vec3d getOffsetTargetLockPoint(MinecraftClient client, LivingEntity target) {
        Vec3d lockPoint = getTargetLockPoint(target);
        if (client.player == null) {
            return lockPoint;
        }

        Vec3d right = Vec3d.fromPolar(0.0F, client.player.getYaw() + 90.0F).multiply(1.0, 0.0, 1.0);
        if (right.lengthSquared() <= 0.0001) {
            return lockPoint;
        }

        return lockPoint.subtract(right.normalize().multiply(CombatClientConfig.lockOnCameraAimLeftOffset()));
    }

    private static boolean tryLockNextTarget(MinecraftClient client) {
        LivingEntity nextTarget = LockOnTargetSelector.findBestNearbyTarget(
                client,
                DEATH_RELOCK_MAX_ANGLE_DEGREES
        );
        if (nextTarget == null) {
            return false;
        }

        LockOnState.lock(nextTarget.getUuid(), nextTarget.getId());
        lastFrameNanos = -1L;
        return true;
    }

    private static Rotation calculateLookAtRotation(Vec3d from, Vec3d to) {
        Vec3d diff = to.subtract(from);

        double dx = diff.x;
        double dy = diff.y;
        double dz = diff.z;

        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (MathHelper.atan2(dz, dx) * 180.0F / Math.PI) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(dy, horizontal) * 180.0F / Math.PI);

        return new Rotation(yaw, pitch);
    }

    private static float smoothAngle(float current, float target, float amount) {
        float delta = MathHelper.wrapDegrees(target - current);
        return current + delta * amount;
    }

    private record Rotation(float yaw, float pitch) {
    }

    public record CameraRotation(float yaw, float pitch) {
    }

    private record ShoulderCameraOffsets(Vec3d baseOffset, Vec3d animationOffset) {
        private static final ShoulderCameraOffsets ZERO = new ShoulderCameraOffsets(Vec3d.ZERO, Vec3d.ZERO);
    }
}
