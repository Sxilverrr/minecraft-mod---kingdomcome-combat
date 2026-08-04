package com.kingdomcomecombat.client.passive;

import com.kingdomcomecombat.combat.BeowulfArmState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientPassiveSkillUnlockState {
    private static final Set<String> UNLOCKED = new HashSet<>();
    private static int combatExperience;
    private static int killReward = 5;
    private static int perfectBlockReward = 8;
    private static int perfectCounterReward = 8;
    private static int attackReward = 3;
    private static int masterCounterReward = 20;
    private static int comboReward = 20;
    private static double vanillaExperienceMultiplier = 0.4;

    private ClientPassiveSkillUnlockState() {
    }

    public static boolean isUnlocked(String skillId) {
        return skillId != null && UNLOCKED.contains(skillId);
    }

    public static void replace(
            List<String> skillIds,
            int syncedCombatExperience,
            int syncedKillReward,
            int syncedPerfectBlockReward,
            int syncedPerfectCounterReward,
            int syncedAttackReward,
            int syncedMasterCounterReward,
            int syncedComboReward,
            double syncedVanillaExperienceMultiplier
    ) {
        UNLOCKED.clear();
        UNLOCKED.addAll(skillIds);
        combatExperience = Math.max(0, syncedCombatExperience);
        killReward = Math.max(0, syncedKillReward);
        perfectBlockReward = Math.max(0, syncedPerfectBlockReward);
        perfectCounterReward = Math.max(0, syncedPerfectCounterReward);
        attackReward = Math.max(0, syncedAttackReward);
        masterCounterReward = Math.max(0, syncedMasterCounterReward);
        comboReward = Math.max(0, syncedComboReward);
        vanillaExperienceMultiplier = Math.max(0.0, syncedVanillaExperienceMultiplier);
        BeowulfArmState.setClientUnlocked(UNLOCKED.contains(BeowulfArmState.SKILL_ID));
    }

    public static int combatExperience() {
        return combatExperience;
    }

    public static int killReward() { return killReward; }
    public static int perfectBlockReward() { return perfectBlockReward; }
    public static int perfectCounterReward() { return perfectCounterReward; }
    public static int attackReward() { return attackReward; }
    public static int masterCounterReward() { return masterCounterReward; }
    public static int comboReward() { return comboReward; }
    public static double vanillaExperienceMultiplier() { return vanillaExperienceMultiplier; }

    public static Set<String> unlockedIds() {
        return Set.copyOf(UNLOCKED);
    }
}
