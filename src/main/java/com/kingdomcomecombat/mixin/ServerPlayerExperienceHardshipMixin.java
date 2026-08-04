package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.hardship.HardshipEffects;
import com.kingdomcomecombat.passive.PlayerPassiveSkillProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerExperienceHardshipMixin {
    @ModifyVariable(method = "addExperience", at = @At("HEAD"), argsOnly = true)
    private int kingdomcomecombat$reduceHardshipExperience(int amount) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        int scaledAmount = HardshipEffects.scaleExperienceGain(player, amount);
        PlayerPassiveSkillProgress.addCombatExperienceFromVanilla(player, scaledAmount);
        return scaledAmount;
    }
}
