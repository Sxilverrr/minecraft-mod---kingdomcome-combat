package com.kingdomcomecombat.ai;

import com.kingdomcomecombat.combat.ActiveServerAttack;
import com.kingdomcomecombat.combat.AttackMoveConfig;
import com.kingdomcomecombat.combat.AttackMoveConfigs;
import com.kingdomcomecombat.combat.CombatAttackTiming;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.ServerCombatControlState;
import com.kingdomcomecombat.combat.ServerCombatState;
import com.kingdomcomecombat.game.ModGameRules;
import com.kingdomcomecombat.network.EntityAttackAnimationPayload;
import com.kingdomcomecombat.stamina.ServerStaminaState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class BeastCombatAiTicker {
    private static final int STATE_CLEANUP_INTERVAL_TICKS = 100;
    private static final Map<UUID, BeastCombatAiState> STATES = new HashMap<>();
    private static long nextStateCleanupTick = 0L;

    private BeastCombatAiTicker() {
    }

    public static void register() {
        // Beast AI is dispatched from HumanoidCombatAiTicker so all mobs are
        // visited once instead of performing a second full-world entity scan.
    }

    static void tickMobFromSharedPass(MobEntity mob) {
        tickMob(mob);
    }

    static void cleanupDeadStatesFromSharedPass(net.minecraft.server.MinecraftServer server) {
        long time = server.getOverworld().getTime();
        if (time >= nextStateCleanupTick) {
            cleanupDeadStates(server);
            nextStateCleanupTick = time + STATE_CLEANUP_INTERVAL_TICKS;
        }
    }

    public static boolean shouldUseVanillaAttack(MobEntity mob) {
        BeastCombatAiProfile profile = BeastCombatAiProfiles.getProfile(mob);
        if (profile == null || !profile.usesCustomAttackMove()) {
            return true;
        }

        LivingEntity target = mob.getTarget();
        return !canEnterCombat(mob, target, profile);
    }

    private static void tickMob(MobEntity mob) {
        BeastCombatAiProfile profile = BeastCombatAiProfiles.getProfile(mob);
        if (profile == null || !mob.isAlive() || mob.isRemoved()) {
            return;
        }

        LivingEntity target = mob.getTarget();
        if (!canEnterCombat(mob, target, profile)) {
            return;
        }

        BeastCombatAiState state = STATES.computeIfAbsent(mob.getUuid(), ignored -> new BeastCombatAiState());
        HumanoidCombatAiTicker.markActiveCombatMob(mob);
        state.resetForMode(profile);
        state.tick();
        ServerStaminaState.setEntityMaxStamina(mob.getUuid(), ServerStaminaState.getMax(mob));

        ActiveServerAttack activeAttack = ServerCombatState.getAttack(mob.getUuid());
        if (activeAttack != null) {
            state.hadActiveAttack = true;
            faceTarget(mob, target);
            if (mob.distanceTo(target) <= profile.attackMinDistance()) {
                stopAtMinimumDistance(mob);
            }
            if (state.phase == BeastCombatAiState.Phase.RUSHING) {
                state.phase = BeastCombatAiState.Phase.PURSUING;
            }
            return;
        }

        if (state.hadActiveAttack) {
            state.hadActiveAttack = false;
            onAttackFinished(mob, target, profile, state);
        }

        faceTarget(mob, target);
        if (profile.mode() == BeastCombatAiProfile.Mode.CIRCLE_LUNGE) {
            tickCircleLunge(mob, target, profile, state);
        } else {
            tickHoldAndRush(mob, target, profile, state);
        }
    }

    private static void tickCircleLunge(
            MobEntity mob,
            LivingEntity target,
            BeastCombatAiProfile profile,
            BeastCombatAiState state
    ) {
        double distance = mob.distanceTo(target);
        if (state.pursuing) {
            if (distance <= profile.attackDistance()) {
                if (tryStartCustomAttack(mob, target, profile, state, true)) {
                    return;
                }
            } else {
                attackMoveToward(mob, target, profile.pursueSpeed(), profile.attackMinDistance());
            }
            if (distance <= profile.attackMinDistance() || (distance <= profile.minDistance() && state.attackCooldownTicks > 0)) {
                holdAtMinimumDistance(
                        mob,
                        target,
                        state.attackCooldownTicks > 0 ? profile.minDistance() : profile.attackMinDistance(),
                        profile.approachSpeed()
                );
            } else if (distance > profile.chaseDistance()) {
                state.pursuing = false;
                beginCircling(mob, profile, state);
            }
            return;
        }

        if (state.phase != BeastCombatAiState.Phase.CIRCLING) {
            beginCircling(mob, profile, state);
        }

        circleTarget(mob, target, profile, state);
        if (state.attackCooldownTicks <= 0 && state.phaseTicks <= 0) {
            state.phase = BeastCombatAiState.Phase.RUSHING;
        } else if (state.attackCooldownTicks > 0 && state.phaseTicks <= 0) {
            beginCircling(mob, profile, state);
        }

        if (state.phase == BeastCombatAiState.Phase.RUSHING) {
            if (distance <= profile.attackDistance()) {
                if (tryStartCustomAttack(mob, target, profile, state, true)) {
                    return;
                }
            } else {
                attackMoveToward(mob, target, profile.lungeSpeed(), profile.attackMinDistance());
            }
        }
    }

    private static void tickHoldAndRush(
            MobEntity mob,
            LivingEntity target,
            BeastCombatAiProfile profile,
            BeastCombatAiState state
    ) {
        double distance = mob.distanceTo(target);
        if (state.phase == BeastCombatAiState.Phase.PURSUING) {
            if (distance <= profile.attackDistance()) {
                if (tryStartCustomAttack(mob, target, profile, state, true)) {
                    return;
                }
            } else {
                attackMoveToward(mob, target, profile.pursueSpeed(), profile.attackMinDistance());
            }
            if (distance <= profile.attackMinDistance() || (distance <= profile.minDistance() && state.attackCooldownTicks > 0)) {
                holdAtMinimumDistance(
                        mob,
                        target,
                        state.attackCooldownTicks > 0 ? profile.minDistance() : profile.attackMinDistance(),
                        profile.approachSpeed()
                );
            } else if (distance > profile.breakOffDistance()) {
                state.phase = profile.alwaysApproachUntilClose()
                        ? BeastCombatAiState.Phase.RUSHING
                        : BeastCombatAiState.Phase.HOLDING;
                state.chainAttacks = 0;
                state.phaseTicks = profile.holdDistanceTicks();
            }
            return;
        }

        if (profile.alwaysApproachUntilClose()) {
            state.phase = BeastCombatAiState.Phase.RUSHING;
        } else if (state.phase != BeastCombatAiState.Phase.HOLDING
                && state.phase != BeastCombatAiState.Phase.RUSHING) {
            state.phase = BeastCombatAiState.Phase.HOLDING;
            state.phaseTicks = profile.holdDistanceTicks();
        }

        if (state.phase == BeastCombatAiState.Phase.HOLDING) {
            holdDistance(mob, target, profile);
            if (state.phaseTicks <= 0) {
                state.phase = BeastCombatAiState.Phase.RUSHING;
            }
            return;
        }

        double speed = profile.canLunge() ? profile.lungeSpeed() : profile.approachSpeed();
        if (distance <= profile.attackDistance()) {
            tryStartCustomAttack(mob, target, profile, state, true);
        } else {
            attackMoveToward(
                    mob,
                    target,
                    speed,
                    state.attackCooldownTicks <= 0 ? profile.attackMinDistance() : profile.minDistance()
            );
        }
    }

    private static void onAttackFinished(
            MobEntity mob,
            LivingEntity target,
            BeastCombatAiProfile profile,
            BeastCombatAiState state
    ) {
        ActiveServerAttack targetAttack = ServerCombatState.getAttack(target.getUuid());
        boolean interrupted = targetAttack != null && targetAttack.hitTargets.contains(mob.getUuid());
        if (interrupted || state.chainAttacks >= profile.maxChainAttacks()) {
            state.pursuing = false;
            state.chainAttacks = 0;
            state.attackCooldownTicks = profile.minAttackIntervalTicks();
            if (profile.mode() == BeastCombatAiProfile.Mode.CIRCLE_LUNGE) {
                beginCircling(mob, profile, state);
            } else {
                state.phase = profile.alwaysApproachUntilClose()
                        ? BeastCombatAiState.Phase.RUSHING
                        : BeastCombatAiState.Phase.HOLDING;
                state.phaseTicks = profile.holdDistanceTicks();
            }
            return;
        }

        state.pursuing = true;
        state.phase = BeastCombatAiState.Phase.PURSUING;
        state.attackCooldownTicks = Math.min(profile.minAttackIntervalTicks(), 6);
    }

    private static void beginCircling(MobEntity mob, BeastCombatAiProfile profile, BeastCombatAiState state) {
        state.phase = BeastCombatAiState.Phase.CIRCLING;
        state.circleDirection = mob.getRandom().nextBoolean() ? 1 : -1;
        state.circleSpeed = randomRange(mob, profile.circleSpeedMin(), profile.circleSpeedMax());
        state.phaseTicks = randomIntRange(mob, profile.circleMinTicks(), profile.circleMaxTicks());
    }

    private static boolean tryStartCustomAttack(
            MobEntity mob,
            LivingEntity target,
            BeastCombatAiProfile profile,
            BeastCombatAiState state,
            boolean countChain
    ) {
        if (state.attackCooldownTicks > 0 || !profile.usesCustomAttackMove()) {
            return false;
        }
        if (!ServerCombatControlState.canAttack(mob)) {
            return false;
        }
        if (!AiAttackCoordinator.canStartAttack(mob, target)) {
            return false;
        }

        AttackMoveConfig moveConfig = resolveMove(profile);
        if (moveConfig == null) {
            return false;
        }

        double staminaCost = moveConfig.staminaCost();
        if (!ServerStaminaState.hasAtLeast(mob, staminaCost)) {
            return false;
        }

        ServerStaminaState.consume(mob, staminaCost);
        mob.getNavigation().stop();
        int rawTotalTicks = CombatAttackTiming.getAttackTotalTicks(CombatDirection.UP, moveConfig);
        if (moveConfig.directHitTick() >= 0) {
            rawTotalTicks = Math.max(rawTotalTicks, moveConfig.directHitTick() + 8);
        }
        double attackAnimationSpeed = profile.attackAnimationSpeed() * ModGameRules.combatSpeed(mob);
        int totalTicks = Math.max(1, (int) Math.ceil(rawTotalTicks / attackAnimationSpeed));
        ServerCombatState.startAttack(
                mob.getUuid(),
                CombatDirection.UP,
                mob.getYaw(),
                target.getId(),
                true,
                totalTicks,
                (float) attackAnimationSpeed,
                null,
                moveConfig,
                mob.getWorld().getTime(),
                false,
                0.0
        );
        AiAttackCoordinator.recordAttackStart(mob, target, rawTotalTicks);
        if (profile.jumpAttack() && mob.isOnGround()) {
            applyJumpAttackVelocity(mob, target, profile);
        }
        syncAttackAnimation(mob, moveConfig, attackAnimationSpeed);
        state.hadActiveAttack = true;
        state.attackCooldownTicks = profile.minAttackIntervalTicks();
        if (countChain) {
            state.chainAttacks++;
        }
        return true;
    }

    private static void applyJumpAttackVelocity(
            MobEntity mob,
            LivingEntity target,
            BeastCombatAiProfile profile
    ) {
        Vec3d toward = target.getPos().subtract(mob.getPos());
        Vec3d horizontal = new Vec3d(toward.x, 0.0, toward.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            return;
        }

        double distance = horizontal.length();
        double forwardSpeed = profile.canLunge()
                ? Math.max(profile.lungeSpeed(), profile.approachSpeed())
                : profile.approachSpeed();
        if (distance <= profile.attackMinDistance()) {
            forwardSpeed = 0.0;
        } else {
            forwardSpeed = Math.min(forwardSpeed, Math.max(0.0, distance - profile.attackMinDistance()) * 0.45);
        }
        Vec3d forward = horizontal.normalize().multiply(forwardSpeed);
        mob.setVelocity(forward.x, profile.jumpVelocity(), forward.z);
        mob.velocityModified = true;
    }

    private static AttackMoveConfig resolveMove(BeastCombatAiProfile profile) {
        if (!profile.attackMoveId().isBlank()) {
            AttackMoveConfig configured = AttackMoveConfigs.getNamed(profile.attackMoveId());
            if (configured != null) {
                return configured;
            }
        }
        if (!profile.attackAnimationName().isBlank()) {
            AttackMoveConfig byAnimation = AttackMoveConfigs.getNamed(profile.attackAnimationName());
            if (byAnimation != null) {
                return byAnimation;
            }
            return new AttackMoveConfig(
                    profile.attackAnimationName(),
                    1.0,
                    0.0,
                    0.0,
                    java.util.List.of(new AttackMoveConfig.HitZoneRule(
                            AttackMoveConfig.HitZone.BODY,
                            java.util.List.of("chest")
                    )),
                    1.0,
                    0.0,
                    false,
                    profile.attackAnimationName(),
                    4,
                    8
            );
        }
        return null;
    }

    private static void syncAttackAnimation(MobEntity mob, AttackMoveConfig moveConfig, double speed) {
        if (!(mob.getWorld() instanceof ServerWorld world) || moveConfig.animationName().isBlank()) {
            return;
        }
        EntityAttackAnimationPayload payload = new EntityAttackAnimationPayload(
                mob.getId(),
                CombatDirection.UP.ordinal(),
                (float) speed,
                0.0F,
                moveConfig.animationName()
        );
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(mob) <= 96.0 * 96.0) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void holdDistance(MobEntity mob, LivingEntity target, BeastCombatAiProfile profile) {
        double distance = mob.distanceTo(target);
        if (distance > profile.circleDistance() + 0.35) {
            moveToward(mob, target, profile.approachSpeed(), profile.minDistance());
        } else if (distance < Math.max(0.2, profile.circleDistance() - 0.35)) {
            moveAway(mob, target, profile.approachSpeed());
        } else {
            mob.getNavigation().stop();
            horizontalVelocity(mob, Vec3d.ZERO);
        }
    }

    private static void circleTarget(
            MobEntity mob,
            LivingEntity target,
            BeastCombatAiProfile profile,
            BeastCombatAiState state
    ) {
        Vec3d toTarget = target.getPos().subtract(mob.getPos());
        Vec3d horizontal = new Vec3d(toTarget.x, 0.0, toTarget.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            return;
        }

        double distance = horizontal.length();
        Vec3d radial = horizontal.normalize();
        Vec3d tangent = new Vec3d(-radial.z, 0.0, radial.x).multiply(state.circleDirection);
        double radialCorrection = Math.max(-0.55, Math.min(0.55, (distance - profile.circleDistance()) * 0.35));
        Vec3d desired = tangent.multiply(state.circleSpeed).add(radial.multiply(radialCorrection));
        horizontalVelocity(mob, desired);
    }

    private static void moveToward(MobEntity mob, LivingEntity target, double speed, double minDistance) {
        if (mob.distanceTo(target) <= minDistance) {
            holdAtMinimumDistance(mob, target, minDistance, 0.18);
            return;
        }
        Vec3d direction = target.getPos().subtract(mob.getPos());
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.lengthSquared() <= 0.000001 || speed <= 0.0) {
            horizontalVelocity(mob, Vec3d.ZERO);
            return;
        }
        horizontalVelocity(mob, horizontal.normalize().multiply(speed));
    }

    private static void attackMoveToward(MobEntity mob, LivingEntity target, double speed, double minDistance) {
        if (mob.distanceTo(target) <= minDistance) {
            stopAtMinimumDistance(mob);
            return;
        }
        Vec3d direction = target.getPos().subtract(mob.getPos());
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.lengthSquared() <= 0.000001 || speed <= 0.0) {
            horizontalVelocity(mob, Vec3d.ZERO);
            return;
        }
        horizontalVelocity(mob, horizontal.normalize().multiply(speed));
    }

    private static void stopAtMinimumDistance(MobEntity mob) {
        mob.getNavigation().stop();
        horizontalVelocity(mob, Vec3d.ZERO);
    }

    private static void holdAtMinimumDistance(
            MobEntity mob,
            LivingEntity target,
            double minDistance,
            double retreatSpeed
    ) {
        double distance = mob.distanceTo(target);
        if (distance < Math.max(0.1, minDistance - 0.1)) {
            moveAway(mob, target, retreatSpeed);
            return;
        }
        mob.getNavigation().stop();
        horizontalVelocity(mob, Vec3d.ZERO);
    }

    private static void moveAway(MobEntity mob, LivingEntity target, double speed) {
        Vec3d direction = mob.getPos().subtract(target.getPos());
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.lengthSquared() <= 0.000001 || speed <= 0.0) {
            horizontalVelocity(mob, Vec3d.ZERO);
            return;
        }
        horizontalVelocity(mob, horizontal.normalize().multiply(speed));
    }

    private static void horizontalVelocity(MobEntity mob, Vec3d horizontal) {
        Vec3d current = mob.getVelocity();
        mob.setVelocity(horizontal.x, current.y, horizontal.z);
        mob.velocityModified = true;
    }

    private static void faceTarget(MobEntity mob, LivingEntity target) {
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        mob.setYaw(yaw);
        mob.setBodyYaw(yaw);
        mob.setHeadYaw(yaw);
    }

    private static boolean canEnterCombat(MobEntity mob, LivingEntity target, BeastCombatAiProfile profile) {
        return target != null
                && target.isAlive()
                && !target.isRemoved()
                && mob.squaredDistanceTo(target) <= profile.combatEnterDistance() * profile.combatEnterDistance();
    }

    private static double randomRange(MobEntity mob, double min, double max) {
        if (max <= min) {
            return min;
        }
        return min + mob.getRandom().nextDouble() * (max - min);
    }

    private static int randomIntRange(MobEntity mob, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + mob.getRandom().nextInt(max - min + 1);
    }

    private static void cleanupDeadStates(net.minecraft.server.MinecraftServer server) {
        Iterator<UUID> iterator = STATES.keySet().iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            boolean alive = false;
            for (ServerWorld world : server.getWorlds()) {
                Entity entity = world.getEntity(uuid);
                if (entity instanceof MobEntity mob && mob.isAlive() && !mob.isRemoved()) {
                    alive = true;
                    break;
                }
            }
            if (!alive) {
                iterator.remove();
            }
        }
    }
}
