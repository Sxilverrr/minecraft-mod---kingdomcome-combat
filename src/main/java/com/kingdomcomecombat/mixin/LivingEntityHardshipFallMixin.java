package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.hardship.HardshipSelectionState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityHardshipFallMixin {
    @Inject(method = "computeFallDamage", at = @At("RETURN"), cancellable = true)
    private void kingdomcomecombat$increaseHardshipFallDamage(CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof ServerPlayerEntity player)
                || !HardshipSelectionState.active(player, "hardship_04")) return;
        double safe = HardshipSelectionState.prek(player, "hardship_04", "safe_fall_distance", 2.0);
        int base = Math.max(cir.getReturnValue(), (int) Math.ceil(Math.max(0.0, player.fallDistance - safe)));
        cir.setReturnValue((int) Math.ceil(base * HardshipSelectionState.prek(
                player, "hardship_04", "fall_damage_multiplier", 1.5)));
    }
}
