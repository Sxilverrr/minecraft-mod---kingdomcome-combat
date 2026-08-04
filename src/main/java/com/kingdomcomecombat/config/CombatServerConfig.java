package com.kingdomcomecombat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.combat.CombatMovementConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CombatServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = KingdomComeCombat.MOD_ID + "_server.json";
    private static final int DEFAULT_MASTER_COUNTER_WINDOW_TICKS = 8;
    private static final int DEFAULT_BLOCK_WINDOW_TICKS = 8;
    private static final int DEFAULT_UNPERFECT_BLOCK_WINDOW_TICKS = 10;

    private static boolean lightweightDamageModeEnabled = false;
    private static boolean lightweightBlockingModeEnabled = false;
    private static boolean modEquipmentGenerationEnabled = true;
    private static boolean zombieLeaderHealthFixEnabled = false;
    private static boolean mobToughnessEnabled = true;
    private static boolean alwaysEnablePlayerInterrupt = true;
    private static boolean experimentalIllagerUndeadHostilityEnabled = false;
    private static boolean disableVanillaLeftHandedMobs = true;
    private static boolean enderDragonOverhaulEnabled = true;
    private static boolean witherOverhaulEnabled = true;
    private static boolean skeletonBodyAimEnabled = true;
    private static boolean legacyCollisionCalculationEnabled = false;
    private static boolean clientProjectileHurtboxEnabled = false;
    private static boolean reachAttributeHitboxScalingEnabled = true;
    private static boolean blockingMovementSlowdownEnabled = true;
    private static boolean changedBandageUseEnabled = false;
    private static boolean attacksDoNotConsumeStamina = false;
    private static boolean automaticHumanoidAiCompatibilityEnabled = true;
    private static boolean mountedKccCombatEnabled = false;
    private static double reachAttributeHitboxScalePerBlock = 0.20;
    private static int hitStopTicks = 2;
    private static double vanillaHurtSoundVolumeMultiplier = 0.7;
    private static int masterCounterWindowTicks = DEFAULT_MASTER_COUNTER_WINDOW_TICKS;
    private static int blockWindowTicks = DEFAULT_BLOCK_WINDOW_TICKS;
    private static int unperfectBlockWindowTicks = DEFAULT_UNPERFECT_BLOCK_WINDOW_TICKS;
    private static double combatMinDistance = CombatMovementConfig.DEFAULT_COMBAT_MIN_DISTANCE;
    private static double collisionCacheRadius = 8.0;
    private static List<String> vanillaAttackWeaponIds = new ArrayList<>();
    private static List<String> vanillaAttackEntityIds = new ArrayList<>();

    private CombatServerConfig() {
    }

    public static void load() {
        Path path = configPath();
        if (Files.notExists(path)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("enable_lightweight_damage_mode")) {
                lightweightDamageModeEnabled = root.get("enable_lightweight_damage_mode").getAsBoolean();
            }
            if (root.has("enable_lightweight_blocking_mode")) {
                lightweightBlockingModeEnabled = root.get("enable_lightweight_blocking_mode").getAsBoolean();
            }
            if (root.has("enable_mod_equipment_generation")) {
                modEquipmentGenerationEnabled = root.get("enable_mod_equipment_generation").getAsBoolean();
            }
            if (root.has("fix_zombie_leader_initial_health")) {
                zombieLeaderHealthFixEnabled = root.get("fix_zombie_leader_initial_health").getAsBoolean();
            }
            if (root.has("enable_mob_toughness")) {
                mobToughnessEnabled = root.get("enable_mob_toughness").getAsBoolean();
            }
            if (root.has("always_enable_player_interrupt")) {
                alwaysEnablePlayerInterrupt = root.get("always_enable_player_interrupt").getAsBoolean();
            }
            if (root.has("experimental_illager_undead_hostility")) {
                experimentalIllagerUndeadHostilityEnabled = root.get("experimental_illager_undead_hostility").getAsBoolean();
            }
            if (root.has("disable_vanilla_left_handed_mobs")) {
                disableVanillaLeftHandedMobs = root.get("disable_vanilla_left_handed_mobs").getAsBoolean();
            }
            if (root.has("enable_ender_dragon_overhaul")) {
                enderDragonOverhaulEnabled = root.get("enable_ender_dragon_overhaul").getAsBoolean();
            }
            if (root.has("enable_wither_overhaul")) witherOverhaulEnabled = root.get("enable_wither_overhaul").getAsBoolean();
            if (root.has("enable_skeleton_body_aim")) skeletonBodyAimEnabled = root.get("enable_skeleton_body_aim").getAsBoolean();
            if (root.has("use_legacy_collision_calculation")) {
                legacyCollisionCalculationEnabled = root.get("use_legacy_collision_calculation").getAsBoolean();
            }
            if (root.has("enable_client_projectile_hurtbox")) {
                clientProjectileHurtboxEnabled = root.get("enable_client_projectile_hurtbox").getAsBoolean();
            }
            if (root.has("enable_reach_attribute_hitbox_scaling")) {
                reachAttributeHitboxScalingEnabled = root.get("enable_reach_attribute_hitbox_scaling").getAsBoolean();
            }
            if (root.has("enable_blocking_movement_slowdown")) {
                blockingMovementSlowdownEnabled = root.get("enable_blocking_movement_slowdown").getAsBoolean();
            }
            if (root.has("enable_changed_bandage_use")) {
                changedBandageUseEnabled = root.get("enable_changed_bandage_use").getAsBoolean();
            }
            if (root.has("attacks_do_not_consume_stamina")) {
                attacksDoNotConsumeStamina = root.get("attacks_do_not_consume_stamina").getAsBoolean();
            }
            if (root.has("enable_automatic_humanoid_ai_compatibility")) {
                automaticHumanoidAiCompatibilityEnabled =
                        root.get("enable_automatic_humanoid_ai_compatibility").getAsBoolean();
            }
            if (root.has("enable_mounted_kcc_combat")) {
                mountedKccCombatEnabled = root.get("enable_mounted_kcc_combat").getAsBoolean();
            }
            if (root.has("reach_attribute_hitbox_scale_per_block")) {
                reachAttributeHitboxScalePerBlock = clamp(
                        root.get("reach_attribute_hitbox_scale_per_block").getAsDouble(), 0.0, 1.0);
            }
            if (root.has("hit_stop_ticks")) {
                hitStopTicks = clamp(root.get("hit_stop_ticks").getAsInt(), 0, 20);
            }
            if (root.has("vanilla_hurt_sound_volume_multiplier")) {
                vanillaHurtSoundVolumeMultiplier = clamp(
                        root.get("vanilla_hurt_sound_volume_multiplier").getAsDouble(),
                        0.0,
                        2.0
                );
            }
            if (root.has("master_counter_window_ticks")) {
                masterCounterWindowTicks = clamp(root.get("master_counter_window_ticks").getAsInt(), 0, 40);
            }
            if (root.has("block_window_ticks")) {
                blockWindowTicks = clamp(root.get("block_window_ticks").getAsInt(), 0, 40);
            }
            if (root.has("unperfect_block_window_ticks")) {
                unperfectBlockWindowTicks = clamp(root.get("unperfect_block_window_ticks").getAsInt(), 0, 40);
            }
            if (root.has("combat_min_distance")) {
                combatMinDistance = clamp(root.get("combat_min_distance").getAsDouble(), 0.5, 4.0);
            }
            if (root.has("collision_cache_radius")) {
                collisionCacheRadius = clamp(root.get("collision_cache_radius").getAsDouble(), 2.0, 32.0);
            }
            if (root.has("vanilla_attack_weapon_ids") && root.get("vanilla_attack_weapon_ids").isJsonArray()) {
                vanillaAttackWeaponIds = readIdentifierList(root.getAsJsonArray("vanilla_attack_weapon_ids"));
            }
            if (root.has("vanilla_attack_entity_ids") && root.get("vanilla_attack_entity_ids").isJsonArray()) {
                vanillaAttackEntityIds = readIdentifierList(root.getAsJsonArray("vanilla_attack_entity_ids"));
            }
        } catch (Exception exception) {
            KingdomComeCombat.LOGGER.warn("Failed to load combat server config, using defaults.", exception);
        }
    }

    public static void save() {
        Path path = configPath();
        JsonObject root = new JsonObject();
        root.addProperty("enable_lightweight_damage_mode", lightweightDamageModeEnabled);
        root.addProperty("enable_lightweight_blocking_mode", lightweightBlockingModeEnabled);
        root.addProperty("enable_mod_equipment_generation", modEquipmentGenerationEnabled);
        root.addProperty("fix_zombie_leader_initial_health", zombieLeaderHealthFixEnabled);
        root.addProperty("enable_mob_toughness", mobToughnessEnabled);
        root.addProperty("always_enable_player_interrupt", alwaysEnablePlayerInterrupt);
        root.addProperty("experimental_illager_undead_hostility", experimentalIllagerUndeadHostilityEnabled);
        root.addProperty("disable_vanilla_left_handed_mobs", disableVanillaLeftHandedMobs);
        root.addProperty("enable_ender_dragon_overhaul", enderDragonOverhaulEnabled);
        root.addProperty("enable_wither_overhaul", witherOverhaulEnabled);
        root.addProperty("enable_skeleton_body_aim", skeletonBodyAimEnabled);
        root.addProperty("use_legacy_collision_calculation", legacyCollisionCalculationEnabled);
        root.addProperty("enable_client_projectile_hurtbox", clientProjectileHurtboxEnabled);
        root.addProperty("enable_reach_attribute_hitbox_scaling", reachAttributeHitboxScalingEnabled);
        root.addProperty("enable_blocking_movement_slowdown", blockingMovementSlowdownEnabled);
        root.addProperty("enable_changed_bandage_use", changedBandageUseEnabled);
        root.addProperty("attacks_do_not_consume_stamina", attacksDoNotConsumeStamina);
        root.addProperty(
                "enable_automatic_humanoid_ai_compatibility",
                automaticHumanoidAiCompatibilityEnabled
        );
        root.addProperty("enable_mounted_kcc_combat", mountedKccCombatEnabled);
        root.addProperty("reach_attribute_hitbox_scale_per_block", reachAttributeHitboxScalePerBlock);
        root.addProperty("hit_stop_ticks", hitStopTicks);
        root.addProperty("vanilla_hurt_sound_volume_multiplier", vanillaHurtSoundVolumeMultiplier);
        root.addProperty("master_counter_window_ticks", masterCounterWindowTicks);
        root.addProperty("block_window_ticks", blockWindowTicks);
        root.addProperty("unperfect_block_window_ticks", unperfectBlockWindowTicks);
        root.addProperty("combat_min_distance", combatMinDistance);
        root.addProperty("collision_cache_radius", collisionCacheRadius);
        JsonArray vanillaAttackWeapons = new JsonArray();
        for (String itemId : vanillaAttackWeaponIds) {
            vanillaAttackWeapons.add(itemId);
        }
        root.add("vanilla_attack_weapon_ids", vanillaAttackWeapons);
        JsonArray vanillaAttackEntities = new JsonArray();
        for (String entityId : vanillaAttackEntityIds) vanillaAttackEntities.add(entityId);
        root.add("vanilla_attack_entity_ids", vanillaAttackEntities);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException exception) {
            KingdomComeCombat.LOGGER.warn("Failed to write combat server config.", exception);
        }
    }

    public static boolean lightweightDamageModeEnabled() { return lightweightDamageModeEnabled; }
    public static void setLightweightDamageModeEnabled(boolean enabled) { lightweightDamageModeEnabled = enabled; }
    public static boolean lightweightBlockingModeEnabled() { return lightweightBlockingModeEnabled; }
    public static void setLightweightBlockingModeEnabled(boolean enabled) { lightweightBlockingModeEnabled = enabled; }

    public static boolean modEquipmentGenerationEnabled() {
        return modEquipmentGenerationEnabled;
    }

    public static void setModEquipmentGenerationEnabled(boolean enabled) {
        modEquipmentGenerationEnabled = enabled;
    }

    public static boolean zombieLeaderHealthFixEnabled() {
        return zombieLeaderHealthFixEnabled;
    }

    public static void setZombieLeaderHealthFixEnabled(boolean enabled) {
        zombieLeaderHealthFixEnabled = enabled;
    }

    public static boolean mobToughnessEnabled() {
        return mobToughnessEnabled;
    }

    public static boolean alwaysEnablePlayerInterrupt() {
        return alwaysEnablePlayerInterrupt;
    }

    public static void setAlwaysEnablePlayerInterrupt(boolean enabled) {
        alwaysEnablePlayerInterrupt = enabled;
    }

    public static boolean experimentalIllagerUndeadHostilityEnabled() {
        return experimentalIllagerUndeadHostilityEnabled;
    }

    public static void setExperimentalIllagerUndeadHostilityEnabled(boolean enabled) {
        experimentalIllagerUndeadHostilityEnabled = enabled;
    }

    public static boolean disableVanillaLeftHandedMobs() {
        return disableVanillaLeftHandedMobs;
    }

    public static void setDisableVanillaLeftHandedMobs(boolean disabled) {
        disableVanillaLeftHandedMobs = disabled;
    }

    public static boolean enderDragonOverhaulEnabled() {
        return enderDragonOverhaulEnabled;
    }

    public static void setEnderDragonOverhaulEnabled(boolean enabled) {
        enderDragonOverhaulEnabled = enabled;
    }
    public static boolean witherOverhaulEnabled() { return witherOverhaulEnabled; }
    public static void setWitherOverhaulEnabled(boolean enabled) { witherOverhaulEnabled = enabled; }
    public static boolean skeletonBodyAimEnabled() { return skeletonBodyAimEnabled; }
    public static void setSkeletonBodyAimEnabled(boolean enabled) { skeletonBodyAimEnabled = enabled; }
    public static boolean legacyCollisionCalculationEnabled() { return legacyCollisionCalculationEnabled; }
    public static void setLegacyCollisionCalculationEnabled(boolean enabled) { legacyCollisionCalculationEnabled = enabled; }
    public static boolean clientProjectileHurtboxEnabled() {
        return clientProjectileHurtboxEnabled && !lightweightDamageModeEnabled;
    }
    public static void setClientProjectileHurtboxEnabled(boolean enabled) { clientProjectileHurtboxEnabled = enabled; }

    public static void setMobToughnessEnabled(boolean enabled) {
        mobToughnessEnabled = enabled;
    }

    public static int hitStopTicks() {
        return hitStopTicks;
    }

    public static void setHitStopTicks(int ticks) {
        hitStopTicks = clamp(ticks, 0, 20);
    }

    public static double vanillaHurtSoundVolumeMultiplier() {
        return vanillaHurtSoundVolumeMultiplier;
    }

    public static void setVanillaHurtSoundVolumeMultiplier(double multiplier) {
        vanillaHurtSoundVolumeMultiplier = clamp(multiplier, 0.0, 2.0);
    }

    public static int masterCounterWindowTicks() {
        return masterCounterWindowTicks;
    }

    public static void setMasterCounterWindowTicks(int ticks) {
        masterCounterWindowTicks = clamp(ticks, 0, 40);
    }

    public static int blockWindowTicks() {
        return blockWindowTicks;
    }

    public static void setBlockWindowTicks(int ticks) {
        blockWindowTicks = clamp(ticks, 0, 40);
    }

    public static int unperfectBlockWindowTicks() {
        return unperfectBlockWindowTicks;
    }

    public static void setUnperfectBlockWindowTicks(int ticks) {
        unperfectBlockWindowTicks = clamp(ticks, 0, 40);
    }

    public static double combatMinDistance() {
        return combatMinDistance;
    }

    public static void setCombatMinDistance(double distance) {
        combatMinDistance = clamp(distance, 0.5, 4.0);
        CombatMovementConfig.setCombatMinDistance(combatMinDistance);
    }

    public static double collisionCacheRadius() { return collisionCacheRadius; }
    public static void setCollisionCacheRadius(double radius) {
        collisionCacheRadius = clamp(radius, 2.0, 32.0);
    }

    public static List<String> vanillaAttackWeaponIds() {
        return List.copyOf(vanillaAttackWeaponIds);
    }

    public static boolean reachAttributeHitboxScalingEnabled() { return reachAttributeHitboxScalingEnabled; }
    public static boolean blockingMovementSlowdownEnabled() { return blockingMovementSlowdownEnabled; }
    public static boolean changedBandageUseEnabled() { return changedBandageUseEnabled; }
    public static void setChangedBandageUseEnabled(boolean enabled) { changedBandageUseEnabled = enabled; }
    public static boolean attacksDoNotConsumeStamina() { return attacksDoNotConsumeStamina; }
    public static void setAttacksDoNotConsumeStamina(boolean enabled) { attacksDoNotConsumeStamina = enabled; }
    public static boolean automaticHumanoidAiCompatibilityEnabled() {
        return automaticHumanoidAiCompatibilityEnabled;
    }
    public static void setAutomaticHumanoidAiCompatibilityEnabled(boolean enabled) {
        automaticHumanoidAiCompatibilityEnabled = enabled;
    }
    public static boolean mountedKccCombatEnabled() { return mountedKccCombatEnabled; }
    public static void setMountedKccCombatEnabled(boolean enabled) { mountedKccCombatEnabled = enabled; }
    public static void setBlockingMovementSlowdownEnabled(boolean enabled) {
        blockingMovementSlowdownEnabled = enabled;
    }
    public static void setReachAttributeHitboxScalingEnabled(boolean enabled) {
        reachAttributeHitboxScalingEnabled = enabled;
    }
    public static double reachAttributeHitboxScalePerBlock() { return reachAttributeHitboxScalePerBlock; }
    public static void setReachAttributeHitboxScalePerBlock(double value) {
        reachAttributeHitboxScalePerBlock = clamp(value, 0.0, 1.0);
    }

    public static void setVanillaAttackWeaponIds(List<String> itemIds) {
        vanillaAttackWeaponIds = sanitizeIdentifierList(itemIds);
    }

    public static boolean allowsVanillaAttackWeapon(Identifier itemId) {
        return itemId != null && vanillaAttackWeaponIds.contains(itemId.toString());
    }

    public static List<String> vanillaAttackEntityIds() { return List.copyOf(vanillaAttackEntityIds); }
    public static void setVanillaAttackEntityIds(List<String> entityIds) {
        vanillaAttackEntityIds = sanitizeIdentifierList(entityIds);
    }
    public static boolean allowsVanillaAttackEntity(Identifier entityId) {
        return entityId != null && vanillaAttackEntityIds.contains(entityId.toString());
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static List<String> readIdentifierList(JsonArray array) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                continue;
            }
            String value = element.getAsString();
            if (Identifier.tryParse(value) != null) {
                values.add(value.trim());
            }
        }
        return sanitizeIdentifierList(values);
    }

    private static List<String> sanitizeIdentifierList(List<String> itemIds) {
        List<String> values = new ArrayList<>();
        if (itemIds == null) {
            return values;
        }
        for (String itemId : itemIds) {
            String trimmed = itemId == null ? "" : itemId.trim();
            if (trimmed.isEmpty() || Identifier.tryParse(trimmed) == null || values.contains(trimmed)) {
                continue;
            }
            values.add(trimmed);
        }
        return values;
    }
}
