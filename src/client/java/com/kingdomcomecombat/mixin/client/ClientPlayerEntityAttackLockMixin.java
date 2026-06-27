package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.combat.MountedAttackCooldown;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class ClientPlayerEntityAttackLockMixin {
    @Inject(method = "swapHandStacks", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$preventHandSwapDuringAttack(CallbackInfo ci) {
        if ((Object) this == MinecraftClient.getInstance().player
                && (CombatClientState.attacking
                || MountedAttackCooldown.isCoolingDown())) {
            ci.cancel();
        }
    }
}
