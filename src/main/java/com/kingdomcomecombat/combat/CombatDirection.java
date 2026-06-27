package com.kingdomcomecombat.combat;

public enum CombatDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN;

    public static CombatDirection fromOrdinalSafe(int ordinal) {
        CombatDirection[] values = values();

        if (ordinal < 0 || ordinal >= values.length) {
            return RIGHT;
        }

        return values[ordinal];
    }

    public static CombatDirection afterSuccessfulAttack(CombatDirection attackDirection) {
        return switch (attackDirection) {
            case RIGHT -> LEFT;
            case LEFT -> RIGHT;
            case UP -> LEFT;
            case DOWN -> RIGHT;
        };
    }

    public static CombatDirection afterPerfectBlock(CombatDirection blockDirection) {
        return switch (blockDirection) {
            case LEFT -> LEFT;
            case RIGHT -> UP;
            case UP, DOWN -> RIGHT;
        };
    }
}
