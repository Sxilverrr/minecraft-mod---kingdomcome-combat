package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.ai.HumanoidCombatAiTicker;
import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import com.kingdomcomecombat.collision.ServerHitDetectionSystem;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.MobCombatAttributesRegistry;
import com.kingdomcomecombat.stamina.ServerStaminaState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

public class ServerCombatTicker {
    private static final double MOB_ATTACK_TRACKING_TURN_DEGREES = 180.0;
    private static final double PLAYER_ATTACK_TRACKING_TURN_DEGREES = 24.0;
    private static final double LOCKED_ATTACK_STOP_BUFFER = 0.12;
    private static final double MOB_LOCKED_ATTACK_STOP_DISTANCE = 1.72;
    private static final double DIRECT_ACTION_INTERRUPT_VELOCITY = 0.42;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerStaminaState.tick(server);
            ServerBlockState.tick();
            ServerCombatStanceState.tick();
            ServerCombatControlState.tick();

            var entries = new ArrayList<>(ServerCombatState.activeAttacks().entrySet());

            for (var entry : entries) {
                UUID playerUuid = entry.getKey();
                ActiveServerAttack attack = entry.getValue();

                LivingEntity attacker = findLivingEntity(server, playerUuid);

                if (attacker == null || attacker.isRemoved() || !attacker.isAlive()) {
                    ServerCombatState.removeAttack(playerUuid);
                    continue;
                }

                boolean attached = applyAttachedAttackControl(attacker, attack);
                if (!attached && shouldInterruptDirectAction(attacker, attack)) {
                    ServerCombatState.removeAttack(playerUuid);
                    ServerCombatControlState.clearMovementDisable(attacker.getUuid());
                    if (attacker.getWorld() instanceof ServerWorld world) {
                        Entity target = attack.targetEntityId >= 0
                                ? world.getEntityById(attack.targetEntityId)
                                : null;
                        if (target instanceof LivingEntity livingTarget) {
                            ServerCombatControlState.clearMovementDisable(livingTarget.getUuid());
                            ServerHitDetectionSystem.syncAttackInterrupt(world, livingTarget);
                        }
                        ServerHitDetectionSystem.syncAttackInterrupt(world, attacker);
                    }
                    continue;
                }

                if (!attached) {
                    applyAttackMovementControl(attacker, attack);
                }
                if (attacker instanceof MobEntity mob && attack.ageTicks > 0 && attack.ageTicks <= 2) {
                    HumanoidCombatAiTicker.resyncAttackAnimation(mob, attack);
                }
                tryTriggerComboWeaponClash(attacker, attack);
                tryTriggerComboAttackChain(attacker, attack);

                if (attack.comboMove != null && !attack.comboMove.attackChain().isEmpty()) {
                    // Chain-driven combos resolve at configured ticks instead of the normal swing hitbox.
                } else if (attack.moveConfig().directHitTick() >= 0) {
                    tryTriggerDirectConfiguredHit(attacker, attack);
                } else {
                    ServerHitDetectionSystem.detect(attacker, attack);
                }

                attack.tick();

                if (attack.isFinished()) {
                    if ((attack.moveConfig().directHitTick() >= 0 || hasComboAttackChain(attack))
                            && attacker.getWorld() instanceof ServerWorld world) {
                        if (attack.moveConfig().directHitTick() >= 0) {
                            applyDirectActionFinishKnockback(world, attacker, attack);
                            clearMasterCounterVictimBlockDisable(world, attack);
                        }
                        ServerHitDetectionSystem.finishDelayedLethalHit(world, attacker, attack);
                    }
                    if (attack.moveConfig().directHitTick() < 0) {
                        ServerComboState.recordFinishedAttack(
                                playerUuid,
                                attack,
                                attacker.getWorld().getTime()
                        );
                    }
                    ServerCombatState.removeAttack(playerUuid);
                    if (attacker instanceof MobEntity mob) {
                        HumanoidCombatAiTicker.onAttackFinished(mob, attack);
                    }
                }
            }

