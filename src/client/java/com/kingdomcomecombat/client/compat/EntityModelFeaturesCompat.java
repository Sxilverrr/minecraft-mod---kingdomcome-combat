package com.kingdomcomecombat.client.compat;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.function.Function;

public final class EntityModelFeaturesCompat {
    private EntityModelFeaturesCompat() {
    }

    public static void register() {
        try {
            Class<?> api = Class.forName("traben.entity_model_features.EMFAnimationApi");
            Method registerPauseCondition = api.getMethod("registerPauseCondition", Function.class);
            registerPauseCondition.invoke(null, (Function<Object, Boolean>) EntityModelFeaturesCompat::shouldPauseEmfAnimation);
            KingdomComeCombat.LOGGER.info("Registered Entity Model Features combat animation compatibility.");
        } catch (ClassNotFoundException ignored) {
            // EMF is optional; no compatibility hook is needed when it is not installed.
        } catch (ReflectiveOperationException | LinkageError e) {
            KingdomComeCombat.LOGGER.warn("Failed to register Entity Model Features compatibility.", e);
        }
    }

    private static Boolean shouldPauseEmfAnimation(Object emfEntity) {
        if (!(emfEntity instanceof Entity entity)
                || !(entity instanceof LivingEntity)) {
            return false;
        }

        if (ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entity.getId())) {
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        return entity == client.player && CombatAnimationClient.hasLocalPlayerCombatAnimation();
    }
}
