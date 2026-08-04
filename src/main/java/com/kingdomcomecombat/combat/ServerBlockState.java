package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.config.CombatServerConfig;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

public class ServerBlockState {
    public static final int BLOCK_WINDOW_TICKS = 12;
    public static final int BLOCK_MOVEMENT_LOCK_TICKS = 5;
    private static final int CLASSIC_HOLD_KEEPALIVE_TICKS = 7;

    private static final Map<UUID, BlockWindow> BLOCK_WINDOWS = new HashMap<>();
    private static final Map<UUID, Integer> MOVEMENT_LOCKS = new HashMap<>();
    private static final Map<UUID, Integer> INPUT_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, HeldBlock> HELD_BLOCKS = new HashMap<>();
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
        return startBlock(uuid, direction, shieldBlock, classicMode, 1.0);
    }

    public static boolean startBlock(UUID uuid, CombatDirection direction, boolean shieldBlock,
                                     boolean classicMode, double shieldPerfectWindowMultiplier) {
        if (BLOCK_WINDOWS.containsKey(uuid) || isInputCoolingDown(uuid)) {
            return false;
        }

        int perfectTicks = shieldBlock
                ? Math.max(1, (int) Math.ceil(CombatServerConfig.blockWindowTicks()
                        * Math.max(1.0, shieldPerfectWindowMultiplier)))
                : CombatServerConfig.blockWindowTicks();
        int unperfectTicks = CombatServerConfig.unperfectBlockWindowTicks();
        BLOCK_WINDOWS.put(uuid, new BlockWindow(direction, perfectTicks, unperfectTicks, classicMode));
        MOVEMENT_LOCKS.put(uuid, BLOCK_MOVEMENT_LOCK_TICKS);
        INPUT_COOLDOWNS.put(uuid, Math.max(1, perfectTicks + unperfectTicks));
        return true;
    }

    public static BlockWindow get(UUID uuid) {
        return BLOCK_WINDOWS.get(uuid);
    }

    public static boolean keepClassicHoldAlive(UUID uuid) {
        BlockWindow window = BLOCK_WINDOWS.get(uuid);
        if (window == null || !window.classicMode) {
            return false;
        }
        window.holdKeepAliveTicks = CLASSIC_HOLD_KEEPALIVE_TICKS;
        return true;
    }

    public static void releaseClassicHold(UUID uuid) {
        BlockWindow window = BLOCK_WINDOWS.get(uuid);
        if (window != null) {
            window.holdKeepAliveTicks = 0;
        }
    }

    public static void updateHeld(UUID uuid, boolean holding) {
        if (!holding) {
            HELD_BLOCKS.remove(uuid);
            return;
        }
        HeldBlock held = HELD_BLOCKS.computeIfAbsent(uuid, ignored -> new HeldBlock());
        held.keepAliveTicks = 8;
    }

    public static boolean isLongHeld(UUID uuid) {
        HeldBlock held = HELD_BLOCKS.get(uuid);
        return held != null && held.keepAliveTicks > 0 && held.ageTicks >= 12;
    }

    public static boolean isHeld(UUID uuid) {
        HeldBlock held = HELD_BLOCKS.get(uuid);
        return held != null && held.keepAliveTicks > 0;
    }

    public static void collectHeldUuids(Set<UUID> destination) {
        destination.addAll(HELD_BLOCKS.keySet());
    }

    public static void clear(UUID uuid) {
        BlockWindow window = BLOCK_WINDOWS.get(uuid);
        if (window != null && window.preserveClassicImperfectHold) {
            return;
        }
        BLOCK_WINDOWS.remove(uuid);
        // A resolved block is immediately ready for the next input heartbeat.
        // Keeping this cooldown after the server has consumed the window creates
        // an artificial gap for clients with latency.
        INPUT_COOLDOWNS.remove(uuid);
    }

    public static void clearAll(UUID uuid) {
        BLOCK_WINDOWS.remove(uuid);
        MOVEMENT_LOCKS.remove(uuid);
        INPUT_COOLDOWNS.remove(uuid);
        HELD_BLOCKS.remove(uuid);
    }

    public static boolean isMovementLocked(UUID uuid) {
        return MOVEMENT_LOCKS.getOrDefault(uuid, 0) > 0;
    }

    public static void collectMovementLockedUuids(Set<UUID> destination) {
        destination.addAll(MOVEMENT_LOCKS.keySet());
    }

    public static boolean isInputCoolingDown(UUID uuid) {
        return INPUT_COOLDOWNS.getOrDefault(uuid, 0) > 0;
    }

    public static int nextUnperfectVariant() {
        nextUnperfectVariant = !nextUnperfectVariant;
        return nextUnperfectVariant ? 1 : 2;
    }

    public static void preserveClassicImperfectHold(UUID uuid) {
        BlockWindow window = BLOCK_WINDOWS.get(uuid);
        if (window == null || !window.classicMode || window.holdKeepAliveTicks <= 0) return;
        window.ageTicks = Math.max(window.ageTicks, window.totalTicks());
        window.holdKeepAliveTicks = CLASSIC_HOLD_KEEPALIVE_TICKS;
        window.preserveClassicImperfectHold = true;
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, BlockWindow>> iterator = BLOCK_WINDOWS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BlockWindow> entry = iterator.next();
            BlockWindow window = entry.getValue();
            window.ageTicks++;
            if (window.holdKeepAliveTicks > 0) {
                window.holdKeepAliveTicks--;
            }
            if (window.ageTicks >= window.totalTicks() && !window.isExtendedClassicHold()) {
                iterator.remove();
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


        Iterator<Map.Entry<UUID, HeldBlock>> heldIterator = HELD_BLOCKS.entrySet().iterator();
        while (heldIterator.hasNext()) {
            HeldBlock held = heldIterator.next().getValue();
            held.ageTicks++;
            if (--held.keepAliveTicks <= 0) heldIterator.remove();
        }
    }

    private static final class HeldBlock {
        private int ageTicks;
        private int keepAliveTicks;
    }

    public static class BlockWindow {
        public final CombatDirection direction;
        private final int perfectTicks;
        private final int unperfectTicks;
        private final boolean classicMode;
        private int ageTicks;
        private int holdKeepAliveTicks;
        private boolean preserveClassicImperfectHold;

        private BlockWindow(CombatDirection direction, int perfectTicks, int unperfectTicks, boolean classicMode) {
            this.direction = direction;
            this.perfectTicks = Math.max(0, perfectTicks);
            this.unperfectTicks = Math.max(0, unperfectTicks);
            this.classicMode = classicMode;
            this.holdKeepAliveTicks = classicMode ? CLASSIC_HOLD_KEEPALIVE_TICKS : 0;
        }

        public boolean isPerfectPhase() {
            return ageTicks < perfectTicks;
        }

        public boolean isUnperfectPhase() {
            return ageTicks >= perfectTicks
                    && (ageTicks < totalTicks() || isExtendedClassicHold());
        }

        public boolean isExtendedClassicHold() {
            return classicMode && ageTicks >= totalTicks() && holdKeepAliveTicks > 0;
        }

        private int totalTicks() {
            return perfectTicks + unperfectTicks;
        }
    }

}
