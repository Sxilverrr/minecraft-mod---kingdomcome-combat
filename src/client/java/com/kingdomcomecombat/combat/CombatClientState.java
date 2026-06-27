package com.kingdomcomecombat.client.combat;

import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.CombatControlConfig;
import com.kingdomcomecombat.combat.CombatTiming;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.item.ItemStack;

public class CombatClientState {
    private static final int MAX_RECENT_COMBO_DIRECTIONS = 6;

    public static CombatDirection currentDirection = CombatDirection.RIGHT;

    public static boolean attacking = false;
    public static int attackTicks = 0;
    public static int attackTotalTicks = CombatTiming.DEFAULT_ATTACK_TOTAL_TICKS;
    public static long attackInstanceId = 0L;
    public static int attackTransitionTicks = CombatTiming.LIGHT_ATTACK_TRANSITION_TICKS;
    public static int attackPreInputWindowTicks = CombatTiming.ATTACK_PREINPUT_WINDOW_TICKS;
    private static int postAttackBufferWindowTicks = CombatTiming.POST_ATTACK_BUFFER_WINDOW_TICKS;
    private static int realAttackTicks = 0;
    private static double elapsedAttackTicks = 0.0;
    private static double attackAnimationSpeedMultiplier = 1.0;
    private static boolean perfectCounterSlow = false;
    private static int perfectCounterWindowTicks = 0;
    public static boolean recoveryBlendStarted = false;
    public static CombatDirection lastAttackDirection = null;
    public static String currentAttackAnimationName = null;
    private static CombatDirection directionBeforeCurrentAttack = currentDirection;
    private static boolean currentAttackMovementLocked = false;

    public static int hitStopTicks = 0;
    private static final List<CombatDirection> recentComboDirections = new ArrayList<>();

    /**
     * 攻击完全结束后的补偿预输入窗口。
     */
    public static boolean postAttackWindow = false;
    public static int postAttackWindowTicks = 0;

    /**
     * 只在 postAttackWindow 中有效。
     */
    public static boolean bufferedAttack = false;
    public static CombatDirection bufferedAttackDirection = CombatDirection.RIGHT;

    public static void setDirection(CombatDirection direction) {
        currentDirection = direction;
    }

    public static void startAttack(CombatDirection attackDirection, int totalTicks) {
        startAttack(attackDirection, totalTicks, false);
    }

    public static void startAttack(
            CombatDirection attackDirection,
            int totalTicks,
            boolean perfectCounterSlow
    ) {
        startAttack(attackDirection, totalTicks, perfectCounterSlow, 1.0);
    }

    public static void startAttack(
            CombatDirection attackDirection,
            int totalTicks,
            boolean perfectCounterSlow,
            double attackAnimationSpeedMultiplier
    ) {
        startAttack(
                attackDirection,
                totalTicks,
                perfectCounterSlow,
                attackAnimationSpeedMultiplier,
                CombatTiming.LIGHT_ATTACK_TRANSITION_TICKS
        );
    }

    public static void startAttack(
            CombatDirection attackDirection,
            int totalTicks,
            boolean perfectCounterSlow,
            double attackAnimationSpeedMultiplier,
            int transitionTicks
    ) {
        attacking = true;
        attackInstanceId++;
        attackTicks = 0;
        attackTotalTicks = CombatTiming.sanitizeAttackTotalTicks(totalTicks);
        CombatClientState.attackAnimationSpeedMultiplier = Math.max(0.05, attackAnimationSpeedMultiplier);
        attackTransitionTicks = CombatTiming.scaleTicksForAttackSpeed(
                transitionTicks,
                CombatClientState.attackAnimationSpeedMultiplier
        );
        attackPreInputWindowTicks = CombatTiming.scaledPreInputWindowTicks(
                CombatClientState.attackAnimationSpeedMultiplier
        );
        postAttackBufferWindowTicks = CombatTiming.scaledPostAttackBufferWindowTicks(
                CombatClientState.attackAnimationSpeedMultiplier
        );
        directionBeforeCurrentAttack = currentDirection;
        realAttackTicks = 0;
        elapsedAttackTicks = 0.0;
        CombatClientState.perfectCounterSlow = perfectCounterSlow;
        recoveryBlendStarted = false;
        lastAttackDirection = attackDirection;
        currentAttackAnimationName = null;
        currentAttackMovementLocked = false;
        hitStopTicks = 0;

        postAttackWindow = false;
        postAttackWindowTicks = 0;
        clearBufferedAttack();
    }

    public static void retimeCurrentAttack(int totalTicks) {
        retimeCurrentAttack(totalTicks, attackAnimationSpeedMultiplier);
    }

    public static void retimeCurrentAttack(int totalTicks, double attackAnimationSpeedMultiplier) {
        retimeCurrentAttack(totalTicks, attackAnimationSpeedMultiplier, CombatTiming.LIGHT_ATTACK_TRANSITION_TICKS);
    }

