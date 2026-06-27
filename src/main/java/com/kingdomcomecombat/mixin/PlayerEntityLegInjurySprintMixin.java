package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.injury.ModStatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public class PlayerEntityLegInjurySprintMixin {
    @ModifyVariable(method = "setSprinting", at = @At("HEAD"), argsOnly = true)
    private boolean kingdomComeCombat$disableSprintingWithSevereLegInjury(boolean sprinting) {
        if (!((Object) this instanceof PlayerEntity player)) {
            return sprinting;
        }

        if (sprinting && ModStatusEffects.effectiveLevel(player, ModStatusEffects.LEG_INJURY) >= 5) {
            return false;
        }

        return sprinting;
    }
}
