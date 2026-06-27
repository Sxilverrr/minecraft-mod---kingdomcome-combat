package com.kingdomcomecombat.client.combat;

import com.kingdomcomecombat.combat.ComboMoveConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientComboUnlockState {
    private static final Set<String> DEFAULT_UNLOCKS = Set.of("comboleft", "comboright");
    private static final Set<String> UNLOCKED = new HashSet<>(DEFAULT_UNLOCKS);

    private ClientComboUnlockState() {
    }

    public static boolean isUnlocked(ComboMoveConfig combo) {
        return combo != null && UNLOCKED.contains(combo.id());
    }

    public static void replace(List<String> comboIds) {
        UNLOCKED.clear();
        UNLOCKED.addAll(DEFAULT_UNLOCKS);
        UNLOCKED.addAll(comboIds);
    }

    public static Set<String> unlockedIds() {
        return Set.copyOf(UNLOCKED);
    }
}
