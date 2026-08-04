package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.LegacyItemBoneTransformApplier;
import com.kingdomcomecombat.client.collision.ClientItemHitboxCache;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemFeatureRenderer.class)
public class LegacyHeldItemFeatureRendererItemBoneMixin {
    @Inject(
            method = "renderItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")
    )
    private void kingdomcomecombat$applyLegacyItemBone(
            LivingEntity entity,
            ItemStack stack,
            ModelTransformationMode mode,
            Arm arm,
            MatrixStack matrices,
            VertexConsumerProvider vertices,
            int light,
            CallbackInfo ci
    ) {
        LegacyItemBoneTransformApplier.apply(entity, arm, matrices);
        if (arm == entity.getMainArm()) {
            ClientItemHitboxCache.beginCapture(entity.getId());
        }
    }

    @Inject(
            method = "renderItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", shift = At.Shift.AFTER)
    )
    private void kingdomcomecombat$finishLegacyItemCapture(
            LivingEntity entity, ItemStack stack, ModelTransformationMode mode, Arm arm,
            MatrixStack matrices, VertexConsumerProvider vertices, int light, CallbackInfo ci) {
        ClientItemHitboxCache.endCapture();
    }
}
