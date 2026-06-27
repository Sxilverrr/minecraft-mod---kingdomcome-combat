package com.kingdomcomecombat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.combat.CombatMovementConfig;
import com.kingdomcomecombat.platform.PlatformServices;
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
    private static final int DEFAULT_BLOCK_WINDOW_TICKS = 12;

    private static boolean modEquipmentGenerationEnabled = true;
    private static boolean zombieLeaderHealthFixEnabled = false;
    private static boolean mobToughnessEnabled = true;
    private static int hitStopTicks = 2;
    private static double vanillaHurtSoundVolumeMultiplier = 0.7;
    private static int masterCounterWindowTicks = DEFAULT_MASTER_COUNTER_WINDOW_TICKS;
    private static int blockWindowTicks = DEFAULT_BLOCK_WINDOW_TICKS;
    private static double combatMinDistance = CombatMovementConfig.DEFAULT_COMBAT_MIN_DISTANCE;
    private static List<String> vanillaAttackWeaponIds = new ArrayList<>();

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
            if (root.has("enable_mod_equipment_generation")) {
                modEquipmentGenerationEnabled = root.get("enable_mod_equipment_generation").getAsBoolean();
            }
            if (root.has("fix_zombie_leader_initial_health")) {
                zombieLeaderHealthFixEnabled = root.get("fix_zombie_leader_initial_health").getAsBoolean();
            }
            if (root.has("enable_mob_toughness")) {
                mobToughnessEnabled = root.get("enable_mob_toughness").getAsBoolean();
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
            if (root.has("combat_min_distance")) {
                combatMinDistance = clamp(root.get("combat_min_distance").getAsDouble(), 0.5, 4.0);
            }
            if (root.has("vanilla_attack_weapon_ids") && root.get("vanilla_attack_weapon_ids").isJsonArray()) {
                vanillaAttackWeaponIds = readIdentifierList(root.getAsJsonArray("vanilla_attack_weapon_ids"));
            }
        } catch (Exception exception) {
            KingdomComeCombat.LOGGER.warn("Failed to load combat server config, using defaults.", exception);
        }
    }

    public static void save() {
        Path path = configPath();
        JsonObject root = new JsonObject();
        root.addProperty("enable_mod_equipment_generation", modEquipmentGenerationEnabled);
        root.addProperty("fix_zombie_leader_initial_health", zombieLeaderHealthFixEnabled);
        root.addProperty("enable_mob_toughness", mobToughnessEnabled);
        root.addProperty("hit_stop_ticks", hitStopTicks);
        root.addProperty("vanilla_hurt_sound_volume_multiplier", vanillaHurtSoundVolumeMultiplier);
        root.addProperty("master_counter_window_ticks", masterCounterWindowTicks);
        root.addProperty("block_window_ticks", blockWindowTicks);
        root.addProperty("combat_min_distance", combatMinDistance);
        JsonArray vanillaAttackWeapons = new JsonArray();
        for (String itemId : vanillaAttackWeaponIds) {
            vanillaAttackWeapons.add(itemId);
        }
        root.add("vanilla_attack_weapon_ids", vanillaAttackWeapons);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException exception) {
            KingdomComeCombat.LOGGER.warn("Failed to write combat server config.", exception);
        }
    }

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

    public static double combatMinDistance() {
        return combatMinDistance;
    }

    public static void setCombatMinDistance(double distance) {
        combatMinDistance = clamp(distance, 0.5, 4.0);
        CombatMovementConfig.setCombatMinDistance(combatMinDistance);
    }

    public static List<String> vanillaAttackWeaponIds() {
        return List.copyOf(vanillaAttackWeaponIds);
    }

    public static void setVanillaAttackWeaponIds(List<String> itemIds) {
        vanillaAttackWeaponIds = sanitizeIdentifierList(itemIds);
    }

    public static boolean allowsVanillaAttackWeapon(Identifier itemId) {
        return itemId != null && vanillaAttackWeaponIds.contains(itemId.toString());
    }

    private static Path configPath() {
        return PlatformServices.loader().configDirectory().resolve(FILE_NAME);
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
