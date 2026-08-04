package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.collision.ClientGenericModelTracker;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import com.kingdomcomecombat.compat.GuardVillagersCompat;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LegacyLivingEntityRendererGenericFeedbackMixin<T extends LivingEntity> {
    @Shadow protected EntityModel<T> model;
    @Unique private boolean kingdomcomecombat$trackingFinalModel;

    @Inject(method = "render", at = @At("HEAD"))
    private void kingdomcomecombat$begin(T entity, float yaw, float tickDelta, MatrixStack matrices,
                                         VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        ClientGenericModelTracker.resetPendingAnimation();
        kingdomcomecombat$trackingFinalModel = !FirstPersonRenderCompat.isExternalBodyRender()
                && ClientGenericModelTracker.begin(entity.getId(), model, matrices);
    }

    /**
     * Arms the final pose here, then applies it at the first ModelPart render.
     * That boundary is after every renderer callback has finished changing
     * model angles, including callbacks from third-party animation mods.
     */
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    private void kingdomcomecombat$applyFinalConfiguredHumanoidAnimation(
            T entity, float yaw, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider consumers, int light, CallbackInfo ci
    ) {
        boolean active = ClientEntityGeckoAnimationState.hasActiveCombatLayer(entity.getId());
        if (!active || (HumanoidHurtboxLibrary.isBuiltInHumanoidTarget(entity)
                && !GuardVillagersCompat.isGuard(entity))) {
            return;
        }

        ClientGenericModelTracker.armFinalCombatAnimation(entity, model);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void kingdomcomecombat$end(T entity, float yaw, float tickDelta, MatrixStack matrices,
                                       VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        ClientGenericModelTracker.end(matrices);
        kingdomcomecombat$trackingFinalModel = false;
    }
}
