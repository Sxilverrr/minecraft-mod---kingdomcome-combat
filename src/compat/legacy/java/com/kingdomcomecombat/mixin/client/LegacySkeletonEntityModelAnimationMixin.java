package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.LegacyBipedAnimationApplier;
import com.kingdomcomecombat.client.animation.LegacyCombatFeedbackApplier;
import com.kingdomcomecombat.client.animation.LegacyMobStanceHeadTargeting;
import net.minecraft.client.render.entity.model.SkeletonEntityModel;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonEntityModel.class)
public class LegacySkeletonEntityModelAnimationMixin {
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
        if (!ClientEntityGeckoAnimationState.hasActiveCombatLayer(entity.getId())) {
            return;
        }
        net.minecraft.client.render.entity.model.BipedEntityModel model =
                (net.minecraft.client.render.entity.model.BipedEntityModel) (Object) this;
        LegacyBipedAnimationApplier.apply(model, entity);
        if (ClientEntityGeckoAnimationState.isStanceOnly(entity.getId())) {
            LegacyMobStanceHeadTargeting.apply(
                    entity, headYaw, headPitch, model.head, model.body);
        }
        LegacyCombatFeedbackApplier.applyBipedExtra(entity.getId(), model.body, model.head,
                model.rightArm, model.leftArm, model.rightLeg, model.leftLeg);
    }
}
