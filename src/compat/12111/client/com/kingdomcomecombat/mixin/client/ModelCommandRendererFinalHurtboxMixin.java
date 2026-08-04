package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.collision.ClientGenericModelTracker;
import com.kingdomcomecombat.client.render.CollisionOnlyVertexConsumers;
import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.Model;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads body hitboxes from the matrices used by the actual deferred model render. */
@Mixin(ModelCommandRenderer.class)
public abstract class ModelCommandRendererFinalHurtboxMixin {
    @Shadow private MatrixStack matrices;
    @Unique private boolean kingdomcomecombat$capturingBody;

    @WrapOperation(
            method = "render(Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/OutlineVertexConsumerProvider;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    private void kingdomcomecombat$discardFirstPersonThirdPersonModel(
            Model<?> model,
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            int color,
            Operation<Void> original,
            OrderedRenderCommandQueueImpl.ModelCommand<?> command
    ) {
        VertexConsumer output = vertices;
        if (CollisionOnlyRenderContext.isMarked(command)) {
            output = CollisionOnlyVertexConsumers.consumer();
        }
        original.call(model, matrices, output, light, overlay, color);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/OutlineVertexConsumerProvider;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;setAngles(Ljava/lang/Object;)V"
            )
    )
    private void kingdomcomecombat$beginFinalBodyRender(
            OrderedRenderCommandQueueImpl.ModelCommand<?> command,
            RenderLayer layer,
            VertexConsumer vertices,
            OutlineVertexConsumerProvider outlines,
            VertexConsumerProvider.Immediate immediate,
            CallbackInfo ci
    ) {
        Model<?> model = command.model();
        Object state = command.state();
        MinecraftClient client = MinecraftClient.getInstance();
        int entityId = state instanceof EntityRenderState entityState
                ? ((EntityRenderStateKccAccess) entityState).kingdomcomecombat$getEntityId()
                : -1;
        boolean localFirstPerson = client.player != null
                && entityId == client.player.getId()
                && client.options.getPerspective().isFirstPerson();
        // In 1.21.11 FirstPerson's camera-space body and KCC's world-space
        // collision body are both deferred model commands. Only the marked
        // world-space command may update the local player's collision cache.
        if (localFirstPerson && !CollisionOnlyRenderContext.isMarked(command)) {
            kingdomcomecombat$capturingBody = false;
            return;
        }
        kingdomcomecombat$capturingBody = model instanceof EntityModel<?> entityModel
                && state instanceof EntityRenderState entityState
                && ClientGenericModelTracker.beginFinalRender(
                        entityId,
                        entityModel,
                        matrices
                );
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/OutlineVertexConsumerProvider;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void kingdomcomecombat$finishFinalBodyRender(
            OrderedRenderCommandQueueImpl.ModelCommand<?> command,
            RenderLayer layer,
            VertexConsumer vertices,
            OutlineVertexConsumerProvider outlines,
            VertexConsumerProvider.Immediate immediate,
            CallbackInfo ci
    ) {
        if (kingdomcomecombat$capturingBody) {
            ClientGenericModelTracker.end(matrices);
            kingdomcomecombat$capturingBody = false;
        }
        CollisionOnlyRenderContext.finishCommand(command);
    }
}
