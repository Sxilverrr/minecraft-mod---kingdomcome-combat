package com.kingdomcomecombat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public class PersistentProjectileCritTrailMixin {
    @Unique
    private int kingdomcomecombat$crossbowCritParticleIndex;

    @Inject(method = "tick", at = @At("HEAD"))
    private void kingdomcomecombat$resetCritTrailIndex(CallbackInfo ci) {
        kingdomcomecombat$crossbowCritParticleIndex = 0;
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;addParticleClient(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"
            )
    )
    private void kingdomcomecombat$alignCrossbowCritTrail(
            World world,
            ParticleEffect particle,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            Operation<Void> original
    ) {
        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
        net.minecraft.item.ItemStack weapon = projectile.getWeaponStack();
        if (particle != ParticleTypes.CRIT
                || weapon == null
                || !(weapon.getItem() instanceof CrossbowItem)) {
            original.call(world, particle, x, y, z, velocityX, velocityY, velocityZ);
            return;
        }

        Vec3d motion = projectile.getVelocity();
        double trailFraction = (++kingdomcomecombat$crossbowCritParticleIndex) / 4.0;
        Vec3d position = projectile.getPos().subtract(motion.multiply(trailFraction));
        original.call(
                world,
                particle,
                position.x,
                position.y,
                position.z,
                velocityX,
                velocityY,
                velocityZ
        );
    }
}
