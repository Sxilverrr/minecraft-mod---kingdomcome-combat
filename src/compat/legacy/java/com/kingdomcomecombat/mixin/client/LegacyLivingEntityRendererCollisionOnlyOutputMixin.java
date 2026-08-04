package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import com.kingdomcomecombat.client.render.CollisionOnlyVertexConsumers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntityRenderer.class)
public abstract class LegacyLivingEntityRendererCollisionOnlyOutputMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private VertexConsumerProvider kingdomcomecombat$discardHiddenThirdPersonOutput(
            VertexConsumerProvider original,
            LivingEntity entity
    ) {
        if (CollisionOnlyRenderContext.isActive()) {
            return CollisionOnlyVertexConsumers.provider();
        }
        return original;
    }
}
