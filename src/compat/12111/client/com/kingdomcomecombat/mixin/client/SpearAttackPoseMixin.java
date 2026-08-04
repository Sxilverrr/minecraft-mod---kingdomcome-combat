package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.combat.CombatItemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.util.SwingAnimationType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents the vanilla 1.21.11 spear thrust pose from layering over a KCC attack. */
@Mixin(BipedEntityModel.class)
public class SpearAttackPoseMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At("HEAD"))
    private void kingdomcomecombat$cancelVanillaSpearAttackPose(
            BipedEntityRenderState state, CallbackInfo ci) {
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        EntityRenderStateKccAccess access = (EntityRenderStateKccAccess) state;
        MinecraftClient client = MinecraftClient.getInstance();
        boolean kccAttack = client.player != null && entityId == client.player.getId()
                ? CombatClientState.attacking
                : ClientEntityGeckoAnimationState.getAttackLayer(entityId) != null;
        // HeldItemFeatureRenderer applies Lancing's STAB matrix directly from
        // these render-state fields, independently of the arm pose. Clear the
        // vanilla swing for every KCC polearm before any feature renderer sees it.
        boolean vanillaSpearSwing = state.handSwingProgress > 0.0F
                && state.swingAnimationType == SwingAnimationType.STAB
                && CombatItemUtil.isPolearm(access.kingdomcomecombat$getMainHandStack());
        if (vanillaSpearSwing) {
            state.handSwingProgress = 0.0F;
        }
        if (!kccAttack && !vanillaSpearSwing) return;
        if (state.rightArmPose == BipedEntityModel.ArmPose.SPEAR) {
            state.rightArmPose = BipedEntityModel.ArmPose.ITEM;
        }
        if (state.leftArmPose == BipedEntityModel.ArmPose.SPEAR) {
            state.leftArmPose = BipedEntityModel.ArmPose.ITEM;
        }
    }
}
