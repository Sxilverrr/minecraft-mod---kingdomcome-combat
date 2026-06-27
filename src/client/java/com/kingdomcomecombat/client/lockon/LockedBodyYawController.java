package com.kingdomcomecombat.client.lockon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public class LockedBodyYawController {
    /**
     * 锁定状态下身体侧向偏转保留比例。
     *
     * 1.0 = 原版效果
     * 0.5 = 一半
     * 0.25 = 原来的 1/4
     * 0.0 = 身体完全跟随视角
     */
    private static final float LOCKED_BODY_YAW_FACTOR = 0.25F;

    public static void tick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        if (!LockOnState.locked) {
            return;
        }

        applyLockedBodyYawLimit(client.player);
    }

    private static void applyLockedBodyYawLimit(ClientPlayerEntity player) {
        float viewYaw = player.getYaw();

        float currentBodyYaw = player.bodyYaw;
        float currentLastBodyYaw = player.lastBodyYaw;

        float bodyOffset = MathHelper.wrapDegrees(currentBodyYaw - viewYaw);
        float lastBodyOffset = MathHelper.wrapDegrees(currentLastBodyYaw - viewYaw);

        float limitedBodyYaw = viewYaw + bodyOffset * LOCKED_BODY_YAW_FACTOR;
        float limitedLastBodyYaw = viewYaw + lastBodyOffset * LOCKED_BODY_YAW_FACTOR;

        player.bodyYaw = limitedBodyYaw;
        player.lastBodyYaw = limitedLastBodyYaw;
    }
}