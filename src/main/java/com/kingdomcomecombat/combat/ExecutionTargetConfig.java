package com.kingdomcomecombat.combat;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public final class ExecutionTargetConfig {
    private static final Set<Identifier> EXTRA_EXECUTABLE_TYPES = new HashSet<>();

    private ExecutionTargetConfig() {
    }

    public static void reset() {
        EXTRA_EXECUTABLE_TYPES.clear();
        EXTRA_EXECUTABLE_TYPES.add(Identifier.ofVanilla("creeper"));
    }

    public static void add(Identifier entityTypeId) {
        if (entityTypeId != null) {
            EXTRA_EXECUTABLE_TYPES.add(entityTypeId);
        }
    }

    public static boolean isExtraExecutable(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return EXTRA_EXECUTABLE_TYPES.contains(Registries.ENTITY_TYPE.getId(entity.getType()));
    }

    public static boolean isExtraExecutable(EntityType<?> entityType) {
        return entityType != null && EXTRA_EXECUTABLE_TYPES.contains(Registries.ENTITY_TYPE.getId(entityType));
    }

    public static Set<String> snapshot() {
        return EXTRA_EXECUTABLE_TYPES.stream().map(Identifier::toString).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static void replace(Set<String> entityTypeIds) {
        EXTRA_EXECUTABLE_TYPES.clear();
        if (entityTypeIds == null) return;
        entityTypeIds.stream().map(Identifier::tryParse).filter(java.util.Objects::nonNull)
                .forEach(EXTRA_EXECUTABLE_TYPES::add);
    }
}
