package com.kingdomcomecombat.client.render;

import com.kingdomcomecombat.equipment.BloodiedEquipment;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

public final class EquipmentOverlayRenderer {
    private EquipmentOverlayRenderer() {
    }

    public static void renderItemOverlay(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            List<BakedQuad> quads,
            int light,
            int overlay
    ) {
        ItemStack stack = EquipmentOverlayRenderContext.currentStack();
        if (!CombatClientConfig.renderBlood()
                || stack.isEmpty()
                || !BloodiedEquipment.canShowOverlay(stack)
                || quads.isEmpty()) {
            return;
        }

        double blood = BloodiedEquipment.getBloodPercent(stack);
        if (blood <= 0.0) {
            return;
        }

        MatrixStack.Entry entry = matrices.peek();
        for (BakedQuad quad : quads) {
            Sprite sprite = quad.sprite();
            Identifier sourceTexture = sprite.getContents().getId();
            Identifier texture = BloodiedTextureCache.getBloodiedTexture(sourceTexture, stack);
            if (texture.equals(sourceTexture)) {
                continue;
            }
            VertexConsumer consumer = new SpriteUvRemappingVertexConsumer(
                    vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture)),
                    sprite
            );
            consumer.quad(entry, quad, 1.0F, 1.0F, 1.0F, 1.0F, light, overlay);
        }
    }

    public static boolean renderItemReplacement(
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            int[] tints,
            List<BakedQuad> quads,
            RenderLayer originalLayer,
            ItemRenderState.Glint glint
    ) {
        ItemStack stack = EquipmentOverlayRenderContext.currentStack();
        if (stack.isEmpty()
                || !BloodiedEquipment.canShowOverlay(stack)
                || !BloodiedTextureCache.shouldUseEquipmentTexture(stack)
                || quads.isEmpty()) {
            return false;
        }

        boolean rendered = false;
        MatrixStack.Entry entry = matrices.peek();
        for (BakedQuad quad : quads) {
            Sprite sprite = quad.sprite();
            Identifier sourceTexture = sprite.getContents().getId();
            Identifier texture = BloodiedTextureCache.getBloodiedTexture(sourceTexture, stack);
            if (texture.equals(sourceTexture)) {
                continue;
            }
            float red = 1.0F;
            float green = 1.0F;
            float blue = 1.0F;
            if (quad.hasTint()) {
                int tintIndex = quad.tintIndex();
                if (tintIndex >= 0 && tintIndex < tints.length) {
                    int tint = tints[tintIndex];
                    red = ((tint >> 16) & 255) / 255.0F;
                    green = ((tint >> 8) & 255) / 255.0F;
                    blue = (tint & 255) / 255.0F;
                }
            }
            RenderLayer layer = RenderLayer.getEntityTranslucent(texture);
            VertexConsumer consumer = new SpriteUvRemappingVertexConsumer(
                    itemConsumer(displayContext, matrices, vertexConsumers, layer, glint),
                    sprite
            );
            consumer.quad(entry, quad, red, green, blue, 1.0F, light, overlay);
            rendered = true;
        }
        return rendered;
    }

    private static VertexConsumer itemConsumer(
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            RenderLayer layer,
            ItemRenderState.Glint glint
    ) {
        return ItemRenderer.getItemGlintConsumer(
                vertexConsumers,
                layer,
                true,
                glint != ItemRenderState.Glint.NONE
        );
    }

    public static void renderArmorOverlay(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            Model model,
            ItemStack stack,
            int light
    ) {
        // Armor blood is applied by EquipmentRendererBloodiedTextureMixin by replacing the base texture.
    }

    private record SpriteUvRemappingVertexConsumer(VertexConsumer delegate, Sprite sprite) implements VertexConsumer {
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
            float frameWidth = Math.max(1.0F, sprite.getFrameFromU(sprite.getMaxU()));
            float frameHeight = Math.max(1.0F, sprite.getFrameFromV(sprite.getMaxV()));
            delegate.texture(sprite.getFrameFromU(u) / frameWidth, sprite.getFrameFromV(v) / frameHeight);
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
