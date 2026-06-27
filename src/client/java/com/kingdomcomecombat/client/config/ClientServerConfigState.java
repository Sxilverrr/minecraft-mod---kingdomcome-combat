package com.kingdomcomecombat.client.config;

import com.kingdomcomecombat.combat.CombatMovementConfig;
import java.util.ArrayList;
import java.util.List;

public final class ClientServerConfigState {
    private static boolean modEquipmentGenerationEnabled = true;
    private static boolean zombieLeaderHealthFixEnabled = false;
    private static boolean mobToughnessEnabled = true;
    private static int hitStopTicks = 2;
    private static double vanillaHurtSoundVolumeMultiplier = 0.7;
    private static int masterCounterWindowTicks = 8;
    private static int blockWindowTicks = 12;
    private static double combatMinDistance = CombatMovementConfig.DEFAULT_COMBAT_MIN_DISTANCE;
    private static List<String> vanillaAttackWeaponIds = new ArrayList<>();
    private static boolean canEdit;

    private ClientServerConfigState() {
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

    public static double combatMinDistance() {
        return combatMinDistance;
    }

    public static List<String> vanillaAttackWeaponIds() {
        return List.copyOf(vanillaAttackWeaponIds);
    }

    public static boolean canEdit() {
        return canEdit;
    }
}
