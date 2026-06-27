package com.kingdomcomecombat.client.animation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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

        float parentYaw = bodyIsHeadParent ? animatedBody.yaw : 0.0F;
        float parentPitch = bodyIsHeadParent ? animatedBody.pitch : 0.0F;
        head.yaw = relativeYaw * DEG_TO_RAD - parentYaw;
        head.pitch = pitch * DEG_TO_RAD - parentPitch;
    }
}
