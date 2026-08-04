package com.kingdomcomecombat.client.collision;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.GeckoLikeAnimationLibrary;
import com.kingdomcomecombat.client.config.ClientServerConfigState;
import com.kingdomcomecombat.collision.HumanoidAnimationPoseLibrary;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

/**
 * Resolves combat hurtboxes independently from the entity's visual animation backend.
 * Player Animation Library, GeckoLib and vanilla models may all render an entity, while
 * synchronized combat state remains the authoritative source for animated hurtboxes.
 */
public final class ClientHumanoidHurtboxResolver {
    private ClientHumanoidHurtboxResolver() {
    }

    public static List<HumanoidHurtboxLibrary.PartBox> resolve(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity == client.player) {
            if (!ClientServerConfigState.legacyCollisionCalculationEnabled()) {
                Optional<List<HumanoidHurtboxLibrary.PartBox>> rendered =
                        ClientModelHurtboxCache.getRenderedPlayer(entity);
                if (rendered.isPresent()) {
                    return rendered.get();
                }
            }
            return ClientModelHurtboxCache.simulateLocalPlayer(entity);
        }

        if (HumanoidHurtboxLibrary.isHumanoidTarget(entity)) {
            ClientEntityGeckoAnimationState.ActiveAnimation animation =
                    ClientEntityGeckoAnimationState.get(entity.getId());
            if (ClientServerConfigState.legacyCollisionCalculationEnabled()) {
                List<HumanoidHurtboxLibrary.PartBox> animated = resolveAnimation(entity, animation);
                if (animated != null) {
                    return animated;
                }
            }
        }

        Optional<List<HumanoidHurtboxLibrary.PartBox>> modelBoxes =
                ClientModelHurtboxCache.get(entity);
        if (modelBoxes.isPresent()) {
            return modelBoxes.get();
        }

        if (HumanoidHurtboxLibrary.isHumanoidTarget(entity)) {
            List<HumanoidHurtboxLibrary.PartBox> animated = resolveAnimation(
                    entity, ClientEntityGeckoAnimationState.get(entity.getId()));
            if (animated != null) {
                return animated;
            }
        }

        return HumanoidHurtboxLibrary.getHurtboxes(entity);
    }

    private static List<HumanoidHurtboxLibrary.PartBox> resolveAnimation(
            LivingEntity entity,
            ClientEntityGeckoAnimationState.ActiveAnimation animation
    ) {
        if (animation == null) {
            return null;
        }

        HumanoidAnimationPoseLibrary.Kind poseKind;
        if (animation.kind() == GeckoLikeAnimationLibrary.Kind.ATTACK) {
            poseKind = HumanoidAnimationPoseLibrary.Kind.ATTACK;
        } else if (animation.kind() == GeckoLikeAnimationLibrary.Kind.STANCE) {
            poseKind = HumanoidAnimationPoseLibrary.Kind.STANCE;
        } else {
            return null;
        }

        return HumanoidHurtboxLibrary.getAnimatedHurtboxes(
                entity,
                poseKind,
                animation.direction(),
                animation.elapsedSeconds()
        );
    }
}
