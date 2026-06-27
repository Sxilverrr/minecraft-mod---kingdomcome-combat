package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.equipment.DamageTypeProfile;

import java.util.List;

public record ComboMoveConfig(
        String id,
        String displayName,
        List<CombatDirection> sequence,
        DamageTypeProfile damageModifiers,
        String animationName,
        List<AttackMoveConfig.HitZoneRule> hitZoneRules,
        double impact,
        double staminaCost,
        double horizontalKnockback,
        boolean bladeTrail,
        boolean hitReaction,
        boolean suctionCombo,
        boolean lunges,
        boolean useRealHitbox,
        String injuryType,
        int injuryLevel,
        String requiredWeaponTag,
        String hitReactionAnimation,
        boolean hitReactionInterruptsAttack,
        int hitReactionMovementLockTicks,
        int weaponClashTick,
        String weaponClashSound,
        double suctionDistance,
        double suctionMinDistance,
        double suctionFixedDistance,
        String victimAnimationName,
        List<AttackChainEvent> attackChain,
        int level
) {
    public record AttackChainEvent(
            int tick,
            List<AttackMoveConfig.HitZoneRule> hitZoneRules,
            DamageTypeProfile damageModifiers,
            boolean weaponHit,
            String weaponClashSound
    ) {
        public AttackChainEvent {
            tick = Math.max(0, tick);
            hitZoneRules = List.copyOf(hitZoneRules);
            weaponClashSound = weaponClashSound == null ? "" : weaponClashSound;
        }
    }

    public ComboMoveConfig(
            String id,
            String displayName,
            List<CombatDirection> sequence,
            DamageTypeProfile damageModifiers,
            String animationName,
            List<AttackMoveConfig.HitZoneRule> hitZoneRules,
            double impact,
            double staminaCost,
            double horizontalKnockback,
            boolean bladeTrail,
            boolean hitReaction,
            boolean suctionCombo,
            boolean lunges,
            boolean useRealHitbox,
            String injuryType,
            int injuryLevel
    ) {
        this(
                id,
                displayName,
                sequence,
                damageModifiers,
                animationName,
                hitZoneRules,
                impact,
                staminaCost,
                horizontalKnockback,
                bladeTrail,
                hitReaction,
                suctionCombo,
                lunges,
                useRealHitbox,
                injuryType,
                injuryLevel,
                "",
                "",
                false,
                0,
                -1,
                "",
                0.0,
                0.0,
                1.15,
                "",
                List.of(),
                0
        );
    }

    public ComboMoveConfig {
        sequence = List.copyOf(sequence);
        hitZoneRules = List.copyOf(hitZoneRules);
        impact = Math.max(0.0, impact);
        staminaCost = Math.max(0.0, staminaCost);
        horizontalKnockback = Math.max(0.0, horizontalKnockback);
        injuryLevel = Math.max(0, Math.min(8, injuryLevel));
        requiredWeaponTag = requiredWeaponTag == null ? "" : requiredWeaponTag;
        hitReactionAnimation = hitReactionAnimation == null ? "" : hitReactionAnimation;
        hitReactionMovementLockTicks = Math.max(0, hitReactionMovementLockTicks);
        weaponClashTick = weaponClashTick < 0 ? -1 : weaponClashTick;
        weaponClashSound = weaponClashSound == null ? "" : weaponClashSound;
        suctionDistance = Math.max(0.0, suctionDistance);
        suctionMinDistance = Math.max(0.0, suctionMinDistance);
        suctionFixedDistance = Math.max(0.0, suctionFixedDistance);
        victimAnimationName = victimAnimationName == null ? "" : victimAnimationName;
        attackChain = List.copyOf(attackChain);
        level = Math.max(0, level);
    }
}
