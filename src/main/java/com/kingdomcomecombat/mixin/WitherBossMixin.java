package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.item.HandCannonProjectileTracker;
import com.kingdomcomecombat.boss.WitherBossHandler;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherEntity.class)
public class WitherBossMixin {
    @Inject(method = "shootSkullAt(IDDDZ)V", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$limitSkullFireRate(
            int headIndex, double targetX, double targetY, double targetZ, boolean charged,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    ) {
        if (!com.kingdomcomecombat.config.CombatServerConfig.witherOverhaulEnabled()) return;
        if (!WitherBossHandler.canFireSkull((WitherEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$ignoreHandCannon(
            ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir
    ) {
        if (!com.kingdomcomecombat.config.CombatServerConfig.witherOverhaulEnabled()) return;
        if (HandCannonProjectileTracker.isTracked(source.getSource())) {
            cir.setReturnValue(false);
        }
    }
}
