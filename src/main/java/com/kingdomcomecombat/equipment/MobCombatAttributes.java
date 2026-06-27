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
        meleeHitZoneRules = List.copyOf(meleeHitZoneRules);
    }

    public enum MeleeDefenseTier {
        BLOCKABLE,
        SHIELD_BLOCKABLE,
        DODGEABLE
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
