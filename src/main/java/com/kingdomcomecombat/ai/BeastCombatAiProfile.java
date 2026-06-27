package com.kingdomcomecombat.ai;

public record BeastCombatAiProfile(
        Mode mode,
        double combatEnterDistance,
        double attackDistance,
        int minAttackIntervalTicks,
        double attackAnimationSpeed,
        String attackMoveId,
        String attackAnimationName,
        double minDistance,
        double attackMinDistance,
        double circleDistance,
        double circleSpeedMin,
        double circleSpeedMax,
        int circleMinTicks,
        int circleMaxTicks,
        double lungeSpeed,
        double chaseDistance,
        int holdDistanceTicks,
        boolean canLunge,
        boolean jumpAttack,
        double jumpVelocity,
        boolean alwaysApproachUntilClose,
        double approachSpeed,
        double pursueSpeed,
        double breakOffDistance,
        int maxChainAttacks
) {
    public BeastCombatAiProfile {
        mode = mode == null ? Mode.CIRCLE_LUNGE : mode;
        combatEnterDistance = Math.max(0.0, combatEnterDistance);
        attackDistance = Math.max(0.1, attackDistance);
        minAttackIntervalTicks = Math.max(0, minAttackIntervalTicks);
        attackAnimationSpeed = Math.max(0.05, attackAnimationSpeed);
        attackMoveId = attackMoveId == null ? "" : attackMoveId;
        attackAnimationName = attackAnimationName == null ? "" : attackAnimationName;
        minDistance = Math.max(0.0, minDistance);
        attackMinDistance = Math.max(0.0, Math.min(minDistance, attackMinDistance));
        circleDistance = Math.max(0.1, circleDistance);
        circleSpeedMin = Math.max(0.0, circleSpeedMin);
        circleSpeedMax = Math.max(circleSpeedMin, circleSpeedMax);
        circleMinTicks = Math.max(1, circleMinTicks);
        circleMaxTicks = Math.max(circleMinTicks, circleMaxTicks);
        lungeSpeed = Math.max(0.0, lungeSpeed);
        chaseDistance = Math.max(attackDistance, chaseDistance);
        holdDistanceTicks = Math.max(0, holdDistanceTicks);
        jumpVelocity = Math.max(0.0, jumpVelocity);
        approachSpeed = Math.max(0.0, approachSpeed);
        pursueSpeed = Math.max(0.0, pursueSpeed);
        breakOffDistance = Math.max(attackDistance, breakOffDistance);
        maxChainAttacks = Math.max(1, maxChainAttacks);
    }

    public boolean usesCustomAttackMove() {
        return !attackMoveId.isBlank() || !attackAnimationName.isBlank();
    }

    public enum Mode {
        CIRCLE_LUNGE,
        HOLD_AND_RUSH
    }
}
