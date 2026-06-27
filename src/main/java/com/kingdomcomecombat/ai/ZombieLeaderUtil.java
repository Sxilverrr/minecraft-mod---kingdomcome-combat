package com.kingdomcomecombat.ai;

import com.kingdomcomecombat.config.CombatServerConfig;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ZombieLeaderUtil {
    private static final Identifier LEADER_ZOMBIE_BONUS_MODIFIER_ID =
            Identifier.ofVanilla("leader_zombie_bonus");
    private static final Set<UUID> HEALTH_FIXED_LEADERS = new HashSet<>();

    private ZombieLeaderUtil() {
    }

    public static boolean isLeader(LivingEntity entity) {
        return entity instanceof ZombieEntity zombie
                && !zombie.isBaby()
                && hasLeaderModifier(zombie);
    }

    public static void fixInitialHealthIfEnabled(LivingEntity entity) {
        if (!CombatServerConfig.zombieLeaderHealthFixEnabled()
                || !isLeader(entity)
                || HEALTH_FIXED_LEADERS.contains(entity.getUuid())) {
            return;
        }

        float maxHealth = entity.getMaxHealth();
        if (maxHealth > 0.0F && entity.getHealth() < maxHealth) {
            entity.setHealth(maxHealth);
        }
        HEALTH_FIXED_LEADERS.add(entity.getUuid());
    }

    private static boolean hasLeaderModifier(ZombieEntity zombie) {
        EntityAttributeInstance reinforcementAttribute =
                zombie.getAttributeInstance(EntityAttributes.SPAWN_REINFORCEMENTS);
        return reinforcementAttribute != null
                && reinforcementAttribute.hasModifier(LEADER_ZOMBIE_BONUS_MODIFIER_ID);
    }
}
