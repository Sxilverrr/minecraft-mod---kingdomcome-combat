package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.LegacyMobPalBodyTransformApplier;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LegacyLivingEntityRendererMobPalBodyMixin {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;setupTransforms(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void kingdomcomecombat$applyPalBodyTransformToMobs(
            LivingEntity entity,
            float bodyYaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertices,
            int light,
            CallbackInfo ci
    ) {
        LegacyMobPalBodyTransformApplier.apply(entity, matrices);
    }
}
