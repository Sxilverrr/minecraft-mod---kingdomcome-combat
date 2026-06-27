package com.kingdomcomecombat.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackWoodenSwordDurabilityMixin {
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void kingdomcomecombat$multiplyWoodenSwordDurability(CallbackInfoReturnable<Integer> cir) {
        if (((ItemStack) (Object) this).isOf(Items.WOODEN_SWORD)) {
            cir.setReturnValue(cir.getReturnValue() * 20);
        }
    }
}
