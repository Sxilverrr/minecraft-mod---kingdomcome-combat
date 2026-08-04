package com.kingdomcomecombat.equipment;

import com.kingdomcomecombat.combat.AttackMoveConfig;

import java.util.List;

public record MobCombatAttributes(
        NaturalArmor headArmor,
        NaturalArmor bodyArmor,
        double maxHealth,
        double maxStamina,
        double staminaRegenPerTick,
        double meleeImpact,
        double meleeKnockback,
        double meleeVerticalKnockback,
        MeleeDefenseTier meleeDefenseTier,
        double blockedTrueStaminaDamage,
        double blockedAttackerKnockback,
        AttackBehavior attackBehavior,
        DamageTypeProfile meleeDamageModifiers,
        List<AttackMoveConfig.HitZoneRule> meleeHitZoneRules
) {
    public MobCombatAttributes {
        maxHealth = Math.max(0.0, maxHealth);
        maxStamina = Math.max(0.0, maxStamina);
        staminaRegenPerTick = Math.max(0.0, staminaRegenPerTick);
        meleeImpact = Math.max(0.0, meleeImpact);
        meleeKnockback = Math.max(0.0, meleeKnockback);
        meleeVerticalKnockback = Math.max(0.0, meleeVerticalKnockback);
        blockedTrueStaminaDamage = Math.max(0.0, blockedTrueStaminaDamage);
        blockedAttackerKnockback = Math.max(0.0, blockedAttackerKnockback);
        meleeHitZoneRules = List.copyOf(meleeHitZoneRules);
    }

    public enum MeleeDefenseTier {
        BLOCKABLE,
        PERFECT_BLOCK_ONLY,
        SHIELD_BLOCKABLE,
        SHIELD_PERFECT_BLOCK_ONLY,
        UNBLOCKABLE
    }

    public record AttackBehavior(
            Mode mode,
            int windupTicks,
            int cooldownTicks,
            int activeTicks,
            double startDistance,
            double speed,
            double jumpVelocity,
            boolean airborneLunge,
            MeleeDefenseTier lungeDefenseTier,
            MeleeDefenseTier jumpDefenseTier
    ) {
        public AttackBehavior {
            windupTicks = Math.max(0, windupTicks);
            cooldownTicks = Math.max(1, cooldownTicks);
            activeTicks = Math.max(1, activeTicks);
            startDistance = Math.max(0.1, startDistance);
            speed = Math.max(0.0, speed);
            jumpVelocity = Math.max(0.0, jumpVelocity);
        }
        public static AttackBehavior vanilla() {
            return new AttackBehavior(Mode.VANILLA, 0, 20, 1, 2.0, 0.0, 0.0, false,
                    MeleeDefenseTier.BLOCKABLE, MeleeDefenseTier.BLOCKABLE);
        }
        public enum Mode { VANILLA, LUNGE, JUMP, MIXED }
    }

    public record NaturalArmor(
            DamageTypeProfile defense,
            double impactMitigation,
            int durability
    ) {
        public NaturalArmor {
            impactMitigation = Math.max(0.0, Math.min(0.95, impactMitigation));
            durability = Math.max(0, durability);
        }
    }
}
