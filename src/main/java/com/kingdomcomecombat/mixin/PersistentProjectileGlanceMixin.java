package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.projectile.ProjectileGlanceState;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public class PersistentProjectileGlanceMixin {
    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void kingdomcomecombat$applyArmorGlance(EntityHitResult hitResult, CallbackInfo ci) {
        ProjectileGlanceState.applyPending((PersistentProjectileEntity) (Object) this);
    }
}
