package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.collision.ClientGenericModelTracker;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererGenericFeedbackMixin {
    @Shadow protected EntityModel<?> model;
    @Unique private boolean kingdomcomecombat$trackingGenericModel;

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void kingdomcomecombat$beginGenericFeedback(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            int light,
            CallbackInfo ci
    ) {
        if (FirstPersonRenderCompat.isExternalBodyRender()) {
            kingdomcomecombat$trackingGenericModel = false;
            return;
        }
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null
                && entityId == client.player.getId()
                && client.options.getPerspective().isFirstPerson()
                && !CollisionOnlyRenderContext.isActive()) {
            kingdomcomecombat$trackingGenericModel = false;
            return;
        }
        kingdomcomecombat$trackingGenericModel =
                ClientGenericModelTracker.begin(entityId, model, matrices);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN")
    )
    private void kingdomcomecombat$endGenericFeedback(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            int light,
            CallbackInfo ci
    ) {
        if (kingdomcomecombat$trackingGenericModel) {
            ClientGenericModelTracker.end(matrices);
            kingdomcomecombat$trackingGenericModel = false;
        }
    }
}
