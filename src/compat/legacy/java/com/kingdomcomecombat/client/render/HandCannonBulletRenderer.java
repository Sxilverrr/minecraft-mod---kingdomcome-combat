package com.kingdomcomecombat.client.render;

import com.kingdomcomecombat.entity.HandCannonBulletEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class HandCannonBulletRenderer extends EntityRenderer<HandCannonBulletEntity> {
    public HandCannonBulletRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.05F;
    }

    @Override
    public void render(
            HandCannonBulletEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
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
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(HandCannonBulletEntity entity) {
        return Identifier.ofVanilla("textures/block/black_concrete.png");
    }
}
