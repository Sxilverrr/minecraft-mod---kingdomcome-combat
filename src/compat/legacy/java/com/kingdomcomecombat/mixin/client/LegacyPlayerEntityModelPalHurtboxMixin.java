package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.collision.ClientCollisionTrackingPolicy;
import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the 1.21.1 player pose after PAL's PlayerEntityModel return hook. */
@Mixin(value = PlayerEntityModel.class, priority = 500)
public abstract class LegacyPlayerEntityModelPalHurtboxMixin {
    @Inject(method = "setAngles", at = @At("RETURN"))
    private void kingdomcomecombat$capturePalFinalPose(
            LivingEntity entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!ClientCollisionTrackingPolicy.shouldCaptureHurtbox(entity)
                || !ClientModelHurtboxCache.shouldCaptureRenderedPose(entity.getId())) {
            return;
        }
        BipedEntityModel<?> model = (BipedEntityModel<?>) (Object) this;
        ClientModelHurtboxCache.update(
                entity.getId(), model.head, model.body,
                model.rightArm, model.leftArm, model.rightLeg, model.leftLeg
        );
    }
}
