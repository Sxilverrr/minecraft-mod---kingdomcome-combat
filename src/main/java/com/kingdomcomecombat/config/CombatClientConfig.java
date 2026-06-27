package com.kingdomcomecombat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingdomcomecombat.KingdomComeCombat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CombatClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = KingdomComeCombat.MOD_ID + "_equipment_render.json";

    private static double bloodPrimaryNoiseScale = 0.0975;
    private static double bloodDetailNoiseScale = 0.1885;
    private static double bloodDripNoiseScale = 0.065;
    private static double bloodMinimumBlendStrength = 0.04;
    private static double bloodMaximumBlendStrength = 0.68;
    private static double bloodNoiseContrast = 1.0;
    private static int signature = 0;

    private static boolean renderBlood = true;
    private static boolean renderPlayerBlood = true;
    private static boolean playerBloodOverlayCompat = false;
    private static boolean renderArmorHoles = true;
    private static boolean bladeTrailsEnabled = true;
    private static boolean armorPhysicsCompat = false;
    private static boolean showLockOnCrosshair = true;
    private static boolean autoLockOnHit = false;
    private static double lockOnAcquireAngleDegrees = 70.0;
    private static double lockOnReleaseDistance = 10.0;
    private static double lockOnSwitchAngleDegrees = 35.0;
    private static double lockOnSwitchSideThreshold = 0.18;
    private static double lockOnCameraShoulderOffset = 1.55;
    private static double lockOnCameraHeightOffset = 0.16;
    private static double lockOnCameraPullIn = 2.15;
    private static double lockOnCameraAimLeftOffset = 0.48;
    private static double lockOnCameraYawFollowSpeed = 14.0;
    private static double lockOnCameraPitchFollowSpeed = 10.0;
    private static double lockOnCameraTransitionSpeed = 12.0;
    private static double lockOnFirstPersonCameraForwardOffset = 0.0;
    private static double sparkParticlePercent = 1.0;
    private static double bloodParticlePercent = 1.0;
    private static double bloodMistOpacity = 1.0;
    private static double bloodStainPercent = 1.0;
    private static boolean showDebugMessages = false;
    private static boolean disableVanillaLeftHandedMobs = true;
    private static boolean screenEffectsEnabled = true;
    private static boolean hurtCameraMovementEnabled = true;
    private static double screenRedEffectStrength = 1.0;
    private static double screenBlueEffectStrength = 1.0;
    private static double hitReactionAnimationStrength = 1.0;
    private static int hitReactionReturnTicks = 8;
    private static FirstPersonModelMode firstPersonModelMode = FirstPersonModelMode.WHEN_NEEDED;
    private static final Set<Identifier> firstPersonModelDisabledItems = new LinkedHashSet<>();
    private static double firstPersonBodyForwardOffset = 0.0;
    private static double firstPersonCameraForwardOffset = 0.0;
    private static boolean firstPersonCameraHeadBindingEnabled = false;
    private static boolean firstPersonSimpleEyeSimulationEnabled = false;

    private CombatClientConfig() {
    }

    public static void load() {
        Path path = configPath();
        if (Files.notExists(path)) {
            write(path);
            updateSignature();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject rendering = object(root, "rendering", root);
            renderBlood = bool(rendering, "enable_blood", renderBlood);
            renderPlayerBlood = bool(rendering, "enable_player_blood", renderPlayerBlood);
            playerBloodOverlayCompat = bool(rendering, "player_blood_overlay_compat", playerBloodOverlayCompat);
            renderArmorHoles = bool(rendering, "enable_armor_holes", renderArmorHoles);
            bladeTrailsEnabled = bool(rendering, "enable_blade_trails", bladeTrailsEnabled);
            disableVanillaLeftHandedMobs = bool(
                    rendering,
                    "disable_vanilla_left_handed_mobs",
                    disableVanillaLeftHandedMobs
            );
            armorPhysicsCompat = bool(rendering, "enable_armor_physics_compat", armorPhysicsCompat);
            screenEffectsEnabled = bool(rendering, "enable_screen_effects", screenEffectsEnabled);
            hurtCameraMovementEnabled = bool(
                    rendering,
                    "enable_hurt_camera_movement",
                    hurtCameraMovementEnabled
            );

            JsonObject firstPerson = object(root, "first_person", root);
            firstPersonModelMode = FirstPersonModelMode.fromConfigName(
                    string(firstPerson, "model_mode", firstPersonModelMode.configName())
            );
            firstPersonBodyForwardOffset = clampedNumber(
                    firstPerson,
                    "body_forward_offset",
                    firstPersonBodyForwardOffset,
                    -1.0,
                    1.0
            );
            firstPersonCameraForwardOffset = clampedNumber(
                    firstPerson,
                    "camera_forward_offset",
                    firstPersonCameraForwardOffset,
                    -0.5,
                    0.5
            );
            firstPersonCameraHeadBindingEnabled = bool(
                    firstPerson,
                    "bind_camera_to_head_front",
                    firstPersonCameraHeadBindingEnabled
            );
            firstPersonSimpleEyeSimulationEnabled = bool(
                    firstPerson,
                    "simple_eye_simulation",
                    firstPersonSimpleEyeSimulationEnabled
            );
            firstPersonModelDisabledItems.clear();
            if (firstPerson.has("disabled_items") && firstPerson.get("disabled_items").isJsonArray()) {
                for (var element : firstPerson.getAsJsonArray("disabled_items")) {
                    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                        continue;
                    }
                    Identifier id = Identifier.tryParse(element.getAsString());
                    if (id != null) {
                        firstPersonModelDisabledItems.add(id);
                    }
                }
            }

            JsonObject blood = object(root, "blood", root);
            bloodPrimaryNoiseScale = positiveNumber(blood, "primary_noise_scale", bloodPrimaryNoiseScale);
            bloodDetailNoiseScale = positiveNumber(blood, "detail_noise_scale", bloodDetailNoiseScale);
            bloodDripNoiseScale = positiveNumber(blood, "drip_noise_scale", bloodDripNoiseScale);
            bloodMinimumBlendStrength = rangedNumber(blood, "minimum_blend_strength", bloodMinimumBlendStrength);
            bloodMaximumBlendStrength = rangedNumber(blood, "maximum_blend_strength", bloodMaximumBlendStrength);
            bloodNoiseContrast = positiveNumber(blood, "noise_contrast", bloodNoiseContrast);
            bloodParticlePercent = percentNumber(blood, "particle_percent", bloodParticlePercent);
            bloodStainPercent = percentNumber(blood, "stain_percent", bloodStainPercent);
            if (bloodMaximumBlendStrength < bloodMinimumBlendStrength) {
                bloodMaximumBlendStrength = bloodMinimumBlendStrength;
            }

            JsonObject lockOn = object(root, "lock_on", root);
            showLockOnCrosshair = bool(lockOn, "show_crosshair", showLockOnCrosshair);
            autoLockOnHit = bool(lockOn, "auto_lock_on_hit", autoLockOnHit);
            lockOnAcquireAngleDegrees = positiveNumber(lockOn, "crosshair_target_angle_degrees", lockOnAcquireAngleDegrees);
            lockOnReleaseDistance = positiveNumber(lockOn, "crosshair_release_distance", lockOnReleaseDistance);
            lockOnSwitchAngleDegrees = positiveNumber(lockOn, "crosshair_switch_angle_degrees", lockOnSwitchAngleDegrees);
            lockOnSwitchSideThreshold = rangedNumber(lockOn, "crosshair_switch_side_threshold", lockOnSwitchSideThreshold);
            lockOnCameraShoulderOffset = clampedNumber(lockOn, "camera_shoulder_offset", lockOnCameraShoulderOffset, 0.0, 3.0);
            lockOnCameraHeightOffset = clampedNumber(lockOn, "camera_height_offset", lockOnCameraHeightOffset, -1.0, 1.0);
            lockOnCameraPullIn = clampedNumber(lockOn, "camera_pull_in", lockOnCameraPullIn, 0.0, 4.0);
            lockOnCameraAimLeftOffset = clampedNumber(lockOn, "camera_aim_left_offset", lockOnCameraAimLeftOffset, -2.0, 2.0);
            lockOnCameraYawFollowSpeed = clampedNumber(lockOn, "camera_yaw_follow_speed", lockOnCameraYawFollowSpeed, 1.0, 18.0);
            lockOnCameraPitchFollowSpeed = clampedNumber(lockOn, "camera_pitch_follow_speed", lockOnCameraPitchFollowSpeed, 1.0, 24.0);
            lockOnCameraTransitionSpeed = clampedNumber(lockOn, "camera_transition_speed", lockOnCameraTransitionSpeed, 1.0, 30.0);
            lockOnFirstPersonCameraForwardOffset = clampedNumber(
                    lockOn,
                    "first_person_camera_forward_offset",
                    lockOnFirstPersonCameraForwardOffset,
                    -1.0,
                    1.0
            );

            JsonObject particles = object(root, "particles", root);
            sparkParticlePercent = percentNumber(particles, "spark_percent", sparkParticlePercent);
            bloodParticlePercent = percentNumber(particles, "blood_percent", bloodParticlePercent);
            bloodMistOpacity = clampedNumber(particles, "blood_mist_opacity", bloodMistOpacity, 0.0, 3.0);

            JsonObject feedback = object(root, "feedback", root);
            screenRedEffectStrength = clampedNumber(
                    feedback,
                    "screen_red_effect_strength",
                    screenRedEffectStrength,
                    0.0,
                    3.0
            );
            screenBlueEffectStrength = clampedNumber(
                    feedback,
                    "screen_blue_effect_strength",
                    screenBlueEffectStrength,
                    0.0,
                    3.0
            );
            hitReactionAnimationStrength = clampedNumber(
                    feedback,
                    "hit_reaction_animation_strength",
                    hitReactionAnimationStrength,
                    0.0,
                    3.0
            );
            hitReactionReturnTicks = clampedInt(feedback, "hit_reaction_return_ticks", hitReactionReturnTicks, 1, 60);

            JsonObject debug = object(root, "debug", root);
            showDebugMessages = bool(debug, "show_messages", showDebugMessages);
        } catch (Exception exception) {
            KingdomComeCombat.LOGGER.warn("Failed to load combat client config, using defaults.", exception);
        }
        updateSignature();
    }

    public static void save() {
        write(configPath());
        updateSignature();
    }

    public static double bloodPrimaryNoiseScale() {
        return bloodPrimaryNoiseScale;
    }

    public static double bloodDetailNoiseScale() {
        return bloodDetailNoiseScale;
    }

    public static double bloodDripNoiseScale() {
        return bloodDripNoiseScale;
    }

    public static double bloodMinimumBlendStrength() {
        return bloodMinimumBlendStrength;
    }

    public static double bloodMaximumBlendStrength() {
        return bloodMaximumBlendStrength;
    }

    public static double bloodNoiseContrast() {
        return bloodNoiseContrast;
    }

    public static int signature() {
        return signature;
    }

    public static boolean renderBlood() {
        return renderBlood;
    }

    public static boolean renderArmorHoles() {
        return renderArmorHoles;
    }

    public static boolean renderPlayerBlood() {
        return renderPlayerBlood;
    }

    public static void setRenderPlayerBlood(boolean value) {
        renderPlayerBlood = value;
    }

    public static boolean playerBloodOverlayCompat() {
        return playerBloodOverlayCompat;
    }

    public static void setPlayerBloodOverlayCompat(boolean value) {
        playerBloodOverlayCompat = value;
    }

    public static boolean disableVanillaLeftHandedMobs() {
        return disableVanillaLeftHandedMobs;
    }

    public static void setDisableVanillaLeftHandedMobs(boolean value) {
        disableVanillaLeftHandedMobs = value;
    }

    public static boolean armorPhysicsCompat() {
        return armorPhysicsCompat;
    }

    public static void setRenderBlood(boolean value) {
        renderBlood = value;
    }

    public static void setRenderArmorHoles(boolean value) {
        renderArmorHoles = value;
    }

    public static boolean bladeTrailsEnabled() {
        return bladeTrailsEnabled;
    }

    public static void setBladeTrailsEnabled(boolean value) {
        bladeTrailsEnabled = value;
    }

    public static void setArmorPhysicsCompat(boolean value) {
        armorPhysicsCompat = value;
    }

    public static boolean showLockOnCrosshair() {
        return showLockOnCrosshair;
    }

    public static void setShowLockOnCrosshair(boolean value) {
        showLockOnCrosshair = value;
    }

    public static boolean autoLockOnHit() {
        return autoLockOnHit;
    }

    public static void setAutoLockOnHit(boolean value) {
        autoLockOnHit = value;
    }

    public static double lockOnAcquireAngleDegrees() {
        return lockOnAcquireAngleDegrees;
    }

    public static void setLockOnAcquireAngleDegrees(double value) {
        lockOnAcquireAngleDegrees = clamp(value, 10.0, 120.0);
    }

    public static double lockOnReleaseDistance() {
        return lockOnReleaseDistance;
    }

    public static void setLockOnReleaseDistance(double value) {
        lockOnReleaseDistance = clamp(value, 3.0, 32.0);
    }

    public static double lockOnSwitchAngleDegrees() {
        return lockOnSwitchAngleDegrees;
    }

    public static void setLockOnSwitchAngleDegrees(double value) {
        lockOnSwitchAngleDegrees = clamp(value, 10.0, 140.0);
    }

    public static double lockOnSwitchSideThreshold() {
        return lockOnSwitchSideThreshold;
    }

    public static void setLockOnSwitchSideThreshold(double value) {
        lockOnSwitchSideThreshold = clamp(value, 0.0, 1.0);
    }

    public static double lockOnCameraShoulderOffset() {
        return lockOnCameraShoulderOffset;
    }

    public static void setLockOnCameraShoulderOffset(double value) {
        lockOnCameraShoulderOffset = clamp(value, 0.0, 3.0);
    }

    public static double lockOnCameraHeightOffset() {
        return lockOnCameraHeightOffset;
    }

    public static void setLockOnCameraHeightOffset(double value) {
        lockOnCameraHeightOffset = clamp(value, -1.0, 1.0);
    }

    public static double lockOnCameraPullIn() {
        return lockOnCameraPullIn;
    }

    public static void setLockOnCameraPullIn(double value) {
        lockOnCameraPullIn = clamp(value, 0.0, 4.0);
    }

    public static double lockOnCameraAimLeftOffset() {
        return lockOnCameraAimLeftOffset;
    }

    public static void setLockOnCameraAimLeftOffset(double value) {
        lockOnCameraAimLeftOffset = clamp(value, -2.0, 2.0);
    }

    public static double lockOnCameraYawFollowSpeed() {
        return lockOnCameraYawFollowSpeed;
    }

    public static void setLockOnCameraYawFollowSpeed(double value) {
        lockOnCameraYawFollowSpeed = clamp(value, 1.0, 18.0);
    }

    public static double lockOnCameraPitchFollowSpeed() {
        return lockOnCameraPitchFollowSpeed;
    }

    public static void setLockOnCameraPitchFollowSpeed(double value) {
        lockOnCameraPitchFollowSpeed = clamp(value, 1.0, 24.0);
    }

    public static double lockOnCameraTransitionSpeed() {
        return lockOnCameraTransitionSpeed;
    }

    public static void setLockOnCameraTransitionSpeed(double value) {
        lockOnCameraTransitionSpeed = clamp(value, 1.0, 30.0);
    }

    public static double lockOnFirstPersonCameraForwardOffset() {
        return lockOnFirstPersonCameraForwardOffset;
    }

    public static void setLockOnFirstPersonCameraForwardOffset(double value) {
        lockOnFirstPersonCameraForwardOffset = clamp(value, -1.0, 1.0);
    }

    public static double sparkParticlePercent() {
        return sparkParticlePercent;
    }

    public static void setSparkParticlePercent(double value) {
        sparkParticlePercent = Math.max(0.0, value);
    }

    public static double bloodParticlePercent() {
        return bloodParticlePercent;
    }

    public static void setBloodParticlePercent(double value) {
        bloodParticlePercent = Math.max(0.0, value);
    }

    public static double bloodMistOpacity() {
        return bloodMistOpacity;
    }

    public static void setBloodMistOpacity(double value) {
        bloodMistOpacity = clamp(value, 0.0, 3.0);
    }

    public static double bloodStainPercent() {
        return bloodStainPercent;
    }

    public static void setBloodStainPercent(double value) {
        bloodStainPercent = Math.max(0.0, value);
    }

    public static boolean showDebugMessages() {
        return showDebugMessages;
    }

    public static void setShowDebugMessages(boolean value) {
        showDebugMessages = value;
    }

    public static boolean screenEffectsEnabled() {
        return screenEffectsEnabled;
    }

    public static void setScreenEffectsEnabled(boolean enabled) {
        screenEffectsEnabled = enabled;
    }

    public static boolean hurtCameraMovementEnabled() {
        return hurtCameraMovementEnabled;
    }

    public static void setHurtCameraMovementEnabled(boolean enabled) {
        hurtCameraMovementEnabled = enabled;
    }

    public static double screenRedEffectStrength() {
        return screenRedEffectStrength;
    }

    public static void setScreenRedEffectStrength(double value) {
        screenRedEffectStrength = clamp(value, 0.0, 3.0);
    }

    public static double screenBlueEffectStrength() {
        return screenBlueEffectStrength;
    }

    public static void setScreenBlueEffectStrength(double value) {
        screenBlueEffectStrength = clamp(value, 0.0, 3.0);
    }

    public static double hitReactionAnimationStrength() {
        return hitReactionAnimationStrength;
    }

    public static void setHitReactionAnimationStrength(double value) {
        hitReactionAnimationStrength = clamp(value, 0.0, 3.0);
    }

    public static int hitReactionReturnTicks() {
        return hitReactionReturnTicks;
    }

    public static void setHitReactionReturnTicks(int ticks) {
        hitReactionReturnTicks = clamp(ticks, 1, 60);
    }

    public static FirstPersonModelMode firstPersonModelMode() {
        return firstPersonModelMode;
    }

    public static void setFirstPersonModelMode(FirstPersonModelMode mode) {
        firstPersonModelMode = mode == null ? FirstPersonModelMode.WHEN_NEEDED : mode;
    }

    public static List<String> firstPersonModelDisabledItemIds() {
        return firstPersonModelDisabledItems.stream().map(Identifier::toString).toList();
    }

    public static void setFirstPersonModelDisabledItemIds(Iterable<String> values) {
        firstPersonModelDisabledItems.clear();
        for (String value : values) {
            Identifier id = Identifier.tryParse(value == null ? "" : value.trim());
            if (id != null) {
                firstPersonModelDisabledItems.add(id);
            }
        }
    }

    public static boolean disablesSpecialFirstPerson(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && firstPersonModelDisabledItems.contains(Registries.ITEM.getId(stack.getItem()));
    }

    public static double firstPersonBodyForwardOffset() {
        return firstPersonBodyForwardOffset;
    }

    public static void setFirstPersonBodyForwardOffset(double value) {
        firstPersonBodyForwardOffset = clamp(value, -1.0, 1.0);
    }

    public static double firstPersonCameraForwardOffset() {
        return firstPersonCameraForwardOffset;
    }

    public static void setFirstPersonCameraForwardOffset(double value) {
        firstPersonCameraForwardOffset = clamp(value, -0.5, 0.5);
    }

    public static boolean firstPersonCameraHeadBindingEnabled() {
        return firstPersonCameraHeadBindingEnabled;
    }

    public static void setFirstPersonCameraHeadBindingEnabled(boolean enabled) {
        firstPersonCameraHeadBindingEnabled = enabled;
    }

    public static boolean firstPersonSimpleEyeSimulationEnabled() {
        return firstPersonSimpleEyeSimulationEnabled;
    }

    public static void setFirstPersonSimpleEyeSimulationEnabled(boolean enabled) {
        firstPersonSimpleEyeSimulationEnabled = enabled;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static void write(Path path) {
        JsonObject root = new JsonObject();
        JsonObject rendering = new JsonObject();
        rendering.addProperty("enable_blood", renderBlood);
        rendering.addProperty("enable_player_blood", renderPlayerBlood);
        rendering.addProperty("player_blood_overlay_compat", playerBloodOverlayCompat);
        rendering.addProperty("enable_armor_holes", renderArmorHoles);
        rendering.addProperty("enable_blade_trails", bladeTrailsEnabled);
        rendering.addProperty("enable_armor_physics_compat", armorPhysicsCompat);
        rendering.addProperty("disable_vanilla_left_handed_mobs", disableVanillaLeftHandedMobs);
        rendering.addProperty("enable_screen_effects", screenEffectsEnabled);
        rendering.addProperty("enable_hurt_camera_movement", hurtCameraMovementEnabled);
        root.add("rendering", rendering);

        JsonObject firstPerson = new JsonObject();
        firstPerson.addProperty("model_mode", firstPersonModelMode.configName());
        firstPerson.addProperty("body_forward_offset", firstPersonBodyForwardOffset);
        firstPerson.addProperty("camera_forward_offset", firstPersonCameraForwardOffset);
        firstPerson.addProperty("bind_camera_to_head_front", firstPersonCameraHeadBindingEnabled);
        firstPerson.addProperty("simple_eye_simulation", firstPersonSimpleEyeSimulationEnabled);
        JsonArray disabledItems = new JsonArray();
        for (Identifier id : firstPersonModelDisabledItems) {
            disabledItems.add(id.toString());
        }
        firstPerson.add("disabled_items", disabledItems);
        root.add("first_person", firstPerson);

        JsonObject blood = new JsonObject();
        blood.addProperty("primary_noise_scale", bloodPrimaryNoiseScale);
        blood.addProperty("detail_noise_scale", bloodDetailNoiseScale);
        blood.addProperty("drip_noise_scale", bloodDripNoiseScale);
        blood.addProperty("minimum_blend_strength", bloodMinimumBlendStrength);
        blood.addProperty("maximum_blend_strength", bloodMaximumBlendStrength);
        blood.addProperty("noise_contrast", bloodNoiseContrast);
        blood.addProperty("particle_percent", bloodParticlePercent);
        blood.addProperty("stain_percent", bloodStainPercent);
        root.add("blood", blood);

        JsonObject lockOn = new JsonObject();
        lockOn.addProperty("show_crosshair", showLockOnCrosshair);
        lockOn.addProperty("auto_lock_on_hit", autoLockOnHit);
        lockOn.addProperty("crosshair_target_angle_degrees", lockOnAcquireAngleDegrees);
        lockOn.addProperty("crosshair_release_distance", lockOnReleaseDistance);
        lockOn.addProperty("crosshair_switch_angle_degrees", lockOnSwitchAngleDegrees);
        lockOn.addProperty("crosshair_switch_side_threshold", lockOnSwitchSideThreshold);
        lockOn.addProperty("camera_shoulder_offset", lockOnCameraShoulderOffset);
        lockOn.addProperty("camera_height_offset", lockOnCameraHeightOffset);
        lockOn.addProperty("camera_pull_in", lockOnCameraPullIn);
        lockOn.addProperty("camera_aim_left_offset", lockOnCameraAimLeftOffset);
        lockOn.addProperty("camera_yaw_follow_speed", lockOnCameraYawFollowSpeed);
        lockOn.addProperty("camera_pitch_follow_speed", lockOnCameraPitchFollowSpeed);
        lockOn.addProperty("camera_transition_speed", lockOnCameraTransitionSpeed);
        lockOn.addProperty("first_person_camera_forward_offset", lockOnFirstPersonCameraForwardOffset);
        root.add("lock_on", lockOn);

        JsonObject particles = new JsonObject();
        particles.addProperty("spark_percent", sparkParticlePercent);
        particles.addProperty("blood_percent", bloodParticlePercent);
        particles.addProperty("blood_mist_opacity", bloodMistOpacity);
        root.add("particles", particles);

        JsonObject feedback = new JsonObject();
        feedback.addProperty("screen_red_effect_strength", screenRedEffectStrength);
        feedback.addProperty("screen_blue_effect_strength", screenBlueEffectStrength);
        feedback.addProperty("hit_reaction_animation_strength", hitReactionAnimationStrength);
        feedback.addProperty("hit_reaction_return_ticks", hitReactionReturnTicks);
        root.add("feedback", feedback);

        JsonObject debug = new JsonObject();
        debug.addProperty("show_messages", showDebugMessages);
        root.add("debug", debug);

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException exception) {
            KingdomComeCombat.LOGGER.warn("Failed to write default combat client config.", exception);
        }
    }

    private static JsonObject object(JsonObject parent, String key, JsonObject fallback) {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject()
                ? parent.getAsJsonObject(key)
                : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return object.get(key).getAsBoolean();
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return object.get(key).getAsString();
    }

    public enum FirstPersonModelMode {
        WHEN_NEEDED("when_needed", "仅需要时启动"),
        ALWAYS("always", "保持启动"),
        LOCKED_ONLY("locked_only", "仅锁定时启用");

        private final String configName;
        private final String displayName;

        FirstPersonModelMode(String configName, String displayName) {
            this.configName = configName;
            this.displayName = displayName;
        }

        public String configName() {
            return configName;
        }

        public String displayName() {
            return displayName;
        }

        public FirstPersonModelMode next() {
            FirstPersonModelMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        private static FirstPersonModelMode fromConfigName(String value) {
            for (FirstPersonModelMode mode : values()) {
                if (mode.configName.equalsIgnoreCase(value)) {
                    return mode;
                }
            }
            return WHEN_NEEDED;
        }
    }

    private static double positiveNumber(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        double value = object.get(key).getAsDouble();
        return value > 0.0 ? value : fallback;
    }

    private static double percentNumber(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return Math.max(0.0, object.get(key).getAsDouble());
    }

    private static double rangedNumber(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return Math.max(0.0, Math.min(1.0, object.get(key).getAsDouble()));
    }

    private static double clampedNumber(JsonObject object, String key, double fallback, double min, double max) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return clamp(object.get(key).getAsDouble(), min, max);
    }

    private static int clampedInt(JsonObject object, String key, int fallback, int min, int max) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        return clamp(object.get(key).getAsInt(), min, max);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void updateSignature() {
        int result = Double.hashCode(bloodPrimaryNoiseScale);
        result = 31 * result + Double.hashCode(bloodDetailNoiseScale);
        result = 31 * result + Double.hashCode(bloodDripNoiseScale);
        result = 31 * result + Double.hashCode(bloodMinimumBlendStrength);
        result = 31 * result + Double.hashCode(bloodMaximumBlendStrength);
        result = 31 * result + Double.hashCode(bloodNoiseContrast);
        result = 31 * result + Boolean.hashCode(renderBlood);
        result = 31 * result + Boolean.hashCode(renderArmorHoles);
        result = 31 * result + Boolean.hashCode(armorPhysicsCompat);
        result = 31 * result + Double.hashCode(hitReactionAnimationStrength);
        result = 31 * result + Integer.hashCode(hitReactionReturnTicks);
        signature = result;
    }
}
