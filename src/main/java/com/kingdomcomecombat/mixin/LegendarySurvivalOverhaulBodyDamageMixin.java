package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.compat.LegendarySurvivalOverhaulCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Replaces LSO's inferred/random limb with KCC's exact hit part for active KCC damage. */
@Pseudo
@Mixin(targets = "sfiomn.legendarysurvivaloverhaul.api.bodydamage.BodyDamageUtil", remap = false)
public abstract class LegendarySurvivalOverhaulBodyDamageMixin {
    @Inject(method = "balancedHurtBodyParts", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void kingdomcomecombat$useExactHitPart(
            @Coerce Object player,
            List<?> ignoredParts,
            float damageValue,
            CallbackInfo ci
    ) {
        if (LegendarySurvivalOverhaulCompat.applyKccLimbDamage(player, damageValue)) {
            ci.cancel();
        }
    }
}
