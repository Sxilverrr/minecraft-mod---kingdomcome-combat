package com.kingdomcomecombat.combat;

public class CombatControlConfig {
    public static final int PERFECT_BLOCK_ATTACK_DISABLE_TICKS = 20;
    public static final int UNPERFECT_BLOCK_ATTACK_DISABLE_TICKS = 12;
    public static final int HIT_ATTACK_DISABLE_TICKS = 12;
    public static final int HIT_BLOCK_DISABLE_TICKS = 12;
    public static final int COMBO_HIT_REACTION_ATTACK_DISABLE_EXTRA_TICKS = 6;
    public static final int PERFECT_COUNTER_WINDOW_TICKS = 8;
    public static final int PERFECT_COUNTER_SLOW_TICKS = 3;
    public static final double PERFECT_COUNTER_ATTACK_SPEED_SCALE = 0.3;
    public static final double MOB_ATTACK_STAMINA_COST_MULTIPLIER = 0.4;
    public static final int MASTER_COUNTER_WINDOW_TICKS = 8;
    public static final int MASTER_COUNTER_EARLY_GRACE_TICKS = 2;
    public static final int BLOCK_INPUT_COOLDOWN_TICKS = 16;

    public static final int DODGE_TOTAL_TICKS = 10;
    public static final int DODGE_COOLDOWN_TICKS = 0;
    public static final int DODGE_INVULN_START_TICK = 1;
    public static final int DODGE_INVULN_END_TICK = 11;
    public static final int DODGE_ATTACK_DISABLE_TICKS = 12;
    public static final int FORWARD_STEP_ATTACK_DISABLE_TICKS = 4;
    public static final int DODGE_BLOCK_DISABLE_TICKS = 12;
    public static final int FORWARD_STEP_BLOCK_DISABLE_TICKS = 5;

    public static final double DODGE_STAMINA_COST = 18.0;
    public static final double FORWARD_STEP_STAMINA_COST = 7.0;
    public static final double DODGE_SIDE_SPEED = 0.21;
    public static final double DODGE_BACK_SPEED = 0.21;
    public static final double FORWARD_STEP_SPEED = 0.13;

    public static final double PERFECT_BLOCK_STAMINA_COST = 5.0;
    public static final double PERFECT_BLOCK_ATTACKER_STAMINA_COST = 15.0;
    public static final double UNPERFECT_BLOCK_STAMINA_COST = 10.0;
    public static final int SHIELD_PERFECT_BLOCK_BONUS_TICKS = 8;
    public static final double SHIELD_PERFECT_BLOCK_ATTACKER_STAMINA_MULTIPLIER = 1.6;
    public static final double SMALL_SHIELD_BLOCK_IMPACT_MITIGATION = 0.88;
    public static final double DEFAULT_ATTACK_HORIZONTAL_KNOCKBACK = 0.08;

    private CombatControlConfig() {
    }

    public static double getDodgeSpeedScale(int ageTicks) {
        double progress = Math.max(0.0, Math.min(1.0, ageTicks / (double) DODGE_TOTAL_TICKS));
        if (progress < 0.50) {
            return lerp(1.75, 1.05, progress / 0.50);
        }

        return lerp(0.78, 0.24, (progress - 0.50) / 0.50);
    }

    private static double lerp(double from, double to, double progress) {
        progress = Math.max(0.0, Math.min(1.0, progress));
        return from + (to - from) * progress;
    }
}
