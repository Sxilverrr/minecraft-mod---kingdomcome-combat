package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.potion.PotionCoatingHandler;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileDragonBreathTrailMixin {

    @Shadow
    protected abstract boolean isInGround();

    @Inject(method = "tick", at = @At("TAIL"))
    private void kingdomcomecombat$dragonBreathTrail(CallbackInfo ci) {
        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;

        if (!(projectile.getWorld() instanceof ServerWorld world)
                || this.isInGround()
                || !PotionCoatingHandler.isDragonBreathArrow(projectile)) {
            return;
        }

        world.spawnParticles(
                ParticleTypes.DRAGON_BREATH,
                projectile.getX(),
                projectile.getY(),
                projectile.getZ(),
                2,
                0.025,
                0.025,
                0.025,
                0.005
        );
    }
}