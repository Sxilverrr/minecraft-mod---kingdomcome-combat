package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.injury.ModStatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StatusEffect.class)
public class InstantHealthClearsInjuryMixin {
    @Inject(method = "applyInstantEffect", at = @At("HEAD"))
    private void kingdomcomecombat$clearInjuriesOnInstantHealth(
            ServerWorld world,
            Entity source,
            Entity attacker,
            LivingEntity target,
            int amplifier,
            double proximity,
            CallbackInfo ci
    ) {
        if ((Object) this == StatusEffects.INSTANT_HEALTH.value()) {
            ModStatusEffects.clearInjuries(target);
        }
    }
}
