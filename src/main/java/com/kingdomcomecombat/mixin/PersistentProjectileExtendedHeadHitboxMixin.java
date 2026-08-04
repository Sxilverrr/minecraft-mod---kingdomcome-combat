package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.collision.ServerHitDetectionSystem;
import com.kingdomcomecombat.projectile.ProjectileGlanceState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileExtendedHeadHitboxMixin {
    @Shadow
    protected abstract boolean canHit(Entity entity);

    @Inject(method = "getEntityCollision", at = @At("RETURN"), cancellable = true)
    private void kingdomcomecombat$includeExtendedHeadHitboxes(
            Vec3d start,
            Vec3d end,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
        if (projectile.getWorld().isClient() || start.squaredDistanceTo(end) <= 1.0E-8) {
            return;
        }
        EntityHitResult vanillaHit = cir.getReturnValue();
        if (vanillaHit != null
                && vanillaHit.getEntity() instanceof LivingEntity living
                && ProjectileGlanceState.shouldIgnore(projectile, living)) {
            vanillaHit = null;
        }
        double bestDistanceSquared = vanillaHit == null
                ? Double.POSITIVE_INFINITY
                : start.squaredDistanceTo(vanillaHit.getPos());
        EntityHitResult bestHit = vanillaHit;
        Box searchBox = new Box(start, end).expand(1.25);

        for (LivingEntity target : projectile.getWorld().getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                entity -> entity.isAlive()
                        && canHit(entity)
                        && !ProjectileGlanceState.shouldIgnore(projectile, entity)
        )) {
            var headHit = ServerHitDetectionSystem.traceProjectileHead(target, start, end);
            if (headHit.isEmpty()) {
                continue;
            }
            double distanceSquared = start.squaredDistanceTo(headHit.get());
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestHit = new EntityHitResult(target, headHit.get());
            }
        }

        cir.setReturnValue(bestHit);
    }
}
