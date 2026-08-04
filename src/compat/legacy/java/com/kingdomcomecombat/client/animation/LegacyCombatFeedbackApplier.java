package com.kingdomcomecombat.client.animation;

import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import net.minecraft.client.model.ModelPart;

/** Mirrors the extra feedback pass used by high-version entity model mixins. */
public final class LegacyCombatFeedbackApplier {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private LegacyCombatFeedbackApplier() {
    }

    public static void applyBipedExtra(
            int entityId, ModelPart body, ModelPart head,
            ModelPart rightArm, ModelPart leftArm,
            ModelPart rightLeg, ModelPart leftLeg
    ) {
        ClientHitReactionState.BoneDelta bodyDelta = delta(entityId, HumanoidHurtboxLibrary.Part.BODY);
        applyRotation(body, bodyDelta);
        applyRotation(head, parented(delta(entityId, HumanoidHurtboxLibrary.Part.HEAD), bodyDelta, 0.55F));
        applyRotation(rightArm, parented(delta(entityId, HumanoidHurtboxLibrary.Part.RIGHT_ARM), bodyDelta, 1.0F));
        applyRotation(leftArm, parented(delta(entityId, HumanoidHurtboxLibrary.Part.LEFT_ARM), bodyDelta, 1.0F));
        applyRotation(rightLeg, delta(entityId, HumanoidHurtboxLibrary.Part.RIGHT_LEG));
        applyRotation(leftLeg, delta(entityId, HumanoidHurtboxLibrary.Part.LEFT_LEG));
        ClientDodgeAnimationState.applyBiped(entityId, body, head, rightArm, leftArm, rightLeg, leftLeg);
        ClientLockedMovementLeanState.applyBiped(entityId, body, head, rightArm, leftArm, rightLeg, leftLeg);
    }

    public static void applyIllager(
            int entityId, ModelPart body, ModelPart head,
            ModelPart rightArm, ModelPart leftArm,
            ModelPart rightLeg, ModelPart leftLeg
    ) {
        ClientHitReactionState.BoneDelta bodyDelta = delta(entityId, HumanoidHurtboxLibrary.Part.BODY);
        apply(head, parented(delta(entityId, HumanoidHurtboxLibrary.Part.HEAD), bodyDelta, 0.55F));
        apply(rightArm, parented(delta(entityId, HumanoidHurtboxLibrary.Part.RIGHT_ARM), bodyDelta, 0.85F));
        apply(leftArm, parented(delta(entityId, HumanoidHurtboxLibrary.Part.LEFT_ARM), bodyDelta, 0.85F));
        apply(rightLeg, delta(entityId, HumanoidHurtboxLibrary.Part.RIGHT_LEG));
        apply(leftLeg, delta(entityId, HumanoidHurtboxLibrary.Part.LEFT_LEG));
        ClientDodgeAnimationState.applyBiped(entityId, body, head, rightArm, leftArm, rightLeg, leftLeg);
        ClientLockedMovementLeanState.applyBiped(entityId, body, head, rightArm, leftArm, rightLeg, leftLeg);
    }

    private static ClientHitReactionState.BoneDelta delta(int entityId, HumanoidHurtboxLibrary.Part part) {
        return ClientHitReactionState.getDelta(entityId, part);
    }

    private static void apply(ModelPart part, ClientHitReactionState.BoneDelta delta) {
        part.pivotX += delta.positionX();
        part.pivotY += delta.positionY();
        part.pivotZ += delta.positionZ();
        applyRotation(part, delta);
    }

    private static void applyRotation(ModelPart part, ClientHitReactionState.BoneDelta delta) {
        part.pitch += delta.rotationX() * DEG_TO_RAD;
        part.yaw += delta.rotationY() * DEG_TO_RAD;
        part.roll += delta.rotationZ() * DEG_TO_RAD;
    }

    private static ClientHitReactionState.BoneDelta parented(
            ClientHitReactionState.BoneDelta child,
            ClientHitReactionState.BoneDelta parent,
            float influence
    ) {
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
