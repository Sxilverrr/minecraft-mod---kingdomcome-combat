package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.render.IllagerArmorFeatureRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.IllagerEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.client.render.entity.state.IllagerEntityRenderState;
import net.minecraft.entity.mob.IllagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IllagerEntityRenderer.class)
public abstract class IllagerEntityRendererArmorMixin
        extends MobEntityRenderer<IllagerEntity, IllagerEntityRenderState, IllagerEntityModel<IllagerEntityRenderState>> {
    protected IllagerEntityRendererArmorMixin(
            EntityRendererFactory.Context context,
            IllagerEntityModel<IllagerEntityRenderState> model,
            float shadowRadius
    ) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void kingdomcomecombat$addArmorFeature(
            EntityRendererFactory.Context context,
            IllagerEntityModel<IllagerEntityRenderState> model,
            float shadowRadius,
            CallbackInfo ci
    ) {
        this.addFeature(new IllagerArmorFeatureRenderer(
                (IllagerEntityRenderer<? extends IllagerEntity, IllagerEntityRenderState>) (Object) this,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                context.getEquipmentRenderer()
        ));
    }
}
