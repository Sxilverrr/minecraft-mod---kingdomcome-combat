package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.collision.ServerHitDetectionSystem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class SpearKineticDamageMixin {
    @WrapOperation(
            method = "pierce",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z")
    )
    private boolean kingdomcomecombat$applySpearCombatRules(
            Entity target, ServerWorld world, DamageSource source, float damage,
            Operation<Boolean> original
    ) {
        LivingEntity attacker = (LivingEntity) (Object) this;
        if (ServerHitDetectionSystem.tryHandleSpearCharge(world, attacker, target, damage)) {
            return true;
        }
        return original.call(target, world, source, damage);
    }
}
