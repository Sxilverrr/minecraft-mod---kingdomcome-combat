package com.kingdomcomecombat.network;

import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.ActiveServerAttack;
import com.kingdomcomecombat.combat.AttackMoveConfig;
import com.kingdomcomecombat.combat.AttackMoveConfigs;
import com.kingdomcomecombat.combat.BeowulfArmState;
import com.kingdomcomecombat.combat.CombatAttackTiming;
import com.kingdomcomecombat.combat.CombatControlConfig;
import com.kingdomcomecombat.combat.CombatTiming;
import com.kingdomcomecombat.combat.CombatWeaponUtil;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.DodgeDirection;
import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.combat.PlayerComboProgress;
import com.kingdomcomecombat.combat.ServerBlockState;
import com.kingdomcomecombat.combat.ServerComboState;
import com.kingdomcomecombat.combat.ServerCombatControlState;
import com.kingdomcomecombat.combat.ServerCombatState;
import com.kingdomcomecombat.combat.ServerCombatStanceState;
import com.kingdomcomecombat.config.CombatServerConfig;
import com.kingdomcomecombat.ai.HumanoidCombatAiTicker;
import com.kingdomcomecombat.collision.ServerHitDetectionSystem;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.game.ModGameRules;
import com.kingdomcomecombat.item.SkillBookItem;
import com.kingdomcomecombat.network.PassiveSkillUnlocksSyncPayload;
import com.kingdomcomecombat.passive.PlayerPassiveSkillProgress;
import com.kingdomcomecombat.passive.PassiveSkillPerks;
import com.kingdomcomecombat.riding.ServerHorseControlState;
import com.kingdomcomecombat.stamina.ServerStaminaState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class CombatNetworking {
    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(
                StartAttackPayload.ID,
                StartAttackPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                StartBlockPayload.ID,
                StartBlockPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                StartDodgePayload.ID,
                StartDodgePayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                UpdateCombatStancePayload.ID,
                UpdateCombatStancePayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                ClientAttackHitPayload.ID,
                ClientAttackHitPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                LearnSkillBookPayload.ID,
                LearnSkillBookPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                LearnExperiencePassiveSkillPayload.ID,
                LearnExperiencePassiveSkillPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                HorseControlPayload.ID,
                HorseControlPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                UpdateServerConfigPayload.ID,
                UpdateServerConfigPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                HitFeedbackPayload.ID,
                HitFeedbackPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                StaminaSyncPayload.ID,
                StaminaSyncPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                ComboUnlocksSyncPayload.ID,
                ComboUnlocksSyncPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                PassiveSkillUnlocksSyncPayload.ID,
                PassiveSkillUnlocksSyncPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                EntityAttackAnimationPayload.ID,
                EntityAttackAnimationPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                EntityComboAttackAnimationPayload.ID,
                EntityComboAttackAnimationPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                EntityCombatStancePayload.ID,
                EntityCombatStancePayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                EntityBlockAnimationPayload.ID,
                EntityBlockAnimationPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                EntityHitReactionPayload.ID,
                EntityHitReactionPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                EntitySuppressHurtOverlayPayload.ID,
                EntitySuppressHurtOverlayPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                EntityAttackImpactPayload.ID,
                EntityAttackImpactPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                EntityAttackInterruptPayload.ID,
                EntityAttackInterruptPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                IncomingAttackWarningPayload.ID,
                IncomingAttackWarningPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                OpenSkillBookPayload.ID,
                OpenSkillBookPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                HardcoreModeSyncPayload.ID,
                HardcoreModeSyncPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                ServerConfigSyncPayload.ID,
                ServerConfigSyncPayload.CODEC
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                syncServerConfig(handler.player));

        ServerPlayNetworking.registerGlobalReceiver(
                UpdateServerConfigPayload.ID,
                (payload, context) -> {
                    if (!context.player().hasPermissionLevel(2)) {
                        syncServerConfig(context.player());
                        return;
                    }
                    CombatServerConfig.setModEquipmentGenerationEnabled(payload.modEquipmentGenerationEnabled());
                    CombatServerConfig.setZombieLeaderHealthFixEnabled(payload.zombieLeaderHealthFixEnabled());
                    CombatServerConfig.setMobToughnessEnabled(payload.mobToughnessEnabled());
                    CombatServerConfig.setHitStopTicks(payload.hitStopTicks());
                    CombatServerConfig.setVanillaHurtSoundVolumeMultiplier(
                            payload.vanillaHurtSoundVolumeMultiplier()
                    );
                    CombatServerConfig.setMasterCounterWindowTicks(payload.masterCounterWindowTicks());
                    CombatServerConfig.setBlockWindowTicks(payload.blockWindowTicks());
                    CombatServerConfig.setCombatMinDistance(payload.combatMinDistance());
                    CombatServerConfig.setVanillaAttackWeaponIds(payload.vanillaAttackWeaponIds());
                    CombatServerConfig.save();
                    for (ServerPlayerEntity player : context.server().getPlayerManager().getPlayerList()) {
                        syncServerConfig(player);
                    }
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ClientAttackHitPayload.ID,
                (payload, context) -> ServerHitDetectionSystem.handleClientReportedHit(
                        context.player(),
                        payload.attackerEntityId(),
                        payload.targetEntityId(),
                        payload.partOrdinal(),
                        new Vec3d(payload.hitX(), payload.hitY(), payload.hitZ()),
                        payload.extraHeadHit(),
                        payload.attackInstanceId()
                )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                LearnSkillBookPayload.ID,
                (payload, context) -> SkillBookItem.learnFromBookScreen(
                        context.player(),
                        payload.comboId(),
                        payload.passiveId()
                )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                LearnExperiencePassiveSkillPayload.ID,
                (payload, context) -> PlayerPassiveSkillProgress.learnWithExperience(
                        context.player(),
                        payload.passiveId()
                )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                HorseControlPayload.ID,
                (payload, context) -> ServerHorseControlState.update(
                        context.player(),
                        payload.sideways(),
                        payload.forward(),
                        payload.sprintPressed(),
                        payload.jumpPressed(),
                        payload.yaw()
                )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                UpdateCombatStancePayload.ID,
                (payload, context) -> ServerCombatStanceState.set(
                        context.player().getUuid(),
                        CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
                        payload.locked()
                )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                StartAttackPayload.ID,
                (payload, context) -> {
                    if (!CombatItemUtil.canUseCustomCombat(context.player())) {
                        return;
                    }

                    if (!context.player().isOnGround()) {
                        return;
                    }

                    CombatDirection direction = CombatDirection.fromOrdinalSafe(
                            payload.directionOrdinal()
                    );
                    ServerCombatStanceState.set(context.player().getUuid(), direction);

                    long startWorldTick = context.player().getWorld().getTime();

                    // 大师反：
                    // 玩家攻击方向 == 敌人正在攻击方向时，
                    // 尝试读取 master_counter_right / left / up / down。
                    // 如果没有对应 JSON 配置，则 return false，继续普通攻击。
                    if (tryStartMasterCounter(
                            context.player(),
                            direction,
                            payload.targetEntityId(),
                            startWorldTick
                    )) {
                        return;
                    }

                    ActiveServerAttack previousAttack = ServerCombatState.getAttack(
                            context.player().getUuid()
                    );

                    if (previousAttack != null) {
                        ServerComboState.recordFinishedAttack(
                                context.player().getUuid(),
                                previousAttack,
                                startWorldTick
                        );
                        ServerCombatState.removeAttack(context.player().getUuid());
                    }

                    ComboMoveConfig comboMove = ServerComboState.selectComboForStart(
                            context.player().getUuid(),
                            direction,
                            startWorldTick,
                            context.player().getMainHandStack(),
                            BeowulfArmState.isActive(context.player()),
                            combo -> PlayerComboProgress.isUnlocked(context.player(), combo)
                    ).orElse(null);

                    var moveConfig = CombatWeaponUtil.resolveAttackMove(context.player(), direction);
                    LivingEntity suctionTarget = null;
                    int attackTargetEntityId = payload.targetEntityId();
                    boolean lockedLunge = payload.lockedLunge();

                    if (comboMove != null && comboMove.suctionCombo()) {
                        suctionTarget = resolveComboTarget(context.player(), payload.targetEntityId());
                        if (suctionTarget == null) {
                            comboMove = null;
                        } else if (!canPlaySuctionVictimAnimation(suctionTarget)) {
                            comboMove = null;
                            suctionTarget = null;
                        } else {
                            double horizontalDistance = horizontalDistance(context.player(), suctionTarget);
                            if (comboMove.suctionMinDistance() > 0.0
                                    && horizontalDistance < comboMove.suctionMinDistance()) {
                                comboMove = null;
                                suctionTarget = null;
                            } else if (comboMove.suctionDistance() > 0.0
                                    && horizontalDistance > comboMove.suctionDistance()) {
                                comboMove = null;
                                suctionTarget = null;
	                            } else {
	                                attackTargetEntityId = suctionTarget.getId();
	                                lockedLunge = false;
	                            }
	                        }
	                    }

	                    double staminaCost = comboMove != null
                            ? comboMove.staminaCost()
                            : moveConfig.staminaCost();

                    staminaCost *= EquipmentCombatAttributesRegistry.armorStaminaCostMultiplier(
                            context.player()
                    );
                    staminaCost *= PassiveSkillPerks.staminaCostMultiplier(
                            context.player(),
                            context.player().getMainHandStack(),
                            false,
                            true
                    );
                    if (hasLockedLargeShield(context.player())) {
                        staminaCost *= CombatControlConfig.LARGE_SHIELD_ATTACK_STAMINA_COST_MULTIPLIER;
                    }

                    if (!ServerCombatControlState.canAttack(context.player())) {
                        syncAttackRejected(context.player());
                        return;
                    }

                    if (!ServerStaminaState.consume(context.player(), staminaCost)) {
                        syncAttackRejected(context.player());
                        return;
                    }

                    double attackSpeedMultiplier =
                            EquipmentCombatAttributesRegistry.weaponAttackSpeedMultiplier(
                                    context.player()
                            );
                    attackSpeedMultiplier *= EquipmentCombatAttributesRegistry.armorAttackSpeedMultiplier(
                                    context.player()
                            );
                    attackSpeedMultiplier *= PassiveSkillPerks.attackSpeedMultiplier(
                            context.player(),
                            context.player().getMainHandStack()
                    );
                    if (BeowulfArmState.isActive(context.player())) {
                        attackSpeedMultiplier *= BeowulfArmState.ATTACK_SPEED_MULTIPLIER;
                    }
                    if (hasLargeShield(context.player())) {
                        attackSpeedMultiplier *= CombatControlConfig.LARGE_SHIELD_ATTACK_SPEED_MULTIPLIER;
                    }
                    if (comboMove == null) {
                        attackSpeedMultiplier *= ModGameRules.combatSpeed(context.player());
                    }

                    int totalTicks = comboMove != null
                            ? CombatAttackTiming.getComboAttackTotalTicks(comboMove.animationName())
                            : CombatAttackTiming.getAttackTotalTicks(direction, moveConfig);

                    totalTicks = Math.max(1, (int) Math.ceil(totalTicks / attackSpeedMultiplier));

                    if (suctionTarget != null) {
                        ServerCombatState.removeAttack(suctionTarget.getUuid());
                        ServerComboState.clear(context.player().getUuid());
                        ServerComboState.clear(suctionTarget.getUuid());
                        if (suctionTarget instanceof MobEntity mob) {
                            HumanoidCombatAiTicker.interruptFollowUps(mob);
                        }
                        alignMasterCounterPair(
                                context.player(),
                                suctionTarget,
                                comboMove.suctionFixedDistance()
                        );
                        ServerCombatControlState.disableMovement(context.player().getUuid(), totalTicks);
                        ServerCombatControlState.disableMovement(suctionTarget.getUuid(), totalTicks);
                        ServerCombatControlState.disableAttack(
                                suctionTarget.getUuid(),
                                totalTicks + CombatControlConfig.COMBO_HIT_REACTION_ATTACK_DISABLE_EXTRA_TICKS
                        );
                    }

                    boolean perfectCounterSlow =
                            ServerCombatControlState.consumePerfectCounterAttackSlow(
                                    context.player().getUuid()
                            );

                    ServerCombatState.startAttack(
                            context.player().getUuid(),
                            direction,
                            context.player().getYaw(),
                            attackTargetEntityId,
                            lockedLunge,
                            totalTicks,
                            (float) attackSpeedMultiplier,
                            comboMove,
                            moveConfig,
                            startWorldTick,
                            perfectCounterSlow
                    );
                    ActiveServerAttack startedAttack = ServerCombatState.getAttack(context.player().getUuid());
                    if (startedAttack != null) {
                        startedAttack.clientAttackInstanceId = payload.attackInstanceId();
                        startedAttack.setLungeInput(
                                payload.movementKeyPressed(),
                                payload.lungeForwardInput(),
                                payload.lungeSideInput()
                        );
                    }
                    ServerHitDetectionSystem.flushPendingClientReportedHits(
                            context.player(),
                            payload.attackInstanceId()
                    );
                    ServerCombatStanceState.set(
                            context.player().getUuid(),
                            CombatDirection.afterSuccessfulAttack(direction)
                    );

                    if (comboMove != null) {
                        syncComboAttackAnimation(
                                context.player(),
                                direction,
                                comboMove,
                                (float) attackSpeedMultiplier
                        );
                        if (suctionTarget != null && !comboMove.victimAnimationName().isBlank()) {
                            syncComboAttackAnimation(
                                    suctionTarget,
                                    direction,
                                    comboMove.victimAnimationName(),
                                    (float) attackSpeedMultiplier,
                                false
                            );
                        }
                    } else {
                        syncAttackAnimation(
                                context.player(),
                                direction,
                                moveConfig,
                                (float) attackSpeedMultiplier,
                                0.0F
                        );
                    }

                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                StartBlockPayload.ID,
                (payload, context) -> {
                    if (!CombatItemUtil.canUseCustomCombat(context.player())) {
                        return;
                    }

                    if (!context.player().isOnGround()) {
                        return;
                    }

                    if (isInAttackStartupNoDefenseWindow(context.player().getUuid())) {
                        return;
                    }

                    if (!ServerCombatControlState.canBlock(context.player())) {
                        return;
                    }

                    boolean canBlockWithShield = EquipmentCombatAttributesRegistry.isShield(context.player().getOffHandStack())
                            && (!EquipmentCombatAttributesRegistry.isLargeShield(context.player().getOffHandStack())
                            || ServerCombatStanceState.canUseLargeShield(context.player().getUuid()));
                    if (!EquipmentCombatAttributesRegistry.canBlockWithHeldItem(context.player().getMainHandStack())
                            && !canBlockWithShield) {
                        return;
                    }

                    // 起手防御本身不立刻扣体力，格挡命中时在命中系统里按护甲消耗倍率处理。
                    if (!ServerBlockState.startBlock(
                            context.player().getUuid(),
                            CombatDirection.fromOrdinalSafe(payload.directionOrdinal()),
                            canBlockWithShield,
                            ModGameRules.classicMode(context.player())
                    )) {
                        return;
                    }
                    ServerCombatStanceState.set(
                            context.player().getUuid(),
                            CombatDirection.fromOrdinalSafe(payload.directionOrdinal())
                    );

                    Vec3d currentVelocity = context.player().getVelocity();
                    context.player().setVelocity(0.0, currentVelocity.y, 0.0);
                    context.player().velocityModified = true;
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                StartDodgePayload.ID,
                (payload, context) -> {
                    if (!CombatItemUtil.canUseCustomCombat(context.player())) {
                        return;
                    }

                    if (!PlayerPassiveSkillProgress.isUnlocked(context.player(), "dodge")) {
                        return;
                    }

                    if (!context.player().isOnGround()) {
                        return;
                    }

                    if (ServerCombatState.getAttack(context.player().getUuid()) != null) {
                        return;
                    }

                    if (!ServerCombatControlState.canDodge(context.player())) {
                        return;
                    }

                    DodgeDirection direction = DodgeDirection.fromOrdinalSafe(
                            payload.directionOrdinal()
                    );

                    double staminaCost = direction == DodgeDirection.FORWARD
                            ? CombatControlConfig.FORWARD_STEP_STAMINA_COST
                            : CombatControlConfig.DODGE_STAMINA_COST;

                    staminaCost *= EquipmentCombatAttributesRegistry.armorStaminaCostMultiplier(
                            context.player()
                    );
                    staminaCost *= PassiveSkillPerks.staminaCostMultiplier(
                            context.player(),
                            context.player().getMainHandStack(),
                            true,
                            false
                    );
                    if (hasLockedLargeShield(context.player())) {
                        staminaCost *= CombatControlConfig.LARGE_SHIELD_DODGE_STAMINA_COST_MULTIPLIER;
                    }

                    if (!ServerStaminaState.consume(context.player(), staminaCost)) {
                        return;
                    }

                    ServerCombatControlState.startDodge(context.player().getUuid(), direction);

                    Vec3d dodgeVelocity = getDodgeVelocity(context.player().getYaw(), direction, 0);
                    Vec3d currentVelocity = context.player().getVelocity();

                    context.player().setVelocity(
                            dodgeVelocity.x,
                            currentVelocity.y,
                            dodgeVelocity.z
                    );
                    context.player().velocityModified = true;
                }
        );
    }

    private static void syncServerConfig(ServerPlayerEntity player) {
        ServerPlayNetworking.send(
                player,
                new ServerConfigSyncPayload(
                        CombatServerConfig.modEquipmentGenerationEnabled(),
                        CombatServerConfig.zombieLeaderHealthFixEnabled(),
                        CombatServerConfig.mobToughnessEnabled(),
                        CombatServerConfig.hitStopTicks(),
                        CombatServerConfig.vanillaHurtSoundVolumeMultiplier(),
                        CombatServerConfig.masterCounterWindowTicks(),
                        CombatServerConfig.blockWindowTicks(),
                        CombatServerConfig.combatMinDistance(),
                        CombatServerConfig.vanillaAttackWeaponIds(),
                        player.hasPermissionLevel(2)
                )
        );
    }

    private static Vec3d getDodgeVelocity(float yaw, DodgeDirection direction, int ageTicks) {
        double rad = Math.toRadians(yaw);

        Vec3d forward = new Vec3d(
                -Math.sin(rad),
                0.0,
                Math.cos(rad)
        ).normalize();

        Vec3d right = new Vec3d(
                -forward.z,
                0.0,
                forward.x
        ).normalize();

        Vec3d base = switch (direction) {
            case FORWARD -> forward.multiply(CombatControlConfig.FORWARD_STEP_SPEED);
            case BACK -> forward.multiply(-CombatControlConfig.DODGE_BACK_SPEED);
            case LEFT -> right.multiply(-CombatControlConfig.DODGE_SIDE_SPEED);
            case RIGHT -> right.multiply(CombatControlConfig.DODGE_SIDE_SPEED);
        };

        return base.multiply(CombatControlConfig.getDodgeSpeedScale(ageTicks));
    }

    private static boolean hasLockedLargeShield(ServerPlayerEntity player) {
        return ServerCombatStanceState.isLocked(player.getUuid())
                && EquipmentCombatAttributesRegistry.isLargeShield(player.getOffHandStack());
    }

    private static boolean hasLargeShield(ServerPlayerEntity player) {
        return EquipmentCombatAttributesRegistry.isLargeShield(player.getOffHandStack());
    }

    private static boolean isInAttackStartupNoDefenseWindow(java.util.UUID uuid) {
        ActiveServerAttack attack = ServerCombatState.getAttack(uuid);

        return attack != null
                && CombatTiming.isInAttackStartupNoDefenseWindow(attack.ageTicks);
    }
    private static boolean matchesMasterCounterInput(
            CombatDirection playerDirection,
            CombatDirection enemyDirection
    ) {
        return switch (playerDirection) {
            case LEFT -> enemyDirection == CombatDirection.LEFT;
            case RIGHT -> enemyDirection == CombatDirection.RIGHT;

            // 重点：上下互反
            case UP -> enemyDirection == CombatDirection.DOWN;
            case DOWN -> enemyDirection == CombatDirection.UP;
        };
    }
    private static boolean tryStartMasterCounter(
            ServerPlayerEntity player,
            CombatDirection direction,
            int targetEntityId,
            long startWorldTick
    ) {
        if (!PlayerPassiveSkillProgress.isUnlocked(player, "master_counter")
                || !canUseMasterCounterWeapon(player)) {
            return false;
        }

        if (targetEntityId < 0) {
            return false;
        }

        if (ServerCombatState.getAttack(player.getUuid()) != null) {
            return false;
        }

        Entity entity = player.getWorld().getEntityById(targetEntityId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            return false;
        }

        if (player.squaredDistanceTo(target) > 2.0 * 2.0) {
            return false;
        }

        ActiveServerAttack targetAttack = ServerCombatState.getAttack(target.getUuid());

        if (targetAttack == null) {
            return false;
        }

        // Treat the input as arriving up to 0.1 seconds earlier to compensate
        // for packet/tick ordering, then apply the slightly wider base window.
        int compensatedAttackAge = Math.max(
                0,
                targetAttack.ageTicks - CombatControlConfig.MASTER_COUNTER_EARLY_GRACE_TICKS
        );
        if (compensatedAttackAge > CombatServerConfig.masterCounterWindowTicks()) {
            return false;
        }

        // 核心条件：
        // 玩家攻击方向必须和敌人当前攻击方向一致。
        // RIGHT + RIGHT -> master_counter_right
        // LEFT  + LEFT  -> master_counter_left
        // UP    + UP    -> master_counter_up
        // DOWN  + DOWN  -> master_counter_down
        if (!matchesMasterCounterInput(direction, targetAttack.direction)) {
            return false;
        }

        String configName = getMasterCounterConfigName(direction);

        AttackMoveConfig moveConfig = AttackMoveConfigs.getNamed(configName);

        // 如果 JSON 里没有对应方向的大师反配置，
        // 不吃掉这次输入，继续走普通攻击逻辑。
        if (moveConfig == null) {
            return false;
        }

        double staminaCost = moveConfig.staminaCost()
                * EquipmentCombatAttributesRegistry.armorStaminaCostMultiplier(player);
        staminaCost *= PassiveSkillPerks.staminaCostMultiplier(player, player.getMainHandStack(), false, true);
        if (hasLockedLargeShield(player)) {
            staminaCost *= CombatControlConfig.LARGE_SHIELD_ATTACK_STAMINA_COST_MULTIPLIER;
        }

        if (!ServerCombatControlState.canAttack(player)) {
            syncAttackRejected(player);
            return true;
        }

        if (!ServerStaminaState.consume(player, staminaCost)) {
            syncAttackRejected(player);
            return true;
        }

        ServerCombatState.removeAttack(target.getUuid());
        PassiveSkillPerks.afterMasterCounter(player, player.getMainHandStack());
        if (target instanceof MobEntity mob) {
            HumanoidCombatAiTicker.interruptFollowUps(mob);
        }
        ServerComboState.clear(player.getUuid());
        ServerComboState.clear(target.getUuid());

        // 每个方向的大师反可以在 JSON 里单独设置：
        // "master_counter_spacing": 1.15
        alignMasterCounterPair(player, target, moveConfig.masterCounterSpacing());

        int totalTicks = CombatAttackTiming.getAttackTotalTicks(direction, moveConfig);

        // 兜底：大师反至少要活到 direct_hit_tick 和 weapon_clash_tick 之后，
        // 否则会出现只播开头、没有伤害或碰撞音效的问题。
        int minTotalTicks = 20;

        if (moveConfig.directHitTick() >= 0) {
            minTotalTicks = Math.max(minTotalTicks, moveConfig.directHitTick() + 8);
        }

        if (moveConfig.weaponClashTick() >= 0) {
            minTotalTicks = Math.max(minTotalTicks, moveConfig.weaponClashTick() + 8);
        }

        totalTicks = Math.max(totalTicks, minTotalTicks);

        double attackSpeedMultiplier = EquipmentCombatAttributesRegistry.weaponAttackSpeedMultiplier(player)
                * EquipmentCombatAttributesRegistry.armorAttackSpeedMultiplier(player)
                * ModGameRules.combatSpeed(player);
        attackSpeedMultiplier *= PassiveSkillPerks.attackSpeedMultiplier(
                player,
                player.getMainHandStack()
        );
        if (BeowulfArmState.isActive(player)) {
            attackSpeedMultiplier *= BeowulfArmState.ATTACK_SPEED_MULTIPLIER;
        }
        if (hasLargeShield(player)) {
            attackSpeedMultiplier *= CombatControlConfig.LARGE_SHIELD_ATTACK_SPEED_MULTIPLIER;
        }
        totalTicks = Math.max(1, (int) Math.ceil(totalTicks / attackSpeedMultiplier));

        ServerCombatControlState.disableMovement(player.getUuid(), totalTicks);
        ServerCombatControlState.disableMovement(target.getUuid(), totalTicks);
        ServerCombatControlState.disableAttack(
                target.getUuid(),
                totalTicks + CombatControlConfig.COMBO_HIT_REACTION_ATTACK_DISABLE_EXTRA_TICKS
        );

        ServerCombatState.startAttack(
                player.getUuid(),
                direction,
                player.getYaw(),
                target.getId(),
                totalTicks,
                (float) attackSpeedMultiplier,
                null,
                moveConfig,
                startWorldTick,
                false
        );
        ServerCombatStanceState.set(player.getUuid(), CombatDirection.afterSuccessfulAttack(direction));

        syncComboAttackAnimation(
                player,
                direction,
                moveConfig.animationName(),
                (float) attackSpeedMultiplier,
                false
        );

        AttackMoveConfig victimMove = AttackMoveConfigs.getNamed(configName + "_victim");

        if (victimMove != null) {
            syncComboAttackAnimation(
                    target,
                    getOppositeDirection(direction),
                    victimMove.animationName(),
                    1.0F,
                    false
            );
        }

        return true;
    }

    private static boolean canUseMasterCounterWeapon(ServerPlayerEntity player) {
        if (player.getMainHandStack().isEmpty()) {
            return false;
        }
        return com.kingdomcomecombat.combat.CombatItemUtil.isSword(player.getMainHandStack());
    }

    private static String getMasterCounterConfigName(CombatDirection direction) {
        return switch (direction) {
            case LEFT -> "master_counter_left";
            case RIGHT -> "master_counter_right";
            case UP -> "master_counter_up";
            case DOWN -> "master_counter_down";
        };
    }

    private static CombatDirection getOppositeDirection(CombatDirection direction) {
        return switch (direction) {
            case LEFT -> CombatDirection.RIGHT;
            case RIGHT -> CombatDirection.LEFT;
            case UP -> CombatDirection.DOWN;
            case DOWN -> CombatDirection.UP;
        };
    }

    private static void alignMasterCounterPair(
            LivingEntity attacker,
            LivingEntity target,
            double spacing
    ) {
        Vec3d delta = target.getPos().subtract(attacker.getPos());
        Vec3d horizontal = new Vec3d(delta.x, 0.0, delta.z);

        if (horizontal.lengthSquared() <= 0.000001) {
            horizontal = new Vec3d(0.0, 0.0, 1.0);
        }

        Vec3d direction = horizontal.normalize();
        Vec3d midpoint = attacker.getPos().add(target.getPos()).multiply(0.5);

        Vec3d attackerPos = new Vec3d(
                midpoint.x - direction.x * spacing * 0.5,
                attacker.getY(),
                midpoint.z - direction.z * spacing * 0.5
        );

        Vec3d targetPos = new Vec3d(
                midpoint.x + direction.x * spacing * 0.5,
                target.getY(),
                midpoint.z + direction.z * spacing * 0.5
        );

        attacker.requestTeleport(attackerPos.x, attackerPos.y, attackerPos.z);
        target.requestTeleport(targetPos.x, targetPos.y, targetPos.z);

        float attackerYaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
        float targetYaw = attackerYaw + 180.0F;

        attacker.setYaw(attackerYaw);
        attacker.setHeadYaw(attackerYaw);
        attacker.setBodyYaw(attackerYaw);

        target.setYaw(targetYaw);
        target.setHeadYaw(targetYaw);
        target.setBodyYaw(targetYaw);

        attacker.setVelocity(Vec3d.ZERO);
        target.setVelocity(Vec3d.ZERO);

        attacker.velocityModified = true;
        target.velocityModified = true;
    }

    private static LivingEntity resolveComboTarget(ServerPlayerEntity player, int targetEntityId) {
        if (targetEntityId < 0 || !(player.getWorld() instanceof ServerWorld world)) {
            return null;
        }

        Entity target = world.getEntityById(targetEntityId);
        if (target instanceof LivingEntity livingTarget
                && livingTarget.isAlive()
                && livingTarget != player) {
            return livingTarget;
        }

        return null;
    }

    private static double horizontalDistance(LivingEntity first, LivingEntity second) {
        Vec3d delta = second.getPos().subtract(first.getPos());
        return Math.sqrt(delta.x * delta.x + delta.z * delta.z);
    }

    private static boolean canPlaySuctionVictimAnimation(LivingEntity target) {
        Identifier id = Registries.ENTITY_TYPE.getId(target.getType());
        if (!"minecraft".equals(id.getNamespace())) {
            return false;
        }

        return switch (id.getPath()) {
            case "zombie",
                 "husk",
                 "drowned",
                 "zombie_villager",
                 "zombified_piglin",
                 "skeleton",
                 "stray",
                 "wither_skeleton",
                 "bogged",
                 "piglin",
                 "piglin_brute",
                 "pillager",
                 "vindicator",
                 "evoker",
                 "illusioner" -> true;
            default -> false;
        };
    }

    private static void syncComboAttackAnimation(
            LivingEntity attacker,
            CombatDirection direction,
            ComboMoveConfig comboMove,
            float speedMultiplier
    ) {
        syncComboAttackAnimation(
                attacker,
                direction,
                comboMove.animationName(),
                speedMultiplier,
                comboMove.bladeTrail()
        );
    }

    private static void syncComboAttackAnimation(
            LivingEntity attacker,
            CombatDirection direction,
            String animationName,
            float speedMultiplier,
            boolean bladeTrail
    ) {
        EntityComboAttackAnimationPayload payload = new EntityComboAttackAnimationPayload(
                attacker.getId(),
                direction.ordinal(),
                animationName,
                speedMultiplier,
                bladeTrail
        );

        for (ServerPlayerEntity player : ((ServerWorld) attacker.getWorld()).getPlayers()) {
            if (player.squaredDistanceTo(attacker) <= 64.0 * 64.0) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void syncAttackAnimation(
            LivingEntity attacker,
            CombatDirection direction,
            AttackMoveConfig moveConfig,
            float speedMultiplier,
            float startupSlowdown
    ) {
        EntityAttackAnimationPayload payload = new EntityAttackAnimationPayload(
                attacker.getId(),
                direction.ordinal(),
                speedMultiplier,
                startupSlowdown,
                moveConfig == null ? "" : moveConfig.animationName()
        );

        for (ServerPlayerEntity player : ((ServerWorld) attacker.getWorld()).getPlayers()) {
            if (player.squaredDistanceTo(attacker) <= 64.0 * 64.0) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void syncAttackRejected(ServerPlayerEntity player) {
        ServerPlayNetworking.send(
                player,
                new EntityAttackInterruptPayload(player.getId())
        );
    }
}
