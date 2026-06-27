package com.kingdomcomecombat.client.passive;

import com.kingdomcomecombat.combat.BeowulfArmState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientPassiveSkillUnlockState {
    private static final Set<String> UNLOCKED = new HashSet<>();

    private ClientPassiveSkillUnlockState() {
    }

    public static boolean isUnlocked(String skillId) {
        return skillId != null && UNLOCKED.contains(skillId);
    }

    public static void replace(List<String> skillIds) {
        UNLOCKED.clear();
        UNLOCKED.addAll(skillIds);
        BeowulfArmState.setClientUnlocked(UNLOCKED.contains(BeowulfArmState.SKILL_ID));
    }

    public static Set<String> unlockedIds() {
        return Set.copyOf(UNLOCKED);
    }
}
