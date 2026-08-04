package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.injury.InjuryTicker;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealInjuryMixin {
    @Unique private float kingdomcomecombat$healthBeforeHeal;

    @Inject(method = "heal", at = @At("HEAD"))
    private void kingdomcomecombat$captureHealth(float amount, CallbackInfo ci) {
        kingdomcomecombat$healthBeforeHeal = ((LivingEntity) (Object) this).getHealth();
    }

    @Inject(method = "heal", at = @At("RETURN"))
    private void kingdomcomecombat$healMobInjuries(float amount, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        InjuryTicker.onEntityHealed(entity, entity.getHealth() - kingdomcomecombat$healthBeforeHeal);
    }
}
