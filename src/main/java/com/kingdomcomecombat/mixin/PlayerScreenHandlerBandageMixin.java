package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.item.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerScreenHandler.class)
public class PlayerScreenHandlerBandageMixin {
    private static final List<EquipmentSlot> KINGDOM_COME_COMBAT_BANDAGE_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS
    );

    @Inject(method = "onClosed", at = @At("TAIL"))
    private void kingdomComeCombat$unequipBandagesOnClose(PlayerEntity player, CallbackInfo ci) {
        if (player.getWorld().isClient) {
            return;
        }

        for (EquipmentSlot slot : KINGDOM_COME_COMBAT_BANDAGE_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (!stack.isOf(ModItems.BANDAGE)) {
                continue;
            }

            player.equipStack(slot, ItemStack.EMPTY);
            player.getInventory().offerOrDrop(stack);
        }
    }
}
