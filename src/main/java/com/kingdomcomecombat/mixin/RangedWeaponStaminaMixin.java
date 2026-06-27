package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.equipment.RangedWeaponUsage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public class RangedWeaponStaminaMixin {
    @Inject(method = "usageTick", at = @At("HEAD"))
    private void kingdomcomecombat$consumeBowDrawStamina(
            World world,
            LivingEntity user,
            ItemStack stack,
            int remainingUseTicks,
            CallbackInfo ci
    ) {
        if (world.isClient() || !(stack.getItem() instanceof BowItem)) {
            return;
        }
        RangedWeaponUsage.consumeDrawStamina(user, stack);
    }
}
