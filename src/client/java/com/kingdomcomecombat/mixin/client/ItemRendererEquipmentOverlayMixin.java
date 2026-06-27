package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.render.EquipmentOverlayRenderContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererEquipmentOverlayMixin {
    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack kingdomcomecombat$showHandCannonRamrodDuringReload(
            ItemStack stack,
            ItemStack originalStack,
            ItemDisplayContext displayContext,
            int light,
            int overlay,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            World world,
            int seed
    ) {
        return CombatAnimationClient.shouldRenderHandCannonRamrod(stack, displayContext)
                ? Items.STICK.getDefaultStack()
                : stack;
    }

    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack kingdomcomecombat$showHandCannonRamrodDuringReloadForEntity(
            ItemStack stack,
            LivingEntity entity,
            ItemStack originalStack,
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            World world,
            int light,
            int overlay,
            int seed
    ) {
        return CombatAnimationClient.shouldRenderHandCannonRamrod(stack, displayContext)
                ? Items.STICK.getDefaultStack()
                : stack;
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("HEAD")
    )
    private void kingdomcomecombat$pushEntityRenderedStack(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            World world,
            int light,
            int overlay,
            int seed,
            CallbackInfo ci
    ) {
        EquipmentOverlayRenderContext.push(kingdomcomecombat$renderedStack(stack, displayContext));
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("RETURN")
    )
    private void kingdomcomecombat$popEntityRenderedStack(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            World world,
            int light,
            int overlay,
            int seed,
            CallbackInfo ci
    ) {
        EquipmentOverlayRenderContext.pop();
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V",
            at = @At("HEAD")
    )
    private void kingdomcomecombat$pushOverlayStack(
            ItemStack stack,
            ItemDisplayContext displayContext,
            int light,
            int overlay,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            World world,
            int seed,
            CallbackInfo ci
    ) {
        EquipmentOverlayRenderContext.push(stack);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V",
            at = @At("RETURN")
    )
    private void kingdomcomecombat$popOverlayStack(
            ItemStack stack,
            ItemDisplayContext displayContext,
            int light,
            int overlay,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            World world,
            int seed,
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
