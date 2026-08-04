package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.hardship.HardshipSelectionState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityHardshipStatusEffectMixin {
    @ModifyVariable(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private StatusEffectInstance kingdomcomecombat$extendVanillaHarmfulEffect(StatusEffectInstance effect) {
        if (!((Object) this instanceof ServerPlayerEntity player)
                || effect.getDuration() <= 0
                || !HardshipSelectionState.active(player, "hardship_17")
                || effect.getEffectType().value().getCategory() != StatusEffectCategory.HARMFUL
                || !"minecraft".equals(Registries.STATUS_EFFECT.getId(effect.getEffectType().value()).getNamespace())) {
            return effect;
        }
        int duration = Math.max(1, (int) Math.ceil(effect.getDuration() * HardshipSelectionState.prek(
                player, "hardship_17", "vanilla_harmful_effect_duration_multiplier", 1.5)));
        return new StatusEffectInstance(effect.getEffectType(), duration, effect.getAmplifier(), effect.isAmbient(),
                effect.shouldShowParticles(), effect.shouldShowIcon());
    }
}
