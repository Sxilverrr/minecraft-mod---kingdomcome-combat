package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityRenderer.class, priority = 1100)
public class LivingEntityRendererFirstPersonBridgeMixin {
    @Unique
    private boolean kingdomcomecombat$externalBodyRender;

    @Inject(method = "render", at = @At("HEAD"))
    private void kingdomcomecombat$beginExternalFirstPersonPass(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        kingdomcomecombat$externalBodyRender = FirstPersonRenderCompat.beginLivingEntityRender();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void kingdomcomecombat$endExternalFirstPersonPass(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        FirstPersonRenderCompat.endLivingEntityRender(kingdomcomecombat$externalBodyRender);
        kingdomcomecombat$externalBodyRender = false;
    }
}
