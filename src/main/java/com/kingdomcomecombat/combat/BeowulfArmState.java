package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.passive.PlayerPassiveSkillProgress;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BeowulfArmState {
    public static final String SKILL_ID = "strong_arm";
    public static final double ATTACK_SPEED_MULTIPLIER = 0.8;

    private static volatile boolean clientUnlocked;

    private BeowulfArmState() {
    }

    public static void setClientUnlocked(boolean unlocked) {
        clientUnlocked = unlocked;
    }

    public static boolean isActive(LivingEntity entity) {
        if (entity == null
                || !CombatItemUtil.isLongsword(entity.getMainHandStack())
                || entity.getOffHandStack().isEmpty()) {
            return false;
        }

        if (entity instanceof ServerPlayerEntity player) {
            return PlayerPassiveSkillProgress.isUnlocked(player, SKILL_ID);
        }
        if (entity instanceof MobEntity) {
            return true;
        }
        return entity.getWorld().isClient() && clientUnlocked;
    }

    public static double attackSpeedMultiplier(LivingEntity entity) {
        return isActive(entity) ? ATTACK_SPEED_MULTIPLIER : 1.0;
    }
}
