package com.kingdomcomecombat.projectile;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class ProjectileImpactHandler {
    private static final int STUCK_ARROW_TICKS = 1200;
    private static final int MAX_STUCK_ARROWS = 8;

    private ProjectileImpactHandler() {
    }

    public static boolean hasImpactKnockback(ServerWorld world, ProjectileEntity projectile, LivingEntity target) {
        if (!(projectile instanceof PersistentProjectileEntity persistentProjectile)) {
            return false;
        }

        ItemStack weapon = persistentProjectile.getWeaponStack();
        if (weapon == null || weapon.isEmpty()) {
            return false;
        }

        DamageSource source = world.getDamageSources().arrow(persistentProjectile, persistentProjectile.getOwner());
        return EnchantmentHelper.modifyKnockback(world, weapon, target, source, 0.0F) > 0.0F;
    }

    public static void removeProjectileKnockback(
            ServerWorld world,
            ProjectileEntity projectile,
            LivingEntity target,
            Vec3d velocityBeforeDamage
    ) {
        if (hasImpactKnockback(world, projectile, target)) {
            return;
        }

        target.setVelocity(velocityBeforeDamage);
        target.velocityModified = true;
    }

    public static void stickArrowInTarget(ProjectileEntity projectile, LivingEntity target) {
        if (!(projectile instanceof ArrowEntity) && !(projectile instanceof SpectralArrowEntity)) {
            return;
        }

        target.setStuckArrowCount(Math.min(MAX_STUCK_ARROWS, target.getStuckArrowCount() + 1));
        target.stuckArrowTimer = STUCK_ARROW_TICKS;
    }
}
