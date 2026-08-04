package com.kingdomcomecombat.ai;

import com.kingdomcomecombat.combat.CombatMovementConfig;
import com.kingdomcomecombat.compat.HundredYearsWarCompat;
import com.kingdomcomecombat.config.CombatServerConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.AbstractPiglinEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.registry.Registries;

public class HumanoidCombatAiProfiles {
    private static final HumanoidCombatAiProfile DEFAULT_HUMANOID =
            new HumanoidCombatAiProfile(
                    20,
                    0.20,
                    0.20,
                    0.04,
                    0,
                    0.0,
                    0.05,
                    0,
                    100.0,
                    0.30,
                    0.60,
                    0.40,
                    3.0,
                    1.0,
                    0.70,
                    3,
                    true,
                    false,
                    6.0,
                    3.25,
                    CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE,
                    3.0,
                    5.0
            );

    private static final Map<EntityType<?>, HumanoidCombatAiProfile> PROFILES =
            new HashMap<>();
    private static final Map<EntityType<?>, HumanoidCombatAiProfileSpec> PROFILE_SPECS =
            new HashMap<>();
    private static final Map<UUID, HumanoidCombatAiProfile> SAMPLED_PROFILES =
            new HashMap<>();
    private static final Map<AutoHealthBand, HumanoidCombatAiProfileSpec> AUTO_PROFILE_SPECS =
            new HashMap<>();
    private static final Set<EntityType<?>> AUTO_INCLUDED_TYPES = new java.util.HashSet<>();
    private static final Set<EntityType<?>> AUTO_EXCLUDED_TYPES = new java.util.HashSet<>();
    private static HumanoidCombatAiProfile zombieLeaderProfile =
            HumanoidCombatAiProfile.zombieLeaderDefault();
    private static HumanoidCombatAiProfileSpec zombieLeaderProfileSpec = null;

    private HumanoidCombatAiProfiles() {
    }

    public static void registerDefaults() {
        PROFILES.put(EntityType.SKELETON, HumanoidCombatAiProfile.skeletonDefault());
        PROFILES.put(EntityType.ZOMBIE, HumanoidCombatAiProfile.zombieDefault());
        PROFILES.put(EntityType.VINDICATOR, HumanoidCombatAiProfile.illagerMeleeDefault());
        PROFILES.put(EntityType.PILLAGER, HumanoidCombatAiProfile.illagerMeleeDefault());
    }

    public static void clear() {
        PROFILES.clear();
        PROFILE_SPECS.clear();
        SAMPLED_PROFILES.clear();
        AUTO_PROFILE_SPECS.clear();
        AUTO_INCLUDED_TYPES.clear();
        AUTO_EXCLUDED_TYPES.clear();
        zombieLeaderProfile = HumanoidCombatAiProfile.zombieLeaderDefault();
        zombieLeaderProfileSpec = null;
    }

    public static boolean hasProfile(LivingEntity entity) {
        return getProfile(entity) != null;
    }

    public static boolean hasConfiguredType(EntityType<?> entityType) {
        return entityType != null && (PROFILES.containsKey(entityType) || PROFILE_SPECS.containsKey(entityType));
    }

