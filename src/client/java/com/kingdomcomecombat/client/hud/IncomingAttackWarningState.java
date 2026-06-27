package com.kingdomcomecombat.client.hud;

import com.kingdomcomecombat.combat.CombatDirection;

public class IncomingAttackWarningState {
    private static final int WARNING_TICKS = 18;
    private static final int PERFECT_BLOCK_TICKS = 12;

    private static int ticksRemaining = 0;
    private static int attackerEntityId = -1;
    private static CombatDirection direction = CombatDirection.RIGHT;
    private static int warningType = 0;

    private IncomingAttackWarningState() {
    }

    public static void start(int attackerId, CombatDirection attackDirection, int type) {
        if (warningType == 2 && ticksRemaining > 0 && type != 2) {
            return;
        }
        attackerEntityId = attackerId;
        direction = attackDirection;
        warningType = Math.max(0, Math.min(2, type));
        ticksRemaining = warningType == 2 ? PERFECT_BLOCK_TICKS : WARNING_TICKS;
    }

    public static void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    public static boolean active() {
        return ticksRemaining > 0;
    }

    public static int attackerEntityId() {
        return attackerEntityId;
    }

    public static CombatDirection direction() {
        return direction;
    }

    public static int warningType() {
        return warningType;
    }
}
