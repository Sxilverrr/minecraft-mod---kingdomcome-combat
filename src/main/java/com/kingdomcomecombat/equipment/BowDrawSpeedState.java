package com.kingdomcomecombat.equipment;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BowDrawSpeedState {
    private static final Identifier RAPID_FIRE_ID =
            Identifier.of(KingdomComeCombat.MOD_ID, "rapid_fire");
    private static final Map<RapidFireKey, RapidFireState> RAPID_FIRE_STATE = new HashMap<>();

    private BowDrawSpeedState() {
    }

    public static float getDrawSpeed(ItemStack stack, LivingEntity user, World world) {
        float multiplier = (float) RangedWeaponAttributesRegistry.get(stack).drawSpeed();
        int level = enchantmentLevel(stack, RAPID_FIRE_ID);
        RapidFireKey key = new RapidFireKey(user.getUuid(), world.isClient());
        RapidFireState state = RAPID_FIRE_STATE.get(key);
        if (level <= 0 || state == null || world.getTime() >= state.expiresAt()) {
            RAPID_FIRE_STATE.remove(key);
            return multiplier;
        }
        return multiplier * (1.0F + Math.min(level, state.stacks()) * 0.15F);
    }

    public static void grantAfterShot(ItemStack stack, LivingEntity user, World world) {
        int level = enchantmentLevel(stack, RAPID_FIRE_ID);
        if (level <= 0) {
            return;
        }

        RapidFireKey key = new RapidFireKey(user.getUuid(), world.isClient());
        RapidFireState previous = RAPID_FIRE_STATE.get(key);
        int stacks = previous == null || world.getTime() >= previous.expiresAt()
                ? 1
                : Math.min(level, previous.stacks() + 1);
        RAPID_FIRE_STATE.put(key, new RapidFireState(stacks, world.getTime() + level * 40L));
    }

    private static int enchantmentLevel(ItemStack stack, Identifier enchantmentId) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return 0;
        }

        int level = 0;
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(enchantmentId)) {
                level = Math.max(level, entry.getIntValue());
            }
        }
        return level;
    }

    private record RapidFireState(int stacks, long expiresAt) {
    }

    private record RapidFireKey(UUID userId, boolean client) {
    }
}
