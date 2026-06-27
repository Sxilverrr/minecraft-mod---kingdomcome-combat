package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.potion.PotionCoatingHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackPotionCoatingClientMixin {
    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$getPotionCoatingUseAction(CallbackInfoReturnable<UseAction> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || !PotionCoatingHandler.isPotionCoatingCandidate(player, stack)) {
            return;
        }

        if (PotionCoatingHandler.hasActiveAnimationMarker(stack)
                || !player.isUsingItem()
                || player.getActiveHand() == Hand.MAIN_HAND) {
            cir.setReturnValue(UseAction.BOW);
        }
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$getPotionCoatingUseTime(
            LivingEntity user,
            CallbackInfoReturnable<Integer> cir
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        if (user instanceof ClientPlayerEntity player && PotionCoatingHandler.isPotionCoatingCandidate(player, stack)) {
            cir.setReturnValue(72000);
        }
    }
}
