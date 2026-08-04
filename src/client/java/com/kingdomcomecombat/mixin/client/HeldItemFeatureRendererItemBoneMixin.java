package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.collision.ClientItemHitboxCache;
import com.kingdomcomecombat.client.animation.ItemBoneTransformApplier;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.client.render.EquipmentOverlayRenderContext;
import com.kingdomcomecombat.item.HandCannonItem;
import com.kingdomcomecombat.item.ModItems;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemFeatureRenderer.class)
public class HeldItemFeatureRendererItemBoneMixin {
    @Unique
    private static final float HAND_CANNON_RAMROD_SCALE = 0.68F;

    @Inject(
            method = "renderItem(Lnet/minecraft/client/render/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V"
            )
    )
    private void kingdomcomecombat$applyItemBoneTransform(
            ArmedEntityRenderState state,
            ItemRenderState itemState,
            Arm arm,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        matrices.push();
        ItemBoneTransformApplier.apply(state, arm, matrices);
        EntityRenderStateKccAccess access = (EntityRenderStateKccAccess) state;
        ItemStack stack = arm == state.mainArm
                ? access.kingdomcomecombat$getMainHandStack()
                : access.kingdomcomecombat$getOffHandStack();
        if (arm == state.mainArm && stack.isOf(ModItems.HAND_CANNON) && HandCannonItem.isFuseLit(stack)) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F));
        }
        boolean ramrod = kingdomcomecombat$isRamrodRenderStack(state, arm);
        if (ramrod) {
            matrices.scale(HAND_CANNON_RAMROD_SCALE, HAND_CANNON_RAMROD_SCALE, HAND_CANNON_RAMROD_SCALE);
        }
        EquipmentOverlayRenderContext.push(ramrod ? Items.STICK.getDefaultStack() : stack);
        if (arm == state.mainArm) {
            ClientItemHitboxCache.beginCapture(access.kingdomcomecombat$getEntityId());
        }
        if (stack.isOf(Items.TRIDENT)) {
            matrices.scale(1.2F, 1.2F, 1.2F);
        }
    }

    @Unique
    private static boolean kingdomcomecombat$isRamrodRenderStack(
            ArmedEntityRenderState state,
            Arm arm
    ) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player == null
                || ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId() != client.player.getId()
                || !CombatAnimationClient.isLocalHandCannonReloadRamrodWindow()) {
            return false;
        }
        return arm != state.mainArm;
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/client/render/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
                    shift = At.Shift.AFTER
            )
    )
    private void kingdomcomecombat$clearItemBoneCapture(
            ArmedEntityRenderState state,
            ItemRenderState itemState,
            Arm arm,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        ClientItemHitboxCache.endCapture();
        EquipmentOverlayRenderContext.pop();
        matrices.pop();
    }
}
