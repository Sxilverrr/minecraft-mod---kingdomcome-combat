package com.kingdomcomecombat.passive;

public final class CombatExperienceConfig {
    public static final int DEFAULT_KILL = 5;
    public static final int DEFAULT_PERFECT_BLOCK = 8;
    public static final int DEFAULT_PERFECT_COUNTER = 8;
    public static final int DEFAULT_ATTACK = 3;
    public static final int DEFAULT_MASTER_COUNTER = 20;
    public static final int DEFAULT_COMBO = 20;
    public static final double DEFAULT_VANILLA_EXPERIENCE_MULTIPLIER = 0.40;

    private static int kill = DEFAULT_KILL;
    private static int perfectBlock = DEFAULT_PERFECT_BLOCK;
    private static int perfectCounter = DEFAULT_PERFECT_COUNTER;
    private static int attack = DEFAULT_ATTACK;
    private static int masterCounter = DEFAULT_MASTER_COUNTER;
    private static int combo = DEFAULT_COMBO;
    private static double vanillaExperienceMultiplier = DEFAULT_VANILLA_EXPERIENCE_MULTIPLIER;

    private CombatExperienceConfig() {
    }

    public static void reset() {
        set(DEFAULT_KILL, DEFAULT_PERFECT_BLOCK, DEFAULT_PERFECT_COUNTER, DEFAULT_ATTACK,
                DEFAULT_MASTER_COUNTER, DEFAULT_COMBO, DEFAULT_VANILLA_EXPERIENCE_MULTIPLIER);
    }

    public static void set(
            int killReward,
            int perfectBlockReward,
            int perfectCounterReward,
            int attackReward,
            int masterCounterReward,
            int comboReward,
            double vanillaMultiplier
    ) {
        kill = Math.max(0, killReward);
        perfectBlock = Math.max(0, perfectBlockReward);
        perfectCounter = Math.max(0, perfectCounterReward);
        attack = Math.max(0, attackReward);
        masterCounter = Math.max(0, masterCounterReward);
        combo = Math.max(0, comboReward);
        vanillaExperienceMultiplier = Math.max(0.0, vanillaMultiplier);
    }

    public static int kill() { return kill; }
    public static int perfectBlock() { return perfectBlock; }
    public static int perfectCounter() { return perfectCounter; }
    public static int attack() { return attack; }
    public static int masterCounter() { return masterCounter; }
    public static int combo() { return combo; }
    public static double vanillaExperienceMultiplier() { return vanillaExperienceMultiplier; }
}
