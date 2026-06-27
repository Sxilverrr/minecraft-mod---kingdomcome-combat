package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.combat.MountedAttackCooldown;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class PlayerInventoryAttackLockMixin {
    @Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$preventHotbarSwitchDuringAttack(int slot, CallbackInfo ci) {
        if (CombatClientState.attacking
                || MountedAttackCooldown.isCoolingDown()) {
            ci.cancel();
        }
    }
}
