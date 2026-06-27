package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.combat.ServerCombatStanceState;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.potion.PotionCoatingHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerPotionCoatingMixin {
    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$startPotionCoating(
            ServerPlayerEntity player,
            World world,
            ItemStack stack,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (world.isClient) {
            return;
        }

        if (ServerCombatStanceState.isLocked(player.getUuid())
                && EquipmentCombatAttributesRegistry.isLargeShield(stack)) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        if (hand != Hand.MAIN_HAND) {
            return;
        }

        if (PotionCoatingHandler.tryStart(player)) {
            player.setCurrentHand(Hand.MAIN_HAND);
            cir.setReturnValue(ActionResult.CONSUME);
        }
    }
}
