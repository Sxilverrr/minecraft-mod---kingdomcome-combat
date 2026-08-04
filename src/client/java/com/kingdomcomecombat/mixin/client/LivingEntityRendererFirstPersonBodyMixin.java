package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.compat.PalAnimationStateCompat;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.client.render.FirstPersonBodyRenderOffsetContext;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.client.render.FirstPersonHeadPoseTracker;
import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import com.zigythebird.playeranim.accessors.IPlayerAnimationState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererFirstPersonBodyMixin {
    @Shadow
    protected EntityModel<?> model;

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void kingdomcomecombat$beginHeadPoseTracking(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (kingdomcomecombat$isFirstPersonPass(state)
                && CombatAnimationClient.isKccSpecialFirstPersonActive()) {
            FirstPersonHeadPoseTracker.begin(
                    ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId(),
                    matrices
            );
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = At.Shift.AFTER
            )
    )
    private void kingdomcomecombat$offsetBodyAlongViewDirection(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        boolean localFirstPersonPass = kingdomcomecombat$isFirstPersonPass(state);
        boolean kccFirstPersonPass = localFirstPersonPass
                && CombatAnimationClient.isKccSpecialFirstPersonActive();
        boolean externalFirstPersonPass = FirstPersonRenderCompat.isExternalBodyRender();
        if (!localFirstPersonPass && !externalFirstPersonPass) {
            return;
        }
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        if (localFirstPersonPass && CollisionOnlyRenderContext.isActive()) {
            FirstPersonBodyRenderOffsetContext.begin(entityId, Vec3d.ZERO);
            return;
        }
        if (!kccFirstPersonPass) {
            FirstPersonBodyRenderOffsetContext.begin(entityId, Vec3d.ZERO);
            return;
        }
        double offset = CombatClientConfig.firstPersonBodyForwardOffset();
        if (Math.abs(offset) <= 0.0001) {
            FirstPersonBodyRenderOffsetContext.begin(entityId, Vec3d.ZERO);
            return;
        }
        float cameraYaw = MinecraftClient.getInstance().gameRenderer.getCamera().getYaw();
        Vec3d forward = Vec3d.fromPolar(0.0F, cameraYaw).normalize();
        Matrix4f before = new Matrix4f(matrices.peek().getPositionMatrix());
        matrices.translate(forward.x * offset, 0.0, forward.z * offset);
        Matrix4f after = matrices.peek().getPositionMatrix();
        Vector3f beforeOrigin = before.transformPosition(new Vector3f());
        Vector3f afterOrigin = after.transformPosition(new Vector3f());
        FirstPersonBodyRenderOffsetContext.begin(
                entityId,
                new Vec3d(
                        afterOrigin.x - beforeOrigin.x,
                        afterOrigin.y - beforeOrigin.y,
                        afterOrigin.z - beforeOrigin.z
                )
        );
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN")
    )
    private void kingdomcomecombat$clearBodyRenderOffset(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        FirstPersonBodyRenderOffsetContext.end();
        FirstPersonHeadPoseTracker.end();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    private void kingdomcomecombat$showBodyWithoutHeadBeforeRender(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        boolean shadowPass = FirstPersonRenderCompat.isRenderingShadowPass();
        boolean kccFirstPersonPass = kingdomcomecombat$isFirstPersonPass(state)
                && CombatAnimationClient.isKccSpecialFirstPersonActive();
        boolean externalFirstPersonPass = FirstPersonRenderCompat.isExternalBodyRender();
        boolean localShadowPass = shadowPass && kingdomcomecombat$isFirstPersonPass(state);
        if ((!kccFirstPersonPass && !externalFirstPersonPass && !localShadowPass)
                || !(this.model instanceof PlayerEntityModel playerModel)) {
            return;
        }

        // Keep the head out of the camera pass, but restore it for Iris' shadow
        // pass so first-person shaders receive a complete player silhouette.
        boolean showBody = shadowPass || !CombatClientConfig.firstPersonWeaponOnly();
        playerModel.setVisible(showBody);
        playerModel.head.visible = shadowPass;
        playerModel.hat.visible = shadowPass;
        FirstPersonHeadPoseTracker.track(playerModel.head);
    }

    private static boolean kingdomcomecombat$isFirstPersonPass(LivingEntityRenderState state) {
        if (PalAnimationStateCompat.isFirstPersonPass(state)) {
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        // PAL 1.1.7 can submit the local first-person render before its flag is
        // visible to this mixin. The local entity/perspective check is an
        // equivalent authoritative fallback and must not be disabled merely
        // because the accessor exists.
        return client.player != null
                && client.currentScreen == null
                && client.options.getPerspective().isFirstPerson()
                && ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId() == client.player.getId();
    }
}
