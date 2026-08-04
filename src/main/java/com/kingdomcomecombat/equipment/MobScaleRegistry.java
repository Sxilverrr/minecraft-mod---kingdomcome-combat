package com.kingdomcomecombat.equipment;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

public final class MobScaleRegistry {
    private static final Map<EntityType<?>, Double> SCALES = new HashMap<>();
    private MobScaleRegistry() {}
    public static void clear() { SCALES.clear(); }
    public static void register(Identifier id, double scale) {
        Registries.ENTITY_TYPE.getOptionalValue(id).ifPresent(type -> SCALES.put(type, Math.max(0.1, scale)));
    }
    public static double get(EntityType<?> type) { return SCALES.getOrDefault(type, 1.0); }
}
