package com.kingdomcomecombat.ai;

import com.kingdomcomecombat.combat.CombatMovementConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class HumanoidCombatAiProfiles {
    private static final HumanoidCombatAiProfile DEFAULT_HUMANOID =
            new HumanoidCombatAiProfile(
                    20,
                    0.20,
                    0.20,
                    0.04,
                    0,
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
    private static HumanoidCombatAiProfile zombieLeaderProfile =
            HumanoidCombatAiProfile.zombieLeaderDefault();
    private static HumanoidCombatAiProfileSpec zombieLeaderProfileSpec = null;

    private HumanoidCombatAiProfiles() {
    }

    public static void registerDefaults() {
        PROFILES.put(EntityType.SKELETON, HumanoidCombatAiProfile.skeletonDefault());
        PROFILES.put(EntityType.WITHER_SKELETON, HumanoidCombatAiProfile.witherSkeletonDefault());
        PROFILES.put(EntityType.ZOMBIE, HumanoidCombatAiProfile.zombieDefault());
        PROFILES.put(EntityType.VINDICATOR, HumanoidCombatAiProfile.illagerMeleeDefault());
        PROFILES.put(EntityType.PILLAGER, HumanoidCombatAiProfile.illagerMeleeDefault());
    }

    public static void clear() {
        PROFILES.clear();
        PROFILE_SPECS.clear();
        SAMPLED_PROFILES.clear();
        zombieLeaderProfile = HumanoidCombatAiProfile.zombieLeaderDefault();
        zombieLeaderProfileSpec = null;
    }

    public static boolean hasProfile(LivingEntity entity) {
        return getProfile(entity) != null;
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

        if (entity instanceof SkeletonEntity) {
            return HumanoidCombatAiProfile.skeletonDefault();
        }

        if (entity instanceof ZombieEntity) {
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

    public static HumanoidCombatAiProfile baseProfileFor(EntityType<?> entityType) {
        HumanoidCombatAiProfile profile = PROFILES.get(entityType);
        if (profile != null) {
            return profile;
        }
        if (entityType == EntityType.SKELETON) {
            return HumanoidCombatAiProfile.skeletonDefault();
        }
        if (entityType == EntityType.WITHER_SKELETON) {
            return HumanoidCombatAiProfile.witherSkeletonDefault();
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
