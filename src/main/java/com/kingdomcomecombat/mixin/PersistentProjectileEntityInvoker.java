package com.kingdomcomecombat.mixin;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PersistentProjectileEntity.class)
public interface PersistentProjectileEntityInvoker {
    @Invoker("onEntityHit")
    void kingdomcomecombat$invokeOnEntityHit(EntityHitResult hitResult);
}
