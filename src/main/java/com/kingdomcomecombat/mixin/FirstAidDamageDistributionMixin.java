package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.compat.FirstAidCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies KCC's exact hit part to First Aid New when that optional mod is loaded. */
@Pseudo
@Mixin(targets = "ichttt.mods.firstaid.common.EventHandler", remap = false)
public abstract class FirstAidDamageDistributionMixin {
    @Inject(method = "getForcedDamageDistribution", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void kingdomcomecombat$useHitPart(CallbackInfoReturnable<Object> cir) {
        Object distribution = FirstAidCompat.forcedDistribution();
        if (distribution != null) {
            cir.setReturnValue(distribution);
        }
    }
}
