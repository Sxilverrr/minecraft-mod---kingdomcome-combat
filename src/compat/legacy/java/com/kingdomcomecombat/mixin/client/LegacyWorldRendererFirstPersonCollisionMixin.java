package com.kingdomcomecombat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldRenderer.class)
public abstract class LegacyWorldRendererFirstPersonCollisionMixin {
    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;isThirdPerson()Z")
    )
    private boolean kingdomcomecombat$includeLocalPlayerInFirstPerson(
            Camera camera,
            Operation<Boolean> original
    ) {
        boolean originalResult = original.call(camera);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null
                && camera.getFocusedEntity() == client.player
                && client.options.getPerspective().isFirstPerson()) {
            return true;
        }
        return originalResult;
    }

    @WrapOperation(
            method = "renderEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
            )
    )
    private void kingdomcomecombat$renderVisibleThenCaptureCollision(
            net.minecraft.client.render.entity.EntityRenderDispatcher dispatcher,
            Entity entity, double x, double y, double z, float yaw, float tickProgress,
            MatrixStack matrices, VertexConsumerProvider consumers, int light,
            Operation<Void> original
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean localFirstPerson = entity == client.player
                && client.options.getPerspective().isFirstPerson()
                && !FirstPersonRenderCompat.isExternalBodyRenderOrPreparing();
        if (!localFirstPerson) {
            original.call(dispatcher, entity, x, y, z, yaw, tickProgress, matrices, consumers, light);
            return;
        }
        boolean visibleSpecialPass = FirstPersonMode.isFirstPersonPass()
                || (CombatClientConfig.firstPersonRenderingEnabled()
                && CombatAnimationClient.isKccSpecialFirstPersonActive()
                && !FirstPersonRenderCompat.shouldSuppressPalFirstPersonRenderer());
        if (visibleSpecialPass) {
            original.call(dispatcher, entity, x, y, z, yaw, tickProgress, matrices, consumers, light);
        }
        CollisionOnlyRenderContext.begin();
        try {
            original.call(dispatcher, entity, x, y, z, yaw, tickProgress, matrices, consumers, light);
        } finally {
            CollisionOnlyRenderContext.end();
        }
    }
}
