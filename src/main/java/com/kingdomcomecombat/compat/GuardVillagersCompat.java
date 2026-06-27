package com.kingdomcomecombat.compat;

import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class GuardVillagersCompat {
    private static final Identifier GUARD_ENTITY_ID = Identifier.of("guardvillagers", "guard");

    private GuardVillagersCompat() {
    }

    public static boolean isGuard(Entity entity) {
        return entity != null && GUARD_ENTITY_ID.equals(Registries.ENTITY_TYPE.getId(entity.getType()));
    }
}
