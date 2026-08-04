package com.kingdomcomecombat.client;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.client.animation.ClientPlayerAnimationOverrides;
import com.kingdomcomecombat.client.animation.ClientDodgeAnimationState;
import com.kingdomcomecombat.client.animation.ClientLockedMovementLeanState;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.GeckoLikeAnimationLibrary;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.combat.ClientExecutionState;
import com.kingdomcomecombat.client.combat.ClientComboUnlockState;
import com.kingdomcomecombat.client.compat.EntityModelFeaturesCompat;
import com.kingdomcomecombat.client.compat.YesSteveModelCompat;
import com.kingdomcomecombat.client.animation.ClientHitReactionState;
import com.kingdomcomecombat.client.collision.ClientAttackHitReporter;
import com.kingdomcomecombat.client.debug.CombatCollisionDebugRenderer;
import com.kingdomcomecombat.client.render.BladeTrailRenderer;
import com.kingdomcomecombat.client.render.HandCannonBulletRenderer;
import com.kingdomcomecombat.client.particle.BloodDropParticle;
import com.kingdomcomecombat.client.particle.BloodSparkParticle;
import com.kingdomcomecombat.client.particle.BloodMistParticle;
import com.kingdomcomecombat.client.particle.BloodTraceParticle;
import com.kingdomcomecombat.client.particle.CombatSparkParticle;
import com.kingdomcomecombat.client.passive.ClientPassiveSkillUnlockState;
import com.kingdomcomecombat.client.input.CombatInputClient;
import com.kingdomcomecombat.client.input.CombatKeyBindings;
import com.kingdomcomecombat.client.hud.IncomingAttackWarningState;
import com.kingdomcomecombat.client.game.ClientGameRuleState;
import com.kingdomcomecombat.client.config.ClientServerConfigState;
import com.kingdomcomecombat.client.hud.CombatScreenStatusOverlay;
import com.kingdomcomecombat.client.lockon.LockOnCameraController;
import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.client.stamina.ClientStaminaState;
import com.kingdomcomecombat.client.stamina.ClientEntityStaminaState;
import com.kingdomcomecombat.client.stamina.StaminaExperienceBarDisplay;
import com.kingdomcomecombat.client.ui.CombatAttributeTooltipClient;
import com.kingdomcomecombat.client.ui.SkillBookClientState;
import com.kingdomcomecombat.combat.CombatControlConfig;
import com.kingdomcomecombat.combat.CombatAnimationResource;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.CombatTiming;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import com.kingdomcomecombat.client.feedback.CombatHitFeedbackClient;
import com.kingdomcomecombat.client.feedback.CauldronWashScreenEffect;
import com.kingdomcomecombat.network.HitFeedbackPayload;
import com.kingdomcomecombat.network.ConfirmedBloodTracePayload;
import com.kingdomcomecombat.network.HardcoreModeSyncPayload;
import com.kingdomcomecombat.network.EntityCombatStancePayload;
import com.kingdomcomecombat.network.EntityAttackAnimationPayload;
import com.kingdomcomecombat.network.EntityComboAttackAnimationPayload;
import com.kingdomcomecombat.network.EntityCinematicVictimAnimationPayload;
import com.kingdomcomecombat.network.EntityAttackImpactPayload;
import com.kingdomcomecombat.network.EntityAttackInterruptPayload;
import com.kingdomcomecombat.network.EntityBlockAnimationPayload;
import com.kingdomcomecombat.network.EntityDodgeAnimationPayload;
import com.kingdomcomecombat.network.EntityExecutionStunPayload;
import com.kingdomcomecombat.network.EntityHitReactionPayload;
import com.kingdomcomecombat.network.EntitySuppressHurtOverlayPayload;
import com.kingdomcomecombat.network.IncomingAttackWarningPayload;
import com.kingdomcomecombat.network.OpenSkillBookPayload;
import com.kingdomcomecombat.network.PassiveSkillUnlocksSyncPayload;
import com.kingdomcomecombat.network.PassiveSkillConfigsSyncPayload;
import com.kingdomcomecombat.network.SkillUiDataSyncPayload;
import com.kingdomcomecombat.network.HardshipSelectionPromptPayload;
import com.kingdomcomecombat.network.HardshipSelectionSyncPayload;
import com.kingdomcomecombat.client.hardship.ClientHardshipState;
import com.kingdomcomecombat.client.ui.HardshipSelectionScreen;
import com.kingdomcomecombat.client.passive.ClientPassiveSkillConfigState;
import com.kingdomcomecombat.network.StaminaSyncPayload;
import com.kingdomcomecombat.network.EntityStaminaSyncPayload;
import com.kingdomcomecombat.network.ComboUnlocksSyncPayload;
import com.kingdomcomecombat.network.UpdateCombatStancePayload;
import com.kingdomcomecombat.network.ServerConfigSyncPayload;
import com.kingdomcomecombat.network.ScaledHitFeedbackPayload;
import com.kingdomcomecombat.network.PlayerInterruptConfigSyncPayload;
import com.kingdomcomecombat.particle.ModParticles;
import com.kingdomcomecombat.entity.ModEntities;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.kingdomcomecombat.client.hud.LockOnCrosshairOverlay;
import com.kingdomcomecombat.client.feedback.CustomHurtOverlaySuppressor;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Set;
import com.google.gson.JsonParser;
import java.util.ArrayList;
public class KingdomComeCombatClient implements ClientModInitializer {
	private static boolean localPlayerWasDead = false;

