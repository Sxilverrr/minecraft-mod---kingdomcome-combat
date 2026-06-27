package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.LegacyBipedAnimationApplier;
import net.minecraft.client.render.entity.model.PiglinEntityModel;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinEntityModel.class)
public class LegacyPiglinEntityModelAnimationMixin {
    @Inject(method = "setAngles", at = @At("TAIL"))
    private void kingdomcomecombat$applyLegacyCombatPose(
            MobEntity entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (ClientEntityGeckoAnimationState.hasActiveCombatLayer(entity.getId())) {
            LegacyBipedAnimationApplier.apply(
                    (net.minecraft.client.render.entity.model.BipedEntityModel) (Object) this,
                    entity
            );
        }
    }
}
