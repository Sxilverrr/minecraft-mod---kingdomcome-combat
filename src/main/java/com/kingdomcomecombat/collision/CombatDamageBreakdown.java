package com.kingdomcomecombat.collision;

public record CombatDamageBreakdown(
        double damage,
        double incomingThrust,
        double incomingStrike,
        double incomingSlash,
        double impactMitigation,
        double reduction
) {
}
