package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.combat.VanillaMobAttackControl;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MeleeAttackGoal.class)
public class MeleeAttackGoalVanillaAttackMixin {
    @Shadow
    protected PathAwareEntity mob;

    @Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$disableOverriddenVanillaMeleeGoal(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (VanillaMobAttackControl.disablesVanillaAttack(mob)) {
            mob.setAttacking(false);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldContinue", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$stopRunningOverriddenVanillaMeleeGoal(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (VanillaMobAttackControl.disablesVanillaAttack(mob)) {
            mob.setAttacking(false);
            cir.setReturnValue(false);
        }
    }
}
