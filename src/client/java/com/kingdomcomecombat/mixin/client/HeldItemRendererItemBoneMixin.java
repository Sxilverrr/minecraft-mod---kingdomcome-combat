package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.animation.ItemBoneTransformApplier;
import com.kingdomcomecombat.client.render.EquipmentOverlayRenderContext;
import com.kingdomcomecombat.item.HandCannonItem;
import com.kingdomcomecombat.item.ModItems;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererItemBoneMixin {
    @Unique
    private static final float HAND_CANNON_RAMROD_SCALE = 0.68F;

    @Unique
    private int kingdomcomecombat$itemBoneTransformDepth = 0;

    @ModifyVariable(
            method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack kingdomcomecombat$showConsumedRamrodInOffhand(
            ItemStack stack,
            AbstractClientPlayerEntity player,
            float tickProgress,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack originalStack,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        return hand == Hand.OFF_HAND && CombatAnimationClient.isLocalHandCannonReloadRamrodWindow()
                ? Items.STICK.getDefaultStack()
                : stack;
    }

    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack kingdomcomecombat$showHandCannonRamrodDuringFirstPersonReload(
            ItemStack stack,
            LivingEntity entity,
            ItemStack originalStack,
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        return kingdomcomecombat$renderedStack(stack, displayContext);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void kingdomcomecombat$pushFirstPersonRenderedStack(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        EquipmentOverlayRenderContext.push(kingdomcomecombat$renderedStack(stack, displayContext));
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V"
            )
    )
    private void kingdomcomecombat$applyFirstPersonItemBoneTransform(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        ItemStack renderedStack = kingdomcomecombat$renderedStack(stack, displayContext);
        CombatAnimationClient.getLocalPlayerRenderedItemTransform(renderedStack, displayContext)
                .ifPresent(transform -> {
                    matrices.push();
                    kingdomcomecombat$itemBoneTransformDepth++;
                    ItemBoneTransformApplier.applyTransform(matrices, transform);
                });
        if (renderedStack.isOf(ModItems.HAND_CANNON) && HandCannonItem.isFuseLit(renderedStack)) {
            if (kingdomcomecombat$itemBoneTransformDepth <= 0) {
                matrices.push();
                kingdomcomecombat$itemBoneTransformDepth++;
            }
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F));
        }
        if (CombatAnimationClient.shouldRenderHandCannonRamrod(stack, displayContext)) {
            if (kingdomcomecombat$itemBoneTransformDepth <= 0) {
                matrices.push();
                kingdomcomecombat$itemBoneTransformDepth++;
            }
            matrices.scale(HAND_CANNON_RAMROD_SCALE, HAND_CANNON_RAMROD_SCALE, HAND_CANNON_RAMROD_SCALE);
        }
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
                    shift = At.Shift.AFTER
            )
    )
    private void kingdomcomecombat$clearFirstPersonItemBoneTransform(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (kingdomcomecombat$itemBoneTransformDepth > 0) {
            kingdomcomecombat$itemBoneTransformDepth--;
            matrices.pop();
        }
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN")
    )
    private void kingdomcomecombat$popFirstPersonRenderedStack(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        EquipmentOverlayRenderContext.pop();
    }

    @Unique
    private static ItemStack kingdomcomecombat$renderedStack(ItemStack stack, ItemDisplayContext displayContext) {
        return CombatAnimationClient.shouldRenderHandCannonRamrod(stack, displayContext)
                ? Items.STICK.getDefaultStack()
                : stack;
    }
}
