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

    public static boolean isConfigured(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && ATTRIBUTES.containsKey(Registries.ITEM.getId(stack.getItem()));
    }

    public static Map<String, RangedWeaponAttributes> snapshot() {
        Map<String, RangedWeaponAttributes> result = new HashMap<>();
        ATTRIBUTES.forEach((id, attributes) -> result.put(id.toString(), attributes));
        return Map.copyOf(result);
    }

    public static void replace(Map<String, RangedWeaponAttributes> attributes) {
        ATTRIBUTES.clear();
        if (attributes == null) return;
        attributes.forEach((id, value) -> {
            Identifier parsed = Identifier.tryParse(id);
            if (parsed != null && value != null) ATTRIBUTES.put(parsed, value);
        });
    }
}
