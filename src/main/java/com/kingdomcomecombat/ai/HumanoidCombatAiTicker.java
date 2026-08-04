package com.kingdomcomecombat.ai;

import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.ActiveServerAttack;
import com.kingdomcomecombat.combat.CombatAttackTiming;
import com.kingdomcomecombat.combat.CombatControlConfig;
import com.kingdomcomecombat.combat.DodgeDirection;
import com.kingdomcomecombat.combat.AttackMoveConfigs;
import com.kingdomcomecombat.combat.AttackMoveConfig;
import com.kingdomcomecombat.combat.CombatMovementConfig;
import com.kingdomcomecombat.combat.CombatTiming;
import com.kingdomcomecombat.combat.CombatWeaponUtil;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.combat.ServerComboState;
import com.kingdomcomecombat.combat.ServerCombatControlState;
import com.kingdomcomecombat.injury.ModStatusEffects;
import com.kingdomcomecombat.combat.ServerCombatState;
import com.kingdomcomecombat.config.CombatServerConfig;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.MobCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.MobScaleRegistry;
import com.kingdomcomecombat.game.ModGameRules;
import com.kingdomcomecombat.network.EntityCombatStancePayload;
import com.kingdomcomecombat.network.EntityAttackAnimationPayload;
import com.kingdomcomecombat.network.EntityCinematicVictimAnimationPayload;
import com.kingdomcomecombat.network.EntityComboAttackAnimationPayload;
import com.kingdomcomecombat.network.IncomingAttackWarningPayload;
import com.kingdomcomecombat.network.CombatNetworkBroadcaster;
import com.kingdomcomecombat.stamina.ServerStaminaState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HumanoidCombatAiTicker {
    private static final int DECISION_INTERVAL_TICKS = 10;
    private static final int VISIBILITY_CACHE_TICKS = 5;
    private static final int STATE_CLEANUP_INTERVAL_TICKS = 100;
    private static final int STANCE_SYNC_HEARTBEAT_TICKS = 100;
    private static final int MAX_ATTACK_HOLD_TICKS = 80;
    private static final int FORWARD_STEP_CHASE_COOLDOWN_TICKS = 200;
    private static final int FOLLOW_UP_TRANSITION_COOLDOWN_TICKS = 3;
    private static final int PERFECT_BLOCK_COUNTER_DELAY_TICKS = 8;
    private static final int PERFECT_BLOCK_COUNTER_WINDOW_TICKS = 32;
    private static final int STANCE_SWITCH_ATTACK_LOCK_TICKS = 6;
    private static final double PERFECT_BLOCK_COUNTER_EXTRA_STARTUP_SLOWDOWN = 0.30;
    private static final double UNPERFECT_BLOCK_PERFECT_CHANCE_PENALTY = 0.05;
    private static final double MINIMUM_PERFECT_BLOCK_CHANCE = 0.20;
    private static final Map<UUID, HumanoidCombatAiState> STATES = new HashMap<>();
    private static final Map<UUID, StanceSyncSnapshot> LAST_STANCE_SYNCS = new HashMap<>();
    private static final Set<UUID> ACTIVE_COMBAT_MOBS = new HashSet<>();
    private static long nextStateCleanupTick = 0L;

    private HumanoidCombatAiTicker() {
    }

    public static void register() {
        HumanoidCombatAiProfiles.registerDefaults();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ACTIVE_COMBAT_MOBS.clear();
            AiAttackCoordinator.tick(server.getOverworld().getTime());
            for (ServerWorld world : server.getWorlds()) {
                for (Entity entity : world.iterateEntities()) {
                    if (entity instanceof LivingEntity livingEntity
                            && !(entity instanceof net.minecraft.entity.player.PlayerEntity)) {
                        // Reuse this mandatory entity pass. ServerCombatTicker used
                        // to scan the whole world a second time for the same work.
                        MobCombatAttributesRegistry.applyConfiguredAttributes(livingEntity);
                    }
                    if (entity instanceof MobEntity mob) {
                        if (!isLoadedMob(world, mob)) {
                            continue;
                        }
                        ConfiguredMobAttackTicker.tick(mob);
                        if (!isLoadedMob(world, mob)) {
                            continue;
                        }
                        tickMob(mob);
                        if (!isLoadedMob(world, mob)) {
                            continue;
                        }
                        BeastCombatAiTicker.tickMobFromSharedPass(mob);
                    }
                }
            }
            separateActiveCombatMobs(server);

            long time = server.getOverworld().getTime();
            if (time >= nextStateCleanupTick) {
                cleanupDeadStates(server);
                BeastCombatAiTicker.cleanupDeadStatesFromSharedPass(server);
                nextStateCleanupTick = time + STATE_CLEANUP_INTERVAL_TICKS;
            }
        });
    }

    private static void tickMob(MobEntity mob) {
        if (!mob.isAlive() || mob.isRemoved() || !(mob.getWorld() instanceof ServerWorld)) {
            return;
        }
        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(mob);
        if (profile == null) {
            return;
        }
        var scaleAttribute = mob.getAttributeInstance(EntityAttributes.SCALE);
        double configuredScale = MobScaleRegistry.get(mob.getType());
        if (scaleAttribute != null && Math.abs(scaleAttribute.getBaseValue() - configuredScale) > 0.001) {
            scaleAttribute.setBaseValue(configuredScale);
        }

        HumanoidCombatAiState state = STATES.computeIfAbsent(
                mob.getUuid(),
                ignored -> new HumanoidCombatAiState()
        );
        HumanoidMobEquipmentInitializer.initialize(mob, state);

        if (isUsingRangedWeapon(mob)) {
            return;
        }

        if (profile.requiresWeapon() && mob.getMainHandStack().isEmpty() && !isUnarmedZombie(mob)) {
            return;
        }

        state.tickCooldowns();

        LivingEntity target = mob.getTarget();
        if (!canEnterCombatState(mob, target, profile, state)) {
            clearCombatStance(mob, state);
            recoverVanillaMovement(mob, target, state);
            return;
        }

        ServerStaminaState.setEntityMaxStamina(mob.getUuid(), profile.staminaMax());
        ServerStaminaState.setEntityRegenPerTick(mob.getUuid(), profile.staminaRegenPerTick());
        markActiveCombatMob(mob);
        // In 1.21.1 the Vindicator held-item feature is rendered only while
        // this vanilla flag is true. Vanilla melee goals may clear it every
        // tick, including during a KCC attack, so restore it every combat tick.
        if (mob instanceof VindicatorEntity) {
            mob.setAttacking(true);
        }

        if (com.kingdomcomecombat.combat.CombatItemUtil.isPolearm(mob.getMainHandStack())
                && state.direction == CombatDirection.UP) {
            setDirection(state, CombatDirection.RIGHT);
        }

        ActiveServerAttack activeAttack = ServerCombatState.getAttack(mob.getUuid());
        if (activeAttack != null) {
            faceTarget(mob, target);
            maybeQueueTransitionAttack(mob, target, profile, state, activeAttack);
            return;
        }

        if (ServerCombatControlState.isDodging(mob)) {
            mob.getNavigation().stop();
            faceTarget(mob, target);
            if (!isUnarmedZombie(mob)) {
                syncCombatStance(mob, state.direction, profile);
            }
            return;
        }

        faceTarget(mob, target);
        if (profile.keepDistance()) {
            maintainCombatDistance(mob, target, profile, state);
        } else {
            approachAggressively(mob, target, profile, state);
        }
        if (!isUnarmedZombie(mob)) {
            syncCombatStance(mob, state.direction, profile);
        }

        if (state.decisionTicks > 0) {
            return;
        }
        state.decisionTicks = DECISION_INTERVAL_TICKS;

        if (state.attackCooldownTicks > 0) {
            return;
        }

        if (!ServerCombatControlState.canAttack(mob)) {
            return;
        }

        boolean forcedCounterAttack = state.forcedCounterAttackTicks > 0;
        if (!forcedCounterAttack && state.handSwitchCooldownTicks > 0) {
            return;
        }

        if (mob.distanceTo(target) > profile.attackDistance()) {
            return;
        }

        ActiveServerAttack targetAttack = ServerCombatState.getAttack(target.getUuid());
        if (targetAttack != null) {
            return;
        }

        boolean startedComboPlan = !forcedCounterAttack
                && maybeStartComboPlan(mob, target, profile, state);
        if (forcedCounterAttack && state.forcedCounterDirection != null) {
            setDirection(state, state.forcedCounterDirection);
        } else if (!startedComboPlan) {
            maybeChangeDirection(mob, state, profile, false);
        }

        if (forcedCounterAttack && state.forcedCounterDelayTicks > 0) {
            return;
        }

        if (state.stanceSwitchLockTicks > 0) {
            return;
        }

        boolean forcedFollowUpAttack = state.followUpAttacksRemaining > 0;
        boolean forcedByHold = state.ticksSinceLastAttack >= MAX_ATTACK_HOLD_TICKS;
        if (!forcedCounterAttack
                && !forcedFollowUpAttack
                && !forcedByHold
                && mob.getRandom().nextDouble() > scaledAttackDesire(mob, profile)) {
            return;
        }

        if (!AiAttackCoordinator.canStartAttack(mob, target)) {
            return;
        }

        if (!startAttack(mob, target, profile, state, forcedFollowUpAttack, false)) {
            return;
        }
        syncIncomingAttackWarning(mob, target, state.direction);

        state.attackCooldownTicks = forcedFollowUpAttack && state.followUpAttacksRemaining > 0
                ? Math.min(profile.minAttackIntervalTicks(), 5)
                : profile.minAttackIntervalTicks();
    }

    private static boolean isUnarmedZombie(MobEntity mob) {
        return mob instanceof ZombieEntity && mob.getMainHandStack().isEmpty();
    }

    private static AttackMoveConfig selectAttackMove(MobEntity mob, HumanoidCombatAiState state) {
        if (!isUnarmedZombie(mob)) {
            return CombatWeaponUtil.resolveAttackMove(mob, state.direction);
        }

        CombatDirection direction = state.direction == CombatDirection.LEFT || state.direction == CombatDirection.RIGHT
                ? state.direction
                : (mob.getRandom().nextBoolean() ? CombatDirection.LEFT : CombatDirection.RIGHT);
        setDirection(state, direction);
        AttackMoveConfig config = AttackMoveConfigs.getNamed(
                direction == CombatDirection.LEFT ? "zombie_left" : "zombie_right"
        );
        return config != null ? config : AttackMoveConfigs.get(direction);
    }

    public static void onPerfectBlock(MobEntity mob, LivingEntity attacker, CombatDirection blockDirection) {
        HumanoidCombatAiState state = STATES.computeIfAbsent(
                mob.getUuid(),
                ignored -> new HumanoidCombatAiState()
        );

        CombatDirection counterDirection = com.kingdomcomecombat.combat.CombatItemUtil.isPolearm(mob.getMainHandStack())
                ? state.direction
                : CombatDirection.afterPerfectBlock(blockDirection);
        setDirection(state, counterDirection);
        state.forcedCounterDirection = counterDirection;
        state.attackCooldownTicks = 0;
        state.decisionTicks = 0;
        state.handSwitchCooldownTicks = 0;
        state.forcedCounterDelayTicks = PERFECT_BLOCK_COUNTER_DELAY_TICKS;
        state.forcedCounterAttackTicks = PERFECT_BLOCK_COUNTER_WINDOW_TICKS;
        mob.setTarget(attacker);
        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(mob);
        if (profile != null && !isUnarmedZombie(mob)) {
            syncCombatStance(mob, state.direction, profile);
        }
    }

    public static boolean tryStartMasterCounter(
            MobEntity mob,
            LivingEntity target,
            CombatDirection incomingAttackDirection
    ) {
        if (!ServerCombatControlState.canAttack(mob)) {
            return false;
        }
        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(mob);
        if (profile == null
                || profile.aiLevel() < 3
                || target == null
                || !target.isAlive()
                || com.kingdomcomecombat.combat.CombatItemUtil.isPolearm(target.getMainHandStack())
                || !com.kingdomcomecombat.combat.CombatItemUtil.isSword(mob.getMainHandStack())) {
            return false;
        }

        HumanoidCombatAiState state = STATES.computeIfAbsent(
                mob.getUuid(),
                ignored -> new HumanoidCombatAiState()
        );
        if (state.stanceSwitchLockTicks > 0
                || ServerCombatState.getAttack(mob.getUuid()) != null
                ) {
            return false;
        }

        CombatDirection incomingBlockSide = blockDirectionForAttack(incomingAttackDirection);
        if (incomingBlockSide != oppositeDirection(state.direction)) {
            return false;
        }

        if (mob.getRandom().nextDouble() > scaledBlockChance(mob, profile.perfectBlockChance())) {
            return false;
        }

        AttackMoveConfig moveConfig = AttackMoveConfigs.getNamed(masterCounterConfigName(state.direction));
        if (moveConfig == null) {
            return false;
        }

        double staminaCost = moveConfig.staminaCost()
                * com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry.armorStaminaCostMultiplier(mob)
                * CombatControlConfig.MOB_ATTACK_STAMINA_COST_MULTIPLIER;
        if (!ServerStaminaState.consume(mob, staminaCost)) {
            return false;
        }

        ServerCombatState.removeAttack(target.getUuid());
        if (target instanceof MobEntity targetMob) {
            interruptFollowUps(targetMob);
        }
        ServerComboState.clear(mob.getUuid());
        ServerComboState.clear(target.getUuid());

        faceTarget(mob, target);
        alignMasterCounterPair(mob, target, moveConfig.masterCounterSpacing());

        // Master counters are fixed cinematic actions. Mob profile, weapon and
        // combat-speed modifiers must not retime either the animation or hits.
        float attackAnimationSpeed = 1.0F;
        int totalTicks = CombatAttackTiming.getAttackTotalTicks(state.direction, moveConfig);
        int minTotalTicks = 20;
        if (moveConfig.directHitTick() >= 0) {
            minTotalTicks = Math.max(minTotalTicks, moveConfig.directHitTick() + 8);
        }
        if (moveConfig.weaponClashTick() >= 0) {
            minTotalTicks = Math.max(minTotalTicks, moveConfig.weaponClashTick() + 8);
        }
        int normalSpeedTotalTicks = Math.max(totalTicks, minTotalTicks);
        totalTicks = normalSpeedTotalTicks;

        ServerCombatControlState.disableMovement(mob.getUuid(), totalTicks);
        ServerCombatControlState.disableMovement(target.getUuid(), totalTicks);
        ServerCombatControlState.disableAttack(
                target.getUuid(),
                totalTicks + CombatControlConfig.COMBO_HIT_REACTION_ATTACK_DISABLE_EXTRA_TICKS
        );
        CombatDirection attackDirection = state.direction;
        CombatDirection postAttackDirection = choosePostAttackDirection(mob, state, profile, attackDirection);

        ServerCombatState.startAttack(
                mob.getUuid(),
                attackDirection,
                mob.getYaw(),
                target.getId(),
                true,
                totalTicks,
                attackAnimationSpeed,
                null,
                moveConfig,
                mob.getWorld().getTime(),
                false
        );
        AiAttackCoordinator.recordAttackStart(mob, target, normalSpeedTotalTicks);
        syncMasterCounterAnimation(mob, attackDirection, moveConfig.animationName(), attackAnimationSpeed);
        syncCombatStance(mob, postAttackDirection, profile);

        AttackMoveConfig victimMove = AttackMoveConfigs.getNamed(masterCounterConfigName(attackDirection) + "_victim");
        if (victimMove != null) {
            syncMasterCounterAnimation(target, oppositeDirection(attackDirection), victimMove.animationName(), 1.0F);
        }

        state.attackCooldownTicks = Math.max(state.attackCooldownTicks, profile.minAttackIntervalTicks());
        state.decisionTicks = DECISION_INTERVAL_TICKS;
        return true;
    }

    public static CombatDirection getCurrentDirection(LivingEntity entity) {
        HumanoidCombatAiState state = STATES.get(entity.getUuid());
        return state == null ? CombatDirection.RIGHT : state.direction;
    }

    public static void onAttackFinished(MobEntity mob, ActiveServerAttack attack) {
        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(mob);
        if (profile == null || attack == null) {
            return;
        }

        HumanoidCombatAiState state = STATES.computeIfAbsent(
                mob.getUuid(),
                ignored -> new HumanoidCombatAiState()
        );
        if (attack.comboMove != null || state.followUpAttacksRemaining <= 0) {
            clearComboPlan(state);
        }
        applyPostAttackDirectionChange(state, attack.direction);
        state.decisionTicks = Math.max(state.decisionTicks, DECISION_INTERVAL_TICKS);
        if (!isUnarmedZombie(mob)) {
            syncCombatStance(mob, state.direction, profile);
        }
    }

    public static void interruptFollowUps(MobEntity mob) {
        HumanoidCombatAiState state = STATES.get(mob.getUuid());
        if (state == null) {
            return;
        }

        state.followUpAttacksRemaining = 0;
        state.attackCooldownTicks = Math.max(state.attackCooldownTicks, 10);
    }

    public static double getDefensivePressureBonus(LivingEntity entity) {
        HumanoidCombatAiState state = STATES.get(entity.getUuid());
        return state == null ? 0.0 : state.defensivePressure;
    }

    public static double getPerfectBlockPenalty(LivingEntity entity) {
        HumanoidCombatAiState state = STATES.get(entity.getUuid());
        return state == null ? 0.0 : state.perfectBlockPenalty;
    }

    public static double getAttackLungeStopDistance(MobEntity mob, Entity target) {
        return CombatMovementConfig.LOCKED_STOP_FORWARD_DISTANCE;
    }

    public static double limitAndRecordAttackLungeSpeed(MobEntity mob, double speed) {
        return speed;
    }

    public static void resyncAttackAnimation(MobEntity mob, ActiveServerAttack attack) {
        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(mob);
        if (profile == null || attack == null) {
            return;
        }

        if (attack.comboMove != null) {
            syncComboAttackAnimation(mob, attack.direction, attack.comboMove, attack.animationSpeedMultiplier);
            return;
        }

        syncAttackAnimation(
                mob,
                attack.direction,
                profile,
                attack.moveConfig(),
                attack.animationSpeedMultiplier,
                attack.startupSlowdown
        );
    }

    public static boolean shouldForcePerfectBlock(LivingEntity entity) {
        if (CombatServerConfig.lightweightBlockingModeEnabled()) return false;
        HumanoidCombatAiState state = STATES.get(entity.getUuid());
        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(entity);
        if (state == null || profile == null || profile.perfectBlockChance() < MINIMUM_PERFECT_BLOCK_CHANCE) {
            return false;
        }
        int attacksBeforeForcedPerfectBlock = (int) Math.ceil(2.0 / profile.perfectBlockChance() + 1.0);
        return state != null
                && state.attacksSincePerfectBlock >= attacksBeforeForcedPerfectBlock;
    }

    public static void onPerfectBlockSucceeded(LivingEntity entity) {
        HumanoidCombatAiState state = STATES.get(entity.getUuid());
        if (state == null) {
            return;
        }

        state.attacksSincePerfectBlock = 0;
        state.forcedPerfectBlockTicks = 0;
    }

    public static void onUnperfectBlock(LivingEntity entity) {
        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(entity);
        if (profile == null || !(entity instanceof MobEntity)) {
            return;
        }

        HumanoidCombatAiState state = STATES.computeIfAbsent(
                entity.getUuid(),
                ignored -> new HumanoidCombatAiState()
        );
        state.perfectBlockPenalty = Math.min(
                0.45,
                state.perfectBlockPenalty + UNPERFECT_BLOCK_PERFECT_CHANCE_PENALTY
        );
    }

    public static void recordDefensivePressure(LivingEntity entity) {
        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(entity);
        if (profile == null || !(entity instanceof MobEntity)) {
            return;
        }

        HumanoidCombatAiState state = STATES.computeIfAbsent(
                entity.getUuid(),
                ignored -> new HumanoidCombatAiState()
        );
        state.defensivePressure = Math.min(0.45, state.defensivePressure + 0.07);
        state.attacksSincePerfectBlock++;
    }

    public static void onPlayerVulnerableToFollowUp(MobEntity attacker, LivingEntity target) {
        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(attacker);
        if (profile == null || target == null || !target.isAlive()) {
            return;
        }

        if (profile.maxFollowUpAttacks() <= 0 || profile.followUpAttackChance() <= 0.0) {
            return;
        }

        int followUps = rollFollowUpAttacks(attacker, profile);
        if (followUps <= 0) {
            return;
        }

        HumanoidCombatAiState state = STATES.computeIfAbsent(
                attacker.getUuid(),
                ignored -> new HumanoidCombatAiState()
        );
        state.followUpAttacksRemaining = Math.max(state.followUpAttacksRemaining, followUps);
        state.attackCooldownTicks = Math.min(state.attackCooldownTicks, 4);
        state.decisionTicks = 0;
        attacker.setTarget(target);
    }

    private static void maybeQueueTransitionAttack(
            MobEntity mob,
            LivingEntity target,
            HumanoidCombatAiProfile profile,
            HumanoidCombatAiState state,
            ActiveServerAttack currentAttack
    ) {
        if (state.followUpAttacksRemaining <= 0) {
            return;
        }

        if (!currentAttack.connectedOrNormalBlocked || currentAttack.perfectBlocked) {
            state.followUpAttacksRemaining = 0;
            return;
        }

        if (!CombatTiming.canReleaseBufferedAttack(
                currentAttack.ageTicks,
                currentAttack.attackTotalTicks,
                currentAttack.transitionTicks()
        )) {
            return;
        }

        if (mob.distanceTo(target) > profile.attackDistance() + 0.75) {
            return;
        }

        if (!ServerCombatControlState.canAttack(mob)) {
            return;
        }

        if (state.stanceSwitchLockTicks > 0) {
            return;
        }

        boolean followingComboPlan = applyComboDirectionForFollowUp(
                mob,
                target,
                profile,
                state,
                currentAttack.direction
        );
        if (!followingComboPlan) {
            applyPostAttackDirectionChange(state, currentAttack.direction);
        }

        ServerComboState.recordFinishedAttack(
                mob.getUuid(),
                currentAttack,
                mob.getWorld().getTime()
        );
        if (startAttack(mob, target, profile, state, true, true)) {
            state.attackCooldownTicks = Math.min(profile.minAttackIntervalTicks(), FOLLOW_UP_TRANSITION_COOLDOWN_TICKS);
            syncIncomingAttackWarning(mob, target, state.direction);
        } else {
            ServerComboState.clear(mob.getUuid());
        }
    }

    private static boolean startAttack(
            MobEntity mob,
            LivingEntity target,
            HumanoidCombatAiProfile profile,
            HumanoidCombatAiState state,
            boolean consumesFollowUp,
            boolean transitionAttack
    ) {
        if (ServerCombatState.getAttack(target.getUuid()) != null) {
            return false;
        }
        if (com.kingdomcomecombat.combat.CombatItemUtil.isPolearm(mob.getMainHandStack())
                && state.direction == CombatDirection.UP) {
            setDirection(state, mob.getRandom().nextBoolean() ? CombatDirection.LEFT : CombatDirection.RIGHT);
        }
        // A confirmed follow-up changes stance and starts its blended transition
        // in the same tick. The stance change itself sets this lock, so applying
        // it here would reject every directional follow-up immediately.
        if (!transitionAttack && state.stanceSwitchLockTicks > 0) {
            return false;
        }

        ComboMoveConfig comboMove = ServerComboState.selectComboForStart(
                mob.getUuid(),
                state.direction,
                mob.getWorld().getTime(),
                mob.getMainHandStack(),
                combo -> canAiUseCombo(mob, target, profile, combo)
        ).orElse(null);
        if (!transitionAttack && !AiAttackCoordinator.canStartAttack(mob, target)) {
            return false;
        }

        AttackMoveConfig moveConfig = selectAttackMove(mob, state);
        double staminaCost = (comboMove != null ? comboMove.staminaCost() : moveConfig.staminaCost())
                * CombatControlConfig.MOB_ATTACK_STAMINA_COST_MULTIPLIER;
        if (!ServerStaminaState.hasAtLeast(mob, staminaCost)) {
            return false;
        }

        boolean forcedCounterAttack = state.forcedCounterAttackTicks > 0;
        state.forcedCounterAttackTicks = 0;
        state.forcedCounterDelayTicks = 0;
        state.forcedCounterDirection = null;
        if (consumesFollowUp) {
            state.followUpAttacksRemaining--;
        }
        state.ticksSinceLastAttack = 0;
        ServerStaminaState.consume(mob, staminaCost);
        mob.getNavigation().stop();

        boolean comboAttack = comboMove != null;
        float attackAnimationSpeed = (float) Math.max(
                0.05,
                profile.attackAnimationSpeed() * (comboAttack
                        ? 1.0
                        : EquipmentCombatAttributesRegistry.weaponAttackSpeedMultiplier(mob)
                                * ModGameRules.combatSpeed(mob))
        );
        int rawAttackTicks = comboMove != null
                ? CombatAttackTiming.getComboAttackTotalTicks(comboMove.animationName())
                : CombatAttackTiming.getAttackTotalTicks(state.direction, moveConfig);
        int attackTotalTicks = (int) Math.ceil(rawAttackTicks / attackAnimationSpeed);
        boolean perfectCounterSlow =
                ServerCombatControlState.consumePerfectCounterAttackSlow(mob.getUuid());
        double startupSlowdown = transitionAttack ? 0.0 : profile.attackStartupSlowdown();
        if (forcedCounterAttack) {
            startupSlowdown = Math.min(
                    0.95,
                    startupSlowdown + PERFECT_BLOCK_COUNTER_EXTRA_STARTUP_SLOWDOWN
            );
        }

        CombatDirection attackDirection = state.direction;
        CombatDirection postAttackDirection = choosePostAttackDirection(mob, state, profile, attackDirection);

        ServerCombatState.startAttack(
                mob.getUuid(),
                attackDirection,
                mob.getYaw(),
                target.getId(),
                true,
                attackTotalTicks,
                attackAnimationSpeed,
                comboMove,
                moveConfig,
                mob.getWorld().getTime(),
                perfectCounterSlow,
                startupSlowdown
        );
        AiAttackCoordinator.recordAttackStart(mob, target, comboAttack ? attackTotalTicks : rawAttackTicks);
        if (comboMove != null) {
            clearComboPlan(state);
            syncComboAttackAnimation(mob, attackDirection, comboMove, attackAnimationSpeed);
            if (comboMove.suctionCombo() && !comboMove.victimAnimationName().isBlank()) {
                syncCinematicVictimAnimation(
                        target,
                        attackDirection,
                        comboMove.victimAnimationName(),
                        attackAnimationSpeed
                );
            }
        } else {
            syncAttackAnimation(mob, attackDirection, profile, moveConfig, attackAnimationSpeed, startupSlowdown);
        }
        syncCombatStance(mob, postAttackDirection, profile);
        return true;
    }

    private static double scaledAttackDesire(MobEntity mob, HumanoidCombatAiProfile profile) {
        return Math.min(
                1.0,
                Math.max(0.0, scaleAiChance(mob, profile.attackDesirePerHalfSecond())
                        * (CombatServerConfig.lightweightBlockingModeEnabled() ? 0.9 : 1.0)
                        * ModGameRules.combatSpeed(mob))
        );
    }

    public static double scaleAiChance(LivingEntity entity, double chance) {
        double multiplier = switch (entity.getWorld().getDifficulty()) {
            case EASY, PEACEFUL -> 0.8;
            case NORMAL -> 0.9;
            default -> 1.0;
        };
        return Math.max(0.0, Math.min(1.0, chance * multiplier));
    }

    public static double scaledBlockChance(LivingEntity entity, double chance) {
        double scaled = scaleAiChance(entity, chance);
        if (CombatServerConfig.lightweightBlockingModeEnabled()) {
            scaled = Math.max(0.0, scaled - (1.0 - scaled));
        }
        return scaled;
    }

    private static void applyPostAttackDirectionChange(
            HumanoidCombatAiState state,
            CombatDirection previousAttackDirection
    ) {
        CombatDirection postAttackDirection = state.pendingPostAttackDirection != null
                ? state.pendingPostAttackDirection
                : CombatDirection.afterSuccessfulAttack(previousAttackDirection);
        int handSwitchCooldown = state.pendingPostAttackHandSwitchCooldownTicks;
        state.pendingPostAttackDirection = null;
        state.pendingPostAttackHandSwitchCooldownTicks = -1;
        setDirection(state, postAttackDirection);
        if (handSwitchCooldown >= 0) {
            state.handSwitchCooldownTicks = handSwitchCooldown;
        }
    }

    private static CombatDirection choosePostAttackDirection(
            MobEntity mob,
            HumanoidCombatAiState state,
            HumanoidCombatAiProfile profile,
            CombatDirection attackDirection
    ) {
        double chance = 0.25 + profile.aiLevel() * 0.07;
        CombatDirection postAttackDirection = CombatDirection.afterSuccessfulAttack(attackDirection);
        int handSwitchCooldown = -1;

        if (mob.getRandom().nextDouble() <= Math.min(0.85, chance)) {
            CombatDirection[] directions = CombatDirection.values();
            postAttackDirection = directions[mob.getRandom().nextInt(directions.length)];
            handSwitchCooldown = state.direction != postAttackDirection ? STANCE_SWITCH_ATTACK_LOCK_TICKS : 0;
        }

        state.pendingPostAttackDirection = postAttackDirection;
        state.pendingPostAttackHandSwitchCooldownTicks = handSwitchCooldown;
        return postAttackDirection;
    }

    private static boolean applyComboDirectionForFollowUp(
            MobEntity mob,
            LivingEntity target,
            HumanoidCombatAiProfile profile,
            HumanoidCombatAiState state,
            CombatDirection previousAttackDirection
    ) {
        if (state.plannedCombo != null) {
            if (state.plannedComboIndex < state.plannedCombo.sequence().size()
                    && canAiUseCombo(mob, target, profile, state.plannedCombo)) {
                setDirection(state, state.plannedCombo.sequence().get(state.plannedComboIndex));
                state.plannedComboIndex++;
                state.handSwitchCooldownTicks = 0;
                return true;
            }
            clearComboPlan(state);
        }

        if (profile.comboLevel() <= 0) {
            return false;
        }

        double chance = profile.comboPlanChance();
        if (mob.getRandom().nextDouble() > chance) {
            return false;
        }

        List<ComboMoveConfig> candidates = ComboMoveConfigs.findByPrefix(
                List.of(previousAttackDirection),
                mob.getMainHandStack(),
                combo -> canAiUseCombo(mob, target, profile, combo)
        );
        if (candidates.isEmpty()) {
            return false;
        }

        ComboMoveConfig combo = candidates.get(mob.getRandom().nextInt(candidates.size()));
        state.plannedCombo = combo;
        state.plannedComboIndex = 2;
        setDirection(state, combo.sequence().get(1));
        state.handSwitchCooldownTicks = 0;
        return true;
    }

    private static boolean maybeStartComboPlan(
            MobEntity mob,
            LivingEntity target,
            HumanoidCombatAiProfile profile,
            HumanoidCombatAiState state
    ) {
        if (state.plannedCombo != null) return true;
        if (profile.comboLevel() <= 0 || mob.getRandom().nextDouble() > profile.comboPlanChance()) {
            return false;
        }
        List<ComboMoveConfig> candidates = ComboMoveConfigs.all().stream()
                .filter(combo -> combo.sequence().size() >= 2)
                .filter(combo -> canAiUseCombo(mob, target, profile, combo))
                .toList();
        if (candidates.isEmpty()) return false;

        ComboMoveConfig combo = candidates.get(mob.getRandom().nextInt(candidates.size()));
        state.plannedCombo = combo;
        state.plannedComboIndex = 1;
        state.followUpAttacksRemaining = Math.max(
                state.followUpAttacksRemaining, combo.sequence().size() - 1);
        state.handSwitchCooldownTicks = 0;
        setDirection(state, combo.sequence().getFirst());
        ServerComboState.clear(mob.getUuid());
        return true;
    }

    private static boolean canAiUseCombo(
            MobEntity mob,
            LivingEntity target,
            HumanoidCombatAiProfile profile,
            ComboMoveConfig combo
    ) {
        if (combo.level() > profile.comboLevel()
                || !ComboMoveConfigs.canUseWith(combo, mob.getMainHandStack())) {
            return false;
        }

        if (!combo.suctionCombo() || target == null) {
            return true;
        }

        double horizontalDistance = horizontalDistance(mob, target);
        if (combo.suctionMinDistance() > 0.0 && horizontalDistance < combo.suctionMinDistance()) {
            return false;
        }

        return combo.suctionDistance() <= 0.0 || horizontalDistance <= combo.suctionDistance();
    }

    private static double horizontalDistance(LivingEntity first, LivingEntity second) {
        Vec3d delta = first.getPos().subtract(second.getPos());
        return Math.sqrt(delta.x * delta.x + delta.z * delta.z);
    }

    private static void clearComboPlan(HumanoidCombatAiState state) {
        state.plannedCombo = null;
        state.plannedComboIndex = 0;
    }

    private static int rollFollowUpAttacks(MobEntity attacker, HumanoidCombatAiProfile profile) {
        int max = Math.max(0, profile.maxFollowUpAttacks());
        int count = 0;
        while (count < max && attacker.getRandom().nextDouble() <= scaleAiChance(attacker, profile.followUpAttackChance())) {
            count++;
        }

        return count;
    }

    public static boolean tryDodgeIncomingAttack(MobEntity mob, LivingEntity attacker) {
        if (!ServerCombatControlState.canDodge(mob)) {
            return false;
        }
        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(mob);
        if (profile == null) {
            return false;
        }

        HumanoidCombatAiState state = STATES.computeIfAbsent(
                mob.getUuid(),
                ignored -> new HumanoidCombatAiState()
        );

        if (!mob.isOnGround()
                || state.dodgeCooldownTicks > 0
                || ServerCombatControlState.isDodging(mob)
                || ServerCombatState.getAttack(mob.getUuid()) != null) {
            return false;
        }

        if (!ServerStaminaState.hasAtLeast(mob, CombatControlConfig.DODGE_STAMINA_COST)) {
            return false;
        }

        if (mob.getRandom().nextDouble() > scaleAiChance(mob, profile.dodgeChance())) {
            return false;
        }

        faceTarget(mob, attacker);
        ActiveServerAttack incoming = ServerCombatState.getAttack(attacker.getUuid());
        DodgeDirection dodgeDirection = incoming != null
                && (incoming.direction == CombatDirection.UP || incoming.direction == CombatDirection.DOWN)
                ? preferredSideDodge(mob)
                : DodgeDirection.BACK;
        ServerStaminaState.consume(mob, CombatControlConfig.DODGE_STAMINA_COST);
        ServerCombatControlState.startDodge(mob, dodgeDirection);
        state.dodgeCooldownTicks = 32;
        state.attackCooldownTicks = Math.max(state.attackCooldownTicks, 12);
        mob.getNavigation().stop();
        return true;
    }

    private static DodgeDirection preferredSideDodge(MobEntity mob) {
        double radians = Math.toRadians(mob.getYaw());
        Vec3d right = new Vec3d(Math.cos(radians), 0.0, Math.sin(radians));
        boolean rightClear = mob.getWorld().isSpaceEmpty(mob, mob.getBoundingBox().offset(right.multiply(1.2)));
        boolean leftClear = mob.getWorld().isSpaceEmpty(mob, mob.getBoundingBox().offset(right.multiply(-1.2)));
        if (rightClear && leftClear) return mob.getRandom().nextBoolean() ? DodgeDirection.RIGHT : DodgeDirection.LEFT;
        if (rightClear) return DodgeDirection.RIGHT;
        if (leftClear) return DodgeDirection.LEFT;
        return DodgeDirection.BACK;
    }

    private static boolean canEnterCombatState(
            MobEntity mob,
            LivingEntity target,
            HumanoidCombatAiProfile profile,
            HumanoidCombatAiState state
    ) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            state.visibilityTargetId = -1;
            state.cachedTargetVisible = false;
            return false;
        }

        if (mob.squaredDistanceTo(target) > profile.combatEnterDistance() * profile.combatEnterDistance()) {
            return false;
        }

        if (state.visibilityTargetId != target.getId()) {
            state.visibilityTargetId = target.getId();
            state.visibilityCheckTicks = 0;
        }
        if (state.visibilityCheckTicks <= 0) {
            state.cachedTargetVisible = mob.getVisibilityCache().canSee(target);
            state.visibilityCheckTicks = VISIBILITY_CACHE_TICKS;
        } else {
            state.visibilityCheckTicks--;
        }
        return state.cachedTargetVisible;
    }

    private static void maintainCombatDistance(
            MobEntity mob,
            LivingEntity target,
            HumanoidCombatAiProfile profile,
            HumanoidCombatAiState state
    ) {
        if (enforceMinimumCombatDistance(mob, target, profile)) {
            return;
        }

        double distance = mob.distanceTo(target);

        if (ServerStaminaState.getCurrent(mob) <= 0.5) {
            tryRetreatWhileExhausted(mob, target, distance, profile);
            return;
        }

        if (distance < profile.minDistance()) {
            mob.getNavigation().stop();
            return;
        }

        if (tryForwardStepChase(mob, target, profile, state, distance)) {
            return;
        }

        if (distance > profile.approachDistance()) {
            mob.getNavigation().startMovingTo(target, 0.85);
            return;
        }

        if (distance <= profile.attackDistance()) {
            mob.getNavigation().stop();
            return;
        }

        mob.getNavigation().startMovingTo(target, 0.75);
    }

    private static void approachAggressively(
            MobEntity mob,
            LivingEntity target,
            HumanoidCombatAiProfile profile,
            HumanoidCombatAiState state
    ) {
        if (enforceMinimumCombatDistance(mob, target, profile)) {
            return;
        }

        double distance = mob.distanceTo(target);
        if (tryForwardStepChase(mob, target, profile, state, distance)) {
            return;
        }

        if (distance > profile.minDistance()) {
            mob.getNavigation().startMovingTo(target, 0.92);
        } else {
            mob.getNavigation().stop();
        }
    }

    private static boolean tryForwardStepChase(
            MobEntity mob,
            LivingEntity target,
            HumanoidCombatAiProfile profile,
            HumanoidCombatAiState state,
            double distance
    ) {
        if (profile.dodgeChance() <= 0.0
                || state.forwardStepChaseCooldownTicks > 0
                || ServerCombatState.getAttack(mob.getUuid()) != null
                || !ServerCombatControlState.canDodge(mob)
                || !ServerCombatControlState.canAttack(mob)
                || !ServerStaminaState.hasAtLeast(mob, CombatControlConfig.FORWARD_STEP_STAMINA_COST)) {
            return false;
        }

        double startDistance = Math.max(profile.attackDistance() + 0.45, profile.minDistance() + 0.70);
        double lockedForwardLungeDistance = CombatMovementConfig.PLAYER_ATTACK_LUNGE_DISTANCE
                * CombatMovementConfig.LOCKED_ATTACK_LUNGE_MULTIPLIER;
        double maxDistance = Math.max(
                profile.approachDistance() + lockedForwardLungeDistance * 0.45,
                startDistance + lockedForwardLungeDistance * 0.55
        );
        if (distance < startDistance || distance > maxDistance) {
            return false;
        }

        Vec3d targetVelocity = target.getVelocity();
        Vec3d toTarget = target.getPos().subtract(mob.getPos());
        Vec3d horizontalToTarget = new Vec3d(toTarget.x, 0.0, toTarget.z);
        if (horizontalToTarget.lengthSquared() <= 0.000001) {
            return false;
        }

        double retreatSpeed = new Vec3d(targetVelocity.x, 0.0, targetVelocity.z)
                .dotProduct(horizontalToTarget.normalize());
        double mobilityChanceScale = Math.min(1.0, profile.dodgeChance() / 0.08);
        double chance = (retreatSpeed > 0.03 ? 0.95 : 0.45 + profile.aiLevel() * 0.08)
                * mobilityChanceScale;
        if (mob.getRandom().nextDouble() > Math.min(0.95, chance)) {
            return false;
        }

        mob.getNavigation().stop();
        faceTarget(mob, target);
        ServerStaminaState.consume(mob, CombatControlConfig.FORWARD_STEP_STAMINA_COST);
        ServerCombatControlState.startDodge(mob, DodgeDirection.FORWARD);
        state.forwardStepChaseCooldownTicks = FORWARD_STEP_CHASE_COOLDOWN_TICKS;
        state.decisionTicks = Math.min(state.decisionTicks, 3);
        return true;
    }

    private static void recoverVanillaMovement(
            MobEntity mob,
            LivingEntity target,
            HumanoidCombatAiState state
    ) {
        state.decisionTicks = 0;
        clearComboPlan(state);

        if (target == null
                || !target.isAlive()
                || target.isRemoved()
                || ServerCombatControlState.isDodging(mob)
                || ServerCombatState.getAttack(mob.getUuid()) != null) {
            return;
        }

        if (mob.squaredDistanceTo(target) > 2.25) {
            mob.getNavigation().startMovingTo(target, 0.82);
        }
    }

    private static boolean enforceMinimumCombatDistance(
            MobEntity mob,
            LivingEntity target,
            HumanoidCombatAiProfile profile
    ) {
        // Horizontal spacing must not pin a mob on a ledge while its target is below it.
        // In that case vanilla navigation needs to be allowed to walk off/down immediately.
        if (Math.abs(target.getY() - mob.getY()) > 0.75) {
            return false;
        }
        double minimumDistance = Math.max(
                CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE,
                profile.minDistance()
        );
        Vec3d toTarget = target.getPos().subtract(mob.getPos());
        Vec3d horizontalToTarget = new Vec3d(toTarget.x, 0.0, toTarget.z);
        double distance = horizontalToTarget.length();

        if (distance >= minimumDistance || distance <= 0.000001) {
            return false;
        }

        mob.getNavigation().stop();
        Vec3d toTargetDir = horizontalToTarget.normalize();
        Vec3d velocity = mob.getVelocity();
        Vec3d horizontalVelocity = new Vec3d(velocity.x, 0.0, velocity.z);
        double towardSpeed = horizontalVelocity.dotProduct(toTargetDir);
        if (towardSpeed > 0.0) {
            horizontalVelocity = horizontalVelocity.subtract(toTargetDir.multiply(towardSpeed));
        }

        double separationSpeed = Math.min(0.16, (minimumDistance - distance) * 0.35);
        Vec3d awayVelocity = toTargetDir.multiply(-separationSpeed);
        mob.setVelocity(
                horizontalVelocity.x + awayVelocity.x,
                velocity.y,
                horizontalVelocity.z + awayVelocity.z
        );
        mob.velocityModified = true;
        return true;
    }

    private static void tryRetreatWhileExhausted(
            MobEntity mob,
            LivingEntity target,
            double distance,
            HumanoidCombatAiProfile profile
    ) {
        if (distance >= profile.exhaustedRetreatDistance()) {
            mob.getNavigation().stop();
            return;
        }

        Vec3d away = mob.getPos().subtract(target.getPos());
        Vec3d horizontalAway = new Vec3d(away.x, 0.0, away.z);
        if (horizontalAway.lengthSquared() <= 0.000001) {
            mob.getNavigation().stop();
            return;
        }

        Vec3d retreatTarget = mob.getPos()
                .add(horizontalAway.normalize().multiply(profile.exhaustedRetreatDistance() - distance));
        mob.getNavigation().startMovingTo(retreatTarget.x, retreatTarget.y, retreatTarget.z, 0.92);
    }

    private static void faceTarget(MobEntity mob, LivingEntity target) {
        if (target instanceof net.minecraft.entity.player.PlayerEntity
                && ServerCombatControlState.isDodging(target)) {
            return;
        }
        mob.lookAtEntity(target, 45.0F, 45.0F);

        Vec3d delta = target.getPos().subtract(mob.getPos());
        double yaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0;
        mob.setYaw((float) yaw);
        mob.setHeadYaw((float) yaw);
    }

    private static boolean maybeChangeDirection(
            MobEntity mob,
            HumanoidCombatAiState state,
            HumanoidCombatAiProfile profile,
            boolean afterAttack
    ) {
        double chance = afterAttack
                ? 0.25 + profile.aiLevel() * 0.07
                : 0.08 + profile.aiLevel() * 0.04;

        if (mob.getRandom().nextDouble() > Math.min(0.85, chance)) {
            return false;
        }

        CombatDirection[] directions = CombatDirection.values();
        CombatDirection next = directions[mob.getRandom().nextInt(directions.length)];

        if (com.kingdomcomecombat.combat.CombatItemUtil.isPolearm(mob.getMainHandStack())) {
            directions = new CombatDirection[]{CombatDirection.LEFT, CombatDirection.RIGHT, CombatDirection.DOWN};
            next = directions[mob.getRandom().nextInt(directions.length)];
        }

        if (directions.length > 1) {
            while (next == state.direction) {
                next = directions[mob.getRandom().nextInt(directions.length)];
            }
        }

        setDirection(state, next);
        state.handSwitchCooldownTicks = STANCE_SWITCH_ATTACK_LOCK_TICKS;
        return true;
    }

    private static void setDirection(HumanoidCombatAiState state, CombatDirection direction) {
        if (state.direction != direction) {
            state.direction = direction;
            state.stanceSwitchLockTicks = STANCE_SWITCH_ATTACK_LOCK_TICKS;
        }
    }

    private static boolean isUsingRangedWeapon(MobEntity mob) {
        return mob.getMainHandStack().getItem() instanceof RangedWeaponItem;
    }

    private static void syncCombatStance(
            MobEntity mob,
            CombatDirection direction,
            HumanoidCombatAiProfile profile
    ) {
        STATES.computeIfAbsent(mob.getUuid(), ignored -> new HumanoidCombatAiState()).stanceSynced = true;
        int targetId = mob.getTarget() == null ? -1 : mob.getTarget().getId();
        String animationName = CombatWeaponUtil.stanceAnimationName(mob, direction);
        float animationSpeed = (float) profile.attackAnimationSpeed();
        long now = mob.getWorld().getTime();
        StanceSyncSnapshot previous = LAST_STANCE_SYNCS.get(mob.getUuid());
        if (previous != null
                && previous.targetId() == targetId
                && previous.direction() == direction
                && Float.compare(previous.animationSpeed(), animationSpeed) == 0
                && previous.animationName().equals(animationName)
                && now - previous.syncTick() < STANCE_SYNC_HEARTBEAT_TICKS) {
            return;
        }

        EntityCombatStancePayload payload = new EntityCombatStancePayload(
                mob.getId(),
                targetId,
                direction.ordinal(),
                animationSpeed,
                animationName
        );

        sendToNearbyPlayers(mob, payload);
        LAST_STANCE_SYNCS.put(
                mob.getUuid(),
                new StanceSyncSnapshot(targetId, direction, animationSpeed, animationName, now)
        );
    }

    private static void clearCombatStance(MobEntity mob, HumanoidCombatAiState state) {
        if (mob instanceof VindicatorEntity) {
            mob.setAttacking(false);
        }
        if (!state.stanceSynced) {
            return;
        }
        sendToNearbyPlayers(mob, new EntityCombatStancePayload(mob.getId(), -1, -1, 1.0F, ""));
        state.stanceSynced = false;
        LAST_STANCE_SYNCS.remove(mob.getUuid());
    }

    private static void syncAttackAnimation(
            MobEntity mob,
            CombatDirection direction,
            HumanoidCombatAiProfile profile,
            AttackMoveConfig moveConfig,
            float attackAnimationSpeed,
            double startupSlowdown
    ) {
        EntityAttackAnimationPayload payload = new EntityAttackAnimationPayload(
                mob.getId(),
                direction.ordinal(),
                attackAnimationSpeed,
                (float) startupSlowdown,
                moveConfig == null ? "" : moveConfig.animationName()
        );

        sendToNearbyPlayers(mob, payload);
    }

    private static void syncComboAttackAnimation(
            MobEntity mob,
            CombatDirection direction,
            ComboMoveConfig comboMove,
            float attackAnimationSpeed
    ) {
        EntityComboAttackAnimationPayload payload = new EntityComboAttackAnimationPayload(
                mob.getId(),
                direction.ordinal(),
                comboMove.animationName(),
                attackAnimationSpeed,
                comboMove.bladeTrail()
        );

        sendToNearbyPlayers(mob, payload);
    }

    private static void syncCinematicVictimAnimation(
            LivingEntity victim,
            CombatDirection direction,
            String animationName,
            float attackAnimationSpeed
    ) {
        EntityCinematicVictimAnimationPayload payload = new EntityCinematicVictimAnimationPayload(
                victim.getId(),
                direction.ordinal(),
                animationName,
                attackAnimationSpeed,
                false
        );

        sendToNearbyPlayers(victim, payload);
    }

    private static void syncMasterCounterAnimation(
            LivingEntity entity,
            CombatDirection direction,
            String animationName,
            float attackAnimationSpeed
    ) {
        EntityComboAttackAnimationPayload payload = new EntityComboAttackAnimationPayload(
                entity.getId(),
                direction.ordinal(),
                animationName,
                attackAnimationSpeed,
                false
        );

        sendToNearbyPlayers(entity, payload);
    }

    private static void syncIncomingAttackWarning(
            MobEntity attacker,
            LivingEntity target,
            CombatDirection direction
    ) {
        if (!(target instanceof ServerPlayerEntity player)) {
            return;
        }

        if (!isInFrontCone(player, attacker, 120.0) || !isFacingTarget(attacker, player, 120.0)) {
            return;
        }

        ServerPlayNetworking.send(
                player,
                new IncomingAttackWarningPayload(
                        attacker.getId(),
                        direction.ordinal(),
                        isCurrentAttackUnblockable(attacker)
                                ? IncomingAttackWarningPayload.UNBLOCKABLE
                                : (ModGameRules.classicMode(player)
                                || com.kingdomcomecombat.combat.CombatItemUtil.hasDirectionAgnosticBlock(player)
                                ? IncomingAttackWarningPayload.DIRECTION_FREE_BLOCK
                                : IncomingAttackWarningPayload.DIRECTIONAL_BLOCK)
                )
        );
    }

    private static boolean isCurrentAttackUnblockable(MobEntity attacker) {
        ActiveServerAttack attack = ServerCombatState.getAttack(attacker.getUuid());
        return attack != null && attack.comboMove != null;
    }

    private static boolean isInFrontCone(
            LivingEntity viewer,
            LivingEntity subject,
            double coneDegrees
    ) {
        Vec3d toSubject = subject.getPos().subtract(viewer.getPos());
        Vec3d horizontal = new Vec3d(toSubject.x, 0.0, toSubject.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            return true;
        }

        double threshold = Math.cos(Math.toRadians(coneDegrees * 0.5));
        return horizontalForward(viewer.getYaw()).dotProduct(horizontal.normalize()) >= threshold;
    }

    private static boolean isFacingTarget(
            LivingEntity attacker,
            LivingEntity target,
            double coneDegrees
    ) {
        Vec3d toTarget = target.getPos().subtract(attacker.getPos());
        Vec3d horizontal = new Vec3d(toTarget.x, 0.0, toTarget.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            return true;
        }

        double threshold = Math.cos(Math.toRadians(coneDegrees * 0.5));
        return horizontalForward(attacker.getYaw()).dotProduct(horizontal.normalize()) >= threshold;
    }

    private static Vec3d horizontalForward(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vec3d(-Math.sin(rad), 0.0, Math.cos(rad)).normalize();
    }

    private static void sendToNearbyPlayers(MobEntity mob, net.minecraft.network.packet.CustomPayload payload) {
        sendToNearbyPlayers((LivingEntity) mob, payload);
    }

    private static void sendToNearbyPlayers(LivingEntity entity, net.minecraft.network.packet.CustomPayload payload) {
        CombatNetworkBroadcaster.sendTrackingAndSelf(entity, payload);
    }

    private static CombatDirection blockDirectionForAttack(CombatDirection attackDirection) {
        return switch (attackDirection) {
            case RIGHT -> CombatDirection.LEFT;
            case LEFT -> CombatDirection.RIGHT;
            case UP -> CombatDirection.UP;
            case DOWN -> CombatDirection.DOWN;
        };
    }

    private static CombatDirection oppositeDirection(CombatDirection direction) {
        return switch (direction) {
            case LEFT -> CombatDirection.RIGHT;
            case RIGHT -> CombatDirection.LEFT;
            case UP -> CombatDirection.DOWN;
            case DOWN -> CombatDirection.UP;
        };
    }

    private static String masterCounterConfigName(CombatDirection direction) {
        return switch (direction) {
            case LEFT -> "master_counter_left";
            case RIGHT -> "master_counter_right";
            case UP -> "master_counter_up";
            case DOWN -> "master_counter_down";
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

    private static void cleanupDeadStates(net.minecraft.server.MinecraftServer server) {
        Iterator<UUID> iterator = STATES.keySet().iterator();

        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Entity entity = findEntity(server, uuid);

            if (entity == null || entity.isRemoved()) {
                iterator.remove();
                LAST_STANCE_SYNCS.remove(uuid);
                HumanoidCombatAiProfiles.removeSample(uuid);
            }
        }
    }

    static void markActiveCombatMob(MobEntity mob) {
        if (!mob.isRemoved()) {
            ACTIVE_COMBAT_MOBS.add(mob.getUuid());
        }
    }

    private static void separateActiveCombatMobs(net.minecraft.server.MinecraftServer server) {
        double minimumDistance = Math.max(0.1, CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE - 0.2);
        double minimumDistanceSquared = minimumDistance * minimumDistance;
        Map<CombatCell, List<MobEntity>> cells = new HashMap<>();
        for (UUID uuid : ACTIVE_COMBAT_MOBS) {
            Entity resolved = findEntity(server, uuid);
            if (!(resolved instanceof MobEntity mob)) {
                continue;
            }
            if (!mob.isAlive() || mob.isRemoved()) {
                continue;
            }
            ActiveServerAttack attack = ServerCombatState.getAttack(mob.getUuid());
            if (attack != null && attack.comboMove != null) {
                continue;
            }
            CombatCell cell = CombatCell.of(mob, minimumDistance);
            cells.computeIfAbsent(cell, ignored -> new java.util.ArrayList<>()).add(mob);
        }

        for (Map.Entry<CombatCell, List<MobEntity>> entry : cells.entrySet()) {
            CombatCell cell = entry.getKey();
            for (MobEntity mob : entry.getValue()) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        List<MobEntity> neighbors = cells.get(new CombatCell(cell.x() + dx, cell.z() + dz));
                        if (neighbors == null) {
                            continue;
                        }
                        for (MobEntity other : neighbors) {
                            if (mob.isRemoved() || other.isRemoved() || !mob.isAlive() || !other.isAlive()) {
                                continue;
                            }
                            if (mob.getId() >= other.getId() || mob.getWorld() != other.getWorld()) {
                                continue;
                            }
                            double x = mob.getX() - other.getX();
                            double z = mob.getZ() - other.getZ();
                            double distanceSquared = x * x + z * z;
                            if (distanceSquared >= minimumDistanceSquared) {
                                continue;
                            }
                            if (distanceSquared <= 0.000001) {
                                x = (mob.getId() & 1) == 0 ? 1.0 : -1.0;
                                z = 0.0;
                                distanceSquared = 1.0;
                            }
                            double distance = Math.sqrt(distanceSquared);
                            double push = Math.min(0.08, (minimumDistance - distance) * 0.18);
                            double pushX = x / distance * push;
                            double pushZ = z / distance * push;
                            Vec3d mobVelocity = mob.getVelocity();
                            Vec3d otherVelocity = other.getVelocity();
                            mob.setVelocity(mobVelocity.x + pushX, mobVelocity.y, mobVelocity.z + pushZ);
                            other.setVelocity(otherVelocity.x - pushX, otherVelocity.y, otherVelocity.z - pushZ);
                            mob.velocityModified = true;
                            other.velocityModified = true;
                        }
                    }
                }
            }
        }
    }

    private record CombatCell(int x, int z) {
        private static CombatCell of(MobEntity mob, double cellSize) {
            return new CombatCell(
                    MathHelper.floor(mob.getX() / cellSize),
                    MathHelper.floor(mob.getZ() / cellSize)
            );
        }
    }

    private static boolean isLoadedMob(ServerWorld world, MobEntity mob) {
        // The entity comes directly from this world's live iteration. A ticker
        // can remove it, so recheck the cheap flags between dispatches; a UUID
        // world lookup here was needlessly repeated three times for every mob.
        return !mob.isRemoved() && mob.isAlive() && mob.getWorld() == world;
    }

    private static Entity findEntity(net.minecraft.server.MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }

        return null;
    }

    private record StanceSyncSnapshot(
            int targetId,
            CombatDirection direction,
            float animationSpeed,
            String animationName,
            long syncTick
    ) {
    }
}
