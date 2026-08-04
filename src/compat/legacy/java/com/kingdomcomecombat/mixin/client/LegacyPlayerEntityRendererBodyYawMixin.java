package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.client.lockon.LockOnTargetSelector;
import com.kingdomcomecombat.combat.CombatItemUtil;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.21.1 equivalent of reducing {@code PlayerEntityRenderState.bodyYaw}. */
@Mixin(LivingEntityRenderer.class)
public class LegacyPlayerEntityRendererBodyYawMixin {
    private static final float LOCKED_BODY_YAW_FACTOR = 0.25F;
    private static final float LOCKED_HEAD_YAW_FACTOR = 0.25F;
    private static final float LOCKED_HEAD_PITCH_FACTOR = 0.9F;

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F",
                    ordinal = 0
            )
    )
    private float kingdomComeCombat$limitLockedBodyYaw(
            float bodyYaw,
            LivingEntity renderedEntity,
            float ignoredDispatcherYaw,
            float tickDelta
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null
                || renderedEntity != client.player
                || FirstPersonRenderCompat.isExternalBodyRenderOrPreparing()
                || !LockOnState.locked
                || !CombatItemUtil.hasCombatWeapon(client.player)) {
            return bodyYaw;
        }

        float viewYaw = client.player.getYaw(tickDelta);
        float offset = MathHelper.wrapDegrees(bodyYaw - viewYaw);
        return viewYaw + offset * LOCKED_BODY_YAW_FACTOR;
    }

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F",
                    ordinal = 1
            )
    )
    private float kingdomComeCombat$lockHeadYaw(
            float absoluteHeadYaw,
            LivingEntity renderedEntity,
            float ignoredDispatcherYaw,
            float tickDelta
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!shouldAdjust(client, renderedEntity)) return absoluteHeadYaw;

        float bodyYaw = adjustedBodyYaw(client, tickDelta);
        float relativeHeadYaw = MathHelper.wrapDegrees(absoluteHeadYaw - bodyYaw);
        Float targetYaw = hardLockTargetYaw(client, tickDelta);
        if (targetYaw != null) {
            relativeHeadYaw = MathHelper.clamp(
                    MathHelper.wrapDegrees(targetYaw - bodyYaw
                            - CombatAnimationClient.getLocalStanceBodyYawOffsetDegrees()),
                    -85.0F,
                    85.0F
            );
        } else if (LockOnState.isSoftLocked()) {
            relativeHeadYaw = MathHelper.clamp(
                    MathHelper.wrapDegrees(client.player.getYaw(tickDelta) - bodyYaw
                            - CombatAnimationClient.getLocalStanceBodyYawOffsetDegrees()),
                    -85.0F,
                    85.0F
            );
        } else {
            relativeHeadYaw *= LOCKED_HEAD_YAW_FACTOR;
        }
        return bodyYaw + relativeHeadYaw;
    }

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F",
                    ordinal = 0
            )
    )
    private float kingdomComeCombat$lockHeadPitch(
            float pitch,
            LivingEntity renderedEntity,
            float ignoredDispatcherYaw,
            float tickDelta
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!shouldAdjust(client, renderedEntity)) return pitch;

        Float targetPitch = hardLockTargetPitch(client, tickDelta);
        if (targetPitch != null) return targetPitch;
        if (LockOnState.isSoftLocked()) {
            return MathHelper.clamp(client.player.getPitch(tickDelta), -70.0F, 70.0F);
        }
        return pitch * LOCKED_HEAD_PITCH_FACTOR;
    }

    private static boolean shouldAdjust(MinecraftClient client, LivingEntity renderedEntity) {
        return client.player != null
                && renderedEntity == client.player
                && !FirstPersonRenderCompat.isExternalBodyRenderOrPreparing()
                && LockOnState.locked
                && CombatItemUtil.hasCombatWeapon(client.player);
    }

    private static float adjustedBodyYaw(MinecraftClient client, float tickDelta) {
        float bodyYaw = MathHelper.lerpAngleDegrees(
                tickDelta, client.player.prevBodyYaw, client.player.bodyYaw);
        float viewYaw = client.player.getYaw(tickDelta);
        return viewYaw + MathHelper.wrapDegrees(bodyYaw - viewYaw) * LOCKED_BODY_YAW_FACTOR;
    }

    private static Float hardLockTargetYaw(MinecraftClient client, float tickDelta) {
        Vec3d delta = hardLockTargetDelta(client, tickDelta);
        return delta == null ? null
                : (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
    }

    private static Float hardLockTargetPitch(MinecraftClient client, float tickDelta) {
        Vec3d delta = hardLockTargetDelta(client, tickDelta);
        if (delta == null) return null;
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        return MathHelper.clamp(
                (float) -Math.toDegrees(Math.atan2(delta.y, horizontal)), -70.0F, 70.0F);
    }

    private static Vec3d hardLockTargetDelta(MinecraftClient client, float tickDelta) {
        if (!LockOnState.isHardLocked()
                || CombatClientState.attacking
                || client.world == null
                || LockOnState.targetEntityId < 0) return null;
        net.minecraft.entity.Entity target = client.world.getEntityById(LockOnState.targetEntityId);
        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) return null;
        Vec3d delta = LockOnTargetSelector.getTargetLockPoint(livingTarget)
                .subtract(client.player.getCameraPosVec(tickDelta));
        return delta.x * delta.x + delta.z * delta.z <= 0.00000001 ? null : delta;
    }
}
