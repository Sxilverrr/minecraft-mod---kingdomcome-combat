package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.lockon.LockOnState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientTridentUseMixin {
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$preventLockedTridentThrow(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (LockOnState.locked
                && client.player != null
                && client.player.getMainHandStack().isOf(Items.TRIDENT)) {
            ci.cancel();
        }
    }
}
