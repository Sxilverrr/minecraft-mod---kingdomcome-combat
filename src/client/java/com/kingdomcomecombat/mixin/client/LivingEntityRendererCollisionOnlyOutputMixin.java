package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import com.kingdomcomecombat.client.render.CollisionOnlyVertexConsumers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererCollisionOnlyOutputMixin {
    @ModifyVariable(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private VertexConsumerProvider kingdomcomecombat$discardHiddenThirdPersonOutput(
            VertexConsumerProvider original,
            LivingEntityRenderState state
    ) {
        if (CollisionOnlyRenderContext.isActive()) {
            return CollisionOnlyVertexConsumers.provider();
        }
        return original;
    }
}
