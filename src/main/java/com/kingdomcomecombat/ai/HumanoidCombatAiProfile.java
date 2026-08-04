package com.kingdomcomecombat.ai;

import com.kingdomcomecombat.combat.CombatMovementConfig;

public record HumanoidCombatAiProfile(
        int minAttackIntervalTicks,
        double attackDesirePerHalfSecond,
        double blockChance,
        double perfectBlockChance,
        int comboLevel,
        double comboPlanChance,
        double dodgeChance,
        int aiLevel,
        double staminaMax,
        double staminaRegenPerTick,
        double attackAnimationSpeed,
        double attackStartupSlowdown,
        double toughness,
        double wornArmorDurabilityMultiplier,
        double followUpAttackChance,
        int maxFollowUpAttacks,
        boolean keepDistance,
        boolean requiresWeapon,
        double combatEnterDistance,
        double attackDistance,
        double minDistance,
        double approachDistance,
        double exhaustedRetreatDistance
) {
    public HumanoidCombatAiProfile {
        minAttackIntervalTicks = Math.max(0, minAttackIntervalTicks);
        attackDesirePerHalfSecond = clamp01(attackDesirePerHalfSecond);
        blockChance = clamp01(blockChance);
        perfectBlockChance = clamp01(perfectBlockChance);
        comboLevel = Math.max(0, comboLevel);
        comboPlanChance = clamp01(comboPlanChance);
        dodgeChance = clamp01(dodgeChance);
        aiLevel = Math.max(0, aiLevel);
        staminaMax = Math.max(1.0, staminaMax);
        staminaRegenPerTick = Math.max(0.0, staminaRegenPerTick);
        attackAnimationSpeed = Math.max(0.05, attackAnimationSpeed);
        attackStartupSlowdown = Math.max(0.0, Math.min(0.95, attackStartupSlowdown));
        toughness = Math.max(0.0, toughness);
        wornArmorDurabilityMultiplier = Math.max(0.0, wornArmorDurabilityMultiplier);
        followUpAttackChance = clamp01(followUpAttackChance);
        maxFollowUpAttacks = Math.max(0, maxFollowUpAttacks);
        combatEnterDistance = Math.max(0.0, combatEnterDistance);
        attackDistance = Math.max(0.0, attackDistance);
        minDistance = Math.max(CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE, minDistance);
        approachDistance = Math.max(0.0, approachDistance);
        exhaustedRetreatDistance = Math.max(0.0, exhaustedRetreatDistance);
    }

    public static HumanoidCombatAiProfile skeletonDefault() {
        return new HumanoidCombatAiProfile(
                18,
                0.28,
                0.25,
                0.35,
                0,
                0.0,
                0.06,
                1,
                100.0,
                0.32,
                0.45,
                0.45,
                4.0,
                1.0,
                0.72,
                3,
                true,
                false,
                6.0,
                3.25,
                CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE,
                3.0,
                5.0
        );
    }

    public static HumanoidCombatAiProfile witherSkeletonDefault() {
        return new HumanoidCombatAiProfile(
                16,
                0.42,
                0.45,
                0.20,
                1,
                0.40,
                0.08,
                2,
                120.0,
                0.34,
                0.58,
                0.40,
                6.0,
                1.0,
                0.78,
                4,
                false,
                true,
                7.0,
                3.25,
                CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE,
                3.4,
                4.0
        );
    }

    public static HumanoidCombatAiProfile zombieDefault() {
        return new HumanoidCombatAiProfile(
                28,
                0.40,
                0.70,
                0.0,
                0,
                0.0,
                0.0,
                0,
                100.0,
                0.26,
                0.36,
                0.55,
                5.0,
                1.0,
                0.62,
                3,
                false,
                true,
                6.0,
                2.8,
                CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE,
                3.0,
                3.0
        );
    }

    public static HumanoidCombatAiProfile zombieLeaderDefault() {
        return new HumanoidCombatAiProfile(
                18,
                0.56,
                0.82,
                0.08,
                1,
                0.40,
                0.03,
                2,
                140.0,
                0.34,
                0.48,
                0.42,
                8.0,
                1.0,
                0.78,
                4,
                false,
                true,
                7.0,
                3.0,
                CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE,
                3.4,
                3.5
        );
    }

    public static HumanoidCombatAiProfile illagerMeleeDefault() {
        return new HumanoidCombatAiProfile(
                18,
                0.32,
                0.55,
                0.18,
                0,
                0.0,
                0.04,
                1,
                100.0,
                0.30,
                0.56,
                0.45,
                4.5,
                1.0,
                0.78,
                4,
                false,
                true,
                6.5,
                3.0,
                CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE,
                3.3,
                4.0
        );
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
