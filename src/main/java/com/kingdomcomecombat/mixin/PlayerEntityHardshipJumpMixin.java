package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.hardship.HardshipEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class PlayerEntityHardshipJumpMixin {
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$consumeHardshipJumpStamina(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && !HardshipEffects.tryConsumeJumpStamina(player)) {
            player.setSprinting(false);
            ci.cancel();
        }
    }
}
