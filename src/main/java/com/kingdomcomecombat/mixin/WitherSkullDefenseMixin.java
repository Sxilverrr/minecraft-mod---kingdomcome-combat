package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.boss.WitherBossHandler;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkullEntity.class)
public class WitherSkullDefenseMixin {
    @Inject(
            method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/math/Vec3d;)V",
            at = @At("RETURN")
    )
    private void kingdomcomecombat$slightlyIncreaseLaunchSpeed(
            World world,
            LivingEntity owner,
            Vec3d direction,
            CallbackInfo ci
    ) {
        if (owner instanceof WitherEntity
                && com.kingdomcomecombat.config.CombatServerConfig.witherOverhaulEnabled()) {
            ((WitherSkullEntity) (Object) this).accelerationPower *= 1.12;
        }
    }

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$cancelBlockedSkullExplosion(HitResult hit, CallbackInfo ci) {
        if (!com.kingdomcomecombat.config.CombatServerConfig.witherOverhaulEnabled()) return;
        WitherSkullEntity skull = (WitherSkullEntity) (Object) this;
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof ServerPlayerEntity player
                && WitherBossHandler.handleSkullBlock(skull, player)) {
            ci.cancel();
            return;
        }
        if (!(hit instanceof EntityHitResult)) {
            WitherBossHandler.discardMissedSkull(skull);
            ci.cancel();
        }
    }

    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$blockOrReflectSkull(EntityHitResult hit, CallbackInfo ci) {
        if (!com.kingdomcomecombat.config.CombatServerConfig.witherOverhaulEnabled()) return;
        WitherSkullEntity skull = (WitherSkullEntity) (Object) this;
        if (hit.getEntity() instanceof WitherEntity wither) {
            WitherBossHandler.onReflectedSkullHit(skull, wither);
        }
    }
}
