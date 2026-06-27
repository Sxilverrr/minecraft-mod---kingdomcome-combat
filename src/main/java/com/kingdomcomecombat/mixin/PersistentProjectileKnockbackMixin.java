package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.projectile.ProjectileImpactHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public class PersistentProjectileKnockbackMixin {
    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$cancelProjectileKnockbackWithoutImpact(
            LivingEntity target,
            DamageSource source,
            CallbackInfo ci
    ) {
        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
        if (!(projectile.getWorld() instanceof ServerWorld world)
                || !ProjectileImpactHandler.hasImpactKnockback(world, projectile, target)) {
            ci.cancel();
        }
    }
}
