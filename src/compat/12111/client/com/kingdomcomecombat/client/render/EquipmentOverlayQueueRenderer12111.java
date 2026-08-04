package com.kingdomcomecombat.client.render;

import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.equipment.BloodiedEquipment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

public final class EquipmentOverlayQueueRenderer12111 {
    private EquipmentOverlayQueueRenderer12111() {
    }

    public static boolean submitReplacement(
            ItemStack stack,
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            int overlay,
            int[] tints,
            List<BakedQuad> quads,
            ItemRenderState.Glint glint
    ) {
        if (!CombatClientConfig.renderBlood()
                || stack.isEmpty()
                || !BloodiedEquipment.canShowOverlay(stack)
                || !BloodiedTextureCache.shouldUseItemTexture(stack)
                || quads.isEmpty()) {
            return false;
        }

        boolean submitted = false;
        for (BakedQuad quad : quads) {
            Sprite sprite = quad.sprite();
            Identifier sourceTexture = sprite.getContents().getId();
            Identifier texture = BloodiedTextureCache.getBloodiedTexture(sourceTexture, stack);
            if (texture.equals(sourceTexture)) {
                continue;
            }

            int color = 0xFFFFFFFF;
            if (quad.hasTint()) {
                int tintIndex = quad.tintIndex();
                if (tintIndex >= 0 && tintIndex < tints.length) {
                    color = tints[tintIndex];
                }
            }
            final float red = ((color >> 16) & 255) / 255.0F;
            final float green = ((color >> 8) & 255) / 255.0F;
            final float blue = (color & 255) / 255.0F;
            final float alpha = ((color >>> 24) & 255) / 255.0F;
            queue.submitCustom(matrices, RenderLayers.entityTranslucent(texture), (entry, consumer) -> {
                VertexConsumer remapped = new SpriteUvConsumer(consumer, sprite);
                remapped.quad(entry, quad, red, green, blue, alpha, light, overlay);
            });
            submitted = true;
        }
        return submitted;
    }

    private record SpriteUvConsumer(VertexConsumer delegate, Sprite sprite) implements VertexConsumer {
        @Override public VertexConsumer vertex(float x, float y, float z) { delegate.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int red, int green, int blue, int alpha) { delegate.color(red, green, blue, alpha); return this; }
        @Override public VertexConsumer color(int color) { delegate.color(color); return this; }
        @Override public VertexConsumer texture(float u, float v) {
            delegate.texture(
                    (u - sprite.getMinU()) / Math.max(1.0E-6F, sprite.getMaxU() - sprite.getMinU()),
                    (v - sprite.getMinV()) / Math.max(1.0E-6F, sprite.getMaxV() - sprite.getMinV())
            );
            return this;
        }
        @Override public VertexConsumer overlay(int u, int v) { delegate.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { delegate.light(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
        @Override public VertexConsumer lineWidth(float width) { delegate.lineWidth(width); return this; }
    }
}
