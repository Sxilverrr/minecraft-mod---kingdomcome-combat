package com.kingdomcomecombat.client.lockon;

import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.equipment.MobCombatAttributesRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class LockOnTargetSelector {
    public static final double MAX_DISTANCE = 8.0;

    public static LivingEntity findBestTarget(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return null;
        }

        var player = client.player;
        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F).normalize();

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        var candidates = client.world.getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(MAX_DISTANCE),
                entity -> entity != player
                        && entity.isAlive()
                        && isValidTarget(client, entity)
        );

        for (LivingEntity candidate : candidates) {
            Vec3d lockPoint = getTargetLockPoint(candidate);
            Vec3d toTarget = lockPoint.subtract(eyePos);

            if (toTarget.lengthSquared() <= 0.0001) {
                continue;
            }

            double distance = toTarget.length();

            if (distance > MAX_DISTANCE) {
                continue;
            }

            Vec3d directionToTarget = toTarget.normalize();

            double dot = look.dotProduct(directionToTarget);
            dot = Math.max(-1.0, Math.min(1.0, dot));

            double angle = Math.toDegrees(Math.acos(dot));

            if (angle > CombatClientConfig.lockOnAcquireAngleDegrees()) {
                continue;
            }

            if (!canSeeLockPoint(client, candidate)) {
                continue;
            }

            // 角度越小越优先，其次距离越近越优先
            double score = angle * 2.8 + distance * 0.35;

            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    public static LivingEntity findBestNearbyTarget(MinecraftClient client) {
        return findBestNearbyTarget(client, Double.POSITIVE_INFINITY);
    }

    public static LivingEntity findBestNearbyTarget(MinecraftClient client, double maxAngleDegrees) {
        if (client.player == null || client.world == null) {
            return null;
        }

        var player = client.player;
        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F).normalize();

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        var candidates = client.world.getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(MAX_DISTANCE),
                entity -> entity != player
                        && entity.isAlive()
                        && isValidTarget(client, entity)
        );

        for (LivingEntity candidate : candidates) {
            Vec3d toTarget = getTargetLockPoint(candidate).subtract(eyePos);
            if (toTarget.lengthSquared() <= 0.0001) {
                continue;
            }

            double distance = toTarget.length();
            double dot = Math.max(-1.0, Math.min(1.0, look.dotProduct(toTarget.normalize())));
            double angle = Math.toDegrees(Math.acos(dot));
            if (angle > maxAngleDegrees) {
                continue;
            }
            double score = angle * 0.65 + distance * 1.15;

            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    public static boolean isValidTarget(MinecraftClient client, LivingEntity target) {
        if (client.player == null) {
            return false;
        }

        return target != client.player
                && target.isAlive()
                && isLockableTarget(client, target)
                && client.player.distanceTo(target) <= MAX_DISTANCE
                && canSeeLockPoint(client, target);
    }

    public static boolean isLockableTarget(MinecraftClient client, LivingEntity target) {
        if (target instanceof ArmorStandEntity) {
            return true;
        }

        if (client.player != null
                && target instanceof MobEntity mob
                && mob.getTarget() == client.player) {
            return true;
        }

        if (target instanceof MobEntity mob
                && client.player != null
                && mob.getAttacker() == client.player) {
            return true;
        }

        if (target instanceof MobEntity
                && MobCombatAttributesRegistry.get(target).isPresent()) {
            return true;
        }

        return target instanceof HostileEntity;
    }

    public static Vec3d getTargetLockPoint(LivingEntity target) {
        return target.getPos().add(
                0.0,
                target.getHeight() * 0.70,
                0.0
        );
    }

    public static boolean canSeeLockPoint(MinecraftClient client, LivingEntity target) {
        if (client.player == null || client.world == null) {
            return false;
        }

        Vec3d from = client.player.getEyePos();
        Vec3d to = getTargetLockPoint(target);

        HitResult result = client.world.raycast(new RaycastContext(
                from,
                to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));

        return result.getType() == HitResult.Type.MISS;
    }
}
