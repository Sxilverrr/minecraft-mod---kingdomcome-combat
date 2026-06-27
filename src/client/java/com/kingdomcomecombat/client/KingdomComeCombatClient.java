package com.kingdomcomecombat.client;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.client.animation.ClientPlayerAnimationOverrides;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.GeckoLikeAnimationLibrary;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.combat.ClientComboUnlockState;
import com.kingdomcomecombat.client.compat.EntityModelFeaturesCompat;
import com.kingdomcomecombat.client.animation.ClientHitReactionState;
import com.kingdomcomecombat.client.collision.ClientAttackHitReporter;
import com.kingdomcomecombat.client.debug.CombatCollisionDebugRenderer;
import com.kingdomcomecombat.client.render.BladeTrailRenderer;
import com.kingdomcomecombat.client.render.HandCannonBulletRenderer;
import com.kingdomcomecombat.client.particle.BloodDropParticle;
import com.kingdomcomecombat.client.particle.BloodSparkParticle;
import com.kingdomcomecombat.client.particle.BloodMistParticle;
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
import com.kingdomcomecombat.client.stamina.StaminaExperienceBarDisplay;
import com.kingdomcomecombat.client.ui.CombatAttributeTooltipClient;
import com.kingdomcomecombat.client.ui.SkillBookClientState;
import com.kingdomcomecombat.combat.CombatControlConfig;
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
import com.kingdomcomecombat.client.feedback.CombatHitFeedbackClient;
import com.kingdomcomecombat.network.HitFeedbackPayload;
import com.kingdomcomecombat.network.HardcoreModeSyncPayload;
import com.kingdomcomecombat.network.EntityCombatStancePayload;
import com.kingdomcomecombat.network.EntityAttackAnimationPayload;
import com.kingdomcomecombat.network.EntityComboAttackAnimationPayload;
import com.kingdomcomecombat.network.EntityAttackImpactPayload;
import com.kingdomcomecombat.network.EntityAttackInterruptPayload;
import com.kingdomcomecombat.network.EntityBlockAnimationPayload;
import com.kingdomcomecombat.network.EntityHitReactionPayload;
import com.kingdomcomecombat.network.EntitySuppressHurtOverlayPayload;
import com.kingdomcomecombat.network.IncomingAttackWarningPayload;
import com.kingdomcomecombat.network.OpenSkillBookPayload;
import com.kingdomcomecombat.network.PassiveSkillUnlocksSyncPayload;
import com.kingdomcomecombat.network.StaminaSyncPayload;
import com.kingdomcomecombat.network.ComboUnlocksSyncPayload;
import com.kingdomcomecombat.network.UpdateCombatStancePayload;
import com.kingdomcomecombat.network.ServerConfigSyncPayload;
import com.kingdomcomecombat.particle.ModParticles;
import com.kingdomcomecombat.entity.ModEntities;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.kingdomcomecombat.client.hud.LockOnCrosshairOverlay;
import com.kingdomcomecombat.client.feedback.CustomHurtOverlaySuppressor;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Set;
public class KingdomComeCombatClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CombatClientConfig.load();
		registerAnimationCacheReload();
		CombatKeyBindings.register();
		CombatAnimationClient.register();
		EntityModelFeaturesCompat.register();
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
		EntityRendererRegistry.register(ModEntities.HAND_CANNON_BULLET, HandCannonBulletRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(
				ServerConfigSyncPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientServerConfigState.update(
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
						)
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
				ComboUnlocksSyncPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientComboUnlockState.replace(payload.comboIds())
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				PassiveSkillUnlocksSyncPayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientPassiveSkillUnlockState.replace(payload.skillIds())
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityAttackAnimationPayload.ID,
				(payload, context) -> context.client().execute(
						() -> CombatAnimationClient.playSyncedEntityAttack(
								payload.entityId(),
								CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
								payload.speedMultiplier(),
								payload.startupSlowdown(),
								payload.animationName()
						)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityComboAttackAnimationPayload.ID,
				(payload, context) -> context.client().execute(
						() -> playComboAttackAnimation(context.client(), payload)
				)
		);
		ClientPlayNetworking.registerGlobalReceiver(
				EntityCombatStancePayload.ID,
				(payload, context) -> context.client().execute(
						() -> ClientEntityGeckoAnimationState.setStance(
								payload.entityId(),
								payload.targetEntityId(),
								CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
								payload.speedMultiplier(),
								payload.animationName()
						)
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
								if (context.client().player != null
										&& payload.entityId() == context.client().player.getId()) {
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
								if (context.client().player != null
										&& payload.entityId() == context.client().player.getId()) {
									CombatClientState.cancelAttackLocally();
									CombatAnimationClient.playHitReaction(payload.animationName(), direction);
								} else {
									ClientEntityGeckoAnimationState.startHitReaction(
											payload.entityId(),
											direction,
											payload.animationName()
									);
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
			CustomHurtOverlaySuppressor.tickCleanup();
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> IncomingAttackWarningState.tick());

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
			CombatClientState.cancelAttackLocally();
			CombatInputClient.clearBufferedAttackPlan();
			if (payload.animationType() == EntityBlockAnimationPayload.PERFECT) {
				CombatClientState.startPerfectCounterWindow();
				CombatClientState.setDirection(direction);
				CombatDirection nextDirection = CombatDirection.afterPerfectBlock(direction);
				CombatClientState.setDirection(nextDirection);
				ClientPlayNetworking.send(new UpdateCombatStancePayload(nextDirection.ordinal(), LockOnState.locked));
			}
			CombatAnimationClient.playBlock(payload.animationType(), direction);
			lockBlockedAttacker(client, payload.attackerEntityId());
			return;
		}

		GeckoLikeAnimationLibrary.Kind kind = switch (payload.animationType()) {
			case EntityBlockAnimationPayload.UNPERFECT_1 -> GeckoLikeAnimationLibrary.Kind.BLOCK_UNPERFECT_1;
			case EntityBlockAnimationPayload.UNPERFECT_2 -> GeckoLikeAnimationLibrary.Kind.BLOCK_UNPERFECT_2;
			default -> GeckoLikeAnimationLibrary.Kind.BLOCK;
		};
		ClientEntityGeckoAnimationState.startBlock(payload.entityId(), kind, direction);
	}

	private static void playComboAttackAnimation(MinecraftClient client, EntityComboAttackAnimationPayload payload) {
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
