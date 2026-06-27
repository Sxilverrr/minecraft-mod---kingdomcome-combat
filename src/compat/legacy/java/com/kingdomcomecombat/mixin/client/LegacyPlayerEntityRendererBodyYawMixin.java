package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.combat.CombatItemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 1.21.1 equivalent of the render-state body-yaw adjustment. */
@Mixin(PlayerEntityRenderer.class)
public class LegacyPlayerEntityRendererBodyYawMixin {
    private static final float LOCKED_BODY_YAW_FACTOR = 0.25F;

    @ModifyVariable(
            method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float kingdomComeCombat$limitLockedBodyYaw(
            float bodyYaw,
            AbstractClientPlayerEntity player,
            float ignoredBodyYaw,
            float tickDelta
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null
                || player != client.player
                || FirstPersonRenderCompat.isExternalBodyRenderOrPreparing()
                || !LockOnState.locked
                || !CombatItemUtil.hasCombatWeapon(player)) {
            return bodyYaw;
        }

        float viewYaw = player.getYaw(tickDelta);
        float offset = MathHelper.wrapDegrees(bodyYaw - viewYaw);
        return viewYaw + offset * LOCKED_BODY_YAW_FACTOR;
    }
}
