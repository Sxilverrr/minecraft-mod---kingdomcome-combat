package com.kingdomcomecombat.client.input;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.feedback.CombatHitFeedbackClient;
import com.kingdomcomecombat.client.lockon.LockOnCameraController;
import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.client.lockon.LockOnTargetSelector;
import com.kingdomcomecombat.client.lockon.LockOnTargetSwitcher;
import com.kingdomcomecombat.client.stamina.ClientStaminaState;
import com.kingdomcomecombat.client.passive.ClientPassiveSkillUnlockState;
import com.kingdomcomecombat.client.ui.ComboKnowledgeScreen;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.combat.AttackMoveConfigs;
import com.kingdomcomecombat.combat.CombatControlConfig;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.BeowulfArmState;
import com.kingdomcomecombat.combat.CombatAttackTiming;
import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.combat.CombatMovementConfig;
import com.kingdomcomecombat.combat.CombatTiming;
import com.kingdomcomecombat.combat.CombatWeaponUtil;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.DodgeDirection;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.injury.ModStatusEffects;
import com.kingdomcomecombat.network.HorseControlPayload;
import com.kingdomcomecombat.riding.KccHorseRidingData;
import com.kingdomcomecombat.network.StartAttackPayload;
import com.kingdomcomecombat.network.StartBlockPayload;
import com.kingdomcomecombat.network.StartDodgePayload;
import com.kingdomcomecombat.network.UpdateCombatStancePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

public class CombatInputClient {
    private static boolean lastLeftPressed = false;
    private static boolean lastRightPressed = false;
    private static int leftPressTicks = 0;
    private static boolean leftPressStartedOnCombatItem = false;
    private static CombatDirection leftPressDirection = CombatDirection.RIGHT;
    private static int localAttackDisabledTicks = 0;
    private static int localBlockDisabledTicks = 0;
    private static int localDodgeTicks = 0;
    private static DodgeDirection localDodgeDirection = DodgeDirection.BACK;
    private static boolean lastLockPressed = false;
    private static boolean lastSoftLockPressed = false;
    private static boolean lastDodgeKeyPressed = false;
    private static boolean lastLockedSneakDodgePressed = false;
    private static boolean lockedSneakDodgePressedThisTick = false;
    private static int lockPressTicks = 0;
    private static boolean temporaryUnlockActive = false;
    private static boolean lastSprintTemporaryUnlockPressed = false;
    private static int sprintTemporaryUnlockPressTicks = 0;
    private static boolean sprintTemporaryUnlockActive = false;
    private static boolean lastSyncedLocked = false;
    private static CombatDirection lastSyncedDirection = CombatDirection.RIGHT;
    private static boolean lastHorseSprintPressed = false;
    private static boolean lastHorseJumpPressed = false;
    private static int horseSprintPulseTicks = 0;
    private static final int TEMPORARY_UNLOCK_HOLD_TICKS = 6;

    /**
     * 小于等于这个 tick 数，算短按。
     * 20 tick = 1 秒，所以 5 tick 约等于 0.25 秒。
     */
    private static final int SHORT_ATTACK_MAX_TICKS = 5;

    private static double lastMouseX = 0.0;
    private static double lastMouseY = 0.0;
    private static boolean mouseInitialized = false;
    private static final long TARGET_SWITCH_WINDOW_NANOS = 1_000_000_000L;
    private static final ArrayDeque<MouseTurnSample> targetSwitchSamples = new ArrayDeque<>();
    private static double targetSwitchTurnDegrees = 0.0;

