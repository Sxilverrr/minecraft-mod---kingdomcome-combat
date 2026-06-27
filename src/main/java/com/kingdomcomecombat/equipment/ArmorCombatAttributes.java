package com.kingdomcomecombat.equipment;

import java.util.Map;

public record ArmorCombatAttributes(
        DamageTypeProfile defense,
        Map<String, PartProtection> protectedParts,
        double staminaCostIncrease,
        double attackSpeedPenalty,
        double movementSpeedPenalty
) {
    public ArmorCombatAttributes {
        protectedParts = Map.copyOf(protectedParts);
        staminaCostIncrease = Math.max(0.0, staminaCostIncrease);
        attackSpeedPenalty = Math.max(0.0, Math.min(0.95, attackSpeedPenalty));
        movementSpeedPenalty = Math.max(0.0, Math.min(0.95, movementSpeedPenalty));
    }

    public static ArmorCombatAttributes empty() {
        return new ArmorCombatAttributes(DamageTypeProfile.even(0.0), Map.of(), 0.0, 0.0, 0.0);
    }

    public record PartProtection(
            double protectionPercent,
            double impactMitigationPercent
    ) {
        public PartProtection {
            protectionPercent = Math.max(0.0, protectionPercent);
            impactMitigationPercent = clamp01(impactMitigationPercent);
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
