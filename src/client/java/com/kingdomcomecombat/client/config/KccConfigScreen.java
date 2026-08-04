package com.kingdomcomecombat.client.config;

import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.network.UpdatePlayerInterruptConfigPayload;
import com.kingdomcomecombat.network.UpdateServerConfigPayload;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.StreamSupport;

public final class KccConfigScreen {
    private KccConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Kingdom Come Combat"));
        ConfigEntryBuilder entries = builder.entryBuilder();

        addRenderingCategory(builder, entries);
        addFirstPersonCategory(builder, entries);
        addLockOnCategory(builder, entries);
        addEffectsCategory(builder, entries);
        addDebugCategory(builder, entries);
        addServerCategory(builder, entries);

        builder.setSavingRunnable(CombatClientConfig::save);
        return builder.build();
    }

    private static void addRenderingCategory(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(tr("category.rendering"));
        addBoolean(category, entries, "render_blood", CombatClientConfig.renderBlood(),
                CombatClientConfig::setRenderBlood);
        addBoolean(category, entries, "render_player_blood", CombatClientConfig.renderPlayerBlood(),
                CombatClientConfig::setRenderPlayerBlood);
        addBoolean(category, entries, "player_blood_overlay_compat", CombatClientConfig.playerBloodOverlayCompat(),
                CombatClientConfig::setPlayerBloodOverlayCompat);
        addBoolean(category, entries, "render_armor_holes", CombatClientConfig.renderArmorHoles(),
                CombatClientConfig::setRenderArmorHoles);
        addBoolean(category, entries, "blade_trails", CombatClientConfig.bladeTrailsEnabled(),
                CombatClientConfig::setBladeTrailsEnabled);
        addBoolean(category, entries, "armor_physics_compat", CombatClientConfig.armorPhysicsCompat(),
                CombatClientConfig::setArmorPhysicsCompat);
        addBoolean(category, entries, "screen_effects", CombatClientConfig.screenEffectsEnabled(),
                CombatClientConfig::setScreenEffectsEnabled);
        addBoolean(category, entries, "hurt_camera_movement", CombatClientConfig.hurtCameraMovementEnabled(),
                CombatClientConfig::setHurtCameraMovementEnabled);
        addBoolean(category, entries, "dodge_animation", CombatClientConfig.dodgeAnimationEnabled(),
                CombatClientConfig::setDodgeAnimationEnabled);
        addBoolean(category, entries, "cauldron_face_wash", CombatClientConfig.cauldronFaceWashEnabled(),
                CombatClientConfig::setCauldronFaceWashEnabled);
    }

    private static void addFirstPersonCategory(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(tr("category.first_person"));
        addBoolean(category, entries, "first_person.enable_rendering",
                CombatClientConfig.firstPersonRenderingEnabled(),
                CombatClientConfig::setFirstPersonRenderingEnabled);
        addBoolean(category, entries, "first_person.weapon_only",
                CombatClientConfig.firstPersonWeaponOnly(),
                CombatClientConfig::setFirstPersonWeaponOnly);
        category.addEntry(entries.startEnumSelector(
                        tr("first_person.mode"),
                        CombatClientConfig.FirstPersonModelMode.class,
                        CombatClientConfig.firstPersonModelMode()
                )
                .setEnumNameProvider(value -> tr("first_person.mode."
                        + ((CombatClientConfig.FirstPersonModelMode) value).configName()))
                .setDefaultValue(CombatClientConfig.FirstPersonModelMode.WHEN_NEEDED)
                .setSaveConsumer(CombatClientConfig::setFirstPersonModelMode)
                .setTooltip(tr("first_person.mode.tooltip"))
                .build());
        category.addEntry(entries.startStrList(
                        tr("first_person.disabled_items"),
                        new ArrayList<>(CombatClientConfig.firstPersonModelDisabledItemIds())
                )
                .setDefaultValue(new ArrayList<>())
                .setExpanded(true)
                .setCellErrorSupplier(KccConfigScreen::validateItemId)
                .setSaveConsumer(CombatClientConfig::setFirstPersonModelDisabledItemIds)
                .setTooltip(tr("first_person.disabled_items.tooltip"))
                .build());
        addDouble(category, entries, "first_person.body_forward_offset",
                CombatClientConfig.firstPersonBodyForwardOffset(), -1.0, 1.0,
                CombatClientConfig::setFirstPersonBodyForwardOffset);
        addBoolean(category, entries, "first_person.bind_head_front",
                CombatClientConfig.firstPersonCameraHeadBindingEnabled(),
                CombatClientConfig::setFirstPersonCameraHeadBindingEnabled);
        addBoolean(category, entries, "first_person.simple_eye_simulation",
                CombatClientConfig.firstPersonSimpleEyeSimulationEnabled(),
                CombatClientConfig::setFirstPersonSimpleEyeSimulationEnabled);
        addDouble(category, entries, "first_person.camera_forward_offset",
                CombatClientConfig.firstPersonCameraForwardOffset(), -0.5, 0.5,
                CombatClientConfig::setFirstPersonCameraForwardOffset);
    }

    private static void addLockOnCategory(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(tr("category.lock_on"));
        addBoolean(category, entries, "lock_on.show_crosshair", CombatClientConfig.showLockOnCrosshair(),
                CombatClientConfig::setShowLockOnCrosshair);
        addBoolean(category, entries, "lock_on.show_target_stamina_ring", CombatClientConfig.showTargetStaminaRing(),
                CombatClientConfig::setShowTargetStaminaRing);
        addDouble(category, entries, "lock_on.target_stamina_ring_scale",
                CombatClientConfig.targetStaminaRingScale(), 0.5, 2.0,
                CombatClientConfig::setTargetStaminaRingScale);
        addDouble(category, entries, "lock_on.crosshair_scale",
                CombatClientConfig.lockOnCrosshairScale(), 0.5, 2.0,
                CombatClientConfig::setLockOnCrosshairScale);
        addBoolean(category, entries, "lock_on.auto_on_hit", CombatClientConfig.autoLockOnHit(),
                CombatClientConfig::setAutoLockOnHit);
        category.addEntry(entries.startBooleanToggle(
                        tr("lock_on.third_party_right_click_outside_lock"),
                        CombatClientConfig.thirdPartyRightClickOutsideLockEnabled()
                )
                .setDefaultValue(true)
                .setSaveConsumer(CombatClientConfig::setThirdPartyRightClickOutsideLockEnabled)
                .setTooltip(tr("lock_on.third_party_right_click_outside_lock.tooltip"))
                .build());
        addDouble(category, entries, "lock_on.acquire_angle", CombatClientConfig.lockOnAcquireAngleDegrees(), 10.0, 120.0,
                CombatClientConfig::setLockOnAcquireAngleDegrees);
        addDouble(category, entries, "lock_on.release_distance", CombatClientConfig.lockOnReleaseDistance(), 3.0, 32.0,
                CombatClientConfig::setLockOnReleaseDistance);
        addDouble(category, entries, "lock_on.switch_angle", CombatClientConfig.lockOnSwitchAngleDegrees(), 10.0, 140.0,
                CombatClientConfig::setLockOnSwitchAngleDegrees);
        addDouble(category, entries, "lock_on.switch_threshold", CombatClientConfig.lockOnSwitchSideThreshold(), 0.0, 1.0,
                CombatClientConfig::setLockOnSwitchSideThreshold);
        addDouble(category, entries, "lock_on.shoulder_offset", CombatClientConfig.lockOnCameraShoulderOffset(), 0.0, 3.0,
                CombatClientConfig::setLockOnCameraShoulderOffset);
        addDouble(category, entries, "lock_on.height_offset", CombatClientConfig.lockOnCameraHeightOffset(), -0.5, 0.6,
                CombatClientConfig::setLockOnCameraHeightOffset);
        addDouble(category, entries, "lock_on.pull_in", CombatClientConfig.lockOnCameraPullIn(), 0.0, 4.0,
                CombatClientConfig::setLockOnCameraPullIn);
        addDouble(category, entries, "lock_on.aim_left_offset", CombatClientConfig.lockOnCameraAimLeftOffset(), -1.0, 1.5,
                CombatClientConfig::setLockOnCameraAimLeftOffset);
        addDouble(category, entries, "lock_on.yaw_speed", CombatClientConfig.lockOnCameraYawFollowSpeed(), 1.0, 18.0,
                CombatClientConfig::setLockOnCameraYawFollowSpeed);
        addDouble(category, entries, "lock_on.pitch_speed", CombatClientConfig.lockOnCameraPitchFollowSpeed(), 1.0, 24.0,
                CombatClientConfig::setLockOnCameraPitchFollowSpeed);
        addDouble(category, entries, "lock_on.transition_speed", CombatClientConfig.lockOnCameraTransitionSpeed(), 1.0, 30.0,
                CombatClientConfig::setLockOnCameraTransitionSpeed);
        addDouble(category, entries, "lock_on.first_person_forward_offset",
                CombatClientConfig.lockOnFirstPersonCameraForwardOffset(), -1.0, 1.0,
                CombatClientConfig::setLockOnFirstPersonCameraForwardOffset);
        addDouble(category, entries, "lock_on.stance_gesture_sensitivity",
                CombatClientConfig.stanceGestureSensitivity(), 0.05, 4.0,
                CombatClientConfig::setStanceGestureSensitivity);
    }

    private static void addEffectsCategory(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(tr("category.effects"));
        addPercent(category, entries, "effects.screen_red_effect_strength",
                CombatClientConfig.screenRedEffectStrength(), 300,
                CombatClientConfig::setScreenRedEffectStrength);
        addPercent(category, entries, "effects.screen_blue_effect_strength",
                CombatClientConfig.screenBlueEffectStrength(), 300,
                CombatClientConfig::setScreenBlueEffectStrength);
        addPercent(category, entries, "effects.hit_reaction_animation_strength",
                CombatClientConfig.hitReactionAnimationStrength(), 300,
                CombatClientConfig::setHitReactionAnimationStrength);
        addIntegerSlider(category, entries, "effects.hit_reaction_return_ticks",
                CombatClientConfig.hitReactionReturnTicks(), 1, 60,
                CombatClientConfig::setHitReactionReturnTicks);
        addPercent(category, entries, "effects.spark_percent", CombatClientConfig.sparkParticlePercent(), 200,
                CombatClientConfig::setSparkParticlePercent);
        addPercent(category, entries, "effects.blood_particle_percent", CombatClientConfig.bloodParticlePercent(), 200,
                CombatClientConfig::setBloodParticlePercent);
        addPercent(category, entries, "effects.blood_trace_particle_percent", CombatClientConfig.bloodTraceParticlePercent(), 1000,
                CombatClientConfig::setBloodTraceParticlePercent);
        addPercent(category, entries, "effects.blood_mist_opacity", CombatClientConfig.bloodMistOpacity(), 300,
                CombatClientConfig::setBloodMistOpacity);
        addPercent(category, entries, "effects.blood_stain_percent", CombatClientConfig.bloodStainPercent(), 200,
                CombatClientConfig::setBloodStainPercent);
    }

    private static void addDebugCategory(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(tr("category.debug"));
        addBoolean(category, entries, "debug.messages", CombatClientConfig.showDebugMessages(),
                CombatClientConfig::setShowDebugMessages);
    }

    private static void addServerCategory(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(tr(ClientServerConfigState.canEdit()
                ? "category.server" : "category.server_read_only"));
        category.addEntry(entries.startBooleanToggle(
                        tr("server.lightweight_damage_mode"), ClientServerConfigState.lightweightDamageModeEnabled())
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(enabled -> {
                    if (!ClientServerConfigState.canEdit()) return;
                    ClientServerConfigState.setLightweightDamageModeEnabled(enabled);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.lightweight_damage_mode.tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.lightweight_blocking_mode"), ClientServerConfigState.lightweightBlockingModeEnabled())
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(enabled -> {
                    if (!ClientServerConfigState.canEdit()) return;
                    ClientServerConfigState.setLightweightBlockingModeEnabled(enabled);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.lightweight_blocking_mode.tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.equipment_generation"),
                        ClientServerConfigState.modEquipmentGenerationEnabled()
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(enabled -> {
                    if (!ClientServerConfigState.canEdit()) {
                        return;
                    }
                    ClientServerConfigState.update(
                            enabled,
                            ClientServerConfigState.zombieLeaderHealthFixEnabled(),
                            ClientServerConfigState.mobToughnessEnabled(),
                            ClientServerConfigState.hitStopTicks(),
                            ClientServerConfigState.vanillaHurtSoundVolumeMultiplier(),
                            ClientServerConfigState.masterCounterWindowTicks(),
                            ClientServerConfigState.blockWindowTicks(),
                            ClientServerConfigState.vanillaAttackWeaponIds(),
                            true
                    );
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.permission_tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.zombie_leader_health_fix"),
                        ClientServerConfigState.zombieLeaderHealthFixEnabled()
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(enabled -> {
                    if (!ClientServerConfigState.canEdit()) {
                        return;
                    }
                    ClientServerConfigState.update(
                            ClientServerConfigState.modEquipmentGenerationEnabled(),
                            enabled,
                            ClientServerConfigState.mobToughnessEnabled(),
                            ClientServerConfigState.hitStopTicks(),
                            ClientServerConfigState.vanillaHurtSoundVolumeMultiplier(),
                            ClientServerConfigState.masterCounterWindowTicks(),
                            ClientServerConfigState.blockWindowTicks(),
                            ClientServerConfigState.vanillaAttackWeaponIds(),
                            true
                    );
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.permission_tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.mob_toughness"),
                        ClientServerConfigState.mobToughnessEnabled()
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(enabled -> {
                    if (!ClientServerConfigState.canEdit()) {
                        return;
                    }
                    ClientServerConfigState.update(
                            ClientServerConfigState.modEquipmentGenerationEnabled(),
                            ClientServerConfigState.zombieLeaderHealthFixEnabled(),
                            enabled,
                            ClientServerConfigState.hitStopTicks(),
                            ClientServerConfigState.vanillaHurtSoundVolumeMultiplier(),
                            ClientServerConfigState.masterCounterWindowTicks(),
                            ClientServerConfigState.blockWindowTicks(),
                            ClientServerConfigState.vanillaAttackWeaponIds(),
                            true
                    );
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.mob_toughness.tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.always_player_interrupt"),
                        ClientServerConfigState.alwaysEnablePlayerInterrupt()
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(enabled -> {
                    if (!ClientServerConfigState.canEdit()) {
                        return;
                    }
                    ClientServerConfigState.setAlwaysEnablePlayerInterrupt(enabled);
                    ClientPlayNetworking.send(new UpdatePlayerInterruptConfigPayload(enabled));
                })
                .setTooltip(tr("server.always_player_interrupt.tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.experimental_illager_undead_hostility"),
                        ClientServerConfigState.experimentalIllagerUndeadHostilityEnabled()
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(enabled -> {
                    if (!ClientServerConfigState.canEdit()) return;
                    ClientServerConfigState.setExperimentalIllagerUndeadHostilityEnabled(enabled);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.experimental_illager_undead_hostility.tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.disable_left_handed_mobs"),
                        ClientServerConfigState.disableVanillaLeftHandedMobs()
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(disabled -> {
                    if (!ClientServerConfigState.canEdit()) return;
                    ClientServerConfigState.setDisableVanillaLeftHandedMobs(disabled);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.permission_tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.ender_dragon_overhaul"),
                        ClientServerConfigState.enderDragonOverhaulEnabled()
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(enabled -> {
                    if (!ClientServerConfigState.canEdit()) return;
                    ClientServerConfigState.setEnderDragonOverhaulEnabled(enabled);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.permission_tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.legacy_collision_calculation"),
                        ClientServerConfigState.legacyCollisionCalculationEnabled()
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(enabled -> {
                    if (!ClientServerConfigState.canEdit()) return;
                    ClientServerConfigState.setLegacyCollisionCalculationEnabled(enabled);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.legacy_collision_calculation.tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.client_projectile_hurtbox"),
                        ClientServerConfigState.clientProjectileHurtboxEnabled()
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(enabled -> {
                    if (!ClientServerConfigState.canEdit()) return;
                    ClientServerConfigState.setClientProjectileHurtboxEnabled(enabled);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.client_projectile_hurtbox.tooltip"))
                .build());
        category.addEntry(entries.startIntSlider(
                        tr("server.hit_stop_ticks"),
                        ClientServerConfigState.hitStopTicks(),
                        0,
                        20
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(ticks -> {
                    if (!ClientServerConfigState.canEdit()) {
                        return;
                    }
                    ClientServerConfigState.update(
                            ClientServerConfigState.modEquipmentGenerationEnabled(),
                            ClientServerConfigState.zombieLeaderHealthFixEnabled(),
                            ClientServerConfigState.mobToughnessEnabled(),
                            ticks,
                            ClientServerConfigState.vanillaHurtSoundVolumeMultiplier(),
                            ClientServerConfigState.masterCounterWindowTicks(),
                            ClientServerConfigState.blockWindowTicks(),
                            ClientServerConfigState.vanillaAttackWeaponIds(),
                            true
                    );
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.permission_tooltip"))
                .build());
        category.addEntry(entries.startIntSlider(
                        tr("server.vanilla_hurt_sound_volume_multiplier"),
                        (int) Math.round(ClientServerConfigState.vanillaHurtSoundVolumeMultiplier() * 100.0),
                        0,
                        200
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(percent -> {
                    if (!ClientServerConfigState.canEdit()) {
                        return;
                    }
                    double multiplier = percent / 100.0;
                    ClientServerConfigState.update(
                            ClientServerConfigState.modEquipmentGenerationEnabled(),
                            ClientServerConfigState.zombieLeaderHealthFixEnabled(),
                            ClientServerConfigState.mobToughnessEnabled(),
                            ClientServerConfigState.hitStopTicks(),
                            multiplier,
                            ClientServerConfigState.masterCounterWindowTicks(),
                            ClientServerConfigState.blockWindowTicks(),
                            ClientServerConfigState.vanillaAttackWeaponIds(),
                            true
                    );
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.permission_tooltip"))
                .build());
        category.addEntry(entries.startIntSlider(
                        tr("server.master_counter_window_ticks"),
                        ClientServerConfigState.masterCounterWindowTicks(),
                        0,
                        40
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(ticks -> {
                    if (!ClientServerConfigState.canEdit()) {
                        return;
                    }
                    ClientServerConfigState.update(
                            ClientServerConfigState.modEquipmentGenerationEnabled(),
                            ClientServerConfigState.zombieLeaderHealthFixEnabled(),
                            ClientServerConfigState.mobToughnessEnabled(),
                            ClientServerConfigState.hitStopTicks(),
                            ClientServerConfigState.vanillaHurtSoundVolumeMultiplier(),
                            ticks,
                            ClientServerConfigState.blockWindowTicks(),
                            ClientServerConfigState.vanillaAttackWeaponIds(),
                            true
                    );
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.permission_tooltip"))
                .build());
        category.addEntry(entries.startIntSlider(
                        tr("server.block_window_ticks"),
                        ClientServerConfigState.blockWindowTicks(),
                        0,
                        40
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(ticks -> {
                    if (!ClientServerConfigState.canEdit()) {
                        return;
                    }
                    ClientServerConfigState.update(
                            ClientServerConfigState.modEquipmentGenerationEnabled(),
                            ClientServerConfigState.zombieLeaderHealthFixEnabled(),
                            ClientServerConfigState.mobToughnessEnabled(),
                            ClientServerConfigState.hitStopTicks(),
                            ClientServerConfigState.vanillaHurtSoundVolumeMultiplier(),
                            ClientServerConfigState.masterCounterWindowTicks(),
                            ticks,
                            ClientServerConfigState.vanillaAttackWeaponIds(),
                            true
                    );
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.permission_tooltip"))
                .build());
        category.addEntry(entries.startIntSlider(
                        tr("server.unperfect_block_window_ticks"),
                        ClientServerConfigState.unperfectBlockWindowTicks(),
                        0,
                        40
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(ticks -> {
                    if (!ClientServerConfigState.canEdit()) {
                        return;
                    }
                    ClientServerConfigState.setUnperfectBlockWindowTicks(ticks);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.permission_tooltip"))
                .build());
        category.addEntry(entries.startIntSlider(
                        tr("server.combat_min_distance"),
                        (int) Math.round(ClientServerConfigState.combatMinDistance() * 100.0),
                        50,
                        400
                )
                .setTextGetter(value -> Text.literal(String.format(Locale.ROOT, "%.2f m", value / 100.0)))
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(value -> {
                    if (!ClientServerConfigState.canEdit()) {
                        return;
                    }
                    ClientServerConfigState.update(
                            ClientServerConfigState.modEquipmentGenerationEnabled(),
                            ClientServerConfigState.zombieLeaderHealthFixEnabled(),
                            ClientServerConfigState.mobToughnessEnabled(),
                            ClientServerConfigState.hitStopTicks(),
                            ClientServerConfigState.vanillaHurtSoundVolumeMultiplier(),
                            ClientServerConfigState.masterCounterWindowTicks(),
                            ClientServerConfigState.blockWindowTicks(),
                            value / 100.0,
                            ClientServerConfigState.vanillaAttackWeaponIds(),
                            true
                    );
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.combat_min_distance.tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.reach_attribute_hitbox_scaling"),
                        ClientServerConfigState.reachAttributeHitboxScalingEnabled())
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(enabled -> {
                    ClientServerConfigState.setReachAttributeHitboxScalingEnabled(enabled);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.reach_attribute_hitbox_scaling.tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.blocking_movement_slowdown"),
                        ClientServerConfigState.blockingMovementSlowdownEnabled())
                .setRequirement(ClientServerConfigState::canEdit)
                .setDefaultValue(true)
                .setSaveConsumer(enabled -> {
                    ClientServerConfigState.setBlockingMovementSlowdownEnabled(enabled);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.blocking_movement_slowdown.tooltip"))
                .build());
        category.addEntry(entries.startBooleanToggle(
                        tr("server.mounted_kcc_combat"),
                        ClientServerConfigState.mountedKccCombatEnabled())
                .setRequirement(ClientServerConfigState::canEdit)
                .setDefaultValue(false)
                .setSaveConsumer(enabled -> {
                    ClientServerConfigState.setMountedKccCombatEnabled(enabled);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.mounted_kcc_combat.tooltip"))
                .build());
        category.addEntry(entries.startIntSlider(
                        tr("server.collision_cache_radius"),
                        (int) Math.round(ClientServerConfigState.collisionCacheRadius()),
                        2,
                        32
                )
                .setTextGetter(value -> Text.literal(value + " m"))
                .setRequirement(ClientServerConfigState::canEdit)
                .setDefaultValue(8)
                .setSaveConsumer(value -> {
                    ClientServerConfigState.setCollisionCacheRadius(value);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.collision_cache_radius.tooltip"))
                .build());
        category.addEntry(entries.startIntSlider(
                        tr("server.reach_attribute_hitbox_scale_per_block"),
                        (int) Math.round(ClientServerConfigState.reachAttributeHitboxScalePerBlock() * 100.0),
                        0,
                        100
                )
                .setTextGetter(value -> Text.literal(value + "%"))
                .setRequirement(ClientServerConfigState::canEdit)
                .setDefaultValue(20)
                .setSaveConsumer(value -> {
                    ClientServerConfigState.setReachAttributeHitboxScalePerBlock(value / 100.0);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.reach_attribute_hitbox_scale_per_block.tooltip"))
                .build());
        category.addEntry(entries.startStrList(
                        tr("server.vanilla_attack_weapon_ids"),
                        new ArrayList<>(ClientServerConfigState.vanillaAttackWeaponIds())
                )
                .setRequirement(ClientServerConfigState::canEdit)
                .setDefaultValue(new ArrayList<>())
                .setExpanded(true)
                .setCellErrorSupplier(KccConfigScreen::validateItemId)
                .setSaveConsumer(itemIds -> {
                    if (!ClientServerConfigState.canEdit()) {
                        return;
                    }
                    ClientServerConfigState.update(
                            ClientServerConfigState.modEquipmentGenerationEnabled(),
                            ClientServerConfigState.zombieLeaderHealthFixEnabled(),
                            ClientServerConfigState.mobToughnessEnabled(),
                            ClientServerConfigState.hitStopTicks(),
                            ClientServerConfigState.vanillaHurtSoundVolumeMultiplier(),
                            ClientServerConfigState.masterCounterWindowTicks(),
                            ClientServerConfigState.blockWindowTicks(),
                            itemIds,
                            true
                    );
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.vanilla_attack_weapon_ids.tooltip"))
                .build());
        category.addEntry(entries.startStrList(
                        tr("server.vanilla_attack_entity_ids"),
                        new ArrayList<>(ClientServerConfigState.vanillaAttackEntityIds()))
                .setRequirement(ClientServerConfigState::canEdit)
                .setDefaultValue(new ArrayList<>())
                .setExpanded(true)
                .setCellErrorSupplier(KccConfigScreen::validateEntityId)
                .setSaveConsumer(ids -> {
                    ClientServerConfigState.setVanillaAttackEntityIds(ids);
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.vanilla_attack_entity_ids.tooltip"))
                .build());
        Identifier noSelection = Identifier.ofVanilla("air");
        java.util.List<Identifier> itemSelections = StreamSupport.stream(
                        Registries.ITEM.getIds().spliterator(), false)
                .filter(id -> !id.equals(noSelection))
                .sorted(java.util.Comparator.comparing(Identifier::toString))
                .toList();
        category.addEntry(entries.startDropdownMenu(
                        tr("server.vanilla_attack_weapon_picker"),
                        noSelection,
                        value -> {
                            Identifier id = Identifier.tryParse(value);
                            return id != null && Registries.ITEM.containsId(id) ? id : noSelection;
                        },
                        id -> id.equals(noSelection)
                                ? tr("server.vanilla_attack_weapon_picker.none")
                                : Text.literal(Registries.ITEM.get(id).getName().getString() + " — " + id)
                )
                .setSelections(itemSelections)
                .setSuggestionMode(true)
                .setDefaultValue(noSelection)
                .setRequirement(ClientServerConfigState::canEdit)
                .setSaveConsumer(selected -> {
                    if (!ClientServerConfigState.canEdit() || selected.equals(noSelection)) {
                        return;
                    }
                    ArrayList<String> itemIds = new ArrayList<>(ClientServerConfigState.vanillaAttackWeaponIds());
                    if (!itemIds.contains(selected.toString())) {
                        itemIds.add(selected.toString());
                    }
                    ClientServerConfigState.update(
                            ClientServerConfigState.modEquipmentGenerationEnabled(),
                            ClientServerConfigState.zombieLeaderHealthFixEnabled(),
                            ClientServerConfigState.mobToughnessEnabled(),
                            ClientServerConfigState.hitStopTicks(),
                            ClientServerConfigState.vanillaHurtSoundVolumeMultiplier(),
                            ClientServerConfigState.masterCounterWindowTicks(),
                            ClientServerConfigState.blockWindowTicks(),
                            itemIds,
                            true
                    );
                    sendServerConfigUpdate();
                })
                .setTooltip(tr("server.vanilla_attack_weapon_picker.tooltip"))
                .build());
    }

    private static void sendServerConfigUpdate() {
        ClientPlayNetworking.send(new UpdateServerConfigPayload(
                ClientServerConfigState.lightweightDamageModeEnabled(),
                ClientServerConfigState.lightweightBlockingModeEnabled(),
                ClientServerConfigState.modEquipmentGenerationEnabled(),
                ClientServerConfigState.zombieLeaderHealthFixEnabled(),
                ClientServerConfigState.mobToughnessEnabled(),
                ClientServerConfigState.hitStopTicks(),
                ClientServerConfigState.vanillaHurtSoundVolumeMultiplier(),
                ClientServerConfigState.masterCounterWindowTicks(),
                ClientServerConfigState.blockWindowTicks(),
                ClientServerConfigState.unperfectBlockWindowTicks(),
                ClientServerConfigState.combatMinDistance(),
                ClientServerConfigState.collisionCacheRadius(),
                ClientServerConfigState.experimentalIllagerUndeadHostilityEnabled(),
                ClientServerConfigState.disableVanillaLeftHandedMobs(),
                ClientServerConfigState.enderDragonOverhaulEnabled(),
                ClientServerConfigState.legacyCollisionCalculationEnabled(),
                ClientServerConfigState.clientProjectileHurtboxEnabled(),
                ClientServerConfigState.reachAttributeHitboxScalingEnabled(),
                ClientServerConfigState.blockingMovementSlowdownEnabled(),
                ClientServerConfigState.mountedKccCombatEnabled(),
                ClientServerConfigState.reachAttributeHitboxScalePerBlock(),
                ClientServerConfigState.vanillaAttackWeaponIds()
                ,ClientServerConfigState.vanillaAttackEntityIds()
        ));
    }

    private static void addBoolean(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            String label,
            boolean value,
            BooleanSetter setter
    ) {
        category.addEntry(entries.startBooleanToggle(tr(label), value)
                .setSaveConsumer(setter::set)
                .build());
    }

    private static void addDouble(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            String label,
            double value,
            double min,
            double max,
            DoubleSetter setter
    ) {
        int scale = 100;
        int scaledMin = (int) Math.round(min * scale);
        int scaledMax = (int) Math.round(max * scale);
        int scaledValue = (int) Math.round(value * scale);
        category.addEntry(entries.startIntSlider(tr(label), scaledValue, scaledMin, scaledMax)
                .setTextGetter(scaled -> Text.literal(String.format(Locale.ROOT, "%.2f", scaled / (double) scale)))
                .setSaveConsumer(scaled -> setter.set(scaled / (double) scale))
                .build());
    }

    private static void addPercent(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            String label,
            double value,
            int maxPercent,
            DoubleSetter setter
    ) {
        category.addEntry(entries.startIntSlider(tr(label), (int) Math.round(value * 100.0), 0, maxPercent)
                .setSaveConsumer(percent -> setter.set(percent / 100.0))
                .build());
    }

    private static void addIntegerSlider(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            String label,
            int value,
            int min,
            int max,
            IntSetter setter
    ) {
        category.addEntry(entries.startIntSlider(tr(label), value, min, max)
                .setSaveConsumer(setter::set)
                .build());
    }

    private static Optional<Text> validateItemId(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return Optional.of(tr("error.empty_item_id"));
        }
        return Identifier.tryParse(trimmed) == null
                ? Optional.of(tr("error.invalid_item_id"))
                : Optional.empty();
    }

    private static Optional<Text> validateEntityId(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) return Optional.of(tr("error.empty_entity_id"));
        Identifier id = Identifier.tryParse(trimmed);
        return id == null || !Registries.ENTITY_TYPE.containsId(id)
                ? Optional.of(tr("error.invalid_entity_id")) : Optional.empty();
    }

    private static Text tr(String key) {
        return Text.translatable("config.kingdom_come_combat." + key);
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }

    @FunctionalInterface
    private interface DoubleSetter {
        void set(double value);
    }

    @FunctionalInterface
    private interface IntSetter {
        void set(int value);
    }
}
