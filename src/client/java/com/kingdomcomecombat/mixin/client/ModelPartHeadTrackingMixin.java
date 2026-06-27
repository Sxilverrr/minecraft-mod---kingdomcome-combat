package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.collision.ClientGenericModelTracker;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.render.FirstPersonHeadPoseTracker;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public class ModelPartHeadTrackingMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
            at = @At("HEAD")
    )
    private void kingdomcomecombat$captureHeadBounds(
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            int color,
            CallbackInfo ci
    ) {
        FirstPersonHeadPoseTracker.capture((ModelPart) (Object) this, matrices);
        if (FirstPersonRenderCompat.isExternalBodyRender()) {
            return;
        }
        ClientGenericModelTracker.captureIfHead((ModelPart) (Object) this, matrices);
    }
}
