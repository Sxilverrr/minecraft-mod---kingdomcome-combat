package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.MobPalBodyTransformApplier;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMobPalBodyMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;setupTransforms(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;FF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void kingdomcomecombat$applyPalBodyTransformToMobs(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        MobPalBodyTransformApplier.apply(state, matrices);
    }
}
