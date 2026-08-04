package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.hardship.HardshipEffects;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public class ItemStackHardshipDurabilityMixin {
    @ModifyVariable(method = "damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int kingdomcomecombat$increaseHardshipDurabilityUse(
            int amount, int originalAmount, LivingEntity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayerEntity player)) return amount;
        boolean armor = slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
        return HardshipEffects.scaleDurabilityDamage(player, amount, armor);
    }
}
