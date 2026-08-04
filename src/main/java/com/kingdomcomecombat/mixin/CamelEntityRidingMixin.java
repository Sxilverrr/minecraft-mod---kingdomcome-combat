package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.riding.KccHorseRidingData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes CamelEntity's overridden riding methods through the shared horse controller. */
@Mixin(CamelEntity.class)
public abstract class CamelEntityRidingMixin {
    @Inject(method = "getControlledRotation", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$controlledRotation(
            LivingEntity passenger, CallbackInfoReturnable<Vec2f> cir
    ) {
        if (passenger instanceof PlayerEntity && (Object) this instanceof KccHorseRidingData data) {
            cir.setReturnValue(data.kingdomcomecombat$getControlledRotation(passenger));
        }
    }

    @Inject(method = "getControlledMovementInput", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$controlledInput(
            PlayerEntity player, Vec3d input, CallbackInfoReturnable<Vec3d> cir
    ) {
        if ((Object) this instanceof KccHorseRidingData data) {
            cir.setReturnValue(data.kingdomcomecombat$getControlledMovementInput(player));
        }
    }

    @Inject(method = "getSaddledSpeed", at = @At("RETURN"), cancellable = true)
    private void kingdomcomecombat$controlledSpeed(
            PlayerEntity player, CallbackInfoReturnable<Float> cir
    ) {
        if ((Object) this instanceof KccHorseRidingData data) {
            cir.setReturnValue(data.kingdomcomecombat$modifySaddledSpeed(player, cir.getReturnValueF()));
        }
    }
}
