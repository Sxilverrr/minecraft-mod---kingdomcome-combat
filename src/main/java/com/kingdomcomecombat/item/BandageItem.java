package com.kingdomcomecombat.item;

import com.kingdomcomecombat.config.CombatServerConfig;
import com.kingdomcomecombat.hardship.HardshipSelectionState;
import com.kingdomcomecombat.injury.ModStatusEffects;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Optional direct-use bandage behavior controlled by the server config. */
public final class BandageItem extends Item {
    public BandageItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!CombatServerConfig.changedBandageUseEnabled()) {
            return ActionResult.PASS;
        }
        if (!world.isClient && user instanceof ServerPlayerEntity player
                && (ModStatusEffects.hasBleeding(player)
                || ModStatusEffects.totalWoundLevels(player) > 0)) {
            ModStatusEffects.clearInjuries(player);
            ItemStack stack = player.getStackInHand(hand);
            int damage = HardshipSelectionState.active(player, "hardship_05")
                    ? Math.max(1, (int) Math.ceil(HardshipSelectionState.prek(
                            player, "hardship_05", "bandage_durability_multiplier", 2.0)))
                    : 1;
            stack.damage(damage, player, hand == Hand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return ActionResult.SUCCESS;
    }
}
