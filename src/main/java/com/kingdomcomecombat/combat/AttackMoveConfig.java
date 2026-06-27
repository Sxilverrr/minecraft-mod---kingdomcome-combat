package com.kingdomcomecombat.combat;

import java.util.List;

public record AttackMoveConfig(
        String id,
        double strikeModifier,
        double slashModifier,
        double thrustModifier,
        List<HitZoneRule> hitZoneRules,
        double impact,
        double staminaCost,
        boolean useRealHitbox,
        String animationName,
        int transitionTicks,
        int directHitTick,
        int weaponClashTick,
        String weaponClashSound,
        double masterCounterSpacing,
        double horizontalKnockback,
        boolean hitReaction,
        List<HeightPartRule> directHitHeightParts

) {
    public AttackMoveConfig(
            String id,
            double strikeModifier,
            double slashModifier,
            double thrustModifier,
            List<HitZoneRule> hitZoneRules,
            double impact,
            double staminaCost,
            boolean useRealHitbox,
            String animationName
    ) {
        this(
                id,
                strikeModifier,
                slashModifier,
                thrustModifier,
                hitZoneRules,
                impact,
                staminaCost,
                useRealHitbox,
                animationName,
                CombatTiming.LIGHT_ATTACK_TRANSITION_TICKS,
                -1,
                -1,
                "",
                1.4,
                CombatControlConfig.DEFAULT_ATTACK_HORIZONTAL_KNOCKBACK,
                true,
                List.of()
        );
    }

    public AttackMoveConfig(
            String id,
            double strikeModifier,
            double slashModifier,
            double thrustModifier,
            List<HitZoneRule> hitZoneRules,
            double impact,
            double staminaCost,
            boolean useRealHitbox,
            String animationName,
            int transitionTicks,
            int directHitTick
    ) {
        this(
                id,
                strikeModifier,
                slashModifier,
                thrustModifier,
                hitZoneRules,
                impact,
                staminaCost,
                useRealHitbox,
                animationName,
                transitionTicks,
                directHitTick,
                -1,
                "",
                1.4,
                CombatControlConfig.DEFAULT_ATTACK_HORIZONTAL_KNOCKBACK,
                true,
                List.of()
        );
    }

    public AttackMoveConfig(
            String id,
            double strikeModifier,
            double slashModifier,
            double thrustModifier,
            List<HitZoneRule> hitZoneRules,
            double impact,
            double staminaCost,
            boolean useRealHitbox,
            String animationName,
            int transitionTicks
    ) {
        this(
                id,
                strikeModifier,
                slashModifier,
                thrustModifier,
                hitZoneRules,
                impact,
                staminaCost,
                useRealHitbox,
                animationName,
                transitionTicks,
                -1,
                -1,
                "",
                1.4,
                CombatControlConfig.DEFAULT_ATTACK_HORIZONTAL_KNOCKBACK,
                true,
                List.of()
        );
    }

    public AttackMoveConfig(
            String id,
            double strikeModifier,
            double slashModifier,
            double thrustModifier,
            List<HitZoneRule> hitZoneRules,
            double impact,
            double staminaCost,
            boolean useRealHitbox,
            String animationName,
            int transitionTicks,
            int directHitTick,
            int weaponClashTick,
            String weaponClashSound,
            double masterCounterSpacing,
            double horizontalKnockback,
            boolean hitReaction
    ) {
        this(
                id,
                strikeModifier,
                slashModifier,
                thrustModifier,
                hitZoneRules,
                impact,
                staminaCost,
                useRealHitbox,
                animationName,
                transitionTicks,
                directHitTick,
                weaponClashTick,
                weaponClashSound,
                masterCounterSpacing,
                horizontalKnockback,
                hitReaction,
                List.of()
        );
    }

    public AttackMoveConfig {
        id = id == null || id.isBlank() ? "attack" : id;
        strikeModifier = Math.max(0.0, strikeModifier);
        slashModifier = Math.max(0.0, slashModifier);
        thrustModifier = Math.max(0.0, thrustModifier);
        hitZoneRules = List.copyOf(hitZoneRules);
        impact = Math.max(0.0, impact);
        staminaCost = Math.max(0.0, staminaCost);
        animationName = animationName == null || animationName.isBlank() ? "" : animationName;
        transitionTicks = Math.max(1, transitionTicks);
        directHitTick = directHitTick < 0 ? -1 : directHitTick;
        weaponClashTick = weaponClashTick < 0 ? -1 : weaponClashTick;
        weaponClashSound = weaponClashSound == null ? "" : weaponClashSound;
        masterCounterSpacing = Math.max(0.3, Math.min(3.0, masterCounterSpacing));
        horizontalKnockback = Math.max(0.0, Math.min(1.0, horizontalKnockback));
        directHitHeightParts = directHitHeightParts == null ? List.of() : List.copyOf(directHitHeightParts);
    }

    public record HitZoneRule(
            HitZone whenHit,
            List<String> detailedParts
    ) {
        public HitZoneRule {
            detailedParts = List.copyOf(detailedParts);
        }
    }

    public enum HitZone {
        UPPER,
        MIDDLE,
        LOWER,
        HEAD,
        SHOULDERS,
        BODY,
        LEFT_ARM,
        RIGHT_ARM,
        ARMS,
        LEFT_LEG,
        RIGHT_LEG,
        LEGS,
        ANY
    }

    public record HeightPartRule(
            double maxHeight,
            String detailedPart
    ) {
        public HeightPartRule {
            maxHeight = Math.max(0.0, Math.min(1.0, maxHeight));
            detailedPart = detailedPart == null ? "" : detailedPart;
        }
    }
}
