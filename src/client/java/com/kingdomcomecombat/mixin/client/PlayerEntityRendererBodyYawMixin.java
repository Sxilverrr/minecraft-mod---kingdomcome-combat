package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.client.lockon.LockOnTargetSelector;
import com.kingdomcomecombat.combat.CombatItemUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 锁定状态下减少身体和头部的左右转动幅度。
 *
 * 注意：
 * 不直接改 player.bodyYaw / player.headYaw，
 * 只改当前帧的 RenderState，避免和原版、FirstPersonModel、PAL 抢状态导致抖动。
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererBodyYawMixin {
    /**
     * 身体侧向转动保留比例。
     * 0.25 = 原版的 1/4。
     */
    private static final float LOCKED_BODY_YAW_FACTOR = 0.25F;

    /**
     * 头部左右转动保留比例。
     * 0.25 = 原版的 1/4。
     */
    private static final float LOCKED_HEAD_YAW_FACTOR = 0.25F;

    /**
     * 头部上下俯仰保留比例。
     * 如果你只想限制左右转头，不想限制抬头低头，就设成 1.0F。
     */
    private static final float LOCKED_HEAD_PITCH_FACTOR = 0.9F;

    @Inject(
            method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void kingdomComeCombat$limitBodyAndHeadYawInRenderState(
            AbstractClientPlayerEntity player,
            PlayerEntityRenderState state,
            float tickDelta,
            CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || player != client.player
                || FirstPersonRenderCompat.isExternalBodyRenderOrPreparing()) {
            return;
        }

        if (!LockOnState.locked || !CombatItemUtil.hasCombatWeapon(player)) {
            return;
        }

        /*
         * bodyYaw 是身体朝向。
         * 这里把身体相对视角的偏移压缩到 1/4。
         */
        float viewYaw = player.getYaw(tickDelta);
        float bodyOffset = MathHelper.wrapDegrees(state.bodyYaw - viewYaw);
        state.bodyYaw = viewYaw + bodyOffset * LOCKED_BODY_YAW_FACTOR;

        if (tryLockHeadToTarget(client, player, state, tickDelta)) {
            return;
        }

        state.relativeHeadYaw *= LOCKED_HEAD_YAW_FACTOR;
        state.pitch *= LOCKED_HEAD_PITCH_FACTOR;
    }

    private static boolean tryLockHeadToTarget(
            MinecraftClient client,
            AbstractClientPlayerEntity player,
            PlayerEntityRenderState state,
            float tickDelta
    ) {
        if (!LockOnState.isHardLocked()
                || CombatClientState.attacking
                || client.world == null
                || LockOnState.targetEntityId < 0) {
            return false;
        }

        Entity target = client.world.getEntityById(LockOnState.targetEntityId);
        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            return false;
        }

        Vec3d from = player.getCameraPosVec(tickDelta);
        Vec3d to = LockOnTargetSelector.getTargetLockPoint(livingTarget);
        Vec3d delta = to.subtract(from);
        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontalDistance <= 0.0001) {
            return false;
        }

        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontalDistance));
        float animatedBodyYaw = CombatAnimationClient.getLocalStanceBodyYawOffsetDegrees();
        state.relativeHeadYaw = MathHelper.clamp(
                MathHelper.wrapDegrees(targetYaw - state.bodyYaw - animatedBodyYaw),
                -85.0F,
                85.0F
        );
        state.pitch = MathHelper.clamp(targetPitch, -70.0F, 70.0F);
        return true;
    }
}
