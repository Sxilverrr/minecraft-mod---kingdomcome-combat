package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First-person body handling for the entity-based renderer used by 1.21.1. */
@Mixin(value = LivingEntityRenderer.class, priority = 1100)
public abstract class LegacyLivingEntityRendererFirstPersonMixin {
    @Shadow
    protected EntityModel<?> model;

    @Unique private boolean kingdomcomecombat$firstPersonBody;
    @Unique private boolean kingdomcomecombat$externalBody;
    @Unique private boolean kingdomcomecombat$oldHeadVisible;
    @Unique private boolean kingdomcomecombat$oldHatVisible;

    @Inject(method = "render", at = @At("HEAD"))
    private void kingdomcomecombat$prepareFirstPersonBody(
            LivingEntity entity,
            float bodyYaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertices,
            int light,
            CallbackInfo ci
    ) {
        kingdomcomecombat$externalBody = FirstPersonRenderCompat.beginLivingEntityRender();
        MinecraftClient client = MinecraftClient.getInstance();
        kingdomcomecombat$firstPersonBody = entity == client.player
                && client.options.getPerspective().isFirstPerson()
                && CombatAnimationClient.isKccSpecialFirstPersonActive();
        if (!kingdomcomecombat$firstPersonBody || !(model instanceof PlayerEntityModel<?> playerModel)) {
            return;
        }
        kingdomcomecombat$oldHeadVisible = playerModel.head.visible;
        kingdomcomecombat$oldHatVisible = playerModel.hat.visible;
        boolean showHead = FirstPersonRenderCompat.isRenderingShadowPass();
        playerModel.head.visible = showHead;
        playerModel.hat.visible = showHead;
    }

    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER)
    )
    private void kingdomcomecombat$offsetFirstPersonBody(
            LivingEntity entity,
            float bodyYaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertices,
            int light,
            CallbackInfo ci
    ) {
        if (!kingdomcomecombat$firstPersonBody) {
            return;
        }
        double offset = CombatClientConfig.firstPersonBodyForwardOffset();
        float cameraYaw = MinecraftClient.getInstance().gameRenderer.getCamera().getYaw();
        Vec3d forward = Vec3d.fromPolar(0.0F, cameraYaw).normalize();
        matrices.translate(forward.x * offset, 0.0, forward.z * offset);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void kingdomcomecombat$finishFirstPersonBody(
            LivingEntity entity,
            float bodyYaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertices,
            int light,
            CallbackInfo ci
    ) {
        if (kingdomcomecombat$firstPersonBody && model instanceof PlayerEntityModel<?> playerModel) {
            playerModel.head.visible = kingdomcomecombat$oldHeadVisible;
            playerModel.hat.visible = kingdomcomecombat$oldHatVisible;
        }
        FirstPersonRenderCompat.endLivingEntityRender(kingdomcomecombat$externalBody);
        kingdomcomecombat$firstPersonBody = false;
        kingdomcomecombat$externalBody = false;
    }
}
