package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.potion.PotionCoatingHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndermanEntity.class)
public abstract class EndermanDragonBreathArrowMixin {
    @Shadow
    protected abstract boolean teleportRandomly();

    private static final RegistryKey<DamageType> KCC_CUSTOM_COMBAT_DAMAGE = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE, Identifier.of(KingdomComeCombat.MOD_ID, "custom_combat"));

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private DamageSource kingdomcomecombat$allowDragonBreathArrow(
            DamageSource source, ServerWorld world
    ) {
        if (source.getSource() instanceof ProjectileEntity projectile
                && PotionCoatingHandler.isDragonBreathArrow(projectile)) {
            return world.getDamageSources().create(
                    KCC_CUSTOM_COMBAT_DAMAGE, projectile,
                    projectile.getOwner() == null ? projectile : projectile.getOwner());
        }
        return source;
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float kingdomcomecombat$addDragonBreathTrueDamage(
            float amount, ServerWorld world, DamageSource source
    ) {
        return source.getSource() instanceof ProjectileEntity projectile
                && PotionCoatingHandler.isDragonBreathArrow(projectile)
                ? amount + 2.0F
                : amount;
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$avoidDragonDamage(
            ServerWorld world, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (com.kingdomcomecombat.config.CombatServerConfig.enderDragonOverhaulEnabled()
                && source.getAttacker() instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity) {
            teleportRandomly();
            cir.setReturnValue(false);
        }
    }
}
