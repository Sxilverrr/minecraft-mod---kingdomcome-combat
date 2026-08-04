package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.animation.layered.AnimationStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AnimationStack.class, remap = false)
public class PalFirstPersonModeMixin {
    @Inject(method = "getFirstPersonMode", at = @At("RETURN"), cancellable = true)
    private void kingdomcomecombat$letFirstPersonModelOwnBodyRendering(
            CallbackInfoReturnable<FirstPersonMode> cir
    ) {
        if ((!CombatClientConfig.firstPersonRenderingEnabled()
                || FirstPersonRenderCompat.shouldSuppressPalFirstPersonRenderer())
                && cir.getReturnValue() == FirstPersonMode.THIRD_PERSON_MODEL) {
            cir.setReturnValue(FirstPersonMode.DISABLED);
        }
    }
}
