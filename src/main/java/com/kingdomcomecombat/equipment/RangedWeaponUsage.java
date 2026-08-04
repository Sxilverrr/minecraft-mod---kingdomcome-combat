package com.kingdomcomecombat.equipment;

import com.kingdomcomecombat.stamina.ServerStaminaState;
import com.kingdomcomecombat.config.CombatServerConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public final class RangedWeaponUsage {
    private static final ThreadLocal<Boolean> STOPPING_USE =
            ThreadLocal.withInitial(() -> false);

    private RangedWeaponUsage() {
    }

    public static void consumeDrawStamina(LivingEntity user, ItemStack stack) {
        if (STOPPING_USE.get()) {
            return;
        }
        RangedWeaponAttributes attributes = RangedWeaponAttributesRegistry.get(stack);
        double costPerTick = attributes.hardness() / 20.0;
        if (!CombatServerConfig.attacksDoNotConsumeStamina()
                && costPerTick > 0.0 && !ServerStaminaState.consume(user, costPerTick)) {
            STOPPING_USE.set(true);
            try {
                user.stopUsingItem();
            } finally {
                STOPPING_USE.remove();
            }
        }
    }
}
