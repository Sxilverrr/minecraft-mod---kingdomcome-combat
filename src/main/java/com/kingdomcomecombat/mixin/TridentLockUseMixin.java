package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.combat.ServerCombatStanceState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.TridentItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public class TridentLockUseMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$preventLockedThrow(
            World world,
            PlayerEntity user,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (!world.isClient() && ServerCombatStanceState.isLocked(user.getUuid())) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
