package com.kingdomcomecombat.client.config;

import com.kingdomcomecombat.combat.CombatMovementConfig;
import java.util.ArrayList;
import java.util.List;

public final class ClientServerConfigState {
    private static boolean lightweightDamageModeEnabled;
    private static boolean lightweightBlockingModeEnabled;
    private static boolean modEquipmentGenerationEnabled = true;
    private static boolean zombieLeaderHealthFixEnabled = false;
    private static boolean mobToughnessEnabled = true;
    private static boolean alwaysEnablePlayerInterrupt = true;
    private static boolean experimentalIllagerUndeadHostilityEnabled = false;
    private static boolean disableVanillaLeftHandedMobs = true;
    private static boolean enderDragonOverhaulEnabled = true;
    private static boolean legacyCollisionCalculationEnabled;
    private static boolean clientProjectileHurtboxEnabled;
    private static boolean reachAttributeHitboxScalingEnabled = true;
    private static boolean blockingMovementSlowdownEnabled = true;
    private static boolean mountedKccCombatEnabled;
    private static double reachAttributeHitboxScalePerBlock = 0.20;
    private static int hitStopTicks = 2;
    private static double vanillaHurtSoundVolumeMultiplier = 0.7;
    private static int masterCounterWindowTicks = 8;
    private static int blockWindowTicks = 8;
    private static int unperfectBlockWindowTicks = 10;
    private static double combatMinDistance = CombatMovementConfig.DEFAULT_COMBAT_MIN_DISTANCE;
    private static double collisionCacheRadius = 8.0;
    private static List<String> vanillaAttackWeaponIds = new ArrayList<>();
    private static List<String> vanillaAttackEntityIds = new ArrayList<>();
    private static boolean canEdit;
    private static boolean pvpEnabled;

    private ClientServerConfigState() {
    }

    public static boolean lightweightDamageModeEnabled() { return lightweightDamageModeEnabled; }
    public static void setLightweightDamageModeEnabled(boolean enabled) {
        lightweightDamageModeEnabled = enabled;
        com.kingdomcomecombat.config.CombatServerConfig.setLightweightDamageModeEnabled(enabled);
    }
    public static boolean lightweightBlockingModeEnabled() { return lightweightBlockingModeEnabled; }
    public static void setLightweightBlockingModeEnabled(boolean enabled) {
        lightweightBlockingModeEnabled = enabled;
        com.kingdomcomecombat.config.CombatServerConfig.setLightweightBlockingModeEnabled(enabled);
    }

    public static void update(
            boolean equipmentGenerationEnabled,
            boolean leaderHealthFixEnabled,
            boolean syncedMobToughnessEnabled,
            int syncedHitStopTicks,
            double syncedVanillaHurtSoundVolumeMultiplier,
            int syncedMasterCounterWindowTicks,
            int syncedBlockWindowTicks,
            List<String> syncedVanillaAttackWeaponIds,
            boolean editable
    ) {
        update(
                equipmentGenerationEnabled, leaderHealthFixEnabled, syncedMobToughnessEnabled,
                syncedHitStopTicks, syncedVanillaHurtSoundVolumeMultiplier,
                syncedMasterCounterWindowTicks, syncedBlockWindowTicks, combatMinDistance,
                syncedVanillaAttackWeaponIds, editable
        );
    }

    public static void update(
            boolean equipmentGenerationEnabled,
            boolean leaderHealthFixEnabled,
            boolean syncedMobToughnessEnabled,
            int syncedHitStopTicks,
            double syncedVanillaHurtSoundVolumeMultiplier,
            int syncedMasterCounterWindowTicks,
            int syncedBlockWindowTicks,
            double syncedCombatMinDistance,
            List<String> syncedVanillaAttackWeaponIds,
            boolean editable
    ) {
        modEquipmentGenerationEnabled = equipmentGenerationEnabled;
        zombieLeaderHealthFixEnabled = leaderHealthFixEnabled;
        mobToughnessEnabled = syncedMobToughnessEnabled;
        hitStopTicks = Math.max(0, Math.min(20, syncedHitStopTicks));
        vanillaHurtSoundVolumeMultiplier = Math.max(0.0, Math.min(2.0, syncedVanillaHurtSoundVolumeMultiplier));
        masterCounterWindowTicks = Math.max(0, Math.min(40, syncedMasterCounterWindowTicks));
        blockWindowTicks = Math.max(0, Math.min(40, syncedBlockWindowTicks));
        combatMinDistance = Math.max(0.5, Math.min(4.0, syncedCombatMinDistance));
        CombatMovementConfig.setCombatMinDistance(combatMinDistance);
        vanillaAttackWeaponIds = syncedVanillaAttackWeaponIds == null
                ? new ArrayList<>()
                : new ArrayList<>(syncedVanillaAttackWeaponIds);
        canEdit = editable;
    }

