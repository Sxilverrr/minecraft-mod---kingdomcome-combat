package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.ai.BeastCombatAiTicker;
import com.kingdomcomecombat.ai.ConfiguredMobAttackTicker;
import com.kingdomcomecombat.ai.HumanoidCombatAiProfiles;
import net.minecraft.entity.mob.MobEntity;

public final class VanillaMobAttackControl {
    private VanillaMobAttackControl() {
    }

    public static boolean disablesVanillaAttack(MobEntity mob) {
        return HumanoidCombatAiProfiles.getProfile(mob) != null
                || ConfiguredMobAttackTicker.disablesVanillaAttack(mob)
                || !BeastCombatAiTicker.shouldUseVanillaAttack(mob);
    }
}
