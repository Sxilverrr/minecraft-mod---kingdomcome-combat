package com.kingdomcomecombat.client.animation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

import java.util.Optional;

/** Item-bone rendering for the entity-based renderer used by 1.21.1. */
public final class LegacyItemBoneTransformApplier {
    private static final float PIXEL_TO_BLOCK = 1.0F / 16.0F;

    private LegacyItemBoneTransformApplier() {
    }

    public static void apply(LivingEntity entity, Arm arm, MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        Optional<GeckoLikeAnimationLibrary.BoneTransform> transform;
        if (entity == client.player) {
            transform = arm == entity.getMainArm()
                    ? CombatAnimationClient.getLocalPlayerItemTransform()
                    : CombatAnimationClient.getLocalPlayerOffhandItemTransform();
        } else {
            if (!ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entity.getId())) return;
            transform = ClientEntityGeckoAnimationState.getItemTransform(entity.getId());
        }
        transform.ifPresent(value -> applyTransform(matrices, value));
    }

    public static void applyTransform(
            MatrixStack matrices,
            GeckoLikeAnimationLibrary.BoneTransform transform
    ) {
        transform.position().ifPresent(position -> matrices.translate(
                -position.x() * PIXEL_TO_BLOCK,
                -position.z() * PIXEL_TO_BLOCK,
                position.y() * PIXEL_TO_BLOCK
        ));
        transform.rotation().ifPresent(rotation -> {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-rotation.y()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotation.z()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-rotation.x()));
        });
    }
}
