package com.kingdomcomecombat.client.animation;

import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;

import java.util.HashMap;
import java.util.Map;

/** Applies PAL's renderer-level {@code body} bone to non-player entities. */
public final class MobPalBodyTransformApplier {
    private static final float PIXEL_TO_BLOCK = 1.0F / 16.0F;
    private static final float BODY_PIVOT_Y = 12.0F * PIXEL_TO_BLOCK;
    private static final Map<Integer, BodyPose> FRAME_POSES = new HashMap<>();
    private static double cachedFrameTime = Double.NaN;

    private MobPalBodyTransformApplier() {
    }

    public static void apply(LivingEntityRenderState state, MatrixStack matrices) {
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        if (entityId < 0
                || !ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entityId)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Entity entity = client.world == null ? null : client.world.getEntityById(entityId);
        // PAL already applies this transform to players in PlayerRendererMixin.
        if (entity == null || entity instanceof AbstractClientPlayerEntity) {
            return;
        }

        BodyPose pose = sample(entityId);
        if (!pose.transformed()) return;

        GeckoLikeAnimationLibrary.BonePose rotation = pose.rotation();
        GeckoLikeAnimationLibrary.BonePose position = pose.position();

        // Exact Fabric/Yarn equivalent of PAL PlayerRendererMixin:
        // translate(-x/16, y/16 + 0.75, z/16), rotate ZYX with X/Y negated,
        // then translate back from the 12-pixel body pivot.
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

    public static BodyPose sample(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return BodyPose.IDENTITY;
        double frameTime = client.world.getTime()
                + client.getRenderTickCounter().getTickProgress(false);
        if (Double.compare(frameTime, cachedFrameTime) != 0) {
            FRAME_POSES.clear();
            cachedFrameTime = frameTime;
        }
        BodyPose cached = FRAME_POSES.get(entityId);
        if (cached != null) return cached;

        GeckoLikeAnimationLibrary.BonePose rotation = GeckoLikeAnimationLibrary.BonePose.ZERO;
        GeckoLikeAnimationLibrary.BonePose position = GeckoLikeAnimationLibrary.BonePose.ZERO;
        boolean transformed = false;

        for (ClientEntityGeckoAnimationState.ActiveAnimation layer :
                ClientEntityGeckoAnimationState.getLayers(entityId)) {
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
        FRAME_POSES.put(entityId, result);
        return result;
    }

    public record BodyPose(
            GeckoLikeAnimationLibrary.BonePose rotation,
            GeckoLikeAnimationLibrary.BonePose position,
            boolean transformed
    ) {
        private static final BodyPose IDENTITY = new BodyPose(
                GeckoLikeAnimationLibrary.BonePose.ZERO,
                GeckoLikeAnimationLibrary.BonePose.ZERO,
                false
        );
    }
}
