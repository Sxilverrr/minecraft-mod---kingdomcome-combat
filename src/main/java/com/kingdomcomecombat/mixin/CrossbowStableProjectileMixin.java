package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.equipment.RangedWeaponAttributes;
import com.kingdomcomecombat.equipment.RangedWeaponAttributesRegistry;
import com.kingdomcomecombat.equipment.RangedWeaponUsage;
import com.kingdomcomecombat.projectile.StableProjectileAccess;
import com.kingdomcomecombat.passive.PassiveSkillPerks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public class CrossbowStableProjectileMixin {
    private static final Identifier STABLE_ENCHANTMENT_ID = Identifier.of(KingdomComeCombat.MOD_ID, "stable");
    private static final Identifier POWERFUL_ENCHANTMENT_ID = Identifier.of(KingdomComeCombat.MOD_ID, "powerful");

    @Inject(method = "getPullTime", at = @At("RETURN"), cancellable = true)
    private static void kingdomcomecombat$applyDrawSpeed(
            ItemStack stack,
            LivingEntity user,
            CallbackInfoReturnable<Integer> cir
    ) {
        double speed = RangedWeaponAttributesRegistry.get(stack).drawSpeed();
        cir.setReturnValue(Math.max(1, (int) Math.ceil(cir.getReturnValueI() / speed)));
    }

    @Inject(method = "usageTick", at = @At("HEAD"))
    private void kingdomcomecombat$consumeDrawStamina(
            World world,
            LivingEntity user,
            ItemStack stack,
            int remainingUseTicks,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    ) {
        if (!world.isClient()) {
            RangedWeaponUsage.consumeDrawStamina(user, stack);
        }
    }

    @Inject(method = "createArrowEntity", at = @At("RETURN"))
    private void kingdomcomecombat$markStableCrossbowProjectile(
            World world,
            LivingEntity shooter,
            ItemStack weaponStack,
            ItemStack projectileStack,
            boolean critical,
            CallbackInfoReturnable<ProjectileEntity> cir
    ) {
        int level = kingdomcomecombat$enchantmentLevel(weaponStack, STABLE_ENCHANTMENT_ID);
        ProjectileEntity projectile = cir.getReturnValue();
        if (level <= 0 || !(projectile instanceof StableProjectileAccess stableProjectile)) {
            return;
        }

        stableProjectile.kingdomcomecombat$setStableLevel(level);
    }

    @Inject(method = "shoot", at = @At("TAIL"))
    private void kingdomcomecombat$increasePowerfulProjectileSpeed(
            LivingEntity shooter,
            ProjectileEntity projectile,
            int index,
            float speed,
            float divergence,
            float yaw,
            LivingEntity target,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    ) {
        if (!(projectile instanceof PersistentProjectileEntity persistentProjectile)) {
            RangedWeaponAttributes attributes = RangedWeaponAttributesRegistry.get(shooter.getActiveItem());
            projectile.setVelocity(projectile.getVelocity().multiply(attributes.projectileSpeed()));
            return;
        }

        RangedWeaponAttributes attributes = RangedWeaponAttributesRegistry.get(persistentProjectile.getWeaponStack());
        projectile.setVelocity(projectile.getVelocity().multiply(attributes.projectileSpeed()));

        ItemStack weapon = persistentProjectile.getWeaponStack();
        double spreadMultiplier = PassiveSkillPerks.projectileSpreadMultiplier(shooter, weapon);
        double speedMultiplier = PassiveSkillPerks.projectileSpeedMultiplier(shooter, weapon);
        Vec3d velocity = projectile.getVelocity();
        if (velocity.lengthSquared() > 0.000001) {
            Vec3d intended = shooter.getRotationVec(1.0F).rotateY((float) Math.toRadians(-yaw));
            Vec3d corrected = intended.add(velocity.normalize().subtract(intended).multiply(spreadMultiplier));
            if (corrected.lengthSquared() > 0.000001) {
                projectile.setVelocity(corrected.normalize().multiply(velocity.length() * speedMultiplier));
            }
        }

        int level = kingdomcomecombat$enchantmentLevel(
                weapon,
                POWERFUL_ENCHANTMENT_ID
        );
        if (level <= 0) {
            return;
        }

        projectile.setVelocity(projectile.getVelocity().multiply(1.0 + level * 0.12));
    }

    private static int kingdomcomecombat$enchantmentLevel(ItemStack stack, Identifier enchantmentId) {
        if (stack == null) {
            return 0;
        }

        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return 0;
        }

        int level = 0;
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(enchantmentId)) {
                level = Math.max(level, entry.getIntValue());
            }
        }

        return level;
    }
}
