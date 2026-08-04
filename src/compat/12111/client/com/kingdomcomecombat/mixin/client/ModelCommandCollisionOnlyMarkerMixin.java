package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies the collision-only submission context onto each immutable deferred model command. */
@Mixin(OrderedRenderCommandQueueImpl.ModelCommand.class)
public abstract class ModelCommandCollisionOnlyMarkerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void kingdomcomecombat$markCollisionOnlyCommand(
            MatrixStack.Entry matricesEntry,
            Model<?> model,
            Object state,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            Sprite sprite,
            int outlineColor,
            ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay,
            CallbackInfo ci
    ) {
        CollisionOnlyRenderContext.markCommand(this);
    }
}
