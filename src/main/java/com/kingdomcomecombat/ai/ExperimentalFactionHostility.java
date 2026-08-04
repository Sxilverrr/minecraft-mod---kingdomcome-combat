package com.kingdomcomecombat.ai;

import com.kingdomcomecombat.config.CombatServerConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Comparator;
import java.util.Set;

public final class ExperimentalFactionHostility {
    private static final double RANGE = 24.0;
    private static final Set<EntityType<?>> ILLAGERS = Set.of(
            EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
            EntityType.ILLUSIONER, EntityType.RAVAGER, EntityType.WITCH
    );
    private static final Set<EntityType<?>> UNDEAD = Set.of(
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
            EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED, EntityType.WITHER_SKELETON,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.PHANTOM
    );
    private static int ticks;
    private ExperimentalFactionHostility() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!CombatServerConfig.experimentalIllagerUndeadHostilityEnabled() || ++ticks % 10 != 0) return;
            for (ServerWorld world : server.getWorlds()) {
                for (var entity : world.iterateEntities()) {
                    if (entity instanceof MobEntity mob) {
                        LivingEntity current = mob.getTarget();
                        if (current instanceof MobEntity target
                                && areExperimentalEnemies(mob, target)
                                && !mob.getVisibilityCache().canSee(target)) {
                            mob.setTarget(null);
                        }
                        if (!needsTarget(mob)) continue;
                        if (ILLAGERS.contains(mob.getType())) {
                            findNearest(world, mob, UNDEAD).ifPresent(mob::setTarget);
                        } else if (UNDEAD.contains(mob.getType())) {
                            findNearest(world, mob, ILLAGERS).ifPresent(mob::setTarget);
                        }
                    }
                }
            }
        });
    }

    private static boolean needsTarget(MobEntity mob) {
        return mob.getTarget() == null || !mob.getTarget().isAlive();
    }

    private static java.util.Optional<MobEntity> findNearest(
            ServerWorld world, MobEntity source, Set<EntityType<?>> types) {
        return world.getEntitiesByClass(MobEntity.class, source.getBoundingBox().expand(RANGE),
                        target -> target.isAlive() && target != source && types.contains(target.getType())
                                && source.getVisibilityCache().canSee(target))
                .stream().min(Comparator.comparingDouble(source::squaredDistanceTo));
    }

    private static boolean areExperimentalEnemies(MobEntity first, MobEntity second) {
        return (ILLAGERS.contains(first.getType()) && UNDEAD.contains(second.getType()))
                || (UNDEAD.contains(first.getType()) && ILLAGERS.contains(second.getType()));
    }
}
