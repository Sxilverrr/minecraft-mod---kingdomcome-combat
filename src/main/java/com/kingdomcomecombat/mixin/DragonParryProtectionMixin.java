package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.boss.EnderDragonBossHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class DragonParryProtectionMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$cancelDragonDamage(
            ServerWorld world, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (EnderDragonBossHandler.tryLongHoldChargeCollisionBlock(
                world, (LivingEntity) (Object) this, source.getAttacker())) {
            cir.setReturnValue(false);
            return;
        }
        if (EnderDragonBossHandler.isProtectedFromDragon(
                (LivingEntity) (Object) this, source.getAttacker())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$cancelDragonKnockback(
            double strength, double x, double z, CallbackInfo ci
    ) {
        if (EnderDragonBossHandler.hasDragonParryProtection((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
