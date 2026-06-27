package com.kingdomcomecombat.equipment;

public record RangedWeaponAttributes(
        double drawSpeed,
        double hardness,
        double projectileSpeed
) {
    public static final RangedWeaponAttributes DEFAULT = new RangedWeaponAttributes(1.0, 1.0, 1.0);

    public RangedWeaponAttributes {
        drawSpeed = Math.max(0.01, drawSpeed);
        hardness = Math.max(0.0, hardness);
        projectileSpeed = Math.max(0.0, projectileSpeed);
    }
}
