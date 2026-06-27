package com.kingdomcomecombat.equipment;

import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class ProjectileCombatAttributesRegistry {
    private static final Map<Identifier, DamageTypeProfile> PROJECTILES = new HashMap<>();

    private ProjectileCombatAttributesRegistry() {
    }

    public static void clear() {
        PROJECTILES.clear();
    }

    public static void register(Identifier entityTypeId, DamageTypeProfile profile) {
        PROJECTILES.put(entityTypeId, profile);
    }

    public static DamageTypeProfile get(Entity projectile) {
        Identifier id = Registries.ENTITY_TYPE.getId(projectile.getType());
        return PROJECTILES.getOrDefault(id, defaultProfile(id));
    }

    private static DamageTypeProfile defaultProfile(Identifier id) {
        String path = id.getPath();
        if (path.contains("arrow") || path.contains("trident") || path.contains("spear")) {
            return new DamageTypeProfile(1.0, 0.0, 0.0);
        }

        return DamageTypeProfile.even(0.0);
    }
}
