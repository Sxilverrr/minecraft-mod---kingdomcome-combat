package com.kingdomcomecombat.combat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ServerCombatStanceState {
    private static final int STANCE_TTL_TICKS = 20 * 60 * 10;
    private static final Map<UUID, StanceEntry> STANCES = new HashMap<>();
    private static final Map<UUID, Integer> LARGE_SHIELD_DISABLED = new HashMap<>();

    private ServerCombatStanceState() {
    }

    public static void set(UUID uuid, CombatDirection direction) {
        set(uuid, direction, isLocked(uuid));
    }

    public static void set(UUID uuid, CombatDirection direction, boolean locked) {
        STANCES.put(uuid, new StanceEntry(direction, locked, STANCE_TTL_TICKS));
    }

    public static CombatDirection get(UUID uuid) {
        StanceEntry entry = STANCES.get(uuid);
        return entry == null ? CombatDirection.RIGHT : entry.direction;
    }

    public static boolean isLocked(UUID uuid) {
        StanceEntry entry = STANCES.get(uuid);
        return entry != null && entry.locked;
    }

    public static void disableLargeShield(UUID uuid, int ticks) {
        LARGE_SHIELD_DISABLED.merge(uuid, Math.max(0, ticks), Math::max);
    }

    public static boolean canUseLargeShield(UUID uuid) {
        return LARGE_SHIELD_DISABLED.getOrDefault(uuid, 0) <= 0;
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, StanceEntry>> iterator = STANCES.entrySet().iterator();
        while (iterator.hasNext()) {
            StanceEntry entry = iterator.next().getValue();
            entry.ticksRemaining--;
            if (entry.ticksRemaining <= 0) {
                iterator.remove();
            }
        }

        Iterator<Map.Entry<UUID, Integer>> shieldIterator = LARGE_SHIELD_DISABLED.entrySet().iterator();
        while (shieldIterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = shieldIterator.next();
            int ticksRemaining = entry.getValue() - 1;
            if (ticksRemaining <= 0) {
                shieldIterator.remove();
            } else {
                entry.setValue(ticksRemaining);
            }
        }
    }

    private static class StanceEntry {
        private final CombatDirection direction;
        private final boolean locked;
        private int ticksRemaining;

        private StanceEntry(CombatDirection direction, boolean locked, int ticksRemaining) {
            this.direction = direction;
            this.locked = locked;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
