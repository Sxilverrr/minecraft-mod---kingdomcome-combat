package com.kingdomcomecombat.equipment;

import net.minecraft.util.math.Vec3d;

import java.util.Map;

public record WeaponCombatAttributes(
        DamageTypeProfile damagePanel,
        double blockImpactMitigation,
        double baseImpact,
        double armorBreakMultiplier,
        double attackSpeedMultiplier,
        Vec3d realHitboxSizeUnits,
        Vec3d realHitboxOffsetUnits,
        Vec3d realHitboxRotationDegrees,
        Map<String, String> attackMoveIds,
        Map<String, String> stanceAnimationNames,
        Map<String, String> executionMoveIds,
        double weaponToughness,
        double minimumDurabilityPanelMultiplier,
        double heldMovementSpeedMultiplier
) {
    public WeaponCombatAttributes(
            DamageTypeProfile damagePanel,
            double blockImpactMitigation,
            double baseImpact,
            double armorBreakMultiplier,
            double attackSpeedMultiplier,
            Vec3d realHitboxSizeUnits,
            Vec3d realHitboxOffsetUnits,
            Vec3d realHitboxRotationDegrees,
            Map<String, String> attackMoveIds,
            Map<String, String> stanceAnimationNames,
            double weaponToughness,
            double minimumDurabilityPanelMultiplier,
            double heldMovementSpeedMultiplier
    ) {
        this(
                damagePanel,
                blockImpactMitigation,
                baseImpact,
                armorBreakMultiplier,
                attackSpeedMultiplier,
                realHitboxSizeUnits,
                realHitboxOffsetUnits,
                realHitboxRotationDegrees,
                attackMoveIds,
                stanceAnimationNames,
                Map.of(),
                weaponToughness,
                minimumDurabilityPanelMultiplier,
                heldMovementSpeedMultiplier
        );
    }

    public WeaponCombatAttributes(
            DamageTypeProfile damagePanel,
            double blockImpactMitigation,
            double baseImpact,
            double armorBreakMultiplier,
            double attackSpeedMultiplier,
            Vec3d realHitboxSizeUnits,
            Vec3d realHitboxOffsetUnits,
            Vec3d realHitboxRotationDegrees,
            Map<String, String> attackMoveIds,
            Map<String, String> stanceAnimationNames,
            double weaponToughness,
            double minimumDurabilityPanelMultiplier
    ) {
        this(
                damagePanel, blockImpactMitigation, baseImpact, armorBreakMultiplier,
                attackSpeedMultiplier, realHitboxSizeUnits, realHitboxOffsetUnits,
                realHitboxRotationDegrees, attackMoveIds, stanceAnimationNames,
                Map.of(),
                weaponToughness, minimumDurabilityPanelMultiplier,
                EquipmentFallbackConfig.defaultHeldMovementSpeedMultiplier()
        );
    }

    public WeaponCombatAttributes(
            DamageTypeProfile damagePanel,
            double blockImpactMitigation,
            double baseImpact,
            double armorBreakMultiplier,
            double attackSpeedMultiplier,
            Vec3d realHitboxSizeUnits,
            Vec3d realHitboxOffsetUnits,
            Vec3d realHitboxRotationDegrees,
            Map<String, String> attackMoveIds,
            Map<String, String> stanceAnimationNames
    ) {
        this(
                damagePanel, blockImpactMitigation, baseImpact, armorBreakMultiplier,
                attackSpeedMultiplier, realHitboxSizeUnits, realHitboxOffsetUnits,
                realHitboxRotationDegrees, attackMoveIds, stanceAnimationNames,
                Map.of(),
                EquipmentFallbackConfig.defaultWeaponToughness(),
                EquipmentFallbackConfig.defaultMinimumDurabilityPanelMultiplier(),
                EquipmentFallbackConfig.defaultHeldMovementSpeedMultiplier()
        );
    }

    public WeaponCombatAttributes(DamageTypeProfile damagePanel) {
        this(damagePanel, EquipmentFallbackConfig.defaultWeaponBlockImpactMitigation(), EquipmentFallbackConfig.defaultWeaponBaseImpact());
    }

    public WeaponCombatAttributes(DamageTypeProfile damagePanel, double blockImpactMitigation) {
        this(damagePanel, blockImpactMitigation, EquipmentFallbackConfig.defaultWeaponBaseImpact());
    }

    public WeaponCombatAttributes(DamageTypeProfile damagePanel, double blockImpactMitigation, double baseImpact) {
        this(damagePanel, blockImpactMitigation, baseImpact, 1.0, 1.0, Vec3d.ZERO);
    }

    public WeaponCombatAttributes(
            DamageTypeProfile damagePanel,
            double blockImpactMitigation,
            Vec3d realHitboxSizeUnits
    ) {
        this(damagePanel, blockImpactMitigation, EquipmentFallbackConfig.defaultWeaponBaseImpact(), 1.0, 1.0, realHitboxSizeUnits);
    }

    public WeaponCombatAttributes(
            DamageTypeProfile damagePanel,
            double blockImpactMitigation,
            double baseImpact,
            double armorBreakMultiplier,
            double attackSpeedMultiplier,
            Vec3d realHitboxSizeUnits
    ) {
        this(
                damagePanel,
                blockImpactMitigation,
                baseImpact,
                armorBreakMultiplier,
                attackSpeedMultiplier,
                realHitboxSizeUnits,
                Vec3d.ZERO,
                Vec3d.ZERO,
                Map.of(),
                Map.of()
        );
    }

    public WeaponCombatAttributes {
        blockImpactMitigation = Math.max(0.0, Math.min(1.0, blockImpactMitigation));
        baseImpact = Math.max(0.0, baseImpact);
        armorBreakMultiplier = Math.max(0.0, armorBreakMultiplier);
        attackSpeedMultiplier = Math.max(0.05, attackSpeedMultiplier);
        realHitboxSizeUnits = realHitboxSizeUnits == null ? Vec3d.ZERO : realHitboxSizeUnits;
        realHitboxOffsetUnits = realHitboxOffsetUnits == null ? Vec3d.ZERO : realHitboxOffsetUnits;
        realHitboxRotationDegrees = realHitboxRotationDegrees == null ? Vec3d.ZERO : realHitboxRotationDegrees;
        attackMoveIds = attackMoveIds == null ? Map.of() : Map.copyOf(attackMoveIds);
        stanceAnimationNames = stanceAnimationNames == null ? Map.of() : Map.copyOf(stanceAnimationNames);
        executionMoveIds = executionMoveIds == null ? Map.of() : Map.copyOf(executionMoveIds);
        weaponToughness = Math.max(0.0, weaponToughness);
        minimumDurabilityPanelMultiplier = Math.max(0.0, Math.min(1.0, minimumDurabilityPanelMultiplier));
        heldMovementSpeedMultiplier = Math.max(0.0, Math.min(1.0, heldMovementSpeedMultiplier));
    }

    public static WeaponCombatAttributes defaultWeapon() {
        return new WeaponCombatAttributes(DamageTypeProfile.even(1.0));
    }

    public boolean hasCustomRealHitboxSize() {
        return realHitboxSizeUnits.x > 0.0
                && realHitboxSizeUnits.y > 0.0
                && realHitboxSizeUnits.z > 0.0;
    }

    public boolean hasCustomRealHitboxOffset() {
        return realHitboxOffsetUnits.lengthSquared() > 0.0;
    }

    public boolean hasCustomRealHitboxRotation() {
        return realHitboxRotationDegrees.lengthSquared() > 0.0;
    }

    public String attackMoveId(String directionKey) {
        return attackMoveIds.getOrDefault(directionKey, "");
    }

    public String stanceAnimationName(String directionKey) {
        return stanceAnimationNames.getOrDefault(directionKey, "");
    }

    public String executionMoveId(String directionKey) {
        return executionMoveIds.getOrDefault(directionKey, "");
    }
}
