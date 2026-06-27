package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import net.minecraft.entity.LivingEntity;

public class CombatAttackTiming {
    private CombatAttackTiming() {
    }

    public static int getAttackTotalTicks(LivingEntity attacker) {
        return getAttackTotalTicks(CombatDirection.RIGHT);
    }

    public static int getAttackTotalTicks(CombatDirection direction) {
        return AnimatedAttackHitboxLibrary.getLengthTicks(direction);
    }

    public static int getAttackTotalTicks(CombatDirection direction, AttackMoveConfig moveConfig) {
        if (moveConfig != null && !moveConfig.animationName().isBlank()) {
            return AnimatedAttackHitboxLibrary.getNamedLengthTicks(moveConfig.animationName());
        }

        return getAttackTotalTicks(direction);
    }

    public static int getComboAttackTotalTicks(String animationName) {
        return AnimatedAttackHitboxLibrary.getNamedLengthTicks(animationName);
    }
}
