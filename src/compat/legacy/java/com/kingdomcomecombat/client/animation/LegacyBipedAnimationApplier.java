package com.kingdomcomecombat.client.animation;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

/** Applies the shared data-driven combat poses to the pre-render-state model API. */
public final class LegacyBipedAnimationApplier {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private LegacyBipedAnimationApplier() {
    }

    public static void apply(BipedEntityModel<? extends LivingEntity> model, LivingEntity entity) {
        for (ClientEntityGeckoAnimationState.ActiveAnimation layer
                : ClientEntityGeckoAnimationState.getLayers(entity.getId())) {
            GeckoLikeAnimationLibrary.BoneTransform body = sample(layer, "body");
            apply(model.body, body, layer.weight());
            apply(model.head, sample(layer, "head"), layer.weight());
            apply(model.rightArm, sample(layer, "rightArm"), layer.weight());
            apply(model.leftArm, sample(layer, "leftArm"), layer.weight());
            apply(model.rightLeg, sample(layer, "rightLeg"), layer.weight());
            apply(model.leftLeg, sample(layer, "leftLeg"), layer.weight());
        }
    }

    public static void applyParts(
            LivingEntity entity,
            ModelPart body,
            ModelPart head,
            ModelPart rightArm,
            ModelPart leftArm,
            ModelPart rightLeg,
            ModelPart leftLeg
    ) {
        for (ClientEntityGeckoAnimationState.ActiveAnimation layer
                : ClientEntityGeckoAnimationState.getLayers(entity.getId())) {
            apply(body, sample(layer, "body"), layer.weight());
            apply(head, sample(layer, "head"), layer.weight());
            apply(rightArm, sample(layer, "rightArm"), layer.weight());
            apply(leftArm, sample(layer, "leftArm"), layer.weight());
            apply(rightLeg, sample(layer, "rightLeg"), layer.weight());
            apply(leftLeg, sample(layer, "leftLeg"), layer.weight());
        }
    }

    private static GeckoLikeAnimationLibrary.BoneTransform sample(
            ClientEntityGeckoAnimationState.ActiveAnimation layer,
            String bone
    ) {
        if (!layer.customAnimationName().isBlank()) {
            return GeckoLikeAnimationLibrary.sampleNamedBone(
                    layer.customAnimationName(), bone, layer.elapsedSeconds());
        }
        return GeckoLikeAnimationLibrary.sampleBone(
                layer.kind(), layer.direction(), bone, layer.elapsedSeconds());
    }

    private static void apply(
            ModelPart part,
            GeckoLikeAnimationLibrary.BoneTransform transform,
            float weight
    ) {
        ModelTransform defaults = part.getDefaultTransform();
        transform.rotation().ifPresent(pose -> {
            part.pitch = lerp(part.pitch, defaults.pitch + pose.x() * DEG_TO_RAD, weight);
            part.yaw = lerp(part.yaw, defaults.yaw + pose.y() * DEG_TO_RAD, weight);
            part.roll = lerp(part.roll, defaults.roll + pose.z() * DEG_TO_RAD, weight);
        });
        transform.position().ifPresent(pose -> {
            part.pivotX = lerp(part.pivotX, defaults.pivotX + pose.x(), weight);
            part.pivotY = lerp(part.pivotY, defaults.pivotY + pose.y(), weight);
            part.pivotZ = lerp(part.pivotZ, defaults.pivotZ + pose.z(), weight);
        });
    }

    private static float lerp(float from, float to, float weight) {
        return from + (to - from) * Math.max(0.0F, Math.min(1.0F, weight));
    }
}
