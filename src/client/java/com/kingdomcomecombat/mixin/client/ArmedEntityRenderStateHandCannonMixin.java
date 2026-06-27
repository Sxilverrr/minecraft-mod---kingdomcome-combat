package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.item.ModItems;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArmedEntityRenderState.class)
public class ArmedEntityRenderStateHandCannonMixin {
    @WrapOperation(
            method = "updateRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/item/ItemModelManager;updateForLivingEntity(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/entity/LivingEntity;)V"
            )
    )
    private static void kingdomcomecombat$replaceRamrodRenderStateStack(
            ItemModelManager itemModelManager,
            ItemRenderState itemRenderState,
            ItemStack stack,
            ItemDisplayContext displayContext,
            LivingEntity entity,
            Operation<Void> original
    ) {
        original.call(
                itemModelManager,
                itemRenderState,
                kingdomcomecombat$renderedStack(entity, stack, displayContext),
                displayContext,
                entity
        );
    }

    private static ItemStack kingdomcomecombat$renderedStack(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || entity != client.player) {
            return stack;
        }
        if (!CombatAnimationClient.isLocalHandCannonReloadRamrodWindow()) {
            return stack;
        }
        Arm offArm = entity.getMainArm().getOpposite();
        boolean renderingOffhand = displayContext == (offArm == Arm.RIGHT
                ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                : ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        return renderingOffhand ? Items.STICK.getDefaultStack() : stack;
    }
}
