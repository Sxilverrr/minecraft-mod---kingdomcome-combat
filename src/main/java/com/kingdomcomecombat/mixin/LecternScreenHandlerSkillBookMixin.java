package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.item.SkillBookItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternScreenHandler.class)
public class LecternScreenHandlerSkillBookMixin {
    @Inject(method = "onButtonClick", at = @At("RETURN"))
    private void kingdomcomecombat$learnSkillBookOnLecternSecondPage(
            PlayerEntity player,
            int id,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LecternScreenHandler handler = (LecternScreenHandler) (Object) this;
        if (!cir.getReturnValueZ()
                || handler.getPage() < 1
                || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        SkillBookItem.learnFromStack(serverPlayer, handler.getBookItem());
    }
}
