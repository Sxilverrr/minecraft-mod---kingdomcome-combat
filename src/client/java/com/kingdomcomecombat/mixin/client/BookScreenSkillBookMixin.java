package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.ui.SkillBookClientState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BookScreen.class)
public class BookScreenSkillBookMixin {
    @Shadow
    private int pageIndex;

    @Inject(method = "setPage", at = @At("RETURN"))
    private void kingdomcomecombat$learnSkillBookOnSecondPage(
            int index,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ()) {
            SkillBookClientState.learnIfSecondPage(index);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void kingdomcomecombat$learnSkillBookWhenSecondPageIsVisible(
            DrawContext context,
            int mouseX,
            int mouseY,
            float deltaTicks,
            CallbackInfo ci
    ) {
        SkillBookClientState.learnIfSecondPage(pageIndex);
    }
}
