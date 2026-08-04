package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.injury.ModStatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.ClearAllEffectsConsumeEffect;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ClearAllEffectsConsumeEffect.class)
public class MilkBucketInjuryMixin {
    @Redirect(
            method = "onConsume",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;clearStatusEffects()Z")
    )
    private boolean kingdomcomecombat$milkKeepsInjuriesAndBleeding(
            LivingEntity entity, World world, ItemStack stack, LivingEntity user) {
        if (!stack.isOf(Items.MILK_BUCKET)) {
            return entity.clearStatusEffects();
        }
        boolean removedAny = false;
        for (StatusEffectInstance instance : List.copyOf(entity.getStatusEffects())) {
            if (!ModStatusEffects.isInjuryOrBleeding(instance.getEffectType())) {
                removedAny |= entity.removeStatusEffect(instance.getEffectType());
            }
        }
        return removedAny;
    }
}
