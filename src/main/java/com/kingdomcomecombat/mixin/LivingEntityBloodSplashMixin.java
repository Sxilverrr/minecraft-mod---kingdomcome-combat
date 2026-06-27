package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.equipment.BloodiedEquipment;
import com.kingdomcomecombat.equipment.BloodSplashConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityBloodSplashMixin {
    @Inject(method = "damage", at = @At("RETURN"))
    private void kingdomcomecombat$splashBlood(
            ServerWorld world,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValue()) {
            BloodiedEquipment.splashBlood(
                    world,
                    (LivingEntity) (Object) this,
                    amount,
                    BloodSplashConfig.multiplier(source)
            );
        }
    }
}
