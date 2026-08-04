package com.kingdomcomecombat.client.animation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class MobStanceHeadTargeting {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private MobStanceHeadTargeting() {
    }

    public static void apply(
            int entityId,
            LivingEntityRenderState state,
            ModelPart head,
            ModelPart animatedBody
    ) {
        apply(entityId, state, head, animatedBody, false);
    }

    public static void apply(
            int entityId,
            LivingEntityRenderState state,
            ModelPart head,
            ModelPart animatedBody,
            boolean bodyIsHeadParent
    ) {
        if (!ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entityId)) return;
        float relativeYaw = state.relativeHeadYaw;
        float pitch = state.pitch;
        MinecraftClient client = MinecraftClient.getInstance();
        int targetId = ClientEntityGeckoAnimationState.getStanceTargetEntityId(entityId);

        if (client.world != null && targetId >= 0) {
            Entity source = client.world.getEntityById(entityId);
            Entity target = client.world.getEntityById(targetId);
            if (source != null && target != null && target.isAlive()) {
                Vec3d delta = target.getEyePos().subtract(source.getEyePos());
                double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                if (horizontal > 0.0001) {
                    float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
                    relativeYaw = MathHelper.clamp(
                            MathHelper.wrapDegrees(targetYaw - state.bodyYaw),
                            -85.0F,
                            85.0F
                    );
                    pitch = MathHelper.clamp(
                            (float) -Math.toDegrees(Math.atan2(delta.y, horizontal)),
                            -70.0F,
                            70.0F
                    );
                }
            }
        }

        // The PAL "body" bone rotates the whole rendered entity after bodyYaw.
        // Move the desired look direction back through that root rotation before
        // deriving the head's local yaw/pitch, including root roll compensation.
        MobPalBodyTransformApplier.BodyPose bodyPose = MobPalBodyTransformApplier.sample(entityId);
        if (bodyPose.transformed()) {
            float yawRadians = relativeYaw * DEG_TO_RAD;
            float pitchRadians = pitch * DEG_TO_RAD;
            float horizontal = MathHelper.cos(pitchRadians);
            Vector3f look = new Vector3f(
                    MathHelper.sin(yawRadians) * horizontal,
                    -MathHelper.sin(pitchRadians),
                    MathHelper.cos(yawRadians) * horizontal
            );
            GeckoLikeAnimationLibrary.BonePose root = bodyPose.rotation();
            // The entity renderer applies scale(-1, -1, 1) after the PAL root.
            // Conjugating the root by that flip reverses its X/Y rotations but
            // not Z, so the equivalent model-space root uses +X/+Y here.
            new Quaternionf()
                    .rotateZ(root.z() * DEG_TO_RAD)
                    .rotateY(root.y() * DEG_TO_RAD)
                    .rotateX(root.x() * DEG_TO_RAD)
                    .invert()
                    .transform(look);
            double localHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
            relativeYaw = MathHelper.clamp(
                    (float) Math.toDegrees(Math.atan2(look.x, look.z)),
                    -85.0F,
                    85.0F
            );
            pitch = MathHelper.clamp(
                    (float) Math.toDegrees(Math.atan2(-look.y, localHorizontal)),
                    -70.0F,
                    70.0F
            );
        }

        float parentYaw = bodyIsHeadParent ? animatedBody.yaw : 0.0F;
        float parentPitch = bodyIsHeadParent ? animatedBody.pitch : 0.0F;
        head.yaw = relativeYaw * DEG_TO_RAD - parentYaw;
        head.pitch = pitch * DEG_TO_RAD - parentPitch;
    }
}
