package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.compat.PalAnimationStateCompat;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.zigythebird.playeranim.accessors.IPlayerAnimationState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArmorFeatureRenderer.class, priority = 1100)
public abstract class ArmorFeatureRendererFirstPersonBodyMixin {
    @Unique
    private IPlayerAnimationState kingdomcomecombat$firstPersonState;

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("HEAD")
    )
    private void kingdomcomecombat$useFullArmorVisibility(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            BipedEntityRenderState state,
            float limbAngle,
            float limbDistance,
            CallbackInfo ci
    ) {
        if (state instanceof IPlayerAnimationState animationState
                && kingdomcomecombat$isFirstPersonPass(state, animationState)
                && CombatAnimationClient.isKccSpecialFirstPersonActive()) {
            kingdomcomecombat$firstPersonState = animationState;
            PalAnimationStateCompat.setFirstPersonPass(animationState, false);
        }
    }

    @Unique
    private static boolean kingdomcomecombat$isFirstPersonPass(
            BipedEntityRenderState state,
            IPlayerAnimationState animationState
    ) {
        if (PalAnimationStateCompat.isFirstPersonPass(animationState)) {
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        return !PalAnimationStateCompat.hasFirstPersonPassAccessors(animationState)
                && client.player != null
                && client.options.getPerspective().isFirstPerson()
                && state instanceof EntityRenderStateKccAccess access
                && access.kingdomcomecombat$getEntityId() == client.player.getId();
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$hideFirstPersonHelmet(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            ItemStack stack,
            EquipmentSlot slot,
            int light,
            BipedEntityModel<?> model,
            CallbackInfo ci
    ) {
        if (kingdomcomecombat$firstPersonState != null && slot == EquipmentSlot.HEAD) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("RETURN")
    )
    private void kingdomcomecombat$restoreFirstPersonPass(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            BipedEntityRenderState state,
            float limbAngle,
            float limbDistance,
            CallbackInfo ci
    ) {
        if (kingdomcomecombat$firstPersonState != null) {
            PalAnimationStateCompat.setFirstPersonPass(kingdomcomecombat$firstPersonState, true);
            kingdomcomecombat$firstPersonState = null;
        }
    }
}
