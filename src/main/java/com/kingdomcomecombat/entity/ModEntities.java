package com.kingdomcomecombat.entity;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static final Identifier HAND_CANNON_BULLET_ID = Identifier.of(KingdomComeCombat.MOD_ID, "hand_cannon_bullet");
    public static final EntityType<HandCannonBulletEntity> HAND_CANNON_BULLET = Registry.register(
            Registries.ENTITY_TYPE,
            HAND_CANNON_BULLET_ID,
            EntityType.Builder.<HandCannonBulletEntity>create(HandCannonBulletEntity::new, SpawnGroup.MISC)
                    // Short-lived projectiles are never persisted, so Minecraft must not
                    // look for a world-save DataFixer schema for this custom entity type.
                    .disableSaving()
                    .dimensions(0.18F, 0.18F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, HAND_CANNON_BULLET_ID))
    );

    private ModEntities() {
    }

    public static void register() {
        // Class loading registers the entity type.
    }
}
