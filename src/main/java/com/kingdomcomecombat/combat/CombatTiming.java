package com.kingdomcomecombat.combat;

public class CombatTiming {
    /**
     * 攻击结束后至少还要保留的禁攻时间。
     */
    public static final int POST_ATTACK_BUFFER_WINDOW_TICKS = 2;

    public static final int ATTACK_STARTUP_NO_DEFENSE_TICKS = 6;
    public static final int ATTACK_HITBOX_START_TICKS = 3;
    public static final int ATTACK_PREINPUT_WINDOW_TICKS = 10;

    public static final int ATTACK_CHAIN_TRANSITION_TICKS = 3;
    public static final int DEFAULT_ATTACK_TOTAL_TICKS = 10;

    public static final int ATTACK_CHAIN_BLEND_TICKS = 3;
    public static final int LIGHT_ATTACK_TRANSITION_TICKS = 4;
    public static final int HEAVY_ATTACK_TRANSITION_TICKS = 3;
    public static final int ATTACK_RECOVERY_EXTENSION_TICKS = LIGHT_ATTACK_TRANSITION_TICKS;
    public static final int ATTACK_RECOVER_MOVE_TICKS = ATTACK_RECOVERY_EXTENSION_TICKS;
    public static final int ATTACK_TO_STANCE_BLEND_TICKS = 4;
    public static final int SERVER_BUFFERED_ATTACK_RELEASE_GRACE_TICKS = 2;

    public static final double ATTACK_MAX_FORWARD_SPEED = 0.15;
    public static final double ATTACK_LUNGE_PEAK_RATIO = 0.15;
    public static final double ATTACK_LOCKED_LUNGE_DURATION_SECONDS = 0.13;

    private CombatTiming() {
    }

    public static int getAttackChainBlendStartTick(int attackTotalTicks) {
        return Math.max(
                0,
                sanitizeAttackTotalTicks(attackTotalTicks) - ATTACK_CHAIN_BLEND_TICKS
        );
    }

    public static int getAttackRecoveryBlendStartTick(int attackTotalTicks) {
        return getAttackTransitionStartTick(
                attackTotalTicks,
                LIGHT_ATTACK_TRANSITION_TICKS
        );
    }

    public static int getAttackRecoveryBlendStartTick(
            int attackTotalTicks,
            int transitionTicks
    ) {
        return getAttackTransitionStartTick(attackTotalTicks, transitionTicks);
    }

    public static int getPreInputStartTick(int attackTotalTicks) {
        return getPreInputStartTick(
                attackTotalTicks,
                ATTACK_PREINPUT_WINDOW_TICKS
        );
    }

    public static int getPreInputStartTick(int attackTotalTicks, int preInputWindowTicks) {
        return Math.max(
                0,
                sanitizeAttackTotalTicks(attackTotalTicks) - Math.max(1, preInputWindowTicks)
        );
    }

    public static int getAttackTransitionStartTick(int attackTotalTicks) {
        return getAttackTransitionStartTick(
                attackTotalTicks,
                LIGHT_ATTACK_TRANSITION_TICKS
        );
    }

    public static int getAttackTransitionStartTick(
            int attackTotalTicks,
            int transitionTicks
    ) {
        int totalTicks = sanitizeAttackTotalTicks(attackTotalTicks);
        return Math.max(
                getActiveEndTick(totalTicks) + 1,
                totalTicks - Math.max(1, transitionTicks)
        );
    }

    public static boolean isInPreInputWindow(int ageTicks, int attackTotalTicks) {
        return isInPreInputWindow(
                ageTicks,
                attackTotalTicks,
                ATTACK_PREINPUT_WINDOW_TICKS
        );
    }

    public static boolean isInPreInputWindow(
            int ageTicks,
            int attackTotalTicks,
            int preInputWindowTicks
    ) {
        return ageTicks >= getPreInputStartTick(attackTotalTicks, preInputWindowTicks)
                && ageTicks <= sanitizeAttackTotalTicks(attackTotalTicks);
    }

    public static boolean isInAttackStartupNoDefenseWindow(int ageTicks) {
        return ageTicks < ATTACK_STARTUP_NO_DEFENSE_TICKS;
    }

    public static int getActiveStartTick(int attackTotalTicks) {
        return Math.min(
                sanitizeAttackTotalTicks(attackTotalTicks),
                ATTACK_HITBOX_START_TICKS
        );
    }

