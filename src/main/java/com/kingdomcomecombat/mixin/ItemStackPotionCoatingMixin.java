package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.potion.PotionCoatingHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackPotionCoatingMixin {
    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$getPotionCoatingUseAction(CallbackInfoReturnable<UseAction> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (PotionCoatingHandler.hasActiveAnimationMarker(stack)) {
            cir.setReturnValue(UseAction.BOW);
        }
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$getPotionCoatingUseTime(
            LivingEntity user,
            CallbackInfoReturnable<Integer> cir
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        if (user instanceof ServerPlayerEntity player && PotionCoatingHandler.isActivelyCoating(player, stack)) {
            cir.setReturnValue(72000);
        }
    }
}
