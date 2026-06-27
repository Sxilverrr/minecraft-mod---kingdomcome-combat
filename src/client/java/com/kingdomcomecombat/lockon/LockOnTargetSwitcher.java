package com.kingdomcomecombat.client.lockon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class LockOnTargetSwitcher {
    private static final double MAX_SWITCH_DISTANCE_FROM_PLAYER = 12.0;
    private static final double MAX_SWITCH_DISTANCE_FROM_CURRENT_TARGET = 10.0;
    public enum SwitchSide {
        LEFT,
        RIGHT
    }

    public static LivingEntity findSwitchTarget(
            MinecraftClient client,
            LivingEntity currentTarget,
            SwitchSide side
    ) {
        if (client.player == null || client.world == null || currentTarget == null) {
            return null;
        }

        var player = client.player;

        Vec3d eyePos = player.getEyePos();
        Vec3d toCurrent = LockOnTargetSelector.getTargetLockPoint(currentTarget).subtract(eyePos);
        if (toCurrent.x * toCurrent.x + toCurrent.z * toCurrent.z <= 0.0001) {
            return null;
        }
        double currentBearing = Math.toDegrees(Math.atan2(toCurrent.z, toCurrent.x));

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        var candidates = client.world.getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(MAX_SWITCH_DISTANCE_FROM_PLAYER),
                entity -> entity != player
                        && entity != currentTarget
                        && entity.isAlive()
                        && LockOnTargetSelector.isLockableTarget(client, entity)
        );

        for (LivingEntity candidate : candidates) {
            double distanceFromPlayer = player.distanceTo(candidate);
            if (distanceFromPlayer > MAX_SWITCH_DISTANCE_FROM_PLAYER) {
                continue;
            }

            double distanceFromCurrentTarget = currentTarget.distanceTo(candidate);
            if (distanceFromCurrentTarget > MAX_SWITCH_DISTANCE_FROM_CURRENT_TARGET) {
                continue;
            }

            Vec3d lockPoint = LockOnTargetSelector.getTargetLockPoint(candidate);
            Vec3d toCandidate = lockPoint.subtract(eyePos);

            if (toCandidate.lengthSquared() <= 0.0001) {
                continue;
            }

            double candidateBearing = Math.toDegrees(Math.atan2(toCandidate.z, toCandidate.x));
            double signedAngle = MathHelper.wrapDegrees(candidateBearing - currentBearing);
            if (side == SwitchSide.LEFT && signedAngle >= -0.01) {
                continue;
            }
            if (side == SwitchSide.RIGHT && signedAngle <= 0.01) {
                continue;
            }

            if (!LockOnTargetSelector.canSeeLockPoint(client, candidate)) {
                continue;
            }

            double score = Math.abs(signedAngle) * 1.8
                    + distanceFromPlayer * 0.35
                    + distanceFromCurrentTarget * 0.55;

            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }
}
