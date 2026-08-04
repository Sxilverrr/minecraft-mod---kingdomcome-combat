package com.kingdomcomecombat.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class CreeperExplosionSuppressor {
    private static final Map<UUID, Integer> SUPPRESSED = new HashMap<>();

    private CreeperExplosionSuppressor() {
    }

    public static void suppress(LivingEntity entity, int ticks) {
        if (!(entity instanceof CreeperEntity creeper)) {
            return;
        }
        SUPPRESSED.merge(creeper.getUuid(), Math.max(1, ticks), Math::max);
        creeper.setFuseSpeed(-1);
    }

    public static boolean consumeTick(CreeperEntity creeper) {
        Integer ticks = SUPPRESSED.get(creeper.getUuid());
        if (ticks == null || ticks <= 0) {
            return false;
        }
        if (ticks <= 1) {
            SUPPRESSED.remove(creeper.getUuid());
        } else {
            SUPPRESSED.put(creeper.getUuid(), ticks - 1);
        }
        return true;
    }

    public static void clear(UUID uuid) {
        SUPPRESSED.remove(uuid);
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, Integer>> iterator = SUPPRESSED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            if (entry.getValue() <= 0) {
                iterator.remove();
            }
        }
    }
}