	@Override
	public void onInitializeClient() {
		com.kingdomcomecombat.item.ModItems.setClientLanguageSupplier(() ->
				MinecraftClient.getInstance().getLanguageManager().getLanguage());
		CombatClientConfig.load();
		registerAnimationCacheReload();
		CombatKeyBindings.register();
		CombatAnimationClient.register();
		EntityModelFeaturesCompat.register();
		YesSteveModelCompat.register();
		CombatInputClient.register();
		LockOnCrosshairOverlay.register();
		CombatScreenStatusOverlay.register();
		StaminaExperienceBarDisplay.register();
		CombatAttributeTooltipClient.register();
		CombatCollisionDebugRenderer.register();
		ClientAttackHitReporter.register();
		BladeTrailRenderer.register();
		ParticleFactoryRegistry.getInstance().register(ModParticles.COMBAT_SPARK, CombatSparkParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.BLOOD_SPARK, BloodSparkParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.BLOOD_DROP, BloodDropParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.BLOOD_MIST, BloodMistParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.BLOOD_TRACE, BloodTraceParticle.Factory::new);
		EntityRendererRegistry.register(ModEntities.HAND_CANNON_BULLET, HandCannonBulletRenderer::new);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				resetConnectionScopedState());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			resetConnectionScopedState();
		});
		ClientPlayNetworking.registerGlobalReceiver(

				ServerConfigSyncPayload.ID,
				(payload, context) -> context.client().execute(() -> {
						ClientServerConfigState.setLightweightDamageModeEnabled(payload.lightweightDamageModeEnabled());
						ClientServerConfigState.setLightweightBlockingModeEnabled(payload.lightweightBlockingModeEnabled());
						ClientServerConfigState.update(
								payload.modEquipmentGenerationEnabled(),
								payload.zombieLeaderHealthFixEnabled(),
								payload.mobToughnessEnabled(),
								payload.hitStopTicks(),
								payload.vanillaHurtSoundVolumeMultiplier(),
								payload.masterCounterWindowTicks(),
								payload.blockWindowTicks(),
								payload.combatMinDistance(),
								payload.vanillaAttackWeaponIds(),
								payload.canEdit()
						);
						ClientServerConfigState.setCollisionCacheRadius(payload.collisionCacheRadius());
						ClientServerConfigState.setExperimentalIllagerUndeadHostilityEnabled(
								payload.experimentalIllagerUndeadHostilityEnabled()
						);
						ClientServerConfigState.setDisableVanillaLeftHandedMobs(
								payload.disableVanillaLeftHandedMobs()
						);
						ClientServerConfigState.setEnderDragonOverhaulEnabled(payload.enderDragonOverhaulEnabled());
						ClientServerConfigState.setLegacyCollisionCalculationEnabled(
								payload.legacyCollisionCalculationEnabled()
						);
						ClientServerConfigState.setClientProjectileHurtboxEnabled(
								payload.clientProjectileHurtboxEnabled()
						);
						ClientServerConfigState.setReachAttributeHitboxScalingEnabled(
								payload.reachAttributeHitboxScalingEnabled()
						);
						ClientServerConfigState.setBlockingMovementSlowdownEnabled(
								payload.blockingMovementSlowdownEnabled()
						);
						ClientServerConfigState.setMountedKccCombatEnabled(payload.mountedKccCombatEnabled());
						ClientServerConfigState.setReachAttributeHitboxScalePerBlock(
								payload.reachAttributeHitboxScalePerBlock()
						);
						ClientServerConfigState.setUnperfectBlockWindowTicks(
								payload.unperfectBlockWindowTicks()
						);
						ClientServerConfigState.setPvpEnabled(payload.pvpEnabled());
						ClientServerConfigState.setVanillaAttackEntityIds(payload.vanillaAttackEntityIds());
						com.kingdomcomecombat.config.CombatServerConfig.setVanillaAttackEntityIds(payload.vanillaAttackEntityIds());
						if (!payload.pvpEnabled()
								&& context.client().world != null
								&& context.client().world.getEntityById(LockOnState.targetEntityId)
										instanceof AbstractClientPlayerEntity) {
							LockOnState.clear();
						}
				})
		);
		ClientPlayNetworking.registerGlobalReceiver(
				PlayerInterruptConfigSyncPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientServerConfigState.setAlwaysEnablePlayerInterrupt(payload.enabled())
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				HitFeedbackPayload.ID,
				(payload, context) -> context.client().execute(
						() -> CombatHitFeedbackClient.startHitFeedback(
								CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
								payload.penetratedArmor()
						)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				ScaledHitFeedbackPayload.ID,
				(payload, context) -> context.client().execute(
						() -> CombatHitFeedbackClient.startHitFeedback(
								CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
								payload.penetratedArmor(),
								payload.feedbackScale()
						)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(ConfirmedBloodTracePayload.ID,
				(payload, context) -> context.client().execute(() ->
						ClientAttackHitReporter.confirmBloodTrace(payload.targetEntityId())));
		ClientPlayNetworking.registerGlobalReceiver(
				HardcoreModeSyncPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientGameRuleState.setHardcoreMode(payload.enabled())
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				StaminaSyncPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientStaminaState.update(payload.current(), payload.max())
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityStaminaSyncPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientEntityStaminaState.update(
								payload.entityId(),
								payload.current(),
								payload.max()
						)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				ComboUnlocksSyncPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientComboUnlockState.replace(payload.comboIds())
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				PassiveSkillUnlocksSyncPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientPassiveSkillUnlockState.replace(
								payload.skillIds(),
								payload.combatExperience(),
								payload.killReward(),
								payload.perfectBlockReward(),
								payload.perfectCounterReward(),
								payload.attackReward(),
								payload.masterCounterReward(),
								payload.comboReward(),
								payload.vanillaExperienceMultiplier())
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				PassiveSkillConfigsSyncPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientPassiveSkillConfigState.replaceFromJson(payload.json())
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(SkillUiDataSyncPayload.ID,
				(payload, context) -> context.client().execute(() -> {
					com.kingdomcomecombat.client.ui.ClientSkillUiData.replace(
							payload.combosJson(), payload.textsJson(), payload.shieldsJson(), payload.weaponsJson(),
							payload.equipmentDefaultsJson(),
							payload.armorJson(), payload.rangedWeaponsJson(),
							payload.executionsJson(), payload.executionTargetsJson(),
							payload.attackMoveDirectionsJson(), payload.attackMovesJson(),
							payload.realHitboxSizeX(), payload.realHitboxSizeY(), payload.realHitboxSizeZ(),
							payload.realHitboxOffsetX(), payload.realHitboxOffsetY(), payload.realHitboxOffsetZ(),
							payload.realHitboxRotationX(), payload.realHitboxRotationY(), payload.realHitboxRotationZ());
					com.kingdomcomecombat.client.collision.ClientItemHitboxCache.clear();
					ArrayList<String> humanoidAiTypes = new ArrayList<>();
					for (var element : JsonParser.parseString(payload.humanoidAiEntityIdsJson()).getAsJsonArray()) {
						if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
							humanoidAiTypes.add(element.getAsString());
						}
					}
					HumanoidHurtboxLibrary.replaceSyncedHumanoidAiTypes(humanoidAiTypes);
					ArrayList<String> aiTypes = new ArrayList<>();
					for (var element : JsonParser.parseString(payload.aiEntityIdsJson()).getAsJsonArray()) {
						if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) aiTypes.add(element.getAsString());
					}
					HumanoidHurtboxLibrary.replaceSyncedAiTypes(aiTypes);
					com.kingdomcomecombat.client.collision.ClientGenericModelTracker.clearModelClassificationCache();
				}));
		ClientPlayNetworking.registerGlobalReceiver(
				EntityAttackAnimationPayload.ID,
				(payload, context) -> context.client().execute(
						() -> playSyncedAttackAnimation(context.client(), payload)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(HardshipSelectionPromptPayload.ID,
				(payload, context) -> context.client().execute(() -> {
					ClientHardshipState.replaceDefinitions(payload.definitionsJson());
					context.client().setScreen(new HardshipSelectionScreen(payload.minimum()));
				}));
		ClientPlayNetworking.registerGlobalReceiver(HardshipSelectionSyncPayload.ID,
				(payload, context) -> context.client().execute(() -> {
					ClientHardshipState.setSelected(payload.ids());
					if (context.client().currentScreen instanceof HardshipSelectionScreen) {
						context.client().setScreen(null);
					}
				}));
		ClientPlayNetworking.registerGlobalReceiver(
				EntityComboAttackAnimationPayload.ID,
				(payload, context) -> context.client().execute(
						() -> playComboAttackAnimation(context.client(), payload)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityCinematicVictimAnimationPayload.ID,
				(payload, context) -> context.client().execute(
						() -> playCinematicVictimAnimation(context.client(), payload)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityCombatStancePayload.ID,
				(payload, context) -> context.client().execute(
						() -> {
							var entity = context.client().world == null
									? null : context.client().world.getEntityById(payload.entityId());
							if (payload.directionOrdinal() < 0) {
								ClientEntityGeckoAnimationState.clearStance(payload.entityId());
							} else {
								ClientEntityGeckoAnimationState.setStance(
									payload.entityId(),
									payload.targetEntityId(),
									CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
									payload.speedMultiplier(),
									payload.animationName()
								);
							}
							if (entity instanceof AbstractClientPlayerEntity remote
									&& remote != context.client().player) {
								CombatDirection direction = CombatDirection.fromOrdinalSafe(payload.directionOrdinal());
								Identifier animation = payload.directionOrdinal() < 0
										? Identifier.of(KingdomComeCombat.MOD_ID, "neutral")
										: CombatAnimationClient.syncedStanceAnimation(direction, payload.animationName());
								if (!CombatAnimationClient.isRemoteCombatTransitionActive(remote)) {
									CombatAnimationClient.playRemotePlayerAnimation(remote, animation, false, 6);
								}
								return;
							}
						}
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityBlockAnimationPayload.ID,
				(payload, context) -> context.client().execute(
						() -> playBlockAnimation(context.client(), payload)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityHitReactionPayload.ID,
				(payload, context) -> context.client().execute(
						() -> {
								boolean localPlayerHit = context.client().player != null
										&& payload.entityId() == context.client().player.getId();
								if (localPlayerHit && ClientExecutionState.isLocalExecutionActive()) {
									return;
								}
								if (localPlayerHit) {
									CombatInputClient.disableAttackLocally(
											CombatControlConfig.HIT_ATTACK_DISABLE_TICKS
									);
								}
								ClientHitReactionState.start(
										payload.entityId(),
										partFromOrdinal(payload.partOrdinal()),
									payload.detailedPart(),
									CombatDirection.fromOrdinalSafe(payload.attackDirectionOrdinal()),
									payload.strong(),
									payload.strength()
							);
							if (!payload.animationName().isBlank()) {
								CombatDirection direction = CombatDirection.fromOrdinalSafe(
										payload.attackDirectionOrdinal()
								);
								if (localPlayerHit) {
									CombatInputClient.disableAttackLocally(Math.max(
											CombatControlConfig.HIT_ATTACK_DISABLE_TICKS,
											(int) Math.ceil(
													GeckoLikeAnimationLibrary.getNamedLengthSeconds(payload.animationName())
															* 20.0F
											)
									));
									CombatClientState.cancelAttackLocally();
									CombatAnimationClient.playHitReaction(payload.animationName(), direction);
								} else {
									ClientEntityGeckoAnimationState.startHitReaction(
											payload.entityId(),
											direction,
											payload.animationName()
									);
									var entity = context.client().world == null
											? null : context.client().world.getEntityById(payload.entityId());
									if (entity instanceof AbstractClientPlayerEntity remote) {
										CombatAnimationClient.playRemoteTransientPlayerAnimation(
												remote,
												CombatAnimationResource.playerAnimationId(payload.animationName()),
												false,
												CombatTiming.ATTACK_CHAIN_TRANSITION_TICKS
										);
										return;
									}
								}
							}
						}
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntitySuppressHurtOverlayPayload.ID,
				(payload, context) -> context.client().execute(
						() -> CustomHurtOverlaySuppressor.suppress(
								payload.entityId(),
								payload.ticks()
						)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityAttackImpactPayload.ID,
				(payload, context) -> context.client().execute(
						() -> applyAttackImpactStop(context.client(), payload.entityId())
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityAttackInterruptPayload.ID,
				(payload, context) -> context.client().execute(
						() -> handleAttackInterrupt(context.client(), payload.entityId())
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityDodgeAnimationPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientDodgeAnimationState.start(
								payload.entityId(),
								com.kingdomcomecombat.combat.DodgeDirection.fromOrdinalSafe(payload.directionOrdinal())
						)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				IncomingAttackWarningPayload.ID,
				(payload, context) -> context.client().execute(
						() -> IncomingAttackWarningState.start(
								payload.attackerEntityId(),
								CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
								payload.warningType()
						)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityExecutionStunPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientExecutionState.start(
								payload.targetEntityId(),
								payload.attackerEntityId(),
								CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
								payload.ticks()
						)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				OpenSkillBookPayload.ID,
				(payload, context) -> context.client().execute(
						() -> SkillBookClientState.set(payload)
				)
		);
		WorldRenderEvents.START.register(context -> {
			LockOnCameraController.renderFrame(MinecraftClient.getInstance());
			ClientEntityGeckoAnimationState.tickCleanup();
			com.kingdomcomecombat.client.collision.ClientModelHurtboxCache.cleanup();
			com.kingdomcomecombat.client.collision.ClientItemHitboxCache.cleanup();
			ClientHitReactionState.tickCleanup();
			ClientDodgeAnimationState.tickCleanup();
			ClientLockedMovementLeanState.tickCleanup();
			ClientEntityStaminaState.tickCleanup();
			CustomHurtOverlaySuppressor.tickCleanup();
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			IncomingAttackWarningState.tick();
			if (ClientHardshipState.selected("hardship_03")
					&& client.options.getGamma().getValue() != 0.0) {
				client.options.getGamma().setValue(0.0);
			}
			if (client.player != null) {
				ClientExecutionState.tick(client.player.getId());
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> CauldronWashScreenEffect.tick());
		ClientTickEvents.END_CLIENT_TICK.register(KingdomComeCombatClient::clearCombatAnimationOnDeath);

	}

	private static void resetConnectionScopedState() {
		LockOnState.clear();
		ClientStaminaState.reset();
		ClientHardshipState.setSelected(java.util.List.of());
		StaminaExperienceBarDisplay.reset();
		ClientPassiveSkillUnlockState.replace(java.util.List.of(), 0, 5, 8, 8, 3, 20, 20, 0.4);
		ClientPassiveSkillConfigState.clear();
		ClientServerConfigState.setPvpEnabled(false);
		ClientServerConfigState.setLegacyCollisionCalculationEnabled(false);
		ClientServerConfigState.setClientProjectileHurtboxEnabled(false);
		ClientExecutionState.clear();
		HumanoidHurtboxLibrary.replaceSyncedHumanoidAiTypes(java.util.List.of());
		HumanoidHurtboxLibrary.replaceSyncedAiTypes(java.util.List.of());
	}

	private static void registerAnimationCacheReload() {
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
				.registerReloadListener(new SimpleSynchronousResourceReloadListener() {
					@Override
					public Identifier getFabricId() {
						return Identifier.of(KingdomComeCombat.MOD_ID, "client_animation_cache");
					}

					@Override
					public Collection<Identifier> getFabricDependencies() {
						return Set.of(PlayerAnimResources.KEY);
					}

					@Override
					public void reload(ResourceManager manager) {
						GeckoLikeAnimationLibrary.clearCache();
						ClientPlayerAnimationOverrides.applyStandaloneFileOverrides(manager);
					}
				});
	}

	private static HumanoidHurtboxLibrary.Part partFromOrdinal(int ordinal) {
		HumanoidHurtboxLibrary.Part[] parts = HumanoidHurtboxLibrary.Part.values();
		if (ordinal < 0 || ordinal >= parts.length) {
			return HumanoidHurtboxLibrary.Part.BODY;
		}

		return parts[ordinal];
	}

	private static void playBlockAnimation(MinecraftClient client, EntityBlockAnimationPayload payload) {
		CombatDirection direction = CombatDirection.fromOrdinalSafe(payload.directionOrdinal());
		if (client.player != null && payload.entityId() == client.player.getId()) {
			CombatDirection previousDirection = CombatClientState.currentDirection;
			CombatClientState.cancelAttackLocally();
			CombatInputClient.clearBufferedAttackPlan();
			if (payload.animationType() == EntityBlockAnimationPayload.PERFECT) {
				CombatClientState.startPerfectCounterWindow();
				CombatClientState.setDirection(direction);
				CombatDirection nextDirection = com.kingdomcomecombat.combat.CombatItemUtil.isPolearm(
						client.player.getMainHandStack())
						? previousDirection
						: CombatDirection.afterPerfectBlock(direction);
				CombatClientState.setDirection(nextDirection);
				ClientPlayNetworking.send(new UpdateCombatStancePayload(nextDirection.ordinal(), LockOnState.locked));
			}
			CombatAnimationClient.playBlock(payload.animationType(), direction);
			if (!LockOnState.isSoftLocked()) {
				lockBlockedAttacker(client, payload.attackerEntityId());
			}
			return;
		}

		GeckoLikeAnimationLibrary.Kind kind = switch (payload.animationType()) {
			case EntityBlockAnimationPayload.UNPERFECT_1 -> GeckoLikeAnimationLibrary.Kind.BLOCK_UNPERFECT_1;
			case EntityBlockAnimationPayload.UNPERFECT_2 -> GeckoLikeAnimationLibrary.Kind.BLOCK_UNPERFECT_2;
			default -> GeckoLikeAnimationLibrary.Kind.BLOCK;
		};
		var blockedEntity = client.world == null ? null : client.world.getEntityById(payload.entityId());
		ClientEntityGeckoAnimationState.startBlock(payload.entityId(), kind, direction);
		if (blockedEntity instanceof AbstractClientPlayerEntity remote) {
			CombatAnimationClient.playRemoteTransientPlayerAnimation(
					remote,
					CombatAnimationClient.syncedBlockAnimation(payload.animationType(), direction),
					true,
					CombatAnimationClient.remoteBlockFadeTicks()
			);
			return;
		}
	}

	private static void clearCombatAnimationOnDeath(MinecraftClient client) {
		boolean dead = client.player != null && !client.player.isAlive();
		if (dead && !localPlayerWasDead) {
			CombatClientState.cancelAttackLocally();
			CombatClientState.clearComboInputs();
			CombatInputClient.clearBufferedAttackPlan();
			CombatAnimationClient.resetAfterDeath();
			LockOnState.clear();
		}
		localPlayerWasDead = dead;
	}

	private static void playComboAttackAnimation(MinecraftClient client, EntityComboAttackAnimationPayload payload) {
		var syncedEntity = client.world == null ? null : client.world.getEntityById(payload.entityId());
		YesSteveModelCompat.triggerAttack(syncedEntity);
		if (client.player != null && payload.entityId() == client.player.getId()) {
			CombatClientState.retimeCurrentAttack(
					Math.max(
							1,
							Math.round(GeckoLikeAnimationLibrary.getNamedLengthSeconds(payload.animationName())
									* 20.0F
									/ Math.max(0.05F, payload.speedMultiplier()))
					),
					payload.speedMultiplier(),
					CombatTiming.LIGHT_ATTACK_TRANSITION_TICKS
			);
			CombatClientState.setCurrentAttackAnimationName(payload.animationName());
			CombatClientState.setCurrentAttackMovementLocked(
					ComboMoveConfigs.findByAnimationName(payload.animationName())
							.map(ComboMoveConfig::suctionCombo)
							.orElse(false)
			);
			if (isMasterCounterAnimation(payload.animationName())) {
				CombatClientState.restoreDirectionBeforeCurrentAttack();
			}
			CombatClientState.clearComboInputs();
			CombatAnimationClient.playComboAttack(
					payload.animationName(),
					CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
					true,
					payload.bladeTrail()
			);
			CombatAnimationClient.setCombatAnimationSpeed(payload.speedMultiplier());
		} else {
			ClientEntityGeckoAnimationState.startAttack(
					payload.entityId(),
					CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
					payload.speedMultiplier(),
					payload.animationName()
			);
			var entity = client.world == null ? null : client.world.getEntityById(payload.entityId());
			if (entity instanceof AbstractClientPlayerEntity remote) {
				CombatAnimationClient.playRemoteTransientPlayerAnimation(
						remote,
						CombatAnimationResource.playerAnimationId(payload.animationName()),
						false,
						CombatTiming.ATTACK_CHAIN_TRANSITION_TICKS
				);
				return;
			}
		}
	}

	private static void playSyncedAttackAnimation(MinecraftClient client, EntityAttackAnimationPayload payload) {
		var entity = client.world == null ? null : client.world.getEntityById(payload.entityId());
		YesSteveModelCompat.triggerAttack(entity);
		CombatAnimationClient.playSyncedEntityAttack(
				payload.entityId(),
				CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
				payload.speedMultiplier(),
				payload.startupSlowdown(),
				payload.animationName()
		);
	}

	private static void playCinematicVictimAnimation(
			MinecraftClient client,
			EntityCinematicVictimAnimationPayload payload
	) {
		CombatDirection direction = CombatDirection.fromOrdinalSafe(payload.directionOrdinal());
		int animationTicks = Math.max(1, (int) Math.ceil(
				GeckoLikeAnimationLibrary.getNamedLengthSeconds(payload.animationName())
						* 20.0F / Math.max(0.05F, payload.speedMultiplier())
		));
		if (client.player != null && payload.entityId() == client.player.getId()) {
			CombatClientState.cancelAttackLocally();
			CombatInputClient.clearBufferedAttackPlan();
			CombatInputClient.disableAttackLocally(animationTicks);
			CombatAnimationClient.playCinematicVictimAnimation(
					payload.animationName(),
					direction,
					payload.speedMultiplier(),
					animationTicks
			);
			return;
		}
		ClientEntityGeckoAnimationState.startHitReaction(
				payload.entityId(),
				direction,
				payload.animationName(),
				payload.holdLastFrame(),
				payload.speedMultiplier()
		);
		var entity = client.world == null ? null : client.world.getEntityById(payload.entityId());
		if (entity instanceof AbstractClientPlayerEntity remote) {
			CombatAnimationClient.playRemoteTransientPlayerAnimation(
					remote,
					CombatAnimationResource.playerAnimationId(payload.animationName()),
					false,
					CombatTiming.ATTACK_CHAIN_TRANSITION_TICKS
			);
			return;
		}
	}

	private static boolean isMasterCounterAnimation(String animationName) {
		return "master_counter_right".equals(animationName)
				|| "master_strike_right".equals(animationName);
	}

	private static void applyAttackImpactStop(MinecraftClient client, int entityId) {
		if (client.player != null && entityId == client.player.getId()) {
			CombatHitFeedbackClient.startAnimationImpactStop();
			return;
		}

		ClientEntityGeckoAnimationState.applyAttackImpactSlowdown(entityId);
		var entity = client.world == null ? null : client.world.getEntityById(entityId);
		if (entity instanceof AbstractClientPlayerEntity remote) {
			CombatAnimationClient.refreshRemoteAnimationSpeed(remote);
		}
	}

	private static void handleAttackInterrupt(MinecraftClient client, int entityId) {
		if (client.player != null && entityId == client.player.getId()) {
			CombatClientState.cancelAttackLocally();
			CombatClientState.clearComboInputs();
			CombatInputClient.clearBufferedAttackPlan();
			CombatAnimationClient.resetAnimationSpeeds();
			if (LockOnState.locked) {
				CombatAnimationClient.blendToStanceAfterAttack(CombatClientState.currentDirection);
			} else {
				CombatAnimationClient.blendToNeutralAfterAttack();
			}
			return;
		}
		ClientEntityGeckoAnimationState.interruptAttack(entityId);
		var entity = client.world == null ? null : client.world.getEntityById(entityId);
		if (entity instanceof AbstractClientPlayerEntity remote) {
			CombatAnimationClient.blendRemotePlayerToCurrentStance(remote, 4);
			return;
		}
	}

	private static void lockBlockedAttacker(MinecraftClient client, int attackerEntityId) {
		if (client.world == null || attackerEntityId < 0) {
			return;
		}

		var entity = client.world.getEntityById(attackerEntityId);
		if (entity instanceof net.minecraft.entity.LivingEntity livingEntity && livingEntity.isAlive()) {
			LockOnState.lock(livingEntity.getUuid(), livingEntity.getId());
		}
	}
}
