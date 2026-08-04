package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.render.BloodiedTextureCache;
import com.kingdomcomecombat.equipment.BloodiedEquipment;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** 1.21.1 renders item quads directly from an atlas instead of ItemRenderState. */
@Mixin(ItemRenderer.class)
public class LegacyItemRendererBloodiedTextureMixin {
    @Shadow @Final private ItemColors colors;

    @Unique
    private final ThreadLocal<VertexConsumerProvider> kingdomcomecombat$vertexConsumers = new ThreadLocal<>();

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD")
    )
    private void kingdomcomecombat$captureItemBuffers(
            ItemStack stack,
            net.minecraft.client.render.model.json.ModelTransformationMode mode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            BakedModel model,
            CallbackInfo ci
    ) {
        kingdomcomecombat$vertexConsumers.set(vertexConsumers);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("RETURN")
    )
    private void kingdomcomecombat$clearItemBuffers(
            ItemStack stack,
            net.minecraft.client.render.model.json.ModelTransformationMode mode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            BakedModel model,
            CallbackInfo ci
    ) {
        kingdomcomecombat$vertexConsumers.remove();
    }

    @Inject(
            method = "renderBakedItemModel",
            at = @At("HEAD"),
            cancellable = true
    )
    private void kingdomcomecombat$renderBloodiedItemModel(
            BakedModel model,
            ItemStack stack,
            int light,
            int overlay,
            MatrixStack matrices,
            VertexConsumer originalConsumer,
            CallbackInfo ci
    ) {
        VertexConsumerProvider vertexConsumers = kingdomcomecombat$vertexConsumers.get();
        if (vertexConsumers == null
                || stack.isEmpty()
                || !BloodiedEquipment.canShowOverlay(stack)
                || !BloodiedTextureCache.shouldUseItemTexture(stack)) {
            return;
        }

        Random random = Random.create();
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            kingdomcomecombat$renderBloodiedQuads(
                    matrices, vertexConsumers, model.getQuads(null, direction, random), stack, light, overlay);
        }
        random.setSeed(42L);
        kingdomcomecombat$renderBloodiedQuads(
                matrices, vertexConsumers, model.getQuads(null, null, random), stack, light, overlay);
        ci.cancel();
    }

    @Unique
    private void kingdomcomecombat$renderBloodiedQuads(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            List<BakedQuad> quads,
            ItemStack stack,
            int light,
            int overlay
    ) {
        MatrixStack.Entry entry = matrices.peek();
        for (BakedQuad quad : quads) {
            Sprite sprite = quad.getSprite();
            Identifier sourceTexture = sprite.getContents().getId();
            Identifier texture = BloodiedTextureCache.getBloodiedTexture(sourceTexture, stack);
            RenderLayer layer = RenderLayer.getEntityTranslucent(texture);
            VertexConsumer delegate = ItemRenderer.getItemGlintConsumer(
                    vertexConsumers, layer, true, stack.hasGlint());
            VertexConsumer consumer = new BloodiedSpriteConsumer(delegate, sprite);

            int color = quad.hasColor() ? colors.getColor(stack, quad.getColorIndex()) : 0xFFFFFF;
            float red = ((color >> 16) & 255) / 255.0F;
            float green = ((color >> 8) & 255) / 255.0F;
            float blue = (color & 255) / 255.0F;
            consumer.quad(entry, quad, red, green, blue, 1.0F, light, overlay);
        }
    }

    @Unique
    private record BloodiedSpriteConsumer(VertexConsumer delegate, Sprite sprite) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            float normalizedU = (u - sprite.getMinU())
                    / Math.max(1.0E-6F, sprite.getMaxU() - sprite.getMinU());
            float normalizedV = (v - sprite.getMinV())
                    / Math.max(1.0E-6F, sprite.getMaxV() - sprite.getMinV());
            delegate.texture(normalizedU, normalizedV);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            delegate.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }
    }
}