    public static void retimeCurrentAttack(
            int totalTicks,
            double attackAnimationSpeedMultiplier,
            int transitionTicks
    ) {
        if (!attacking) {
            return;
        }

        attackTotalTicks = CombatTiming.sanitizeAttackTotalTicks(totalTicks);
        CombatClientState.attackAnimationSpeedMultiplier = Math.max(0.05, attackAnimationSpeedMultiplier);
        attackTransitionTicks = CombatTiming.scaleTicksForAttackSpeed(
                transitionTicks,
                CombatClientState.attackAnimationSpeedMultiplier
        );
        attackPreInputWindowTicks = CombatTiming.scaledPreInputWindowTicks(
                CombatClientState.attackAnimationSpeedMultiplier
        );
        postAttackBufferWindowTicks = CombatTiming.scaledPostAttackBufferWindowTicks(
                CombatClientState.attackAnimationSpeedMultiplier
        );
        if (attackTicks > attackTotalTicks) {
            attackTicks = attackTotalTicks;
        }
    }

    /**
     * @return true 表示攻击刚刚结束。
     */
    public static boolean tickAttack() {
        if (!attacking) {
            return false;
        }

        elapsedAttackTicks += currentAttackTickScale();
        realAttackTicks++;
        attackTicks = Math.max(attackTicks, (int) Math.floor(elapsedAttackTicks));
        if (hitStopTicks > 0) {
            hitStopTicks--;
        }

        if (CombatTiming.isAttackFinished(attackTicks, attackTotalTicks)) {
            attacking = false;
            attackTicks = 0;
            realAttackTicks = 0;
            elapsedAttackTicks = 0.0;
            attackAnimationSpeedMultiplier = 1.0;
            attackTransitionTicks = CombatTiming.LIGHT_ATTACK_TRANSITION_TICKS;
            attackPreInputWindowTicks = CombatTiming.ATTACK_PREINPUT_WINDOW_TICKS;
            postAttackBufferWindowTicks = CombatTiming.POST_ATTACK_BUFFER_WINDOW_TICKS;
            perfectCounterSlow = false;
            currentAttackAnimationName = null;
            currentAttackMovementLocked = false;
            hitStopTicks = 0;
            return true;
        }

        return false;
    }

    public static void tickPerfectCounterWindow() {
        if (perfectCounterWindowTicks > 0) {
            perfectCounterWindowTicks--;
        }
    }

    public static void startPerfectCounterWindow() {
        perfectCounterWindowTicks = CombatControlConfig.PERFECT_COUNTER_WINDOW_TICKS;
    }

    public static boolean consumePerfectCounterAttackSlow() {
        if (perfectCounterWindowTicks <= 0) {
            return false;
        }

        perfectCounterWindowTicks = 0;
        return true;
    }

    public static void startPostAttackWindow() {
        startPostAttackWindow(postAttackBufferWindowTicks);
    }

    public static void startPostAttackWindow(int ticks) {
        if (ticks <= 0) {
            postAttackWindow = false;
            postAttackWindowTicks = 0;
            return;
        }

        postAttackWindow = true;
        postAttackWindowTicks = ticks;
    }

    /**
     * @return true 表示 0.3s 预输入窗口刚刚结束。
     */
    public static boolean tickPostAttackWindow() {
        if (!postAttackWindow) {
            return false;
        }

        postAttackWindowTicks--;

        if (postAttackWindowTicks <= 0) {
            postAttackWindow = false;
            postAttackWindowTicks = 0;
            return true;
        }

        return false;
    }

    public static boolean isInPostAttackWindow() {
        return postAttackWindow;
    }

    public static boolean bufferAttackInPostWindow(CombatDirection direction) {
        if (!postAttackWindow && !isInAttackPreInputWindow()) {
            return false;
        }

        if (bufferedAttack) {
            return false;
        }

        bufferedAttack = true;
        bufferedAttackDirection = direction;
        return true;
    }

    public static boolean hasBufferedAttack() {
        return bufferedAttack;
    }

    public static CombatDirection getBufferedAttackDirection() {
        return bufferedAttackDirection;
    }

    public static boolean shouldStartRecoveryBlend() {
        return attacking
                && !recoveryBlendStarted
                && !bufferedAttack
                && attackTicks >= CombatTiming.getAttackRecoveryBlendStartTick(
                        attackTotalTicks,
                        attackTransitionTicks
                );
    }

    public static boolean shouldReleaseBufferedAttack() {
        return attacking
                && bufferedAttack
                && CombatTiming.canReleaseBufferedAttack(
                        attackTicks,
                        attackTotalTicks,
                        attackTransitionTicks
                );
    }

    public static void markRecoveryBlendStarted() {
        recoveryBlendStarted = true;
    }

    public static boolean hasStartedRecoveryBlend() {
        return recoveryBlendStarted;
    }

    public static CombatDirection consumeBufferedAttack() {
        if (!bufferedAttack) {
            return null;
        }

        CombatDirection direction = bufferedAttackDirection;
        clearBufferedAttack();
        return direction;
    }

