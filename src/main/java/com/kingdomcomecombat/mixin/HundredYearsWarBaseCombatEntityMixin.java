package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.compat.HundredYearsWarCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents Hundred Years' Warfare's melee timer from dealing a second hit while
 * the entity is driven by KCC. Ranged and utility NPCs never enter this path.
 */
@Pseudo
@Mixin(targets = "ydmsama.hundred_years_war.main.entity.entities.BaseCombatEntity", remap = false)
public abstract class HundredYearsWarBaseCombatEntityMixin {
    @Inject(method = "handleAttackLogic", at = @At("HEAD"), cancellable = true, require = 0)
    private void kingdomComeCombat$useKcdMeleeLogic(CallbackInfo ci) {
        if (HundredYearsWarCompat.usesKcdCombat((Object) this instanceof net.minecraft.entity.Entity entity
                ? entity
                : null)) {
            ci.cancel();
        }
    }
}
