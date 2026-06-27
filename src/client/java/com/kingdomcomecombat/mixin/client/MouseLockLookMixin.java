package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.client.input.CombatInputClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseLockLookMixin {
    @Shadow
    private double cursorDeltaX;

    @Shadow
    private double cursorDeltaY;

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$lockFirstPersonLook(double timeDelta, CallbackInfo ci) {
        CombatInputClient.acceptRawMouseDelta(cursorDeltaX, cursorDeltaY);
        if (LockOnState.isHardLocked()
                && !LockOnState.delayedClearPending()
                && MinecraftClient.getInstance().currentScreen == null
                && MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {
            // Vanilla normally consumes these at the end of updateMouse. Since
            // this pass is cancelled, consume them here to avoid replaying one
            // physical movement every frame.
            cursorDeltaX = 0.0;
            cursorDeltaY = 0.0;
            ci.cancel();
        }
    }
}
