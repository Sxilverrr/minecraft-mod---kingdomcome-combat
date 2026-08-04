package com.kingdomcomecombat.mixin;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragonPart.class)
public abstract class EnderDragonPartScaleMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void kingdomcomecombat$scaleDragonPart(
            EntityPose pose,
            CallbackInfoReturnable<EntityDimensions> cir
    ) {
        if (com.kingdomcomecombat.config.CombatServerConfig.enderDragonOverhaulEnabled()) {
            cir.setReturnValue(cir.getReturnValue().scaled(1.3F));
        }
    }
}