    public static boolean modEquipmentGenerationEnabled() {
        return modEquipmentGenerationEnabled;
    }

    public static boolean zombieLeaderHealthFixEnabled() {
        return zombieLeaderHealthFixEnabled;
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
        com.kingdomcomecombat.config.CombatServerConfig.setEnderDragonOverhaulEnabled(enabled);
    }

    public static boolean legacyCollisionCalculationEnabled() {
        return legacyCollisionCalculationEnabled;
    }

    public static void setLegacyCollisionCalculationEnabled(boolean enabled) {
        legacyCollisionCalculationEnabled = enabled;
    }

    public static boolean clientProjectileHurtboxEnabled() {
        return clientProjectileHurtboxEnabled;
    }

    public static void setClientProjectileHurtboxEnabled(boolean enabled) {
        clientProjectileHurtboxEnabled = enabled;
    }

    public static boolean reachAttributeHitboxScalingEnabled() { return reachAttributeHitboxScalingEnabled; }
    public static boolean blockingMovementSlowdownEnabled() { return blockingMovementSlowdownEnabled; }
    public static void setBlockingMovementSlowdownEnabled(boolean enabled) {
        blockingMovementSlowdownEnabled = enabled;
    }
    public static boolean mountedKccCombatEnabled() { return mountedKccCombatEnabled; }
    public static void setMountedKccCombatEnabled(boolean enabled) {
        mountedKccCombatEnabled = enabled;
        com.kingdomcomecombat.config.CombatServerConfig.setMountedKccCombatEnabled(enabled);
    }
    public static void setReachAttributeHitboxScalingEnabled(boolean enabled) {
        reachAttributeHitboxScalingEnabled = enabled;
        com.kingdomcomecombat.config.CombatServerConfig.setReachAttributeHitboxScalingEnabled(enabled);
    }
    public static double reachAttributeHitboxScalePerBlock() { return reachAttributeHitboxScalePerBlock; }
    public static void setReachAttributeHitboxScalePerBlock(double value) {
        reachAttributeHitboxScalePerBlock = Math.max(0.0, Math.min(1.0, value));
        com.kingdomcomecombat.config.CombatServerConfig.setReachAttributeHitboxScalePerBlock(
                reachAttributeHitboxScalePerBlock);
    }

    public static int hitStopTicks() {
        return hitStopTicks;
    }

    public static double vanillaHurtSoundVolumeMultiplier() {
        return vanillaHurtSoundVolumeMultiplier;
    }

    public static int masterCounterWindowTicks() {
        return masterCounterWindowTicks;
    }

    public static int blockWindowTicks() {
        return blockWindowTicks;
    }

    public static int unperfectBlockWindowTicks() {
        return unperfectBlockWindowTicks;
    }

    public static void setUnperfectBlockWindowTicks(int ticks) {
        unperfectBlockWindowTicks = Math.max(0, Math.min(40, ticks));
    }

    public static double combatMinDistance() {
        return combatMinDistance;
    }

    public static double collisionCacheRadius() { return collisionCacheRadius; }
    public static void setCollisionCacheRadius(double radius) {
        collisionCacheRadius = Math.max(2.0, Math.min(32.0, radius));
    }

    public static List<String> vanillaAttackWeaponIds() {
        return List.copyOf(vanillaAttackWeaponIds);
    }
    public static List<String> vanillaAttackEntityIds() { return List.copyOf(vanillaAttackEntityIds); }
    public static void setVanillaAttackEntityIds(List<String> ids) {
        vanillaAttackEntityIds = ids == null ? new ArrayList<>() : new ArrayList<>(ids);
    }

    public static boolean canEdit() {
        return canEdit;
    }

    public static boolean pvpEnabled() {
        return pvpEnabled;
    }

    public static void setPvpEnabled(boolean enabled) {
        pvpEnabled = enabled;
    }
}
