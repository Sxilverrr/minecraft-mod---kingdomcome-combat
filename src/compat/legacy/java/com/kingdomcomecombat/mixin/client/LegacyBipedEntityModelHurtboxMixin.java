package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.collision.ClientCollisionTrackingPolicy;
import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import com.kingdomcomecombat.client.animation.ClientDodgeAnimationState;
import com.kingdomcomecombat.client.animation.ClientLockedMovementLeanState;
import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.ClientHitReactionState;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the final posed legacy model, matching the high-version collision path. */
@Mixin(value = BipedEntityModel.class, priority = 500)
public class LegacyBipedEntityModelHurtboxMixin<T extends LivingEntity> {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    @Shadow public net.minecraft.client.model.ModelPart head;
    @Shadow public net.minecraft.client.model.ModelPart body;
    @Shadow public net.minecraft.client.model.ModelPart rightArm;
    @Shadow public net.minecraft.client.model.ModelPart leftArm;
    @Shadow public net.minecraft.client.model.ModelPart rightLeg;
    @Shadow public net.minecraft.client.model.ModelPart leftLeg;

    @Inject(method = "setAngles", at = @At("HEAD"))
    private void kingdomcomecombat$resetAccumulatedLegacyPose(
            T entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        head.resetTransform();
        body.resetTransform();
        rightArm.resetTransform();
        leftArm.resetTransform();
        rightLeg.resetTransform();
        leftLeg.resetTransform();
    }

    @Inject(method = "setAngles", at = @At("RETURN"))
    private void kingdomcomecombat$captureFinalHurtboxes(
            T entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        // The modern BipedEntityModel feedback hook is render-state based and
        // cannot run on 1.21.1. Apply the same final-pose deltas here.
        ClientHitReactionState.BoneDelta bodyDelta =
                ClientHitReactionState.getDelta(entity.getId(), HumanoidHurtboxLibrary.Part.BODY);
        apply(body, bodyDelta);
        apply(head, parented(ClientHitReactionState.getDelta(
                entity.getId(), HumanoidHurtboxLibrary.Part.HEAD), bodyDelta, 0.55F));
        apply(leftArm, parented(ClientHitReactionState.getDelta(
                entity.getId(), HumanoidHurtboxLibrary.Part.LEFT_ARM), bodyDelta, 1.0F));
        apply(rightArm, parented(ClientHitReactionState.getDelta(
                entity.getId(), HumanoidHurtboxLibrary.Part.RIGHT_ARM), bodyDelta, 1.0F));
        apply(leftLeg, ClientHitReactionState.getDelta(
                entity.getId(), HumanoidHurtboxLibrary.Part.LEFT_LEG));
        apply(rightLeg, ClientHitReactionState.getDelta(
                entity.getId(), HumanoidHurtboxLibrary.Part.RIGHT_LEG));
        ClientDodgeAnimationState.applyBiped(
                entity.getId(), body, head, rightArm, leftArm, rightLeg, leftLeg);
        ClientLockedMovementLeanState.applyBiped(
                entity.getId(), body, head, rightArm, leftArm, rightLeg, leftLeg);

        // Newer versions overwrite the stance animation's baked head channel
        // with the entity's actual look direction. Do the same on legacy so a
        // neutral enemy does not retain the animation's small permanent tilt.
        if (ClientEntityGeckoAnimationState.isStanceOnly(entity.getId())) {
            head.yaw = headYaw * DEG_TO_RAD;
            head.pitch = headPitch * DEG_TO_RAD;
        }

        if (!ClientCollisionTrackingPolicy.shouldCaptureHurtbox(entity)
                || !ClientModelHurtboxCache.shouldCaptureRenderedPose(entity.getId())) {
            return;
        }
        ClientModelHurtboxCache.update(
                entity.getId(), head, body, rightArm, leftArm, rightLeg, leftLeg);
    }

    private static void apply(net.minecraft.client.model.ModelPart part,
                              ClientHitReactionState.BoneDelta delta) {
        // On the legacy model every limb pivot is independent. Applying the
        // modern render-state translation to each child tears the joints
        // apart; its rotational component gives the same readable reaction.
        part.pitch += delta.rotationX() * DEG_TO_RAD;
        part.yaw += delta.rotationY() * DEG_TO_RAD;
        part.roll += delta.rotationZ() * DEG_TO_RAD;
    }

    private static ClientHitReactionState.BoneDelta parented(
            ClientHitReactionState.BoneDelta child,
            ClientHitReactionState.BoneDelta parent,
            float influence) {
        return new ClientHitReactionState.BoneDelta(
                child.positionX() + parent.positionX() * influence,
                child.positionY() + parent.positionY() * influence,
                child.positionZ() + parent.positionZ() * influence,
                child.rotationX() + parent.rotationX() * influence,
                child.rotationY() + parent.rotationY() * influence,
                child.rotationZ() + parent.rotationZ() * influence
        );
    }
}
