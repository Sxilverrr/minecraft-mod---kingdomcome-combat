package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tags only the normal world-render commands used for first-person collision capture. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererCollisionOnlyCommandMixin {
    @Unique
    private boolean kingdomcomecombat$collisionOnlyCommandPass;

    @Inject(method = "render", at = @At("HEAD"))
    private void kingdomcomecombat$beginCollisionOnlyCommands(
            LivingEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        kingdomcomecombat$collisionOnlyCommandPass = CollisionOnlyRenderContext.isActive();
        if (kingdomcomecombat$collisionOnlyCommandPass) {
            CollisionOnlyRenderContext.begin();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void kingdomcomecombat$endCollisionOnlyCommands(
            LivingEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (kingdomcomecombat$collisionOnlyCommandPass) {
            CollisionOnlyRenderContext.end();
            kingdomcomecombat$collisionOnlyCommandPass = false;
        }
    }
}
