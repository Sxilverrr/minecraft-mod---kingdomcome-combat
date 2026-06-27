package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.combat.CombatItemUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(PlayerEntity.class)
public class PlayerEntityVanillaAttackMixin {
    private static final int MOUNTED_ATTACK_COOLDOWN_TICKS = 16;
    private static final Map<PlayerEntity, Long> LAST_MOUNTED_ATTACK_TICKS = new WeakHashMap<>();

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$enforceVanillaAttackRule(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity
                && CombatItemUtil.isAllowedVanillaAttackMount(player.getVehicle())) {
            long now = player.getWorld().getTime();
            long lastAttack = LAST_MOUNTED_ATTACK_TICKS.getOrDefault(
                    player,
                    now - MOUNTED_ATTACK_COOLDOWN_TICKS
            );
            if (now - lastAttack < MOUNTED_ATTACK_COOLDOWN_TICKS) {
                ci.cancel();
                return;
            }
            LAST_MOUNTED_ATTACK_TICKS.put(player, now);
        }

        if (player instanceof ServerPlayerEntity
                && !CombatItemUtil.shouldUseVanillaEntityAttack(player, target)) {
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isOnGround()Z")
    )
    private boolean kingdomcomecombat$allowMountedSweep(
            PlayerEntity player,
            Operation<Boolean> original
    ) {
        return original.call(player)
                || CombatItemUtil.isAllowedVanillaAttackMount(player.getVehicle());
    }
}
