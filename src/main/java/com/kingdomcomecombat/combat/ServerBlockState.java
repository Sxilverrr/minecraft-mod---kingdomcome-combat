package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.config.CombatServerConfig;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ServerBlockState {
    public static final int BLOCK_WINDOW_TICKS = 12;
    public static final int CLASSIC_PERFECT_BLOCK_WINDOW_TICKS = 10;
    public static final int CLASSIC_NORMAL_BLOCK_HOLD_TICKS = 40;
    public static final int BLOCK_MOVEMENT_LOCK_TICKS = 5;

    private static final Map<UUID, BlockWindow> BLOCK_WINDOWS = new HashMap<>();
    private static final Map<UUID, BlockHold> BLOCK_HOLDS = new HashMap<>();
    private static final Map<UUID, Integer> MOVEMENT_LOCKS = new HashMap<>();
    private static final Map<UUID, Integer> INPUT_COOLDOWNS = new HashMap<>();
    private static boolean nextUnperfectVariant = false;

    private ServerBlockState() {
    }

    public static boolean startBlock(UUID uuid, CombatDirection direction) {
        return startBlock(uuid, direction, false);
    }

    public static boolean startBlock(UUID uuid, CombatDirection direction, boolean shieldBlock) {
        return startBlock(uuid, direction, shieldBlock, false);
    }

    public static boolean startBlock(UUID uuid, CombatDirection direction, boolean shieldBlock, boolean classicMode) {
        if (isInputCoolingDown(uuid)) {
            return false;
        }

        int windowTicks = classicMode
                ? CLASSIC_PERFECT_BLOCK_WINDOW_TICKS
                : shieldBlock
                ? CombatServerConfig.blockWindowTicks() + CombatControlConfig.SHIELD_PERFECT_BLOCK_BONUS_TICKS
                : CombatServerConfig.blockWindowTicks();
        BLOCK_WINDOWS.put(uuid, new BlockWindow(direction, windowTicks));
        if (classicMode) {
            BLOCK_HOLDS.put(uuid, new BlockHold(direction, CLASSIC_NORMAL_BLOCK_HOLD_TICKS));
        }
        MOVEMENT_LOCKS.put(uuid, BLOCK_MOVEMENT_LOCK_TICKS);
        INPUT_COOLDOWNS.put(uuid, CombatControlConfig.BLOCK_INPUT_COOLDOWN_TICKS);
        return true;
    }

    public static BlockWindow get(UUID uuid) {
        return BLOCK_WINDOWS.get(uuid);
    }

    public static void clear(UUID uuid) {
        BLOCK_WINDOWS.remove(uuid);
        BLOCK_HOLDS.remove(uuid);
    }

    public static BlockHold getHold(UUID uuid) {
        return BLOCK_HOLDS.get(uuid);
    }

    public static boolean isMovementLocked(UUID uuid) {
        return MOVEMENT_LOCKS.getOrDefault(uuid, 0) > 0;
    }

    public static boolean isInputCoolingDown(UUID uuid) {
        return INPUT_COOLDOWNS.getOrDefault(uuid, 0) > 0;
    }

    public static int nextUnperfectVariant() {
        nextUnperfectVariant = !nextUnperfectVariant;
        return nextUnperfectVariant ? 1 : 2;
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, BlockWindow>> iterator = BLOCK_WINDOWS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BlockWindow> entry = iterator.next();
            BlockWindow window = entry.getValue();
            window.ticksRemaining--;
            if (window.ticksRemaining <= 0) {
                iterator.remove();
            }
        }

        Iterator<Map.Entry<UUID, BlockHold>> holdIterator = BLOCK_HOLDS.entrySet().iterator();
        while (holdIterator.hasNext()) {
            Map.Entry<UUID, BlockHold> entry = holdIterator.next();
            BlockHold hold = entry.getValue();
            hold.ticksRemaining--;
            if (hold.ticksRemaining <= 0) {
                holdIterator.remove();
            }
        }

        Iterator<Map.Entry<UUID, Integer>> movementIterator = MOVEMENT_LOCKS.entrySet().iterator();
        while (movementIterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = movementIterator.next();
            int ticksRemaining = entry.getValue() - 1;
            if (ticksRemaining <= 0) {
                movementIterator.remove();
            } else {
                entry.setValue(ticksRemaining);
            }
        }

        Iterator<Map.Entry<UUID, Integer>> cooldownIterator = INPUT_COOLDOWNS.entrySet().iterator();
        while (cooldownIterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = cooldownIterator.next();
            int ticksRemaining = entry.getValue() - 1;
            if (ticksRemaining <= 0) {
                cooldownIterator.remove();
            } else {
                entry.setValue(ticksRemaining);
            }
        }
    }

    public static class BlockWindow {
        public final CombatDirection direction;
        private int ticksRemaining;

        private BlockWindow(CombatDirection direction, int ticksRemaining) {
            this.direction = direction;
            this.ticksRemaining = ticksRemaining;
        }
    }

    public static class BlockHold {
        public final CombatDirection direction;
        private int ticksRemaining;

        private BlockHold(CombatDirection direction, int ticksRemaining) {
            this.direction = direction;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
