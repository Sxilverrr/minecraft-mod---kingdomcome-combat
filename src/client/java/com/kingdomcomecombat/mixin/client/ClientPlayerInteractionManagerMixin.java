package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.feedback.CauldronWashScreenEffect;
import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.interaction.CauldronWashHandler;
import com.kingdomcomecombat.network.WashFacePayload;
import com.kingdomcomecombat.potion.PotionCoatingHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 取消所有原版实体攻击。
 *
 * 只拦截 attackEntity，不拦截 attackBlock，
 * 所以长按左键挖方块仍然保留。
 */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$washFaceInCauldron(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!CombatClientConfig.cauldronFaceWashEnabled()
                || hand != Hand.MAIN_HAND
                || client.world == null
                || !CauldronWashHandler.canWash(player, client.world, hitResult.getBlockPos())) {
            return;
        }

        ClientPlayNetworking.send(new WashFacePayload(hitResult.getBlockPos()));
        CauldronWashScreenEffect.start();
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void kingdomComeCombat$cancelVanillaEntityAttack(
            PlayerEntity player,
            Entity target,
            CallbackInfo ci
    ) {
        if (target instanceof ArmorStandEntity) {
            if (!LockOnState.locked) {
                return;
            }
            ci.cancel();
            return;
        }

        if (CombatItemUtil.shouldUseVanillaEntityAttack(player, target)) {
            return;
        }

        ci.cancel();
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void kingdomComeCombat$cancelLockedLargeShieldUse(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (hand == Hand.MAIN_HAND
                && PotionCoatingHandler.isPotionCoatingCandidate(player, player.getMainHandStack())) {
            player.setCurrentHand(Hand.MAIN_HAND);
            return;
        }

        if (hand == Hand.OFF_HAND
                && player.isUsingItem()
                && player.getActiveHand() == Hand.MAIN_HAND
                && PotionCoatingHandler.isPotionCoatingCandidate(player, player.getMainHandStack())) {
            cir.setReturnValue(ActionResult.CONSUME);
            return;
        }

        if (LockOnState.locked
                && EquipmentCombatAttributesRegistry.isLargeShield(player.getStackInHand(hand))) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

}
