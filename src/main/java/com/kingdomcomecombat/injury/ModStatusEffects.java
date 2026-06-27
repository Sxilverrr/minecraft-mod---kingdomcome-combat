package com.kingdomcomecombat.injury;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.game.ModGameRules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModStatusEffects {
    public static final RegistryEntry<StatusEffect> ARM_INJURY =
            register("arm_injury", 0x8A2D2D);
    public static final RegistryEntry<StatusEffect> HAND_INJURY =
            register("hand_injury", 0x9B3B30);
    public static final RegistryEntry<StatusEffect> TORSO_INJURY =
            register("torso_injury", 0x7D2424);
    public static final RegistryEntry<StatusEffect> LEG_INJURY =
            register("leg_injury", 0x6B3030);
    public static final RegistryEntry<StatusEffect> HEAD_INJURY =
            register("head_injury", 0x4A264F);
    public static final RegistryEntry<StatusEffect> BLEEDING =
            register("bleeding", 0xB00020);
    public static final RegistryEntry<StatusEffect> ANALGESIA =
            register("analgesia", 0x6F8193);

    private static final Identifier TORSO_HEALTH_PENALTY_ID =
            Identifier.of(KingdomComeCombat.MOD_ID, "torso_injury_max_health");
    private static final Identifier LEG_SPEED_PENALTY_ID =
            Identifier.of(KingdomComeCombat.MOD_ID, "leg_injury_speed");
    private static final Identifier LEG_JUMP_PENALTY_ID =
            Identifier.of(KingdomComeCombat.MOD_ID, "leg_injury_jump");

    private static final List<RegistryEntry<StatusEffect>> INJURIES = List.of(
            ARM_INJURY,
            HAND_INJURY,
            TORSO_INJURY,
            LEG_INJURY,
            HEAD_INJURY,
            BLEEDING
    );

    private static final List<RegistryEntry<StatusEffect>> WOUND_INJURIES = List.of(
            ARM_INJURY,
            HAND_INJURY,
            TORSO_INJURY,
            LEG_INJURY,
            HEAD_INJURY
    );

    private ModStatusEffects() {
    }

    public static void registerAll() {
    }

    public static void applyInjury(LivingEntity entity, String type, int level) {
        if (!ModGameRules.traumaEnabled(entity)) {
            return;
        }
        RegistryEntry<StatusEffect> effect = effectFor(type);
        if (effect == null || level <= 0) {
            return;
        }

        int current = 0;
        StatusEffectInstance existing = entity.getStatusEffect(effect);
        if (existing != null) {
            current = existing.getAmplifier() + 1;
        }

        int nextLevel = Math.max(1, Math.min(8, current + level));
        entity.addStatusEffect(new StatusEffectInstance(
                effect,
                StatusEffectInstance.INFINITE,
                nextLevel - 1,
                false,
                true,
                true
        ));
        syncEffectiveAttributeModifiers(entity);
        clampHealthToMax(entity);
    }

    public static void clearInjuries(LivingEntity entity) {
        for (RegistryEntry<StatusEffect> effect : INJURIES) {
            entity.removeStatusEffect(effect);
        }
        syncEffectiveAttributeModifiers(entity);
    }

    public static int level(LivingEntity entity, RegistryEntry<StatusEffect> effect) {
        StatusEffectInstance instance = entity.getStatusEffect(effect);
        return instance == null ? 0 : instance.getAmplifier() + 1;
    }

    public static int effectiveLevel(LivingEntity entity, RegistryEntry<StatusEffect> effect) {
        int rawLevel = level(entity, effect);
        if (rawLevel <= 0 || effect == BLEEDING || effect == ANALGESIA) {
            return rawLevel;
        }
        return Math.max(0, rawLevel - level(entity, ANALGESIA));
    }

    public static int totalWoundLevels(LivingEntity entity) {
        int total = 0;
        for (RegistryEntry<StatusEffect> effect : WOUND_INJURIES) {
            total += level(entity, effect);
        }
        return total;
    }

    public static int totalEffectiveWoundLevels(LivingEntity entity) {
        int total = 0;
        for (RegistryEntry<StatusEffect> effect : WOUND_INJURIES) {
            total += effectiveLevel(entity, effect);
        }
        return total;
    }

    public static boolean hasBleeding(LivingEntity entity) {
        return level(entity, BLEEDING) > 0;
    }

    public static boolean reduceFirstWoundAtOrBelow(LivingEntity entity, int maxLevel) {
        for (RegistryEntry<StatusEffect> effect : WOUND_INJURIES) {
            int current = level(entity, effect);
            if (current > 0 && current <= maxLevel && reduceInjury(entity, effect, 1)) {
                return true;
            }
        }
        return false;
    }

    public static int reduceWoundsAtOrBelow(LivingEntity entity, int maxLevel, int budget) {
        int healed = 0;
        while (healed < budget && reduceFirstWoundAtOrBelow(entity, maxLevel)) {
            healed++;
        }
        return healed;
    }

    public static boolean reduceInjury(LivingEntity entity, RegistryEntry<StatusEffect> effect, int level) {
        if (level <= 0) {
            return false;
        }

        int current = level(entity, effect);
        if (current <= 0) {
            return false;
        }

        int nextLevel = current - level;
        if (nextLevel <= 0) {
            entity.removeStatusEffect(effect);
            syncEffectiveAttributeModifiers(entity);
            clampHealthToMax(entity);
            return true;
        }

        entity.removeStatusEffect(effect);
        entity.addStatusEffect(new StatusEffectInstance(
                effect,
                StatusEffectInstance.INFINITE,
                nextLevel - 1,
                false,
                true,
                true
        ));
        syncEffectiveAttributeModifiers(entity);
        clampHealthToMax(entity);
        return true;
    }

    public static void syncEffectiveAttributeModifiers(LivingEntity entity) {
        EntityAttributeInstance maxHealth = entity.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(TORSO_HEALTH_PENALTY_ID);
            int torsoLevel = effectiveLevel(entity, TORSO_INJURY);
            if (torsoLevel > 0) {
                maxHealth.addTemporaryModifier(new EntityAttributeModifier(
                        TORSO_HEALTH_PENALTY_ID,
                        -torsoLevel,
                        EntityAttributeModifier.Operation.ADD_VALUE
                ));
            }
        }

        EntityAttributeInstance movementSpeed = entity.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(LEG_SPEED_PENALTY_ID);
            int legLevel = effectiveLevel(entity, LEG_INJURY);
            if (legLevel > 0) {
                movementSpeed.addTemporaryModifier(new EntityAttributeModifier(
                        LEG_SPEED_PENALTY_ID,
                        -0.04 * legLevel,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
            }
        }

        EntityAttributeInstance jumpStrength = entity.getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
        if (jumpStrength != null) {
            jumpStrength.removeModifier(LEG_JUMP_PENALTY_ID);
            int legLevel = effectiveLevel(entity, LEG_INJURY);
            if (legLevel >= 6) {
                jumpStrength.addTemporaryModifier(new EntityAttributeModifier(
                        LEG_JUMP_PENALTY_ID,
                        -0.60,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
            }
        }

        if (entity instanceof PlayerEntity player
                && effectiveLevel(player, LEG_INJURY) >= 5
                && player.isSprinting()) {
            player.setSprinting(false);
        }
        clampHealthToMax(entity);
    }

    private static void clampHealthToMax(LivingEntity entity) {
        float maxHealth = entity.getMaxHealth();
        if (entity.getHealth() > maxHealth) {
            entity.setHealth(maxHealth);
        }
    }

    private static RegistryEntry<StatusEffect> effectFor(String type) {
        return switch (type == null ? "" : type.toLowerCase()) {
            case "arm", "arms", "手臂" -> ARM_INJURY;
            case "hand", "hands", "手部" -> HAND_INJURY;
            case "torso", "body", "躯干" -> TORSO_INJURY;
            case "leg", "legs", "腿部" -> LEG_INJURY;
            case "head", "头部" -> HEAD_INJURY;
            case "bleeding", "bleed", "流血" -> BLEEDING;
            case "analgesia", "painkiller", "镇痛" -> ANALGESIA;
            default -> null;
        };
    }

    private static RegistryEntry<StatusEffect> register(String id, int color) {
        StatusEffect effect = new InjuryStatusEffect(color);
        return Registry.registerReference(
                Registries.STATUS_EFFECT,
                Identifier.of(KingdomComeCombat.MOD_ID, id),
                effect
        );
    }
}
