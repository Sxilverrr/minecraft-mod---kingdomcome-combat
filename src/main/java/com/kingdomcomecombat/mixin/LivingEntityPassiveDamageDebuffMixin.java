package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.passive.PassiveSkillPerks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityPassiveDamageDebuffMixin {
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float kingdomcomecombat$applyExecutionFearDamagePenalty(
            float amount,
            ServerWorld world,
            DamageSource source
    ) {
        if (source.getAttacker() instanceof LivingEntity attacker) {
            return (float) (amount * PassiveSkillPerks.outgoingDamageDebuffMultiplier(attacker));
        }
        return amount;
    }
}
