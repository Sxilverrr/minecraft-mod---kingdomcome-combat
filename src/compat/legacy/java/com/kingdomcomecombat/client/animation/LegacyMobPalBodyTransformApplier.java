package com.kingdomcomecombat.client.animation;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;

import java.util.HashMap;
import java.util.Map;

/** Applies PAL's renderer-level {@code body} bone on the entity renderer used before 1.21.2. */
public final class LegacyMobPalBodyTransformApplier {
    private static final float PIXEL_TO_BLOCK = 1.0F / 16.0F;
    private static final float BODY_PIVOT_Y = 12.0F * PIXEL_TO_BLOCK;
    private static final Map<Integer, BodyPose> FRAME_POSES = new HashMap<>();
    private static double cachedFrameTime = Double.NaN;

    private LegacyMobPalBodyTransformApplier() {
    }

    public static void apply(LivingEntity entity, MatrixStack matrices) {
        if (entity instanceof AbstractClientPlayerEntity
                || !ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entity.getId())) {
            return;
        }

        BodyPose pose = sample(entity);
        if (!pose.transformed()) return;

        GeckoLikeAnimationLibrary.BonePose rotation = pose.rotation();
        GeckoLikeAnimationLibrary.BonePose position = pose.position();
        matrices.translate(
                -position.x() * PIXEL_TO_BLOCK,
                position.y() * PIXEL_TO_BLOCK + BODY_PIVOT_Y,
                position.z() * PIXEL_TO_BLOCK
        );
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation.z()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotation.y()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-rotation.x()));
        matrices.translate(0.0F, -BODY_PIVOT_Y, 0.0F);
    }

    public static BodyPose sample(LivingEntity entity) {
        double frameTime = entity.getWorld().getTime()
                + net.minecraft.client.MinecraftClient.getInstance()
                .getRenderTickCounter().getTickDelta(false);
        if (Double.compare(frameTime, cachedFrameTime) != 0) {
            FRAME_POSES.clear();
            cachedFrameTime = frameTime;
        }
        BodyPose cached = FRAME_POSES.get(entity.getId());
        if (cached != null) return cached;

        GeckoLikeAnimationLibrary.BonePose rotation = GeckoLikeAnimationLibrary.BonePose.ZERO;
        GeckoLikeAnimationLibrary.BonePose position = GeckoLikeAnimationLibrary.BonePose.ZERO;
        boolean transformed = false;
        for (ClientEntityGeckoAnimationState.ActiveAnimation layer :
                ClientEntityGeckoAnimationState.getLayers(entity.getId())) {
            GeckoLikeAnimationLibrary.BoneTransform body = layer.customAnimationName().isBlank()
                    ? GeckoLikeAnimationLibrary.sampleBone(
                    layer.kind(), layer.direction(), "body", layer.elapsedSeconds())
                    : GeckoLikeAnimationLibrary.sampleNamedBone(
                    layer.customAnimationName(), "body", layer.elapsedSeconds());
            if (body.rotation().isPresent()) {
                rotation = rotation.lerp(body.rotation().get(), layer.weight());
                transformed = true;
            }
            if (body.position().isPresent()) {
                position = position.lerp(body.position().get(), layer.weight());
                transformed = true;
            }
        }
        BodyPose result = new BodyPose(rotation, position, transformed);
        FRAME_POSES.put(entity.getId(), result);
        return result;
    }

    public record BodyPose(
            GeckoLikeAnimationLibrary.BonePose rotation,
            GeckoLikeAnimationLibrary.BonePose position,
            boolean transformed
    ) {
    }
}
