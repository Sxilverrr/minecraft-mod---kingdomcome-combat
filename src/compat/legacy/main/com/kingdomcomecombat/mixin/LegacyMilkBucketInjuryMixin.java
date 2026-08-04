package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.injury.ModStatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.MilkBucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** 1.21-1.21.4 variant, before milk clearing moved to a consume-effect component. */
@Mixin(MilkBucketItem.class)
public class LegacyMilkBucketInjuryMixin {
    @Redirect(
            method = "finishUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;clearStatusEffects()Z")
    )
    private boolean kingdomcomecombat$milkKeepsInjuriesAndBleeding(LivingEntity entity) {
        boolean removedAny = false;
        for (StatusEffectInstance instance : List.copyOf(entity.getStatusEffects())) {
            if (!ModStatusEffects.isInjuryOrBleeding(instance.getEffectType())) {
                removedAny |= entity.removeStatusEffect(instance.getEffectType());
            }
        }
        return removedAny;
    }
}
