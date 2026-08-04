package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.render.FirstPersonBodyRenderOffsetContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Female Gender Mod adds its body geometry as a player render layer. KCC
 * deliberately suppresses that optional layer in its local first-person body
 * pass so it cannot clip into the camera or duplicate the torso.
 */
@Pseudo
@Mixin(targets = "com.wildfire.render.GenderLayer", remap = false)
public class FemaleGenderLayerFirstPersonMixin {
    @Inject(method = {"render", "submit"}, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kingdomcomecombat$hideFemaleGenderFirstPersonLayer(CallbackInfo ci) {
        if (FirstPersonBodyRenderOffsetContext.isRenderingLocalFirstPersonBody()) {
            ci.cancel();
        }
    }
}
