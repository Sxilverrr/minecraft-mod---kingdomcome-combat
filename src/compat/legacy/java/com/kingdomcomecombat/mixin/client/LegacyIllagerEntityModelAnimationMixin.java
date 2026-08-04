package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.ClientDodgeAnimationState;
import com.kingdomcomecombat.client.animation.LegacyBipedAnimationApplier;
import com.kingdomcomecombat.client.animation.LegacyCombatFeedbackApplier;
import com.kingdomcomecombat.client.animation.LegacyMobStanceHeadTargeting;
import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.entity.mob.IllagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IllagerEntityModel.class)
public class LegacyIllagerEntityModelAnimationMixin {
    @Shadow private ModelPart head;
    @Shadow private ModelPart arms;
    @Shadow private ModelPart rightArm;
    @Shadow private ModelPart leftArm;
    @Shadow private ModelPart rightLeg;
    @Shadow private ModelPart leftLeg;

    @Inject(method = "setAngles", at = @At("HEAD"))
    private void kingdomcomecombat$clearPreviousCombatPose(
            IllagerEntity entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        ModelPart root = ((IllagerEntityModel<?>) (Object) this).getPart();
        ModelPart body = root.hasChild("body") ? root.getChild("body") : root;
        body.resetTransform();
        head.resetTransform();
        arms.resetTransform();
        rightArm.resetTransform();
        leftArm.resetTransform();
        rightLeg.resetTransform();
        leftLeg.resetTransform();
    }

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void kingdomcomecombat$applyLegacyCombatPose(
            IllagerEntity entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        ModelPart root = ((IllagerEntityModel<?>) (Object) this).getPart();
        ModelPart body = root.hasChild("body") ? root.getChild("body") : root;
        if (ClientEntityGeckoAnimationState.hasActiveCombatLayer(entity.getId())) {
            arms.visible = false;
            rightArm.visible = true;
            leftArm.visible = true;
            LegacyBipedAnimationApplier.applyParts(
                    entity, body, head, rightArm, leftArm, rightLeg, leftLeg);
            if (ClientEntityGeckoAnimationState.isStanceOnly(entity.getId())) {
                LegacyMobStanceHeadTargeting.apply(
                        entity, headYaw, headPitch, head, root, true);
            }
        }
        LegacyCombatFeedbackApplier.applyIllager(entity.getId(), body, head,
                rightArm, leftArm, rightLeg, leftLeg);
        ClientDodgeAnimationState.applyBiped(
                entity.getId(), body, head, rightArm, leftArm, rightLeg, leftLeg);
        ClientModelHurtboxCache.updateIllager(
                entity.getId(), head, body, rightArm, leftArm, rightLeg, leftLeg);
    }
}
