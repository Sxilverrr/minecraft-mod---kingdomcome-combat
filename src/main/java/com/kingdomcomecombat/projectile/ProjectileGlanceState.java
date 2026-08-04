package com.kingdomcomecombat.projectile;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public final class ProjectileGlanceState {
    private static final int IGNORE_TICKS = 12;
    private static final WeakHashMap<ProjectileEntity, Map<UUID, Long>> IGNORED_TARGETS = new WeakHashMap<>();
    private static final WeakHashMap<ProjectileEntity, Vec3d> PENDING = new WeakHashMap<>();
    private ProjectileGlanceState() {}
    public static boolean hasHit(ProjectileEntity projectile, LivingEntity target) {
        return shouldIgnore(projectile, target);
    }
    public static boolean shouldIgnore(ProjectileEntity projectile, LivingEntity target) {
        Map<UUID, Long> targets = IGNORED_TARGETS.get(projectile);
        if (targets == null) return false;
        long now = projectile.getWorld().getTime();
        Long expiresAt = targets.get(target.getUuid());
        if (expiresAt == null) return false;
        if (expiresAt <= now) {
            targets.remove(target.getUuid());
            if (targets.isEmpty()) IGNORED_TARGETS.remove(projectile);
            return false;
        }
        return true;
    }
    public static void mark(ProjectileEntity projectile, LivingEntity target) {
        IGNORED_TARGETS.computeIfAbsent(projectile, ignored -> new HashMap<>())
                .put(target.getUuid(), projectile.getWorld().getTime() + IGNORE_TICKS);
        Vec3d incoming = projectile.getVelocity();
        Vec3d normal = projectile.getPos().subtract(target.getBoundingBox().getCenter()).normalize();
        Vec3d tangent = incoming.subtract(normal.multiply(incoming.dotProduct(normal)));
        if (tangent.lengthSquared() < 0.0025) tangent = incoming.crossProduct(new Vec3d(0, 1, 0));
        if (tangent.lengthSquared() > 0.0001) PENDING.put(projectile, tangent.normalize().multiply(incoming.length() * 0.72));
    }
    public static void applyPending(ProjectileEntity projectile) {
        Vec3d velocity = PENDING.remove(projectile);
        if (velocity == null) return;
        projectile.setVelocity(velocity);
        projectile.setPosition(projectile.getPos().add(velocity.normalize().multiply(0.18)));
        projectile.velocityModified = true;
    }
}
