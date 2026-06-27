package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.equipment.BowDrawSpeedState;
import com.kingdomcomecombat.equipment.RangedWeaponAttributes;
import com.kingdomcomecombat.equipment.RangedWeaponAttributesRegistry;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public class BowRapidFireMixin {

    @WrapOperation(
            method = "onStoppedUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/BowItem;getPullProgress(I)F"
            )
    )
    private float kingdomcomecombat$applyRapidFireDrawSpeed(
            int useTicks,
            Operation<Float> original,
            ItemStack stack,
            World world,
            LivingEntity user,
            int remainingUseTicks
    ) {
        return original.call(Math.round(
                useTicks * BowDrawSpeedState.getDrawSpeed(stack, user, world)
        ));
    }

    @Inject(method = "shoot", at = @At("TAIL"))
    private void kingdomcomecombat$applyProjectileSpeed(
            LivingEntity shooter,
            ProjectileEntity projectile,
            int index,
            float speed,
            float divergence,
            float yaw,
            LivingEntity target,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    ) {
        ItemStack weapon = projectile instanceof net.minecraft.entity.projectile.PersistentProjectileEntity persistent
                ? persistent.getWeaponStack()
                : shooter.getActiveItem();
        double multiplier = RangedWeaponAttributesRegistry.get(weapon).projectileSpeed();
        projectile.setVelocity(projectile.getVelocity().multiply(multiplier));
    }

    @Inject(method = "onStoppedUsing", at = @At("RETURN"))
    private void kingdomcomecombat$grantRapidFireAfterShot(
            ItemStack stack,
            World world,
            LivingEntity user,
            int remainingUseTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        int usedTicks = stack.getMaxUseTime(user) - remainingUseTicks;
        boolean clientShot = world.isClient()
                && BowItem.getPullProgress(Math.round(
                usedTicks * BowDrawSpeedState.getDrawSpeed(stack, user, world)
        )) > 0.1F;
        if (!cir.getReturnValueZ() && !clientShot) {
            return;
        }

        BowDrawSpeedState.grantAfterShot(stack, user, world);
    }
}
