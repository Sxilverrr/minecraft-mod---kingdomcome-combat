package com.kingdomcomecombat.equipment;

public record ShieldCombatAttributes(
        Size size,
        double blockImpactMitigation,
        double attackStaminaCostMultiplier,
        double dodgeStaminaCostMultiplier,
        double lockedMovementSpeedMultiplier,
        double attackSpeedMultiplier,
        double attackLungeMultiplier,
        int exhaustedDisableTicks,
        double blockingAngleMultiplier,
        double perfectWindowMultiplier,
        int perfectAttackerDisableBonusTicks
) {
    public ShieldCombatAttributes {
        size = size == null ? Size.SMALL : size;
        blockImpactMitigation = Math.max(0.0, Math.min(1.0, blockImpactMitigation));
        attackStaminaCostMultiplier = Math.max(0.0, attackStaminaCostMultiplier);
        dodgeStaminaCostMultiplier = Math.max(0.0, dodgeStaminaCostMultiplier);
        lockedMovementSpeedMultiplier = Math.max(0.0, lockedMovementSpeedMultiplier);
        attackSpeedMultiplier = Math.max(0.0, attackSpeedMultiplier);
        attackLungeMultiplier = Math.max(0.0, attackLungeMultiplier);
        exhaustedDisableTicks = Math.max(0, exhaustedDisableTicks);
        blockingAngleMultiplier = blockingAngleMultiplier <= 0.0 ? 1.0 : blockingAngleMultiplier;
        perfectWindowMultiplier = perfectWindowMultiplier <= 0.0 ? 1.0 : perfectWindowMultiplier;
        perfectAttackerDisableBonusTicks = Math.max(0, perfectAttackerDisableBonusTicks);
    }

    public enum Size {
        SMALL,
        LARGE
    }
}
