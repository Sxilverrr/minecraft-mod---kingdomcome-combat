package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.injury.InjuryTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntitySleepInjuryMixin {
    @Inject(method = "wakeUp(ZZ)V", at = @At("TAIL"))
    private void kingdomComeCombat$healLowTierWoundsAfterSleep(
            boolean skipSleepTimer,
            boolean updateSleepingPlayers,
            CallbackInfo ci
    ) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            InjuryTicker.healLowTierWoundsFromSleep(serverPlayer);
        }
    }
}
