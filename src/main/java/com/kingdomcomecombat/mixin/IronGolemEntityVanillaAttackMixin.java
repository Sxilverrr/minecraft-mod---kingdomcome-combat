package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.ai.ConfiguredMobAttackTicker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronGolemEntity.class)
public class IronGolemEntityVanillaAttackMixin {
    @Inject(method = "tryAttack", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$onlyHitDuringConfiguredLunge(
            ServerWorld world, Entity target, CallbackInfoReturnable<Boolean> cir) {
        IronGolemEntity golem = (IronGolemEntity) (Object) this;
        if (ConfiguredMobAttackTicker.disablesVanillaAttack(golem)) {
            cir.setReturnValue(false);
        }
    }
}
