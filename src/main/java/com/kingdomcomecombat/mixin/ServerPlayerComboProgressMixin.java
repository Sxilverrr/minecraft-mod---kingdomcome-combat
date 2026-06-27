package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.combat.PlayerComboProgress;
import com.kingdomcomecombat.passive.PlayerPassiveSkillProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerComboProgressMixin {
    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void kingdomcomecombat$readComboProgress(ReadView view, CallbackInfo ci) {
        PlayerComboProgress.load((ServerPlayerEntity) (Object) this, view);
        PlayerPassiveSkillProgress.load((ServerPlayerEntity) (Object) this, view);
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void kingdomcomecombat$writeComboProgress(WriteView view, CallbackInfo ci) {
        PlayerComboProgress.save((ServerPlayerEntity) (Object) this, view);
        PlayerPassiveSkillProgress.save((ServerPlayerEntity) (Object) this, view);
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void kingdomcomecombat$copyComboProgress(
            ServerPlayerEntity oldPlayer,
            boolean alive,
            CallbackInfo ci
    ) {
        PlayerComboProgress.copy(oldPlayer, (ServerPlayerEntity) (Object) this);
        PlayerComboProgress.sync((ServerPlayerEntity) (Object) this);
        PlayerPassiveSkillProgress.copy(oldPlayer, (ServerPlayerEntity) (Object) this);
        PlayerPassiveSkillProgress.sync((ServerPlayerEntity) (Object) this);
    }
}
