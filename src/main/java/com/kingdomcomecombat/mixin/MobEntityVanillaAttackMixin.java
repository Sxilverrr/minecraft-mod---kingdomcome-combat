package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.combat.VanillaMobAttackControl;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public class MobEntityVanillaAttackMixin {
    @Inject(method = "tryAttack", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$disableVanillaAttackForCustomAi(
            ServerWorld world,
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        MobEntity mob = (MobEntity) (Object) this;
        if (VanillaMobAttackControl.disablesVanillaAttack(mob)) {
            mob.setAttacking(false);
            cir.setReturnValue(false);
        }
    }
}
