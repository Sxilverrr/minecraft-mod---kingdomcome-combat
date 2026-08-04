package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.combat.ClientExecutionState;
import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.client.game.ClientGameRuleState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudCrosshairMixin {

    @Inject(
            method = "renderCrosshair",
            at = @At("HEAD"),
            cancellable = true
    )
    private void kingdomComeCombat$hideVanillaCrosshairWhenLocked(
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {
        if (LockOnState.locked || ClientExecutionState.shouldHideCrosshair() || ClientGameRuleState.hardcoreMode()) {
            ci.cancel();
        }
    }
}
