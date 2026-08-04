package com.kingdomcomecombat.stamina;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import com.kingdomcomecombat.injury.ModStatusEffects;
import com.kingdomcomecombat.passive.PassiveSkillPerks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaminaMaxProvider {
    private static final Map<EntityType<?>, Double> ENTITY_TYPE_MAX_STAMINA = new HashMap<>();
    private static final Map<UUID, Double> ENTITY_MAX_STAMINA = new HashMap<>();
    private static final Map<EntityType<?>, Double> ENTITY_TYPE_REGEN_PER_TICK = new HashMap<>();
    private static final Map<UUID, Double> ENTITY_REGEN_PER_TICK = new HashMap<>();

    private StaminaMaxProvider() {
    }

    public static double getMaxStamina(LivingEntity entity) {
        double headInjuryMultiplier = Math.max(
                0.0,
                1.0 - 0.07 * ModStatusEffects.effectiveLevel(entity, ModStatusEffects.HEAD_INJURY)
        );
        return (getBaseMaxStamina(entity) + PassiveSkillPerks.maxStaminaBonus(entity))
                * getHealthMaxMultiplier(entity) * headInjuryMultiplier;
    }

    public static double getBaseMaxStamina(LivingEntity entity) {
        Double entityOverride = ENTITY_MAX_STAMINA.get(entity.getUuid());
        if (entityOverride != null) {
            return entityOverride;
        }

        Double typeOverride = ENTITY_TYPE_MAX_STAMINA.get(entity.getType());
        if (typeOverride != null) {
            return typeOverride;
        }

        if (entity instanceof PlayerEntity) {
            return StaminaConfig.DEFAULT_PLAYER_MAX_STAMINA;
        }

        return StaminaConfig.DEFAULT_MOB_MAX_STAMINA;
    }

    public static double getOriginalMaxStamina(LivingEntity entity) {
        return Math.max(1.0, getBaseMaxStamina(entity) + PassiveSkillPerks.maxStaminaBonus(entity));
    }

    private static double getHealthMaxMultiplier(LivingEntity entity) {
        float maxHealth = entity.getMaxHealth();
        if (maxHealth <= 0.0001F) {
            return 1.0;
        }

        double healthPercent = Math.max(0.0, Math.min(1.0, entity.getHealth() / maxHealth));
        return Math.max(0.60, healthPercent);
    }

    public static double getRegenPerTick(LivingEntity entity) {
        Double entityOverride = ENTITY_REGEN_PER_TICK.get(entity.getUuid());
        if (entityOverride != null) {
            return entityOverride * PassiveSkillPerks.staminaRegenMultiplier(entity);
        }

        Double typeOverride = ENTITY_TYPE_REGEN_PER_TICK.get(entity.getType());
        if (typeOverride != null) {
            return typeOverride * PassiveSkillPerks.staminaRegenMultiplier(entity);
        }

        double torsoMultiplier = Math.max(
                0.0,
                1.0 - 0.05 * ModStatusEffects.effectiveLevel(entity, ModStatusEffects.TORSO_INJURY)
        );
        return StaminaConfig.DEFAULT_REGEN_PER_TICK
                * torsoMultiplier
                * PassiveSkillPerks.staminaRegenMultiplier(entity);
    }

    public static void setEntityTypeMaxStamina(EntityType<?> entityType, double maxStamina) {
        ENTITY_TYPE_MAX_STAMINA.put(entityType, sanitizeMax(maxStamina));
    }

    public static void clearEntityTypeMaxStamina(EntityType<?> entityType) {
        ENTITY_TYPE_MAX_STAMINA.remove(entityType);
    }

    public static void setEntityTypeRegenPerTick(EntityType<?> entityType, double regenPerTick) {
        ENTITY_TYPE_REGEN_PER_TICK.put(entityType, sanitizeRegen(regenPerTick));
    }

    public static void clearEntityTypeRegenPerTick(EntityType<?> entityType) {
        ENTITY_TYPE_REGEN_PER_TICK.remove(entityType);
    }

    public static void setEntityMaxStamina(UUID entityUuid, double maxStamina) {
        ENTITY_MAX_STAMINA.put(entityUuid, sanitizeMax(maxStamina));
    }

    public static void clearEntityMaxStamina(UUID entityUuid) {
        ENTITY_MAX_STAMINA.remove(entityUuid);
    }

    public static void setEntityRegenPerTick(UUID entityUuid, double regenPerTick) {
        ENTITY_REGEN_PER_TICK.put(entityUuid, sanitizeRegen(regenPerTick));
    }

    public static void clearEntityRegenPerTick(UUID entityUuid) {
        ENTITY_REGEN_PER_TICK.remove(entityUuid);
    }

    private static double sanitizeMax(double maxStamina) {
        return Math.max(1.0, maxStamina);
    }

    private static double sanitizeRegen(double regenPerTick) {
        return Math.max(0.0, regenPerTick);
    }
}
