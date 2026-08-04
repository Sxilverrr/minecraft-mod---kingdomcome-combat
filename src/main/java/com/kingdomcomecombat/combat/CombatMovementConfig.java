package com.kingdomcomecombat.combat;

public class CombatMovementConfig {
    public static final double DEFAULT_COMBAT_MIN_DISTANCE = 1.45;
    /**
     * 锁定状态移动倍率。
     *
     * 不要用 0.70 这种太低的值，因为我们现在是按 tick 处理速度，
     * 0.70 会变成明显刹车感。
     *
     * 0.88 会比较像“略微沉重”，但不至于 W 变得很慢。
     */
    public static final double LOCKED_MOVEMENT_MULTIPLIER = 1.0;
    public static final double BLOCK_HOLD_MOVEMENT_MULTIPLIER = 0.75;

    public static final boolean DISABLE_SPRINT_WHEN_LOCKED = true;
    public static final boolean DISABLE_SNEAK_WHEN_LOCKED = true;

    /**
     * 玩家和锁定目标的最小距离。
     */
    public static double LOCKED_MIN_TARGET_DISTANCE = DEFAULT_COMBAT_MIN_DISTANCE;

    /**
     * 只有进入这个距离后，才阻止继续按 W 贴近。
     *
     * 原来如果是 0.95，会比较早就开始挡。
     * 现在改成 0.86，接近 0.8 才挡。
     */
    public static double LOCKED_STOP_FORWARD_DISTANCE = DEFAULT_COMBAT_MIN_DISTANCE;

    /**
     * 锁定攻击前冲倍率。
     */
    public static final double LOCKED_ATTACK_LUNGE_MULTIPLIER = 3.20;
    public static final double PLAYER_ATTACK_LUNGE_DISTANCE = 1.55;
    public static final double PLAYER_ATTACK_LUNGE_MAX_SPEED = 0.65;
    public static final double MOB_ATTACK_LUNGE_DISTANCE = PLAYER_ATTACK_LUNGE_DISTANCE;
    public static final double MOB_ATTACK_LUNGE_MAX_SPEED = PLAYER_ATTACK_LUNGE_MAX_SPEED;

    /**
     * 判定“确实在朝目标前进”的点乘阈值。
     *
     * 越大越严格。
     * 0.35 太宽松，可能斜着走也被挡。
     * 0.65 更合理。
     */
    public static final double LOCKED_FORWARD_BLOCK_DOT = 0.65;

    private CombatMovementConfig() {
    }

    public static void setCombatMinDistance(double distance) {
        double sanitized = Math.max(0.1, distance);
        LOCKED_MIN_TARGET_DISTANCE = sanitized;
        LOCKED_STOP_FORWARD_DISTANCE = sanitized;
    }

    public static void reset() {
        setCombatMinDistance(DEFAULT_COMBAT_MIN_DISTANCE);
    }
}
