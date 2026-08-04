package com.kingdomcomecombat.ai;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class BeastCombatAiProfiles {
    private static final Map<EntityType<?>, BeastCombatAiProfile> PROFILES = new HashMap<>();

    private BeastCombatAiProfiles() {
    }

    public static void clear() {
        PROFILES.clear();
    }

    public static void register(Identifier entityId, BeastCombatAiProfile profile) {
        if (!Registries.ENTITY_TYPE.containsId(entityId)) {
            return;
        }
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(entityId);
        PROFILES.put(entityType, profile);
    }

    public static BeastCombatAiProfile getProfile(MobEntity mob) {
        if (mob == null) {
            return null;
        }
        return PROFILES.get(mob.getType());
    }

    public static Set<String> configuredEntityIds() {
        return PROFILES.keySet().stream().map(Registries.ENTITY_TYPE::getId)
                .map(Object::toString).collect(Collectors.toUnmodifiableSet());
    }
}
