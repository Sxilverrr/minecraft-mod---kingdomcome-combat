package com.kingdomcomecombat.ai;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class BeastCombatAiProfiles {
    private static final Map<EntityType<?>, BeastCombatAiProfile> PROFILES = new HashMap<>();

    private BeastCombatAiProfiles() {
    }

    public static void clear() {
        PROFILES.clear();
    }

    public static void register(Identifier entityId, BeastCombatAiProfile profile) {
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(entityId);
        if (entityType != null) {
            PROFILES.put(entityType, profile);
        }
    }

    public static BeastCombatAiProfile getProfile(MobEntity mob) {
        if (mob == null) {
            return null;
        }
        return PROFILES.get(mob.getType());
    }
}
