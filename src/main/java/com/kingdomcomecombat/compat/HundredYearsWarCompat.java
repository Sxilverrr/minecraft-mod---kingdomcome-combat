package com.kingdomcomecombat.compat;

import com.kingdomcomecombat.ai.HumanoidCombatAiProfile;
import com.kingdomcomecombat.combat.CombatMovementConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;

import java.lang.reflect.Method;
import java.util.Set;

/** Optional, reflection-only integration with Hundred Years' Warfare. */
public final class HundredYearsWarCompat {
    private static final String MOD_NAMESPACE = "hundred_years_war";
    private static final Set<String> KCD_MELEE_NPCS = Set.of(
            "shieldman", "militia", "spear_man", "warrior",
            "bandit_soldier", "bandit_soldier_elite", "bandit_executioner",
            "bandit_shield_axeman", "bandit_raider",
            "cavalry_with_sword", "mounted_lancer_rider", "mounted_light_lancer_rider",
            "hyw_fast_zombie", "hyw_giant", "hyw_skeleton", "hyw_skeleton_light_cavalry",
            "hyw_wither_skeleton", "hyw_zombie", "melee_puppet", "wood_elf_warrior"
    );
    private static final HumanoidCombatAiProfile[] LEVEL_PROFILES = {
            profile(25, 0.28, 0.32, 0.04, 0, 0.10, 0.02, 0, 90, 0.26, 0.68, 0.52, 3.0, 0.54, 2),
            profile(21, 0.38, 0.48, 0.10, 1, 0.28, 0.04, 1, 105, 0.30, 0.78, 0.44, 4.5, 0.64, 3),
            profile(18, 0.49, 0.63, 0.18, 2, 0.50, 0.07, 2, 122, 0.35, 0.88, 0.36, 6.0, 0.75, 4),
            profile(15, 0.60, 0.77, 0.28, 3, 0.74, 0.10, 3, 142, 0.41, 1.00, 0.28, 8.0, 0.86, 5)
    };
    private static final ClassValue<LevelAccess> LEVEL_ACCESS = new ClassValue<>() {
        @Override
        protected LevelAccess computeValue(Class<?> type) {
            return new LevelAccess(findMethod(type, "getEquipmentLevel"), findMethod(type, "getLevel"));
        }
    };

    private HundredYearsWarCompat() {
    }

    public static boolean isNpc(Entity entity) {
        return entity instanceof MobEntity
                && MOD_NAMESPACE.equals(Registries.ENTITY_TYPE.getId(entity.getType()).getNamespace());
    }

    public static boolean usesKcdCombat(Entity entity) {
        if (!(entity instanceof MobEntity) || !isNpc(entity)) {
            return false;
        }
        return KCD_MELEE_NPCS.contains(Registries.ENTITY_TYPE.getId(entity.getType()).getPath());
    }

    public static HumanoidCombatAiProfile getAiProfile(LivingEntity entity) {
        if (!usesKcdCombat(entity)) {
            return null;
        }
        int level = LEVEL_ACCESS.get(entity.getClass()).readLevel(entity);
        return LEVEL_PROFILES[Math.max(0, Math.min(LEVEL_PROFILES.length - 1, level - 1))];
    }

    private static Method findMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private record LevelAccess(Method equipmentLevel, Method soldierLevel) {
        int readLevel(Object entity) {
            return Math.max(read(equipmentLevel, entity), read(soldierLevel, entity));
        }

        private static int read(Method method, Object entity) {
            if (method == null) {
                return 1;
            }
            try {
                Object value = method.invoke(entity);
                return value instanceof Number number ? Math.max(1, number.intValue()) : 1;
            } catch (ReflectiveOperationException ignored) {
                return 1;
            }
        }
    }

    private static HumanoidCombatAiProfile profile(
            int interval, double desire, double block, double perfectBlock,
            int combo, double comboPlan, double dodge, int aiLevel,
            double stamina, double staminaRegen, double animationSpeed,
            double startupSlowdown, double toughness, double followUp, int maxFollowUps
    ) {
        return new HumanoidCombatAiProfile(
                interval, desire, block, perfectBlock, combo, comboPlan, dodge, aiLevel,
                stamina, staminaRegen, animationSpeed, startupSlowdown, toughness, 1.0,
                followUp, maxFollowUps, true, true, 7.0, 3.35,
                CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE, 3.2, 4.5
        );
    }
}