    private static final MouseGestureBuffer gestureBuffer = new MouseGestureBuffer();

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(CombatInputClient::onClientTickStart);
        ClientTickEvents.END_CLIENT_TICK.register(CombatInputClient::onClientTick);
    }

    public static void disableAttackLocally(int ticks) {
        localAttackDisabledTicks = Math.max(localAttackDisabledTicks, Math.max(0, ticks));
    }

    public static void clearBufferedAttackPlan() {
    }

    private static void onClientTickStart(MinecraftClient client) {
        applyLockedInputRestrictionsEarly(client);
    }
    private static void applyLockedInputRestrictionsEarly(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (CombatClientState.attacking || localDodgeTicks > 0) {
            client.options.jumpKey.setPressed(false);
        }

        boolean sprintPressed = client.options.sprintKey.isPressed();
        handleSprintTemporaryUnlock(client, sprintPressed);

        if (!LockOnState.locked) {
            lastLockedSneakDodgePressed = false;
            lockedSneakDodgePressedThisTick = false;
            return;
        }

        boolean sneakPressed = client.options.sneakKey.isPressed();
        lockedSneakDodgePressedThisTick = sneakPressed && !lastLockedSneakDodgePressed;
        lastLockedSneakDodgePressed = sneakPressed;

        // 没按疾跑时仍清掉锁定残留疾跑；按住疾跑时允许真实疾跑并触发临时解锁。
        if (!sprintPressed) {
            client.options.sprintKey.setPressed(false);
            client.player.setSprinting(false);
        }

        // 锁定状态禁止潜行：必须在 START_CLIENT_TICK 做，避免先蹲一下再取消
        client.options.sneakKey.setPressed(false);
        client.player.setSneaking(false);

        // 过近时阻止继续按 W 靠近目标
        if (shouldBlockForwardInputToLockedTarget(client)) {
            client.options.forwardKey.setPressed(false);

            Vec3d velocity = client.player.getVelocity();
            Vec3d fixedVelocity = removeVelocityTowardsLockedTarget(client, velocity);
            client.player.setVelocity(fixedVelocity);
        }
    }
    private static Vec3d getPlayerHorizontalForward(float yaw) {
        double rad = Math.toRadians(yaw);

        return new Vec3d(
                -Math.sin(rad),
                0.0,
                Math.cos(rad)
        ).normalize();
    }

    private static void handleSprintTemporaryUnlock(MinecraftClient client, boolean sprintPressed) {
        boolean justPressed = sprintPressed && !lastSprintTemporaryUnlockPressed;
        boolean justReleased = !sprintPressed && lastSprintTemporaryUnlockPressed;
        lastSprintTemporaryUnlockPressed = sprintPressed;

        if (justPressed) {
            sprintTemporaryUnlockPressTicks = 0;
            sprintTemporaryUnlockActive = false;
        }

        if (sprintPressed) {
            sprintTemporaryUnlockPressTicks++;
            if (LockOnState.locked
                    && !sprintTemporaryUnlockActive
                    && sprintTemporaryUnlockPressTicks >= TEMPORARY_UNLOCK_HOLD_TICKS) {
                LockOnState.clear();
                LockOnCameraController.clearShoulderCameraOffset();
                syncCombatState();
                sprintTemporaryUnlockActive = true;
            }
            return;
        }

        if (!justReleased) {
            return;
        }

        if (sprintTemporaryUnlockActive) {
            relockBestTarget(client);
        }

        sprintTemporaryUnlockActive = false;
        sprintTemporaryUnlockPressTicks = 0;
    }

    private static boolean shouldBlockForwardInputToLockedTarget(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return false;
        }

        if (!LockOnState.locked || LockOnState.targetEntityId < 0) {
            return false;
        }

        Entity target = client.world.getEntityById(LockOnState.targetEntityId);

        if (target == null || !target.isAlive()) {
            return false;
        }

        Vec3d toTarget = target.getPos().subtract(client.player.getPos());
        Vec3d horizontalToTarget = new Vec3d(toTarget.x, 0.0, toTarget.z);

        double distance = horizontalToTarget.length();

        if (distance > CombatMovementConfig.LOCKED_STOP_FORWARD_DISTANCE) {
            return false;
        }

        if (!client.options.forwardKey.isPressed()) {
            return false;
        }

        Vec3d toTargetDir = horizontalToTarget.normalize();
        Vec3d playerForward = getPlayerHorizontalForward(client.player.getYaw());

        return playerForward.dotProduct(toTargetDir)
                > CombatMovementConfig.LOCKED_FORWARD_BLOCK_DOT;
    }
    private static Vec3d removeVelocityTowardsLockedTarget(
            MinecraftClient client,
            Vec3d velocity
    ) {
        if (client.player == null || client.world == null) {
            return velocity;
        }

        if (!LockOnState.locked || LockOnState.targetEntityId < 0) {
            return velocity;
        }

        Entity target = client.world.getEntityById(LockOnState.targetEntityId);

        if (target == null || !target.isAlive()) {
            return velocity;
        }

        Vec3d toTarget = target.getPos().subtract(client.player.getPos());
        Vec3d horizontalToTarget = new Vec3d(toTarget.x, 0.0, toTarget.z);

        if (horizontalToTarget.lengthSquared() <= 0.000001) {
            return velocity;
        }

        if (horizontalToTarget.length() > CombatMovementConfig.LOCKED_STOP_FORWARD_DISTANCE) {
            return velocity;
        }

        Vec3d toTargetDir = horizontalToTarget.normalize();

        Vec3d horizontalVelocity = new Vec3d(
                velocity.x,
                0.0,
                velocity.z
        );

        double towardSpeed = horizontalVelocity.dotProduct(toTargetDir);

        if (towardSpeed <= 0.0) {
            return velocity;
        }

        Vec3d fixedHorizontal = horizontalVelocity.subtract(
                toTargetDir.multiply(towardSpeed)
        );

        return new Vec3d(
                fixedHorizontal.x,
                velocity.y,
                fixedHorizontal.z
        );
    }
    private static void beginPostAttackWindow(MinecraftClient client) {
        boolean alreadyBlendingToRecovery = CombatClientState.hasStartedRecoveryBlend();

        CombatClientState.startPostAttackWindow();

        if (CombatClientState.hasBufferedAttack()) {
            if (localAttackDisabledTicks <= 0) {
                CombatAnimationClient.playAttackTransitionPreview(
                        CombatClientState.getBufferedAttackDirection()
                );
            }
            return;
        }

        if (alreadyBlendingToRecovery) {
            return;
        }

        if (LockOnState.locked) {
            CombatAnimationClient.blendToStanceAfterAttack(
                    CombatClientState.currentDirection
            );
        } else {
            CombatAnimationClient.blendToNeutralAfterAttack();
        }
    }
    private static void tryStartBufferedAttackAfterWindow(MinecraftClient client) {
        if (localAttackDisabledTicks > 0) {
            return;
        }

        CombatDirection bufferedDirection = CombatClientState.consumeBufferedAttack();

        if (bufferedDirection != null) {
            startCustomAttack(client, bufferedDirection, true);
            return;
        }

        if (LockOnState.locked) {
            CombatAnimationClient.blendToStanceAfterAttack(
                    CombatClientState.currentDirection
            );
        } else {
            CombatAnimationClient.blendToNeutralAfterAttack();
        }
    }

    private static void tryStartBufferedAttackAfterDisable(MinecraftClient client) {
        if (localAttackDisabledTicks > 0
                || CombatClientState.attacking
                || CombatClientState.isInPostAttackWindow()) {
            return;
        }

        CombatDirection bufferedDirection = CombatClientState.consumeBufferedAttack();
        if (bufferedDirection == null) {
            return;
        }

        startCustomAttack(client, bufferedDirection, true);
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        syncHorseControl(client);
        handleLockOnKey(client);
        handleSoftLockKey(client);
        handleKnowledgeScreen(client);
        handleMouseGesture(client);
        tickLocalControlTimers();
        CombatClientState.tickPerfectCounterWindow();
        tryStartBufferedAttackAfterDisable(client);
        handleShiftDodge(client);
        handleRightClickBlock(client);
        handleLeftClickAttack(client);

        boolean attackFinished = CombatClientState.tickAttack();

        if (!attackFinished && CombatClientState.shouldReleaseBufferedAttack()) {
            tryStartBufferedAttackAfterWindow(client);
        } else if (CombatClientState.shouldStartRecoveryBlend()) {
            beginAttackRecoveryBlend();
        }

        if (attackFinished) {
            beginPostAttackWindow(client);
        }

        if (!attackFinished) {
            boolean postWindowFinished = CombatClientState.tickPostAttackWindow();

            if (postWindowFinished) {
                tryStartBufferedAttackAfterWindow(client);
            }
        }

        LockOnCameraController.tickLogic(client);
        syncCombatStateIfChanged();

        applyLockedMovementLimit(client);
        updateAttackAnimationSpeed();

        CombatHitFeedbackClient.tick(client);
    }

    private static void handleKnowledgeScreen(MinecraftClient client) {
        while (CombatKeyBindings.SKILL_SCREEN_KEY.wasPressed()) {
            client.setScreen(new ComboKnowledgeScreen());
        }
    }

    private static void handleRightClickBlock(MinecraftClient client) {
        boolean rightPressed = client.options.useKey.isPressed();
        boolean justPressed = rightPressed && !lastRightPressed;
        lastRightPressed = rightPressed;

        if (!justPressed
                || client.currentScreen != null
                || localBlockDisabledTicks > 0
                || CombatClientState.isInAttackStartupNoDefenseWindow()) {
            return;
        }

        if (!CombatItemUtil.canUseCustomCombat(client.player)) {
            return;
        }

        localBlockDisabledTicks = Math.max(
                localBlockDisabledTicks,
                CombatControlConfig.BLOCK_INPUT_COOLDOWN_TICKS
        );
        ClientPlayNetworking.send(new StartBlockPayload(CombatClientState.currentDirection.ordinal()));
    }

    private static void handleShiftDodge(MinecraftClient client) {
        boolean dodgeKeyPressed = CombatKeyBindings.DODGE_KEY.isPressed();
        boolean dodgeKeyJustPressed = dodgeKeyPressed && !lastDodgeKeyPressed;
        lastDodgeKeyPressed = dodgeKeyPressed;

        boolean shouldDodge = lockedSneakDodgePressedThisTick || dodgeKeyJustPressed;
        lockedSneakDodgePressedThisTick = false;

        if (!shouldDodge || client.currentScreen != null) {
            return;
        }

        if (!LockOnState.locked || !CombatItemUtil.canUseCustomCombat(client.player)) {
            return;
        }

        if (!ClientPassiveSkillUnlockState.isUnlocked("dodge")) {
            return;
        }

        if (!client.player.isOnGround() && !client.player.isTouchingWater()) {
            return;
        }

        if (CombatClientState.attacking || CombatClientState.isInPostAttackWindow()) {
            return;
        }

        if (ModStatusEffects.effectiveLevel(client.player, ModStatusEffects.LEG_INJURY) > 0) {
            return;
        }

        DodgeDirection direction = getDodgeDirection(client);
        double staminaCost = direction == DodgeDirection.FORWARD
                ? CombatControlConfig.FORWARD_STEP_STAMINA_COST
                : CombatControlConfig.DODGE_STAMINA_COST;
        if (hasLockedLargeShield(client)) {
            staminaCost *= CombatControlConfig.LARGE_SHIELD_DODGE_STAMINA_COST_MULTIPLIER;
        }
        if (ClientStaminaState.current() < staminaCost) {
            return;
        }

        applyLocalDodgeVelocity(client, direction);
        CombatHitFeedbackClient.startDodgeFeedback(direction);
        localAttackDisabledTicks = Math.max(
                localAttackDisabledTicks,
                CombatControlConfig.DODGE_ATTACK_DISABLE_TICKS
        );
        localBlockDisabledTicks = Math.max(
                localBlockDisabledTicks,
                CombatControlConfig.DODGE_BLOCK_DISABLE_TICKS
        );
        localDodgeTicks = CombatControlConfig.DODGE_TOTAL_TICKS;
        localDodgeDirection = direction;

        ClientPlayNetworking.send(new StartDodgePayload(direction.ordinal()));
    }

    private static void beginAttackRecoveryBlend() {
        CombatClientState.markRecoveryBlendStarted();

        if (LockOnState.locked) {
            CombatAnimationClient.blendToStanceAfterAttack(
                    CombatClientState.currentDirection
            );
        } else {
            CombatAnimationClient.blendToNeutralAfterAttack();
        }
    }

    /**
     * 真正发动攻击。
     *
     * 只有这里才：
     * 1. 开始攻击计时
     * 2. 播放正式攻击动画
     * 3. 发送 StartAttackPayload
     * 4. 根据攻击方向改变下一架势
     */
    private static void startCustomAttack(
            MinecraftClient client,
            CombatDirection attackDirection,
            boolean chained
    ) {
        if (localAttackDisabledTicks > 0) {
            if (CombatClientState.bufferAttackDuringDisabled(attackDirection)) {
                debugMessage(client, "禁攻预输入：" + attackDirection.name());
            }
            return;
        }

        if (CombatItemUtil.shouldUseVanillaEntityAttack(client.player)) {
            return;
        }

        if (!client.player.isOnGround() && !client.player.isTouchingWater()) {
            return;
        }

        var moveConfig = CombatWeaponUtil.resolveAttackMove(client.player, attackDirection);
        double staminaCost = moveConfig.staminaCost();
        if (hasLockedLargeShield(client)) {
            staminaCost *= CombatControlConfig.LARGE_SHIELD_ATTACK_STAMINA_COST_MULTIPLIER;
        }
        if (ClientStaminaState.current() < staminaCost) {
            return;
        }

        double attackAnimationSpeed = 1.0;
        attackAnimationSpeed *= EquipmentCombatAttributesRegistry.weaponAttackSpeedMultiplier(
                client.player
        );
        attackAnimationSpeed *= EquipmentCombatAttributesRegistry.armorAttackSpeedMultiplier(
                client.player
        );
        attackAnimationSpeed *= BeowulfArmState.attackSpeedMultiplier(client.player);
        if (hasLargeShield(client)) {
            attackAnimationSpeed *= CombatControlConfig.LARGE_SHIELD_ATTACK_SPEED_MULTIPLIER;
        }
        int attackTotalTicks = CombatAttackTiming.getAttackTotalTicks(attackDirection, moveConfig);
        attackTotalTicks = Math.max(1, (int) Math.ceil(attackTotalTicks / attackAnimationSpeed));
        int transitionTicks = moveConfig.transitionTicks();

        boolean perfectCounterSlow = CombatClientState.consumePerfectCounterAttackSlow();
        CombatClientState.startAttack(
                attackDirection,
                attackTotalTicks,
                perfectCounterSlow,
                attackAnimationSpeed,
                transitionTicks
        );
        if (!moveConfig.animationName().isBlank()) {
            CombatClientState.setCurrentAttackAnimationName(moveConfig.animationName());
        }
        CombatClientState.recordComboInput(attackDirection);
        clearHorizontalSideMomentum(client);
        debugMessage(
                client,
                chained
                        ? "预输入攻击：" + attackDirection.name()
                        : "短按攻击：" + attackDirection.name()
        );

        if (!moveConfig.animationName().isBlank()) {
            CombatAnimationClient.playComboAttack(
                    moveConfig.animationName(),
                    attackDirection,
                    chained
            );
        } else {
            CombatAnimationClient.playAttack(attackDirection, chained);
        }
        CombatAnimationClient.setCombatAnimationSpeed((float) attackAnimationSpeed);

        int targetEntityId = LockOnState.isHardLocked() ? LockOnState.targetEntityId : -1;
        boolean lockedLunge = LockOnState.locked;
        AttackLungeInput lungeInput = attackLungeInput(client);

        ClientPlayNetworking.send(
                new StartAttackPayload(
                        attackDirection.ordinal(),
                        targetEntityId,
                        lockedLunge,
                        lungeInput.movementKeyPressed(),
                        lungeInput.forward(),
                        lungeInput.side(),
                        CombatClientState.attackInstanceId
                )
        );

        CombatClientState.applyDirectionAfterSuccessfulAttack(attackDirection);
    }

    private static AttackLungeInput attackLungeInput(MinecraftClient client) {
        boolean forward = client.options.forwardKey.isPressed();
        boolean back = client.options.backKey.isPressed();
        boolean left = client.options.leftKey.isPressed();
        boolean right = client.options.rightKey.isPressed();
        boolean movementKeyPressed = forward || back || left || right;
        int forwardInput = forward ? 1 : 0;
        int sideInput = (right ? 1 : 0) - (left ? 1 : 0);
        return new AttackLungeInput(movementKeyPressed, forwardInput, sideInput);
    }

    private static void handleLockOnKey(MinecraftClient client) {
        boolean pressed = CombatKeyBindings.LOCK_ON_KEY.isPressed();
        boolean justPressed = pressed && !lastLockPressed;
        boolean justReleased = !pressed && lastLockPressed;
        lastLockPressed = pressed;

        if (justPressed) {
            lockPressTicks = 0;
            temporaryUnlockActive = false;
        }

        if (pressed) {
            lockPressTicks++;
            if (LockOnState.locked
                    && !temporaryUnlockActive
                    && lockPressTicks >= TEMPORARY_UNLOCK_HOLD_TICKS) {
                LockOnState.clear();
                LockOnCameraController.clearShoulderCameraOffset();
                syncCombatState();
                temporaryUnlockActive = true;
            }
            return;
        }

        if (!justReleased) {
            return;
        }

        if (temporaryUnlockActive) {
            relockBestTarget(client);
            temporaryUnlockActive = false;
            lockPressTicks = 0;
            return;
        }

        if (lockPressTicks < TEMPORARY_UNLOCK_HOLD_TICKS && LockOnState.isSoftLocked()) {
            lockBestTarget(client);
        } else if (lockPressTicks < TEMPORARY_UNLOCK_HOLD_TICKS && LockOnState.locked) {
            LockOnState.clear();
            LockOnCameraController.clearShoulderCameraOffset();
            syncCombatState();
        } else if (lockPressTicks < TEMPORARY_UNLOCK_HOLD_TICKS) {
            lockBestTarget(client);
        }

        lockPressTicks = 0;
    }

    private static void handleSoftLockKey(MinecraftClient client) {
        boolean pressed = CombatKeyBindings.SOFT_LOCK_KEY.isPressed();
        boolean justPressed = pressed && !lastSoftLockPressed;
        lastSoftLockPressed = pressed;

        if (!justPressed) {
            return;
        }

        if (LockOnState.isSoftLocked()) {
            LockOnState.clear();
            LockOnCameraController.clearShoulderCameraOffset();
            syncCombatState();
            return;
        }

        LockOnState.softLock();
        syncCombatState();
    }

    private static boolean lockBestTarget(MinecraftClient client) {
        return lockBestTarget(client, false);
    }

    private static boolean lockBestTarget(MinecraftClient client, boolean softLock) {
        var target = LockOnTargetSelector.findBestTarget(client);

        if (target != null) {
            if (softLock) {
                LockOnState.softLock(target.getUuid(), target.getId());
            } else {
                LockOnState.lock(target.getUuid(), target.getId());
            }

            syncCombatState();
            return true;
        }
        return false;
    }

    public static void autoLockHitTarget(LivingEntity target) {
        if (target == null || LockOnState.locked || !CombatClientConfig.autoLockOnHit()) {
            return;
        }
        LockOnState.lock(target.getUuid(), target.getId());
        syncCombatState();
    }

    private static void relockBestTarget(MinecraftClient client) {
        if (lockBestTarget(client)) {
            return;
        }

    }

    private static void handleMouseGesture(MinecraftClient client) {
        if (!LockOnState.locked) {
            CombatClientState.setDirection(CombatDirection.RIGHT);
            CombatAnimationClient.clearCurrentAnimation();
            updateMousePositionOnly(client);
            return;
        }

        double mouseX = client.mouse.getX();
        double mouseY = client.mouse.getY();

        if (!mouseInitialized) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            mouseInitialized = true;
            return;
        }

        double dx = mouseX - lastMouseX;
        double dy = mouseY - lastMouseY;

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        gestureBuffer.tick(dx, dy);

        CombatDirection direction = gestureBuffer.consumeDirectionIfReady();

        if (direction != null) {
            CombatClientState.setDirection(direction);
            syncCombatState();

            debugMessage(client, "架势：" + direction.name());

            /*
             * 攻击期间允许改变 currentDirection，
             * 但不要打断当前攻击动画。
             *
             * 如果玩家攻击中短按攻击，
             * 预输入会记录当前 direction。
             */
            if (!CombatClientState.isBusyWithAttackOrPostWindow()) {
                CombatAnimationClient.playStance(direction);
            }
        }
    }

    public static void acceptRawMouseDelta(double mouseDx, double mouseDy) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.currentScreen != null) {
            clearTargetSwitchGesture();
            return;
        }
        handleTargetSwitchGesture(client, mouseDx);
    }

    private static void handleTargetSwitchGesture(MinecraftClient client, double mouseDx) {
        if (!LockOnState.isHardLocked() || LockOnState.delayedClearPending()) {
            clearTargetSwitchGesture();
            return;
        }
        if (CombatClientState.isBusyWithAttackOrPostWindow()) {
            clearTargetSwitchGesture();
            return;
        }

        long now = System.nanoTime();
        // Match vanilla's mouse sensitivity curve and convert its look input to degrees.
        double sensitivity = client.options.getMouseSensitivity().getValue();
        double scaled = sensitivity * 0.6 + 0.2;
        // updateMouse multiplies the sensitivity cube by 8 before passing it
        // into the player's 0.15-degree look conversion.
        double degrees = mouseDx * scaled * scaled * scaled * 8.0 * 0.15;
        if (Math.abs(degrees) > 0.00001) {
            targetSwitchSamples.addLast(new MouseTurnSample(now, degrees));
            targetSwitchTurnDegrees += degrees;
        }
        while (!targetSwitchSamples.isEmpty()
                && now - targetSwitchSamples.peekFirst().timeNanos() > TARGET_SWITCH_WINDOW_NANOS) {
            targetSwitchTurnDegrees -= targetSwitchSamples.removeFirst().degrees();
        }

        double threshold = CombatClientConfig.lockOnSwitchAngleDegrees();
        if (Math.abs(targetSwitchTurnDegrees) < threshold) {
            return;
        }

        Entity current = client.world == null ? null : client.world.getEntityById(LockOnState.targetEntityId);
        if (current instanceof LivingEntity currentTarget) {
            LockOnTargetSwitcher.SwitchSide side = targetSwitchTurnDegrees > 0.0
                    ? LockOnTargetSwitcher.SwitchSide.RIGHT
                    : LockOnTargetSwitcher.SwitchSide.LEFT;
            LivingEntity next = LockOnTargetSwitcher.findSwitchTarget(client, currentTarget, side);
            if (next != null) {
                LockOnState.lock(next.getUuid(), next.getId());
                syncCombatState();
            }
        }
        // Require a fresh one-second gesture before another switch attempt.
        clearTargetSwitchGesture();
    }

    private static void clearTargetSwitchGesture() {
        targetSwitchSamples.clear();
        targetSwitchTurnDegrees = 0.0;
    }

    private record MouseTurnSample(long timeNanos, double degrees) {
    }
    private static void updateMousePositionOnly(MinecraftClient client) {
        lastMouseX = client.mouse.getX();
        lastMouseY = client.mouse.getY();
        mouseInitialized = true;
    }

    private static void syncCombatState() {
        ClientPlayNetworking.send(new UpdateCombatStancePayload(
                CombatClientState.currentDirection.ordinal(),
                LockOnState.locked
        ));
        lastSyncedDirection = CombatClientState.currentDirection;
        lastSyncedLocked = LockOnState.locked;
    }

    private static void syncCombatStateIfChanged() {
        if (lastSyncedLocked != LockOnState.locked
                || lastSyncedDirection != CombatClientState.currentDirection) {
            syncCombatState();
        }
    }

    private static void applyLockedMovementLimit(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (shouldHardLockLocalMovement()) {
            clearHorizontalVelocity(client);
            return;
        }

        if (!LockOnState.locked && !hasLargeShield(client)) {
            return;
        }

        if (LockOnState.locked) {
            if (!client.options.sprintKey.isPressed()) {
                client.options.sprintKey.setPressed(false);
                client.player.setSprinting(false);
            }

            client.options.sneakKey.setPressed(false);
            client.player.setSneaking(false);
        }

        if (localDodgeTicks > 0) {
            applyLocalDodgeVelocity(client, localDodgeDirection);
            return;
        }

        if (CombatClientState.attacking) {
            Vec3d velocity = client.player.getVelocity();
            client.player.setVelocity(removeVelocityTowardsLockedTarget(client, velocity));
            return;
        }

        Vec3d velocity = client.player.getVelocity();

        /*
         * 注意：
         * 这里不能太低，否则每 tick 都乘一次会像刹车。
         * 推荐 0.88 到 0.94。
         */
        double movementMultiplier = movementMultiplier(client);
        velocity = new Vec3d(
                velocity.x * movementMultiplier,
                velocity.y,
                velocity.z * movementMultiplier
        );

        if (LockOnState.locked) {
            velocity = removeVelocityTowardsLockedTarget(client, velocity);
            velocity = reduceVelocityAwayFromLockedTarget(client, velocity);
        }

        client.player.setVelocity(velocity);
    }

    private static boolean shouldHardLockLocalMovement() {
        return CombatAnimationClient.isLocalMovementLockedByAnimation();
    }

    private static double movementMultiplier(MinecraftClient client) {
        double multiplier = LockOnState.locked ? CombatMovementConfig.LOCKED_MOVEMENT_MULTIPLIER : 1.0;
        if (hasLargeShield(client)) {
            multiplier *= CombatControlConfig.LARGE_SHIELD_LOCKED_MOVEMENT_SPEED_MULTIPLIER;
        }
        multiplier *= CombatHitFeedbackClient.getCustomHitMovementMultiplier();
        return multiplier;
    }

    private static boolean hasLockedLargeShield(MinecraftClient client) {
        return client.player != null
                && LockOnState.locked
                && EquipmentCombatAttributesRegistry.isLargeShield(client.player.getOffHandStack());
    }

    private static boolean hasLargeShield(MinecraftClient client) {
        return client.player != null
                && EquipmentCombatAttributesRegistry.isLargeShield(client.player.getOffHandStack());
    }

    private static void tickLocalControlTimers() {
        if (localAttackDisabledTicks > 0) {
            localAttackDisabledTicks--;
        }

        if (localBlockDisabledTicks > 0) {
            localBlockDisabledTicks--;
        }

        if (localDodgeTicks > 0) {
            localDodgeTicks--;
        }
    }

    private static void updateAttackAnimationSpeed() {
        if (!CombatClientState.attacking) {
            if (CombatClientState.isInPostAttackWindow() && CombatClientState.hasBufferedAttack()) {
                return;
            }
            CombatAnimationClient.setCombatAnimationSpeed(1.0F);
            return;
        }

        CombatAnimationClient.setCombatAnimationSpeed(
                (float) CombatClientState.currentAttackAnimationSpeedScale()
        );
    }

    private static DodgeDirection getDodgeDirection(MinecraftClient client) {
        if (client.options.forwardKey.isPressed()) {
            return DodgeDirection.FORWARD;
        }

        if (client.options.leftKey.isPressed()) {
            return DodgeDirection.LEFT;
        }

        if (client.options.rightKey.isPressed()) {
            return DodgeDirection.RIGHT;
        }

        return DodgeDirection.BACK;
    }

    private static void applyLocalDodgeVelocity(
            MinecraftClient client,
            DodgeDirection direction
    ) {
        if (client.player == null) {
            return;
        }

        Vec3d forward = getPlayerHorizontalForward(client.player.getYaw());
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x).normalize();
        int ageTicks = CombatControlConfig.DODGE_TOTAL_TICKS - localDodgeTicks;

        Vec3d dodgeVector = switch (direction) {
            case FORWARD -> forward.multiply(CombatControlConfig.FORWARD_STEP_SPEED);
            case BACK -> forward.multiply(-CombatControlConfig.DODGE_BACK_SPEED);
            case LEFT -> right.multiply(-CombatControlConfig.DODGE_SIDE_SPEED);
            case RIGHT -> right.multiply(CombatControlConfig.DODGE_SIDE_SPEED);
        };
        dodgeVector = dodgeVector.multiply(CombatControlConfig.getDodgeSpeedScale(ageTicks));

        Vec3d velocity = client.player.getVelocity();
        client.player.setVelocity(dodgeVector.x, velocity.y, dodgeVector.z);
    }

    private static void clearHorizontalSideMomentum(MinecraftClient client) {
        clearHorizontalVelocity(client);
    }

    private static void clearHorizontalVelocity(MinecraftClient client) {
        Vec3d velocity = client.player.getVelocity();

        /*
         * 攻击期间客户端也先清水平动量。
         * 服务端随后会设置攻击前冲速度。
         */
        client.player.setVelocity(
                0.0,
                velocity.y,
                0.0
        );
    }

    private static Vec3d preventMovingIntoLockedTarget(
            MinecraftClient client,
            Vec3d velocity
    ) {
        if (client.world == null || client.player == null) {
            return velocity;
        }

        if (LockOnState.targetEntityId < 0) {
            return velocity;
        }

        Entity target = client.world.getEntityById(LockOnState.targetEntityId);

        if (target == null || !target.isAlive()) {
            return velocity;
        }

        Vec3d toTarget = target.getPos().subtract(client.player.getPos());
        Vec3d horizontalToTarget = new Vec3d(toTarget.x, 0.0, toTarget.z);

        double distance = horizontalToTarget.length();

        if (distance > CombatMovementConfig.LOCKED_STOP_FORWARD_DISTANCE) {
            return velocity;
        }

        Vec3d horizontalVelocity = new Vec3d(velocity.x, 0.0, velocity.z);

        if (horizontalVelocity.lengthSquared() <= 0.000001) {
            return velocity;
        }

        Vec3d toTargetDir = horizontalToTarget.normalize();

        double towardSpeed = horizontalVelocity.dotProduct(toTargetDir);

        /*
         * 没有朝目标移动，不处理。
         */
        if (towardSpeed <= 0.0) {
            return velocity;
        }

        /*
         * 删除朝向目标的速度分量。
         * 保留切向速度，这样玩家还能绕着目标侧移。
         */
        Vec3d blockedHorizontal = horizontalVelocity.subtract(
                toTargetDir.multiply(towardSpeed)
        );

        return new Vec3d(
                blockedHorizontal.x,
                velocity.y,
                blockedHorizontal.z
        );
    }

    private static Vec3d reduceVelocityAwayFromLockedTarget(
            MinecraftClient client,
            Vec3d velocity
    ) {
        if (client.world == null || client.player == null || LockOnState.targetEntityId < 0) {
            return velocity;
        }

        Entity target = client.world.getEntityById(LockOnState.targetEntityId);
        if (target == null || !target.isAlive()) {
            return velocity;
        }

        Vec3d away = client.player.getPos().subtract(target.getPos());
        Vec3d horizontalAway = new Vec3d(away.x, 0.0, away.z);
        Vec3d horizontalVelocity = new Vec3d(velocity.x, 0.0, velocity.z);
        if (horizontalAway.lengthSquared() <= 0.000001
                || horizontalVelocity.lengthSquared() <= 0.000001) {
            return velocity;
        }

        Vec3d awayDir = horizontalAway.normalize();
        double awaySpeed = horizontalVelocity.dotProduct(awayDir);
        if (awaySpeed <= 0.0) {
            return velocity;
        }

        Vec3d reducedHorizontal = horizontalVelocity.subtract(awayDir.multiply(awaySpeed * 0.55));
        return new Vec3d(reducedHorizontal.x, velocity.y, reducedHorizontal.z);
    }

    private static void handleLeftClickAttack(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        /*
         * 打开物品栏、聊天框、箱子、菜单等 GUI 时，
         * 不触发自定义攻击。
         */
        if (client.currentScreen != null) {
            leftPressTicks = 0;
            leftPressStartedOnCombatItem = false;
            leftPressDirection = CombatClientState.currentDirection;
            lastLeftPressed = false;
            return;
        }

        long handle = client.getWindow().getHandle();

        boolean pressed = GLFW.glfwGetMouseButton(
                handle,
                GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == GLFW.GLFW_PRESS;

        /*
         * 左键刚按下。
         */
        if (pressed && !lastLeftPressed) {
            leftPressTicks = 0;
            leftPressDirection = CombatClientState.currentDirection;

            /*
             * 只有空手 / 剑 / 斧 / 镐 / 锄 / 铲，
             * 才允许这次短按触发自定义攻击。
             */
            leftPressStartedOnCombatItem =
                    CombatItemUtil.canUseCustomCombat(client.player)
                            && !CombatItemUtil.shouldUseVanillaEntityAttack(
                                    client.player,
                                    client.crosshairTarget instanceof EntityHitResult entityHit
                                            ? entityHit.getEntity()
                                            : null
                            );
        }

        /*
         * 左键按住中。
         */
        if (pressed) {
            leftPressTicks++;
        }

        /*
         * 左键刚松开。
         */
        if (!pressed && lastLeftPressed) {
            boolean isShortPress = leftPressTicks <= SHORT_ATTACK_MAX_TICKS;

            if (isShortPress && leftPressStartedOnCombatItem) {
                CombatDirection pressedDirection = leftPressDirection;

                if (CombatClientState.attacking) {
                    if (CombatClientState.isInAttackPreInputWindow()) {
                        boolean buffered = CombatClientState.bufferAttackInPostWindow(pressedDirection);

                        if (buffered) {
                            debugMessage(client, "攻击末段预输入：" + pressedDirection.name());
                        }
                    } else {
                        debugMessage(client, "攻击前段，不能预输入");
                    }
                } else if (CombatClientState.isInPostAttackWindow()) {
                    /*
                     * 只有后摇 0.3s 窗口内的输入才记录为预输入。
                     */
                    boolean buffered = CombatClientState.bufferAttackInPostWindow(pressedDirection);

                    if (buffered) {
                        debugMessage(client, "预输入：" + pressedDirection.name());
                    }
                } else {
                    /*
                     * 不在攻击中，也不在后摇窗口中，正常立刻攻击。
                     */
                    startCustomAttack(client, pressedDirection, false);

                }
            }

            leftPressTicks = 0;
            leftPressStartedOnCombatItem = false;
            leftPressDirection = CombatClientState.currentDirection;
        }

        lastLeftPressed = pressed;
    }

    private static void syncHorseControl(MinecraftClient client) {
        if (!(client.player.getVehicle() instanceof AbstractHorseEntity horse)) {
            lastHorseSprintPressed = false;
            lastHorseJumpPressed = false;
            horseSprintPulseTicks = 0;
            return;
        }

        float sideways = 0.0F;
        if (client.options.leftKey.isPressed()) {
            sideways += 1.0F;
        }
        if (client.options.rightKey.isPressed()) {
            sideways -= 1.0F;
        }

        float forward = 0.0F;
        if (client.options.forwardKey.isPressed()) {
            forward += 1.0F;
        }
        if (client.options.backKey.isPressed()) {
            forward -= 1.0F;
        }

        boolean sprintPressed = client.options.sprintKey.isPressed();
        if (sprintPressed && !lastHorseSprintPressed) {
            horseSprintPulseTicks = 4;
        }
        lastHorseSprintPressed = sprintPressed;

        boolean jumpPressed = client.options.jumpKey.isPressed();
        boolean jumpPulse = jumpPressed && !lastHorseJumpPressed;
        lastHorseJumpPressed = jumpPressed;

        float yaw = horse.getYaw();
        if (Math.abs(sideways) > 0.05F) {
            float oldYaw = yaw;
            double speed = (Object) horse instanceof KccHorseRidingData horseData
                    ? horseData.kingdomcomecombat$getHorseCurrentSpeed()
                    : 0.0;
            float turnRate = (float) (5.2 + (2.0 - 5.2) * Math.min(1.0, speed));
            yaw -= sideways * turnRate;
            horse.setYaw(yaw);
            float carriedYaw = MathHelper.wrapDegrees(yaw - oldYaw);
            client.player.setYaw(client.player.getYaw() + carriedYaw);
            client.player.setHeadYaw(client.player.getHeadYaw() + carriedYaw);
        }

        ClientPlayNetworking.send(new HorseControlPayload(
                sideways,
                forward,
                horseSprintPulseTicks > 0,
                jumpPulse,
                yaw
        ));
        client.player.setSprinting(horseSprintPulseTicks > 0);
        if (horseSprintPulseTicks > 0) {
            horseSprintPulseTicks--;
        }
    }

    private record AttackLungeInput(
            boolean movementKeyPressed,
            int forward,
            int side
    ) {
    }

    private static void debugMessage(MinecraftClient client, String message) {
    }
}
