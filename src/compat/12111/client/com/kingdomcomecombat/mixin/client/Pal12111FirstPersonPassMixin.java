package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.zigythebird.playeranim.util.ClientUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PAL 1.1.7 derives its first-person body pass only from the animation stack.
 * During KCC's client/server attack hand-off that stack can briefly report
 * inactive even though KCC still owns the combat camera. Keep the body pass
 * tied to KCC's authoritative first-person state for the local camera entity.
 */
@Mixin(value = ClientUtil.class, remap = false)
public class Pal12111FirstPersonPassMixin {
    @Inject(
            method = "shouldBeFirstPersonPass(Lnet/minecraft/client/render/Camera;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void kingdomcomecombat$keepCombatBodyPass(
            Camera camera,
            CallbackInfoReturnable<Boolean> cir
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null
                && camera.getFocusedEntity() == client.player
                && !camera.isThirdPerson()
                && CombatAnimationClient.isKccSpecialFirstPersonActive()) {
            cir.setReturnValue(true);
        }
    }
}
