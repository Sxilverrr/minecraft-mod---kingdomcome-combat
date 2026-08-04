package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Runs the normal world player render as a hidden, command-tagged collision pass. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererFirstPersonCollisionMixin {
    @WrapOperation(
            method = "fillEntityRenderStates",
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
            method = "pushEntityRenders",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderManager;render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V"
            )
    )
    private void kingdomcomecombat$markCollisionOnlyEntityRender(
            EntityRenderManager manager,
            EntityRenderState state,
            CameraRenderState cameraState,
            double x,
            double y,
            double z,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            Operation<Void> original
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean collisionOnly = client.player != null
                && client.options.getPerspective().isFirstPerson()
                && ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId() == client.player.getId()
                && !kingdomcomecombat$isExternalCameraState(state)
                && !FirstPersonRenderCompat.isExternalBodyRenderOrPreparing();
        boolean visibleSpecialPass = collisionOnly
                && CombatClientConfig.firstPersonRenderingEnabled()
                && CombatAnimationClient.isKccSpecialFirstPersonActive()
                && !FirstPersonRenderCompat.shouldSuppressPalFirstPersonRenderer();
        if (visibleSpecialPass) {
            original.call(manager, state, cameraState, x, y, z, matrices, queue);
        }
        if (collisionOnly) {
            CollisionOnlyRenderContext.begin();
        }
        List<EntityRenderState.ShadowPiece> shadowPieces = null;
        boolean onFire = state.onFire;
        if (collisionOnly) {
            // EntityRenderManager submits shadows and fire outside the model
            // commands, so the deferred vertex discard cannot hide them.
            shadowPieces = new ArrayList<>(state.shadowPieces);
            state.shadowPieces.clear();
            state.onFire = false;
        }
        try {
            original.call(manager, state, cameraState, x, y, z, matrices, queue);
        } finally {
            if (collisionOnly) {
                state.shadowPieces.addAll(shadowPieces);
                state.onFire = onFire;
                CollisionOnlyRenderContext.end();
            }
        }
    }

    /**
     * FirstPerson 2.7.2 stores this flag on the deferred 1.21.11 render state.
     * Its old global API flag has already been cleared by pushEntityRenders.
     */
    @Unique
    private static boolean kingdomcomecombat$isExternalCameraState(EntityRenderState state) {
        try {
            Class<?> access = Class.forName(
                    "dev.tr7zw.firstperson.access.LivingEntityRenderStateAccess",
                    false,
                    state.getClass().getClassLoader()
            );
            if (!access.isInstance(state)) {
                return false;
            }
            Method getter = access.getMethod("isCameraEntity");
            return Boolean.TRUE.equals(getter.invoke(state));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | LinkageError ignored) {
            return false;
        }
    }
}
