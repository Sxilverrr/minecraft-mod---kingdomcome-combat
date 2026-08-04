package com.kingdomcomecombat.mixin.client;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackWhiteNameMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void kingdomcomecombat$forceWhiteInventoryName(CallbackInfoReturnable<Text> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if ("kingdom_come_combat".equals(Registries.ITEM.getId(stack.getItem()).getNamespace())) {
            cir.setReturnValue(cir.getReturnValue().copy().formatted(Formatting.WHITE));
        }
    }
}
