package com.kingdomcomecombat.client.animation;

import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

import java.util.Optional;

public class ItemBoneTransformApplier {
    private static final float PIXEL_TO_BLOCK = 1.0F / 16.0F;

    private ItemBoneTransformApplier() {
    }

    public static void apply(
            ArmedEntityRenderState state,
            Arm arm,
            MatrixStack matrices
    ) {
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        if (entityId < 0) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.getId() != entityId) {
            if (!ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entityId)) return;
        }

        Optional<GeckoLikeAnimationLibrary.BoneTransform> transform =
                getTransformForEntity(entityId, state.mainArm != null && arm != state.mainArm);
        if (transform.isEmpty()) {
            return;
        }

        Entity entity = client.world == null ? null : client.world.getEntityById(entityId);
        applyTransform(matrices, transform.get(), false);
    }

    private static Optional<GeckoLikeAnimationLibrary.BoneTransform> getTransformForEntity(
            int entityId,
            boolean offHand
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getId() == entityId) {
            return offHand
                    ? CombatAnimationClient.getLocalPlayerOffhandItemTransform()
                    : CombatAnimationClient.getLocalPlayerItemTransform();
        }

        if (client.world == null) {
            return Optional.empty();
        }

        Entity entity = client.world.getEntityById(entityId);
        if (entity == null) {
            return Optional.empty();
        }

        return ClientEntityGeckoAnimationState.getItemTransform(entityId);
    }

    public static void applyTransform(
            MatrixStack matrices,
            GeckoLikeAnimationLibrary.BoneTransform transform
    ) {
        applyTransform(matrices, transform, false);
    }

    public static void applyTransform(
            MatrixStack matrices,
            GeckoLikeAnimationLibrary.BoneTransform transform,
            boolean modelSpaceRotation
    ) {
        transform.position().ifPresent(position ->
                matrices.translate(
                        -position.x() * PIXEL_TO_BLOCK,
                        -position.z() * PIXEL_TO_BLOCK,
                        position.y() * PIXEL_TO_BLOCK
                )
        );

        transform.rotation().ifPresent(rotation -> {
            if (modelSpaceRotation) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation.z()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation.y()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation.x()));
                return;
            }
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-rotation.y()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotation.z()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-rotation.x()));
        });
    }
}
