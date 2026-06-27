package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.client.combat.MountedAttackCooldown;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拦截原版左键“攻击动作”。
 *
 * 目标：
 * 1. 空挥时，不播放原版挥手动画
 * 2. 打实体时，不播放原版挥手动画，也不走原版攻击
 * 3. 对着方块时，不拦截，保留原版挖掘
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientAttackMixin {
    @WrapOperation(
            method = "doAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isRiding()Z"
            )
    )
    private boolean kingdomcomecombat$allowHorseAndCamelAttack(
            ClientPlayerEntity player,
            Operation<Boolean> original
    ) {
        return original.call(player)
                && !CombatItemUtil.isAllowedVanillaAttackMount(player.getVehicle());
    }


    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void kingdomComeCombat$cancelVanillaAttackAnimation(
            CallbackInfoReturnable<Boolean> cir
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.interactionManager == null) {
            return;
        }

        if (CombatItemUtil.isAllowedVanillaAttackMount(client.player.getVehicle())) {
            if (MountedAttackCooldown.isCoolingDown()) {
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }
            MountedAttackCooldown.markAttack(client.player.getWorld().getTime());
            return;
        }

        if (CombatItemUtil.shouldUseVanillaEntityAttack(client.player)) {
            return;
        }

        HitResult hit = client.crosshairTarget;

        if (hit == null) {
            return;
        }

        if (hit instanceof EntityHitResult entityHit
                && CombatItemUtil.shouldUseVanillaEntityAttack(client.player, entityHit.getEntity())) {
            return;
        }

        // 对着方块时不拦截，保留长按挖掘功能。
        if (hit.getType() == HitResult.Type.BLOCK) {
            return;
        }

        // 空气 / 实体：取消原版攻击动作，避免原版挥手动画。
        if (hit.getType() == HitResult.Type.MISS || hit.getType() == HitResult.Type.ENTITY) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
