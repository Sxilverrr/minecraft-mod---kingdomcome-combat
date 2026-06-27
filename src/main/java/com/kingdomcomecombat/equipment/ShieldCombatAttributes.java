package com.kingdomcomecombat.equipment;

public record ShieldCombatAttributes(
        Size size,
        double blockImpactMitigation
) {
    public ShieldCombatAttributes {
        size = size == null ? Size.SMALL : size;
        blockImpactMitigation = Math.max(0.0, Math.min(1.0, blockImpactMitigation));
    }

    public enum Size {
        SMALL,
        LARGE
    }
}
