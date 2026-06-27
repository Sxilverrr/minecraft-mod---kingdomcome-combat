package com.kingdomcomecombat.equipment;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class RangedWeaponAttributesRegistry {
    private static final Map<Identifier, RangedWeaponAttributes> ATTRIBUTES = new HashMap<>();

    private RangedWeaponAttributesRegistry() {
    }

    public static void clear() {
        ATTRIBUTES.clear();
    }

    public static void register(Identifier itemId, RangedWeaponAttributes attributes) {
        ATTRIBUTES.put(itemId, attributes);
    }

    public static RangedWeaponAttributes get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return RangedWeaponAttributes.DEFAULT;
        }
        return ATTRIBUTES.getOrDefault(
                Registries.ITEM.getId(stack.getItem()),
                RangedWeaponAttributes.DEFAULT
        );
    }
}