    public static Set<String> configuredEntityIds() {
        java.util.stream.Stream<EntityType<?>> types = java.util.stream.Stream.concat(
                PROFILES.keySet().stream(), PROFILE_SPECS.keySet().stream());
        return types.map(Registries.ENTITY_TYPE::getId)
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static HumanoidCombatAiProfile getProfile(LivingEntity entity) {
        if (!canUseHumanoidAi(entity)) {
            return null;
        }

        if (ZombieLeaderUtil.isLeader(entity)) {
            return zombieLeaderProfileSpec == null
                    ? zombieLeaderProfile
                    : sampledProfile(entity, zombieLeaderProfileSpec);
        }

        HumanoidCombatAiProfileSpec spec = PROFILE_SPECS.get(entity.getType());
        if (spec != null) {
            return sampledProfile(entity, spec);
        }

        HumanoidCombatAiProfile profile = PROFILES.get(entity.getType());
        if (profile != null) {
            return profile;
        }

        HumanoidCombatAiProfile hundredYearsWarProfile = HundredYearsWarCompat.getAiProfile(entity);
        if (hundredYearsWarProfile != null) {
            return hundredYearsWarProfile;
        }

        HumanoidCombatAiProfileSpec automaticSpec = automaticProfileSpec(entity);
        if (automaticSpec != null) {
            return sampledProfile(entity, automaticSpec);
        }

        // Subclass fallbacks are only defaults for vanilla entity types. Modded
        // subclasses must pass the stricter automatic-compatibility filter above.
        if (!isVanillaType(entity.getType())) {
            return null;
        }

        if (entity instanceof ZombieEntity) {
            HumanoidCombatAiProfileSpec zombieSpec = PROFILE_SPECS.get(EntityType.ZOMBIE);
            return zombieSpec == null
                    ? HumanoidCombatAiProfile.zombieDefault()
                    : sampledProfile(entity, zombieSpec);
        }

        if (entity instanceof AbstractSkeletonEntity) {
            HumanoidCombatAiProfileSpec skeletonSpec = PROFILE_SPECS.get(EntityType.SKELETON);
            return skeletonSpec == null
                    ? HumanoidCombatAiProfile.skeletonDefault()
                    : sampledProfile(entity, skeletonSpec);
        }

        if (entity.getType() == EntityType.SKELETON) {
            return HumanoidCombatAiProfile.skeletonDefault();
        }

        if (entity.getType() == EntityType.ZOMBIE) {
            return HumanoidCombatAiProfile.zombieDefault();
        }

        return null;
    }

    private static boolean canUseHumanoidAi(LivingEntity entity) {
        return true;
    }

    public static HumanoidCombatAiProfile defaultHumanoid() {
        return DEFAULT_HUMANOID;
    }

    public static double getToughness(LivingEntity entity) {
        HumanoidCombatAiProfile profile = getProfile(entity);
        return profile == null ? 0.0 : profile.toughness();
    }

    public static void setProfile(EntityType<?> entityType, HumanoidCombatAiProfile profile) {
        PROFILES.put(entityType, profile);
        PROFILE_SPECS.remove(entityType);
        SAMPLED_PROFILES.clear();
    }

    public static void setProfileSpec(EntityType<?> entityType, HumanoidCombatAiProfileSpec profile) {
        PROFILE_SPECS.put(entityType, profile);
        PROFILES.remove(entityType);
        SAMPLED_PROFILES.clear();
    }

    public static void setZombieLeaderProfile(HumanoidCombatAiProfile profile) {
        zombieLeaderProfile = profile;
        zombieLeaderProfileSpec = null;
        SAMPLED_PROFILES.clear();
    }

    public static void setZombieLeaderProfileSpec(HumanoidCombatAiProfileSpec profile) {
        zombieLeaderProfileSpec = profile;
        SAMPLED_PROFILES.clear();
    }

    public static void setAutomaticProfileSpec(
            AutoHealthBand band,
            HumanoidCombatAiProfileSpec profile
    ) {
        if (band != null && profile != null) {
            AUTO_PROFILE_SPECS.put(band, profile);
            SAMPLED_PROFILES.clear();
        }
    }

    public static void includeAutomaticType(EntityType<?> entityType) {
        if (entityType != null) {
            AUTO_INCLUDED_TYPES.add(entityType);
            AUTO_EXCLUDED_TYPES.remove(entityType);
        }
    }

    public static void excludeAutomaticType(EntityType<?> entityType) {
        if (entityType != null) {
            AUTO_EXCLUDED_TYPES.add(entityType);
            AUTO_INCLUDED_TYPES.remove(entityType);
        }
    }

    private static HumanoidCombatAiProfileSpec automaticProfileSpec(LivingEntity entity) {
        if (!CombatServerConfig.automaticHumanoidAiCompatibilityEnabled()
                || AUTO_PROFILE_SPECS.isEmpty()
                || AUTO_EXCLUDED_TYPES.contains(entity.getType())
                || (entity instanceof MobEntity mob
                && BeastCombatAiProfiles.getProfile(mob) != null)) {
            return null;
        }

        if (!AUTO_INCLUDED_TYPES.contains(entity.getType()) && !looksLikeModdedHumanoid(entity)) {
            return null;
        }
        return AUTO_PROFILE_SPECS.get(AutoHealthBand.forMaxHealth(entity.getMaxHealth()));
    }

    private static boolean looksLikeModdedHumanoid(LivingEntity entity) {
        if (isVanillaType(entity.getType())
                || !(entity instanceof MobEntity)) {
            return false;
        }

        // These server-visible families use skeleton, illager, or vanilla biped
        // renderers. A wholly custom entity class does not expose its renderer
        // on a dedicated server and must opt in through include_entities.
        return entity instanceof AbstractSkeletonEntity
                || entity instanceof IllagerEntity
                || entity instanceof ZombieEntity
                || entity instanceof AbstractPiglinEntity;
    }

    private static boolean isVanillaType(EntityType<?> entityType) {
        return "minecraft".equals(Registries.ENTITY_TYPE.getId(entityType).getNamespace());
    }

    public enum AutoHealthBand {
        UNDER_10,
        FROM_10_TO_20,
        FROM_20_TO_30,
        FROM_30_TO_40,
        OVER_40;

        public static AutoHealthBand forMaxHealth(float maxHealth) {
            if (maxHealth < 10.0F) return UNDER_10;
            if (maxHealth <= 20.0F) return FROM_10_TO_20;
            if (maxHealth <= 30.0F) return FROM_20_TO_30;
            if (maxHealth <= 40.0F) return FROM_30_TO_40;
            return OVER_40;
        }
    }

    public static HumanoidCombatAiProfile baseProfileFor(EntityType<?> entityType) {
        HumanoidCombatAiProfile profile = PROFILES.get(entityType);
        if (profile != null) {
            return profile;
        }
        if (entityType == EntityType.SKELETON) {
            return HumanoidCombatAiProfile.skeletonDefault();
        }
        if (entityType == EntityType.ZOMBIE) {
            return HumanoidCombatAiProfile.zombieDefault();
        }
        if (entityType == EntityType.VINDICATOR || entityType == EntityType.PILLAGER) {
            return HumanoidCombatAiProfile.illagerMeleeDefault();
        }
        return DEFAULT_HUMANOID;
    }

    public static void removeSample(UUID uuid) {
        SAMPLED_PROFILES.remove(uuid);
    }

    private static HumanoidCombatAiProfile sampledProfile(LivingEntity entity, HumanoidCombatAiProfileSpec spec) {
        return SAMPLED_PROFILES.computeIfAbsent(
                entity.getUuid(),
                uuid -> spec.sample(new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits()))
        );
    }
}
