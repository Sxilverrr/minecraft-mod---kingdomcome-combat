package com.kingdomcomecombat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.kingdomcomecombat.injury.InjuryTicker;
import com.kingdomcomecombat.injury.ModStatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(HungerManager.class)
public class HungerManagerInjuryMixin {
    @WrapOperation(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;canFoodHeal()Z"
            )
    )
    private boolean kingdomComeCombat$bleedingPreventsNaturalHealing(
            ServerPlayerEntity player,
            Operation<Boolean> original
    ) {
        return !ModStatusEffects.hasBleeding(player) && original.call(player);
    }

    @WrapOperation(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;heal(F)V"
            )
    )
    private void kingdomComeCombat$creditNaturalHealing(
            ServerPlayerEntity player,
            float amount,
            Operation<Void> original
    ) {
        float before = player.getHealth();
        original.call(player, amount);
        float healed = player.getHealth() - before;
        if (healed > 0.0F) {
            InjuryTicker.advanceNaturalRecoveryFromHeal(player, healed);
        }
    }

    @ModifyConstant(method = "update", constant = @Constant(intValue = 10))
    private int kingdomComeCombat$slowSaturationRegen(int original, ServerPlayerEntity player) {
        return slowedNaturalRegenThreshold(original, player);
    }

    @ModifyConstant(method = "update", constant = @Constant(intValue = 80, ordinal = 0))
    private int kingdomComeCombat$slowFoodRegen(int original, ServerPlayerEntity player) {
        return slowedNaturalRegenThreshold(original, player);
    }

    private static int slowedNaturalRegenThreshold(int original, ServerPlayerEntity player) {
        return Math.max(1, (int) Math.ceil(original / InjuryTicker.naturalRegenerationMultiplier(player)));
    }
}
