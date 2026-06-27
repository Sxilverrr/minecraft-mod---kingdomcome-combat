package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.collision.ClientItemHitboxCache;
import com.kingdomcomecombat.client.render.EquipmentOverlayRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Supplier;

@Mixin(targets = "net.minecraft.client.render.item.ItemRenderState$LayerRenderState")
public class ItemRenderStateLayerRenderStateMixin {
    @Shadow
    private Supplier<Vector3f[]> vertices;
    @Shadow
    private List<BakedQuad> quads;

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/model/json/Transformation;apply(ZLnet/minecraft/client/util/math/MatrixStack$Entry;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void kingdomcomecombat$captureItemModelMatrix(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            CallbackInfo ci
    ) {
        ClientItemHitboxCache.captureItemModel(matrices, vertices);
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V"
            )
    )
    private void kingdomcomecombat$replaceBloodiedItemRender(
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            int[] tints,
            List<BakedQuad> quads,
            RenderLayer layer,
            ItemRenderState.Glint glint,
            Operation<Void> original
    ) {
        if (!EquipmentOverlayRenderer.renderItemReplacement(
                displayContext,
                matrices,
                vertexConsumers,
                light,
                overlay,
                tints,
                quads,
                layer,
                glint
        )) {
            original.call(displayContext, matrices, vertexConsumers, light, overlay, tints, quads, layer, glint);
        }
    }
}
