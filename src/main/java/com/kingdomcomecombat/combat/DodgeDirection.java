package com.kingdomcomecombat.combat;

public enum DodgeDirection {
    FORWARD,
    BACK,
    LEFT,
    RIGHT;

    public static DodgeDirection fromOrdinalSafe(int ordinal) {
        DodgeDirection[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return BACK;
        }

        return values[ordinal];
    }

    public boolean hasInvulnerability() {
        return this != FORWARD;
    }

    public boolean evades(CombatDirection attackDirection) {
        return hasInvulnerability();
    }
}
