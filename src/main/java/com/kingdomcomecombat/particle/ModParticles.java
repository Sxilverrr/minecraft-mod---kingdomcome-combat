package com.kingdomcomecombat.particle;

import com.kingdomcomecombat.KingdomComeCombat;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {
    public static final SimpleParticleType COMBAT_SPARK = FabricParticleTypes.simple();
    public static final SimpleParticleType BLOOD_SPARK = FabricParticleTypes.simple();
    public static final SimpleParticleType BLOOD_DROP = FabricParticleTypes.simple();
    public static final SimpleParticleType BLOOD_MIST = FabricParticleTypes.simple();

    private ModParticles() {
    }

    public static void registerAll() {
        Registry.register(
                Registries.PARTICLE_TYPE,
                Identifier.of(KingdomComeCombat.MOD_ID, "combat_spark"),
                COMBAT_SPARK
        );
        Registry.register(
                Registries.PARTICLE_TYPE,
                Identifier.of(KingdomComeCombat.MOD_ID, "blood_spark"),
                BLOOD_SPARK
        );
        Registry.register(
                Registries.PARTICLE_TYPE,
                Identifier.of(KingdomComeCombat.MOD_ID, "blood_drop"),
                BLOOD_DROP
        );
        Registry.register(
                Registries.PARTICLE_TYPE,
                Identifier.of(KingdomComeCombat.MOD_ID, "blood_mist"),
                BLOOD_MIST
        );
    }
}
