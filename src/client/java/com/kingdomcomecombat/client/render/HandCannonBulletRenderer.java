package com.kingdomcomecombat.client.render;

import com.kingdomcomecombat.entity.HandCannonBulletEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public class HandCannonBulletRenderer extends EntityRenderer<HandCannonBulletEntity, EntityRenderState> {
    public HandCannonBulletRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.05F;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void render(EntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.scale(0.16F, 0.16F, 0.16F);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                Blocks.BLACK_CONCRETE.getDefaultState(),
                matrices,
                vertexConsumers,
                light,
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
        super.render(state, matrices, vertexConsumers, light);
    }
}