    public static void clearBufferedAttack() {
        bufferedAttack = false;
        bufferedAttackDirection = CombatDirection.RIGHT;
    }

    public static void cancelAttackLocally() {
        attacking = false;
        attackTicks = 0;
        realAttackTicks = 0;
        elapsedAttackTicks = 0.0;
        attackTransitionTicks = CombatTiming.LIGHT_ATTACK_TRANSITION_TICKS;
        attackPreInputWindowTicks = CombatTiming.ATTACK_PREINPUT_WINDOW_TICKS;
        postAttackBufferWindowTicks = CombatTiming.POST_ATTACK_BUFFER_WINDOW_TICKS;
        perfectCounterSlow = false;
        currentAttackAnimationName = null;
        currentAttackMovementLocked = false;
        hitStopTicks = 0;
        recoveryBlendStarted = false;
        postAttackWindow = false;
        postAttackWindowTicks = 0;
        clearBufferedAttack();
    }

    public static void startHitStop(int ticks) {
        hitStopTicks = Math.max(hitStopTicks, ticks);
    }

    public static void setCurrentAttackAnimationName(String animationName) {
        if (attacking) {
            currentAttackAnimationName = animationName;
        }
    }

    public static void setCurrentAttackMovementLocked(boolean movementLocked) {
        if (attacking) {
            currentAttackMovementLocked = movementLocked;
        }
    }

    public static Optional<ComboMoveConfig> recordComboInput(CombatDirection direction) {
        recentComboDirections.add(direction);
        while (recentComboDirections.size() > MAX_RECENT_COMBO_DIRECTIONS) {
            recentComboDirections.removeFirst();
        }

        return ComboMoveConfigs.findMatchingTail(
                recentComboDirections,
                ItemStack.EMPTY,
                ClientComboUnlockState::isUnlocked
        );
    }

    public static Optional<ComboMoveConfig> previewComboInput(CombatDirection direction) {
        return previewComboInput(direction, ItemStack.EMPTY);
    }

    public static Optional<ComboMoveConfig> previewComboInput(
            CombatDirection direction,
            ItemStack weapon
    ) {
        List<CombatDirection> preview = new ArrayList<>(recentComboDirections);
        preview.add(direction);
        while (preview.size() > MAX_RECENT_COMBO_DIRECTIONS) {
            preview.removeFirst();
        }

        return ComboMoveConfigs.findMatchingTail(
                preview,
                weapon,
                ClientComboUnlockState::isUnlocked
        );
    }

    public static void clearComboInputs() {
        recentComboDirections.clear();
    }

    public static boolean isInHitStop() {
        return hitStopTicks > 0;
    }

    public static boolean canMoveDuringAttack() {
        if (!attacking) {
            return true;
        }

        if (currentAttackMovementLocked) {
            return false;
        }

        return attackTicks >= CombatTiming.getAttackTransitionStartTick(
                attackTotalTicks,
                attackTransitionTicks
        );
    }

    public static boolean isInAttackPreInputWindow() {
        return attacking && CombatTiming.isInPreInputWindow(
                attackTicks,
                attackTotalTicks,
                attackPreInputWindowTicks
        );
    }

    public static boolean isInAttackStartupNoDefenseWindow() {
        return attacking && CombatTiming.isInAttackStartupNoDefenseWindow(attackTicks);
    }

    public static boolean isBusyWithAttackOrPostWindow() {
        return attacking || postAttackWindow;
    }

    public static boolean bufferAttackDuringDisabled(CombatDirection direction) {
        if (bufferedAttack) {
            return false;
        }

        bufferedAttack = true;
        bufferedAttackDirection = direction;
        return true;
    }

    public static float currentAttackElapsedSeconds() {
        return (float) (elapsedAttackTicks / 20.0);
    }

    public static float currentAttackAnimationElapsedSeconds() {
        return (float) (elapsedAttackTicks / 20.0 * attackAnimationSpeedMultiplier);
    }

    public static double currentAttackTickScale() {
        if (perfectCounterSlow && realAttackTicks < CombatControlConfig.PERFECT_COUNTER_SLOW_TICKS) {
            return CombatControlConfig.PERFECT_COUNTER_ATTACK_SPEED_SCALE;
        }

        return 1.0;
    }

    public static double currentAttackAnimationSpeedScale() {
        return currentAttackTickScale() * attackAnimationSpeedMultiplier;
    }

    /**
     * 成功发动攻击后改变下一架势。
     *
     * RIGHT -> LEFT
     * LEFT  -> RIGHT
     * UP    -> LEFT
     * DOWN  -> RIGHT
     */
    public static void applyDirectionAfterSuccessfulAttack(CombatDirection attackDirection) {
        currentDirection = CombatDirection.afterSuccessfulAttack(attackDirection);
    }

    public static void restoreDirectionBeforeCurrentAttack() {
        currentDirection = directionBeforeCurrentAttack;
    }
}
