package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.compat.PalAnimationStateCompat;
import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import com.llamalad7.mixinextras.sugar.Local;
import com.zigythebird.playeranim.accessors.IPlayerAnimationState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderDispatcher.class, priority = 900)
public class EntityRenderDispatcherPalCompatMixin {
    @Inject(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/client/render/entity/state/EntityRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"
            )
    )
    private <E extends Entity, S extends EntityRenderState> void kingdomcomecombat$clearPalFirstPersonFlag(
            E entity,
            double x,
            double y,
            double z,
            float tickProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            EntityRenderer<? super E, S> renderer,
            CallbackInfo ci,
            @Local S state
    ) {
        if ((FirstPersonRenderCompat.isExternalBodyRenderOrPreparing()
                || CollisionOnlyRenderContext.isActive())
                && state instanceof IPlayerAnimationState playerAnimationState) {
            PalAnimationStateCompat.setFirstPersonPass(playerAnimationState, false);
        }
    }
}
