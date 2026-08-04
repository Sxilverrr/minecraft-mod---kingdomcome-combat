package com.kingdomcomecombat.client.combat;

import com.kingdomcomecombat.combat.CombatDirection;

public class ClientExecutionState {
    private static int targetEntityId = -1;
    private static int attackerEntityId = -1;
    private static CombatDirection direction = CombatDirection.RIGHT;
    private static int ticksRemaining = 0;
    private static int localExecutionHideCrosshairTicks = 0;

    private ClientExecutionState() {
    }

    public static void start(int targetId, int attackerId, CombatDirection stunDirection, int ticks) {
        targetEntityId = targetId;
        attackerEntityId = attackerId;
        direction = stunDirection;
        ticksRemaining = Math.max(0, ticks);
    }

    public static void tick(int localPlayerId) {
        if (localExecutionHideCrosshairTicks > 0) {
            localExecutionHideCrosshairTicks--;
        }
        if (ticksRemaining <= 0) {
            return;
        }
        ticksRemaining--;
    }

    public static boolean canExecuteTarget(int entityId) {
        return ticksRemaining > 0 && targetEntityId == entityId;
    }

    public static boolean hasExecutableTarget() {
        return ticksRemaining > 0 && targetEntityId >= 0;
    }

    public static boolean shouldHideCrosshair() {
        return localExecutionHideCrosshairTicks > 0;
    }

    public static boolean isLocalExecutionActive() {
        return localExecutionHideCrosshairTicks > 0;
    }

    public static void startLocalExecution() {
        localExecutionHideCrosshairTicks = 45;
        ticksRemaining = 0;
        targetEntityId = -1;
    }

    public static void clear() {
        targetEntityId = -1;
        attackerEntityId = -1;
        ticksRemaining = 0;
        localExecutionHideCrosshairTicks = 0;
    }
}
