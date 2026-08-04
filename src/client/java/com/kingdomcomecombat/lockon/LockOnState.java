package com.kingdomcomecombat.client.lockon;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.combat.CombatClientState;

import java.util.UUID;

public class LockOnState {
    public enum Mode {
        HARD,
        SOFT
    }

    public static boolean locked = false;
    public static UUID targetUuid = null;
    public static int targetEntityId = -1;
    public static Mode mode = Mode.HARD;

    public static int blockedTicks = 0;
    private static int delayedClearTicks = 0;
    private static boolean delayedClearStancePlayed = false;

    public static void lock(UUID uuid, int entityId) {
        lock(uuid, entityId, Mode.HARD);
    }

    public static void softLock(UUID uuid, int entityId) {
        lock(uuid, entityId, Mode.SOFT);
    }

    public static void softLock() {
        lock(null, -1, Mode.SOFT);
    }

    public static void promoteToHardLock() {
        if (locked) {
            mode = Mode.HARD;
        }
    }

    public static boolean isSoftLocked() {
        return locked && mode == Mode.SOFT;
    }

    public static boolean isHardLocked() {
        return locked && mode == Mode.HARD;
    }

    private static void lock(UUID uuid, int entityId, Mode lockMode) {
        locked = true;
        mode = lockMode;
        targetUuid = uuid;
        targetEntityId = entityId;
        blockedTicks = 0;
        delayedClearTicks = 0;
        delayedClearStancePlayed = false;

        delayedClearStancePlayed = !CombatClientState.attacking
                && !CombatAnimationClient.isLocalMovementLockedByAnimation();
        if (delayedClearStancePlayed) {
            CombatAnimationClient.playStance(CombatClientState.currentDirection);
        }
    }

    public static boolean beginDelayedClear(int ticks) {
        if (delayedClearTicks > 0) {
            return false;
        }

        delayedClearTicks = Math.max(1, ticks);
        blockedTicks = 0;

        if (!CombatClientState.attacking
                && !CombatAnimationClient.isLocalMovementLockedByAnimation()) {
            CombatAnimationClient.playStance(CombatClientState.currentDirection);
        }

        return true;
    }

    public static boolean delayedClearPending() {
        return delayedClearTicks > 0;
    }

    public static void tickDelayedClear() {
        if (delayedClearTicks <= 0) {
            return;
        }

        if (CombatClientState.attacking
                || CombatAnimationClient.isLocalMovementLockedByAnimation()) {
            return;
        }

        if (!delayedClearStancePlayed
                && !CombatAnimationClient.isLocalMovementLockedByAnimation()) {
            CombatAnimationClient.playStance(CombatClientState.currentDirection);
            delayedClearStancePlayed = true;
        }

        delayedClearTicks--;
        if (delayedClearTicks <= 0) {
            clear();
        }
    }

    public static void clear() {
        locked = false;
        mode = Mode.HARD;
        targetUuid = null;
        targetEntityId = -1;
        blockedTicks = 0;
        delayedClearTicks = 0;
        delayedClearStancePlayed = false;
        LockOnCameraController.clearShoulderCameraOffset();

        if (!CombatClientState.attacking) {
            CombatAnimationClient.exitCombatPose();
        }
    }
}