            for (ServerWorld world : server.getWorlds()) {
                for (Entity entity : world.iterateEntities()) {
                    if (entity instanceof LivingEntity livingEntity
                            && !(entity instanceof net.minecraft.entity.player.PlayerEntity)) {
                        MobCombatAttributesRegistry.applyConfiguredAttributes(livingEntity);
                    }

                    if (entity instanceof LivingEntity livingEntity
                            && ServerCombatControlState.isMovementDisabled(livingEntity.getUuid())) {
                        applyBlockMovementLock(livingEntity);
                        continue;
                    }

                    if (entity instanceof LivingEntity livingEntity
                            && ServerBlockState.isMovementLocked(livingEntity.getUuid())) {
                        applyBlockMovementLock(livingEntity);
                    }

                    if (entity instanceof LivingEntity livingEntity
                            && ServerCombatControlState.isDodging(livingEntity)) {
                        applyDodgeMovement(livingEntity);
                    }

                }
            }
        });
    }

    private static void applyBlockMovementLock(LivingEntity entity) {
        Vec3d currentVelocity = entity.getVelocity();
        entity.setVelocity(0.0, currentVelocity.y, 0.0);
        entity.velocityModified = true;
    }

    private static void applyDodgeMovement(LivingEntity entity) {
        DodgeDirection direction = ServerCombatControlState.getDodgeDirection(entity);
        if (direction == null) {
            return;
        }
        boolean mobForwardStep = direction == DodgeDirection.FORWARD && entity instanceof MobEntity;
        if (mobForwardStep) {
            ((MobEntity) entity).getNavigation().stop();
        }

        Vec3d dodgeVelocity = getDodgeVelocity(
                entity.getYaw(),
                direction,
                ServerCombatControlState.getDodgeAgeTicks(entity)
        );
        if (mobForwardStep) {
            dodgeVelocity = dodgeVelocity.multiply(CombatMovementConfig.LOCKED_ATTACK_LUNGE_MULTIPLIER);
        }
        dodgeVelocity = dodgeVelocity.multiply(
                EquipmentCombatAttributesRegistry.armorMovementSpeedMultiplier(entity)
        );
        if (mobForwardStep) {
            dodgeVelocity = clampMobForwardStepVelocity((MobEntity) entity, dodgeVelocity);
        }
        Vec3d currentVelocity = entity.getVelocity();
        entity.setVelocity(dodgeVelocity.x, currentVelocity.y, dodgeVelocity.z);
        entity.velocityModified = true;
    }

    private static Vec3d clampMobForwardStepVelocity(MobEntity mob, Vec3d velocity) {
        Entity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return velocity;
        }

        double allowedApproach = horizontalDistance(mob, target)
                - CombatMovementConfig.LOCKED_STOP_FORWARD_DISTANCE
                - LOCKED_ATTACK_STOP_BUFFER;
        if (allowedApproach <= 0.0) {
            return Vec3d.ZERO;
        }

        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed <= allowedApproach || horizontalSpeed <= 0.000001) {
            return velocity;
        }

        return velocity.multiply(allowedApproach / horizontalSpeed);
    }

    private static double horizontalDistance(Entity first, Entity second) {
        Vec3d delta = first.getPos().subtract(second.getPos());
        return Math.sqrt(delta.x * delta.x + delta.z * delta.z);
    }

    private static boolean applyAttachedAttackControl(
            LivingEntity attacker,
            ActiveServerAttack attack
    ) {
        double spacing = attachedAttackSpacing(attack);
        if (spacing <= 0.0
                || attack.targetEntityId < 0
                || !(attacker.getWorld() instanceof ServerWorld world)) {
            return false;
        }

        Entity target = world.getEntityById(attack.targetEntityId);
        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            return false;
        }

        alignAttachedPair(attacker, livingTarget, spacing);
        ServerCombatControlState.disableMovement(attacker.getUuid(), 2);
        ServerCombatControlState.disableMovement(livingTarget.getUuid(), 2);
        return true;
    }

    private static double attachedAttackSpacing(ActiveServerAttack attack) {
        if (attack.comboMove != null && attack.comboMove.suctionCombo()) {
            return Math.max(0.3, attack.comboMove.suctionFixedDistance());
        }

        if (isMasterCounterAttack(attack)) {
            return attack.moveConfig().masterCounterSpacing();
        }

        return 0.0;
    }

    private static void alignAttachedPair(
            LivingEntity attacker,
            LivingEntity target,
            double spacing
    ) {
        Vec3d delta = target.getPos().subtract(attacker.getPos());
        Vec3d horizontal = new Vec3d(delta.x, 0.0, delta.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            horizontal = getHorizontalForwardFromYaw(attacker.getYaw());
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

    private static boolean shouldInterruptDirectAction(LivingEntity attacker, ActiveServerAttack attack) {
        if (attack.moveConfig().directHitTick() < 0 || attack.ageTicks <= 1) {
            return false;
        }

        Vec3d velocity = attacker.getVelocity();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        return horizontal > DIRECT_ACTION_INTERRUPT_VELOCITY
                || Math.abs(velocity.y) > DIRECT_ACTION_INTERRUPT_VELOCITY;
    }

    private static void applyAttackMovementControl(LivingEntity attacker, ActiveServerAttack attack) {
        Vec3d currentVelocity = attacker.getVelocity();

        // 攻击结束前 0.2 秒允许恢复移动
        if (attack.canMove()) {
            return;
        }

        ServerWorld world = (ServerWorld) attacker.getWorld();

        boolean hasLockedTarget = attack.targetEntityId >= 0;
        Entity lockedTarget = hasLockedTarget ? world.getEntityById(attack.targetEntityId) : null;

        double movementScale = attack.currentTickScale()
                * EquipmentCombatAttributesRegistry.armorMovementSpeedMultiplier(attacker);
        double speed = attack.getForwardSpeed() * movementScale;
        if (attack.moveConfig().directHitTick() >= 0 && !(attacker instanceof MobEntity)) {
            speed = 0.0;
        }
        if (EquipmentCombatAttributesRegistry.isLargeShield(attacker.getOffHandStack())) {
            speed *= CombatControlConfig.LARGE_SHIELD_ATTACK_LUNGE_MULTIPLIER;
        }

        if (attack.lockedLunge && attacker instanceof MobEntity) {
            speed *= CombatMovementConfig.LOCKED_ATTACK_LUNGE_MULTIPLIER;
        }

        Vec3d forward = getHorizontalForwardFromYaw(attack.startYaw);
        Vec3d lungeDirection = attackLungeDirection(forward, attack);
        boolean inputDirectedLunge = attack.movementKeyLunge
                && (attack.lungeSideInput != 0 || attack.lungeForwardInput == 0);

        if (lockedTarget != null
                && lockedTarget.isAlive()
                && (attack.moveConfig().directHitTick() < 0 || attacker instanceof MobEntity)) {
            boolean targetDodging = lockedTarget instanceof LivingEntity livingTarget
                    && ServerCombatControlState.isDodging(livingTarget);
            float yaw = targetDodging
                    ? attacker.getYaw()
                    : turnYawTowardTarget(attacker, lockedTarget);
            if (!targetDodging) {
                attacker.setYaw(yaw);
                attacker.setHeadYaw(yaw);
                attacker.setBodyYaw(yaw);
            }
            forward = getHorizontalForwardFromYaw(yaw);
            if (!inputDirectedLunge) {
                lungeDirection = attackLungeDirection(forward, attack);
                speed = getLockedAttackLungeSpeed(attacker, lockedTarget, forward, attack, movementScale);
            } else {
                lungeDirection = attackLungeDirection(forward, attack);
                speed = getInputDirectedLockedAttackLungeSpeed(attacker, attack, movementScale);
            }
        }

        Vec3d desiredHorizontalVelocity = lungeDirection.multiply(speed);

        /*
         * 关键：
         * 这里不保留 currentVelocity.x / z。
         * 攻击期间完全覆盖水平速度，避免左右移动动量残留到攻击开始。
         */
        attacker.setVelocity(
                desiredHorizontalVelocity.x,
                currentVelocity.y,
                desiredHorizontalVelocity.z
        );

        attacker.velocityModified = true;
    }

    private static void tryTriggerDirectConfiguredHit(LivingEntity attacker, ActiveServerAttack attack) {
        if (attack.directHitTriggered || attack.ageTicks < attack.moveConfig().directHitTick()) {
            return;
        }

        attack.directHitTriggered = true;
        if (attack.targetEntityId < 0 || !(attacker.getWorld() instanceof ServerWorld world)) {
            return;
        }

        Entity target = world.getEntityById(attack.targetEntityId);
        if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
            ServerHitDetectionSystem.performConfiguredDirectHit(world, attacker, livingTarget, attack);
        }
    }

    private static boolean hasComboAttackChain(ActiveServerAttack attack) {
        return attack.comboMove != null && !attack.comboMove.attackChain().isEmpty();
    }

    private static void clearMasterCounterVictimBlockDisable(ServerWorld world, ActiveServerAttack attack) {
        if (!isMasterCounterAttack(attack) || attack.targetEntityId < 0) {
            return;
        }

        Entity target = world.getEntityById(attack.targetEntityId);
        if (target instanceof LivingEntity livingTarget) {
            ServerCombatControlState.clearBlockDisable(livingTarget.getUuid());
        }
    }

    private static boolean isMasterCounterAttack(ActiveServerAttack attack) {
        String animationName = attack.moveConfig().animationName();
        return animationName != null
                && (animationName.startsWith("master_counter_")
                || animationName.startsWith("master_strike_"));
    }

    private static void tryTriggerComboWeaponClash(LivingEntity attacker, ActiveServerAttack attack) {
        int weaponClashTick = weaponClashTick(attack);
        if (weaponClashTick < 0
                || attack.weaponClashTriggered
                || attack.ageTicks < weaponClashTick
                || attack.targetEntityId < 0
                || !(attacker.getWorld() instanceof ServerWorld world)) {
            return;
        }

        Entity target = world.getEntityById(attack.targetEntityId);
        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            return;
        }

        attack.weaponClashTriggered = true;
        Vec3d position = comboWeaponMiddlePosition(attacker, livingTarget, attack);
        ServerHitDetectionSystem.playWeaponClashEffect(
                world,
                attacker,
                livingTarget,
                position,
                false,
                weaponClashSound(attack)
        );
        ServerHitDetectionSystem.damageHeldWeaponDurability(attacker, 1);
        ServerHitDetectionSystem.damageHeldWeaponDurability(livingTarget, 1);
    }

    private static void tryTriggerComboAttackChain(LivingEntity attacker, ActiveServerAttack attack) {
        if (attack.comboMove == null
                || attack.comboMove.attackChain().isEmpty()
                || attack.targetEntityId < 0
                || !(attacker.getWorld() instanceof ServerWorld world)) {
            return;
        }

        Entity target = world.getEntityById(attack.targetEntityId);
        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            return;
        }

        for (int index = 0; index < attack.comboMove.attackChain().size(); index++) {
            ComboMoveConfig.AttackChainEvent event = attack.comboMove.attackChain().get(index);
            if (attack.triggeredComboChainEvents.contains(index) || attack.ageTicks < event.tick()) {
                continue;
            }

            attack.triggeredComboChainEvents.add(index);
            if (event.weaponHit()) {
                Vec3d position = comboWeaponMiddlePosition(attacker, livingTarget, attack);
                ServerHitDetectionSystem.playWeaponClashEffect(
                        world,
                        attacker,
                        livingTarget,
                        position,
                        false,
                        event.weaponClashSound()
                );
                ServerHitDetectionSystem.damageHeldWeaponDurability(attacker, 1);
                ServerHitDetectionSystem.damageHeldWeaponDurability(livingTarget, 1);
                continue;
            }

            ServerHitDetectionSystem.performComboChainHit(world, attacker, livingTarget, attack, event);
        }
    }

    private static int weaponClashTick(ActiveServerAttack attack) {
        return attack.comboMove != null
                ? attack.comboMove.weaponClashTick()
                : attack.moveConfig().weaponClashTick();
    }

    private static String weaponClashSound(ActiveServerAttack attack) {
        return attack.comboMove != null
                ? attack.comboMove.weaponClashSound()
                : attack.moveConfig().weaponClashSound();
    }

    private static Vec3d comboWeaponMiddlePosition(
            LivingEntity attacker,
            LivingEntity target,
            ActiveServerAttack attack
    ) {
        String animationName = attack.comboMove != null
                ? attack.comboMove.animationName()
                : attack.moveConfig().animationName();
        boolean useRealHitbox = attack.comboMove != null
                ? attack.comboMove.useRealHitbox()
                : attack.moveConfig().useRealHitbox();

        return AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                        animationName,
                        attack.getAnimationElapsedSeconds(),
                        useRealHitbox,
                        EquipmentCombatAttributesRegistry.realHitboxSizeUnits(
                                attacker.getMainHandStack(),
                                AnimatedAttackHitboxLibrary.getRealHitboxSizeUnits()
                        ),
                        EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                                attacker.getMainHandStack(),
                                AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
                        ),
                        EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                                attacker.getMainHandStack(),
                                AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
                        )
                )
                .map(sample -> sample.toWorldBox(attacker).center())
                .orElseGet(() -> attacker.getPos()
                        .add(target.getPos())
                        .multiply(0.5)
                        .add(0.0, attacker.getHeight() * 0.55, 0.0));
    }

    private static void applyDirectActionFinishKnockback(
            ServerWorld world,
            LivingEntity attacker,
            ActiveServerAttack attack
    ) {
        if (attack.targetEntityId < 0) {
            return;
        }

        Entity target = world.getEntityById(attack.targetEntityId);
        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            return;
        }

        Vec3d away = livingTarget.getPos().subtract(attacker.getPos());
        Vec3d horizontalAway = new Vec3d(away.x, 0.0, away.z);
        if (horizontalAway.lengthSquared() <= 0.000001) {
            return;
        }

        Vec3d velocity = livingTarget.getVelocity();
        Vec3d knockback = horizontalAway.normalize().multiply(0.72);
        livingTarget.setVelocity(knockback.x, velocity.y, knockback.z);
        livingTarget.velocityModified = true;
    }

    private static double getLockedAttackLungeSpeed(
            LivingEntity attacker,
            Entity target,
            Vec3d forward,
            ActiveServerAttack attack,
            double movementScale
    ) {
        Vec3d toTarget = target.getPos().subtract(attacker.getPos());
        Vec3d horizontalToTarget = new Vec3d(toTarget.x, 0.0, toTarget.z);

        double distance = horizontalToTarget.length();
        if (distance <= 0.000001) {
            return 0.0;
        }

        Vec3d toTargetDir = horizontalToTarget.normalize();

        /*
         * 只有当前前冲方向确实朝向目标时才限制。
         * 如果玩家因为角度问题不是冲向目标，不额外处理。
         */
        double forwardDot = forward.dotProduct(toTargetDir);

        if (forwardDot <= 0.0) {
            return 0.0;
        }

        double stopDistance = getLockedAttackStopDistance(attacker, target);
        if (distance <= stopDistance) {
            return 0.0;
        }

        double allowedApproach = Math.max(0.0, distance - stopDistance - LOCKED_ATTACK_STOP_BUFFER);
        double lungeDistance = attacker instanceof MobEntity
                ? CombatMovementConfig.MOB_ATTACK_LUNGE_DISTANCE
                : CombatMovementConfig.PLAYER_ATTACK_LUNGE_DISTANCE;
        double maxLungeSpeed = attacker instanceof MobEntity
                ? CombatMovementConfig.MOB_ATTACK_LUNGE_MAX_SPEED
                : CombatMovementConfig.PLAYER_ATTACK_LUNGE_MAX_SPEED;
        if (EquipmentCombatAttributesRegistry.isLargeShield(attacker.getOffHandStack())) {
            lungeDistance *= CombatControlConfig.LARGE_SHIELD_ATTACK_LUNGE_MULTIPLIER;
            maxLungeSpeed *= CombatControlConfig.LARGE_SHIELD_ATTACK_LUNGE_MULTIPLIER;
        }
        if (attack.lockedLunge && attacker instanceof MobEntity) {
            lungeDistance *= CombatMovementConfig.LOCKED_ATTACK_LUNGE_MULTIPLIER;
            maxLungeSpeed *= CombatMovementConfig.LOCKED_ATTACK_LUNGE_MULTIPLIER;
        }
        double maxSafeSpeed = allowedApproach / Math.max(0.001, forwardDot);

        int lungeTicks = CombatTiming.getLockedAttackLungeDurationTicks();
        if (attack.ageTicks < lungeTicks) {
            double fixedLungeSpeed = lungeDistance / lungeTicks;
            return limitMobLungeSpeed(
                    attacker,
                    Math.min(Math.min(fixedLungeSpeed, maxLungeSpeed), maxSafeSpeed)
            );
        }

        int lockedMoveTicks = Math.max(1, CombatTiming.getAttackCanMoveFromTick(attack.attackTotalTicks));
        double baseSpeed = lungeDistance / lockedMoveTicks;
        double progress = Math.max(0.0, Math.min(1.0, attack.ageTicks / (double) lockedMoveTicks));
        double lungeCurve = lockedAttackLungeCurve(progress);
        double trackingFloor = 0.08;
        double desiredSpeed = Math.max(trackingFloor, baseSpeed * lungeCurve) * movementScale;
        double retreatFollowSpeed = getRetreatFollowSpeed(attacker, target, toTargetDir, maxLungeSpeed, movementScale);

        return limitMobLungeSpeed(
                attacker,
                Math.min(Math.max(desiredSpeed, retreatFollowSpeed), Math.min(maxLungeSpeed, maxSafeSpeed))
        );
    }

    private static double getLockedAttackStopDistance(LivingEntity attacker, Entity target) {
        double stopDistance = CombatMovementConfig.LOCKED_STOP_FORWARD_DISTANCE;
        if (!(attacker instanceof MobEntity)) {
            return stopDistance;
        }

        return stopDistance;
    }

    private static double getInputDirectedLockedAttackLungeSpeed(
            LivingEntity attacker,
            ActiveServerAttack attack,
            double movementScale
    ) {
        double lungeDistance = attacker instanceof MobEntity
                ? CombatMovementConfig.MOB_ATTACK_LUNGE_DISTANCE
                : CombatMovementConfig.PLAYER_ATTACK_LUNGE_DISTANCE;
        double maxLungeSpeed = attacker instanceof MobEntity
                ? CombatMovementConfig.MOB_ATTACK_LUNGE_MAX_SPEED
                : CombatMovementConfig.PLAYER_ATTACK_LUNGE_MAX_SPEED;
        if (EquipmentCombatAttributesRegistry.isLargeShield(attacker.getOffHandStack())) {
            lungeDistance *= CombatControlConfig.LARGE_SHIELD_ATTACK_LUNGE_MULTIPLIER;
            maxLungeSpeed *= CombatControlConfig.LARGE_SHIELD_ATTACK_LUNGE_MULTIPLIER;
        }
        if (attack.lockedLunge && attacker instanceof MobEntity) {
            lungeDistance *= CombatMovementConfig.LOCKED_ATTACK_LUNGE_MULTIPLIER;
            maxLungeSpeed *= CombatMovementConfig.LOCKED_ATTACK_LUNGE_MULTIPLIER;
        }

        int lungeTicks = CombatTiming.getLockedAttackLungeDurationTicks();
        if (attack.ageTicks < lungeTicks) {
            double fixedLungeSpeed = lungeDistance / lungeTicks;
            return limitMobLungeSpeed(attacker, Math.min(fixedLungeSpeed, maxLungeSpeed));
        }

        int lockedMoveTicks = Math.max(1, CombatTiming.getAttackCanMoveFromTick(attack.attackTotalTicks));
        double baseSpeed = lungeDistance / lockedMoveTicks;
        double progress = Math.max(0.0, Math.min(1.0, attack.ageTicks / (double) lockedMoveTicks));
        double desiredSpeed = Math.max(0.08, baseSpeed * lockedAttackLungeCurve(progress)) * movementScale;
        return limitMobLungeSpeed(attacker, Math.min(desiredSpeed, maxLungeSpeed));
    }

    private static double limitMobLungeSpeed(LivingEntity attacker, double speed) {
        return speed;
    }

    private static double getRetreatFollowSpeed(
            LivingEntity attacker,
            Entity target,
            Vec3d toTargetDir,
            double maxLungeSpeed,
            double movementScale
    ) {
        double targetRetreatSpeed = new Vec3d(
                target.getVelocity().x,
                0.0,
                target.getVelocity().z
        ).dotProduct(toTargetDir);
        if (targetRetreatSpeed <= 0.0) {
            return 0.0;
        }

        double followBonus = 0.04;
        return Math.min(maxLungeSpeed, (targetRetreatSpeed + followBonus) * movementScale);
    }

    private static Vec3d getForwardToTarget(LivingEntity attacker, Entity target) {
        Vec3d toTarget = target.getPos().subtract(attacker.getPos());
        Vec3d horizontal = new Vec3d(toTarget.x, 0.0, toTarget.z);
        if (horizontal.lengthSquared() <= 0.000001) {
            return getHorizontalForwardFromYaw(attacker.getYaw());
        }

        return horizontal.normalize();
    }

    private static float getYawTowardTarget(LivingEntity attacker, Entity target) {
        Vec3d delta = target.getPos().subtract(attacker.getPos());
        return (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
    }

    private static float turnYawTowardTarget(LivingEntity attacker, Entity target) {
        float currentYaw = attacker.getYaw();
        float targetYaw = getYawTowardTarget(attacker, target);
        double maxTurn = attacker instanceof MobEntity
                ? MOB_ATTACK_TRACKING_TURN_DEGREES
                : PLAYER_ATTACK_TRACKING_TURN_DEGREES;
        double delta = wrapDegrees(targetYaw - currentYaw);
        double clamped = Math.max(-maxTurn, Math.min(maxTurn, delta));
        return currentYaw + (float) clamped;
    }

    private static double lockedAttackLungeCurve(double progress) {
        progress = Math.max(0.0, Math.min(1.0, progress));
        if (progress < 0.22) {
            return lerp(1.80, 1.35, progress / 0.22);
        }

        if (progress < 0.68) {
            return lerp(1.35, 0.58, (progress - 0.22) / 0.46);
        }

        return lerp(0.58, 0.28, (progress - 0.68) / 0.32);
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * Math.max(0.0, Math.min(1.0, progress));
    }

    private static double wrapDegrees(double degrees) {
        degrees %= 360.0;
        if (degrees >= 180.0) {
            degrees -= 360.0;
        }
        if (degrees < -180.0) {
            degrees += 360.0;
        }
        return degrees;
    }

    private static Vec3d attackLungeDirection(Vec3d forward, ActiveServerAttack attack) {
        if (!attack.movementKeyLunge) {
            return forward;
        }

        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x).normalize();
        if (attack.lungeSideInput != 0 && attack.lungeForwardInput > 0) {
            return forward.multiply(0.50)
                    .add(right.multiply(attack.lungeSideInput * 0.30));
        }

        if (attack.lungeSideInput != 0) {
            return forward.multiply(0.20)
                    .add(right.multiply(attack.lungeSideInput * 0.30));
        }

        if (attack.lungeForwardInput > 0) {
            return forward;
        }

        return Vec3d.ZERO;
    }

    private static Vec3d getDodgeVelocity(float yaw, DodgeDirection direction, int ageTicks) {
        Vec3d forward = getHorizontalForwardFromYaw(yaw);
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x).normalize();

        Vec3d base = switch (direction) {
            case FORWARD -> forward.multiply(CombatControlConfig.FORWARD_STEP_SPEED);
            case BACK -> forward.multiply(-CombatControlConfig.DODGE_BACK_SPEED);
            case LEFT -> right.multiply(-CombatControlConfig.DODGE_SIDE_SPEED);
            case RIGHT -> right.multiply(CombatControlConfig.DODGE_SIDE_SPEED);
        };
        return base.multiply(CombatControlConfig.getDodgeSpeedScale(ageTicks));
    }

    private static Vec3d getHorizontalForwardFromYaw(float yaw) {
        double rad = Math.toRadians(yaw);

        return new Vec3d(
                -Math.sin(rad),
                0.0,
                Math.cos(rad)
        ).normalize();
    }

    private static LivingEntity findLivingEntity(net.minecraft.server.MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }

        return null;
    }


}
