package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.config.CombatServerConfig;
import com.kingdomcomecombat.equipment.EquipmentFallbackConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class CombatItemUtil {
    private static final Identifier SWEEPING_EDGE_ENCHANTMENT_ID =
            Identifier.ofVanilla("sweeping_edge");
    public static final TagKey<net.minecraft.item.Item> LONGSWORDS =
            TagKey.of(RegistryKeys.ITEM, Identifier.of(KingdomComeCombat.MOD_ID, "longswords"));
    public static final TagKey<EntityType<?>> ALWAYS_VANILLA_ATTACKABLE =
            TagKey.of(
                    RegistryKeys.ENTITY_TYPE,
                    Identifier.of(KingdomComeCombat.MOD_ID, "always_vanilla_attackable")
            );

    public static boolean canUseCustomCombat(PlayerEntity player) {
        ItemStack stack = player.getMainHandStack();

        // 空手
        if (stack.isEmpty()) {
            return true;
        }

        if (isLongsword(stack)
                && !player.getOffHandStack().isEmpty()
                && !BeowulfArmState.isActive(player)) {
            return false;
        }

        // 剑、斧、镐、锄、铲
        return isLongsword(stack)
                || isShortSword(stack)
                || isHeavyWeapon(stack)
                || isFightingMace(stack)
                || stack.isIn(ItemTags.SWORDS)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.HOES)
                || stack.isIn(ItemTags.SHOVELS);
    }

    public static boolean hasCombatWeapon(PlayerEntity player) {
        return player != null
                && !player.getMainHandStack().isEmpty()
                && canUseCustomCombat(player);
    }

    public static boolean hasSweepingEdge(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return false;
        }

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(SWEEPING_EDGE_ENCHANTMENT_ID)
                    && entry.getIntValue() > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLongsword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        return stack.isIn(LONGSWORDS) || EquipmentFallbackConfig.isConfiguredLongswordItem(itemId);
    }

    public static boolean isShortSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        return EquipmentFallbackConfig.isConfiguredShortSwordItem(itemId)
                || (stack.isIn(ItemTags.SWORDS) && !isLongsword(stack));
    }

    public static boolean isSword(ItemStack stack) {
        return isLongsword(stack) || isShortSword(stack);
    }

    public static boolean isHeavyWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        return EquipmentFallbackConfig.isConfiguredHeavyWeaponItem(itemId)
                || isFightingMace(stack)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.PICKAXES);
    }

    public static boolean shouldUseVanillaEntityAttack(PlayerEntity player) {
        return shouldUseVanillaEntityAttack(player, null);
    }

    public static boolean shouldUseVanillaEntityAttack(PlayerEntity player, Entity target) {
        return player != null
                && (isConfiguredVanillaAttackWeapon(player.getMainHandStack())
                || isAllowedVanillaAttackMount(player.getVehicle())
                || canUseAirborneHeavyHammerAttack(player)
                || isAlwaysVanillaAttackable(target));
    }

    public static boolean isConfiguredVanillaAttackWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return CombatServerConfig.allowsVanillaAttackWeapon(Registries.ITEM.getId(stack.getItem()));
    }

    public static boolean isAlwaysVanillaAttackable(Entity target) {
        return target != null && target.getType().isIn(ALWAYS_VANILLA_ATTACKABLE);
    }

    public static boolean isAllowedVanillaAttackMount(Entity vehicle) {
        return vehicle != null
                && (vehicle.getType() == EntityType.HORSE
                || vehicle.getType() == EntityType.CAMEL);
    }

    public static boolean canUseAirborneHeavyHammerAttack(PlayerEntity player) {
        return player != null
                && !player.isOnGround()
                && !player.isTouchingWater()
                && player.fallDistance >= 4.0F
                && isFightingMace(player.getMainHandStack());
    }

    public static boolean isFightingMace(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        return stack.isOf(Items.MACE)
                || itemId.getPath().contains("mace")
                || itemId.getPath().contains("hammer");
    }
}
