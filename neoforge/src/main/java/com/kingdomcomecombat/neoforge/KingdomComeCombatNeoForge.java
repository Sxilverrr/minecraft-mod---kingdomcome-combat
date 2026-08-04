package com.kingdomcomecombat.neoforge;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.client.KingdomComeCombatClient;
import com.kingdomcomecombat.client.config.KccConfigScreen;
import com.kingdomcomecombat.client.input.CombatKeyBindings;
import com.kingdomcomecombat.client.particle.BloodDropParticle;
import com.kingdomcomecombat.client.particle.BloodMistParticle;
import com.kingdomcomecombat.client.particle.BloodSparkParticle;
import com.kingdomcomecombat.client.particle.BloodTraceParticle;
import com.kingdomcomecombat.client.particle.CombatSparkParticle;
import com.kingdomcomecombat.client.render.HandCannonBulletRenderer;
import com.kingdomcomecombat.entity.ModEntities;
import com.kingdomcomecombat.particle.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;

/** NeoForge loader boundary; gameplay remains in the shared source tree. */
@Mod(KingdomComeCombat.MOD_ID)
public final class KingdomComeCombatNeoForge {
    public KingdomComeCombatNeoForge(IEventBus modBus, ModContainer modContainer) {
        // Shared Fabric-style registrations need only KCC's target registries writable.
        // Do not call GameData.unfreezeData() here: other mods (notably Kiwi) inject
        // lifecycle work into that global method and would register their objects too
        // early while this mod is still being constructed.
        unfreezeKccRegistries();
        new KingdomComeCombat().onInitialize();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Forgified Fabric API seals its key-binding registry before
            // FMLClientSetupEvent deferred work runs, so keys must be added now.
            registerEarlyClientComponents();
            modContainer.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (IConfigScreenFactory) (container, parent) -> KccConfigScreen.create(parent)
            );
            modBus.addListener(this::initializeClient);
        }
    }

    private static void unfreezeKccRegistries() {
        unfreeze(BuiltInRegistries.MOB_EFFECT);
        unfreeze(BuiltInRegistries.SOUND_EVENT);
        unfreeze(BuiltInRegistries.PARTICLE_TYPE);
        unfreeze(BuiltInRegistries.ENTITY_TYPE);
        unfreeze(BuiltInRegistries.RECIPE_SERIALIZER);
        unfreeze(BuiltInRegistries.ITEM);
    }

    private static void unfreeze(net.minecraft.core.Registry<?> registry) {
        if (registry instanceof MappedRegistry<?> mappedRegistry) {
            mappedRegistry.unfreeze();
        }
    }

    private static void registerEarlyClientComponents() {
        CombatKeyBindings.register();
        ParticleFactoryRegistry.getInstance().register(ModParticles.COMBAT_SPARK, CombatSparkParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.BLOOD_SPARK, BloodSparkParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.BLOOD_DROP, BloodDropParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.BLOOD_MIST, BloodMistParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.BLOOD_TRACE, BloodTraceParticle.Factory::new);
        EntityRendererRegistry.register(ModEntities.HAND_CANNON_BULLET, HandCannonBulletRenderer::new);
    }

    private void initializeClient(FMLClientSetupEvent event) {
        event.enqueueWork(() -> new KingdomComeCombatClient().onInitializeClient());
    }
}