    public static int getActiveEndTick(int attackTotalTicks) {
        int totalTicks = sanitizeAttackTotalTicks(attackTotalTicks);

        return Math.min(
                totalTicks,
                getActiveStartTick(totalTicks) + 4
        );
    }

    public static int getAttackCanMoveFromTick(int attackTotalTicks) {
        return getAttackTransitionStartTick(
                attackTotalTicks,
                LIGHT_ATTACK_TRANSITION_TICKS
        );
    }

    public static boolean isAttackActiveFrame(int ageTicks, int attackTotalTicks) {
        return ageTicks >= getActiveStartTick(attackTotalTicks)
                && ageTicks <= sanitizeAttackTotalTicks(attackTotalTicks);
    }

    public static boolean canReleaseBufferedAttack(int ageTicks, int attackTotalTicks) {
        return canReleaseBufferedAttack(
                ageTicks,
                attackTotalTicks,
                LIGHT_ATTACK_TRANSITION_TICKS
        );
    }

    public static boolean canReleaseBufferedAttack(
            int ageTicks,
            int attackTotalTicks,
            int transitionTicks
    ) {
        return ageTicks >= getAttackTransitionStartTick(attackTotalTicks, transitionTicks);
    }

    public static boolean canMoveDuringAttack(int ageTicks, int attackTotalTicks) {
        return ageTicks >= getAttackCanMoveFromTick(attackTotalTicks);
    }

    public static boolean isAttackFinished(int ageTicks, int attackTotalTicks) {
        return ageTicks > sanitizeAttackTotalTicks(attackTotalTicks);
    }

    public static double getAttackForwardSpeed(int ageTicks, int attackTotalTicks) {
        int lockMoveTicks = getAttackCanMoveFromTick(attackTotalTicks);

        if (lockMoveTicks <= 0) {
            return 0.0;
        }

        double t = ageTicks / (double) lockMoveTicks;
        t = clamp01(t);

        double peak = clamp01(ATTACK_LUNGE_PEAK_RATIO);

        if (peak <= 0.0 || peak >= 1.0) {
            return ATTACK_MAX_FORWARD_SPEED * easeInOutSine(1.0 - t);
        }

        if (t <= peak) {
            double local = t / peak;
            return ATTACK_MAX_FORWARD_SPEED * easeInOutSine(local);
        } else {
            double local = (t - peak) / (1.0 - peak);
            return ATTACK_MAX_FORWARD_SPEED * (1.0 - easeInOutSine(local));
        }
    }

    public static int getLockedAttackLungeDurationTicks() {
        return Math.max(1, (int) Math.ceil(ATTACK_LOCKED_LUNGE_DURATION_SECONDS * 20.0));
    }

    public static int sanitizeAttackTotalTicks(int attackTotalTicks) {
        return Math.max(1, attackTotalTicks);
    }

    public static int scaleTicksForAttackSpeed(int ticks, double attackSpeedMultiplier) {
        double speed = Math.max(0.05, attackSpeedMultiplier);
        return Math.max(1, (int) Math.ceil(Math.max(1, ticks) / speed));
    }

    public static int scaledLightTransitionTicks(double attackSpeedMultiplier) {
        return scaleTicksForAttackSpeed(LIGHT_ATTACK_TRANSITION_TICKS, attackSpeedMultiplier);
    }

    public static int scaledPostAttackBufferWindowTicks(double attackSpeedMultiplier) {
        return scaleTicksForAttackSpeed(POST_ATTACK_BUFFER_WINDOW_TICKS, attackSpeedMultiplier);
    }

    public static int scaledPreInputWindowTicks(double attackSpeedMultiplier) {
        return scaleTicksForAttackSpeed(ATTACK_PREINPUT_WINDOW_TICKS, attackSpeedMultiplier);
    }

    public static int scaledServerBufferedAttackReleaseGraceTicks(double attackSpeedMultiplier) {
        return scaleTicksForAttackSpeed(SERVER_BUFFERED_ATTACK_RELEASE_GRACE_TICKS, attackSpeedMultiplier);
    }

    private static double easeInOutSine(double t) {
        t = clamp01(t);
        return 0.5 - 0.5 * Math.cos(Math.PI * t);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
