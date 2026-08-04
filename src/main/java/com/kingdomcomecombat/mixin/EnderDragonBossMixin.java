package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.boss.EnderDragonBossHandler;
import com.kingdomcomecombat.boss.EnderDragonBossStateAccess;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonBossMixin implements EnderDragonBossStateAccess {
    @Unique
    private static final TrackedData<Boolean> KINGDOMCOMECOMBAT_ARMOR_BROKEN =
            DataTracker.registerData(EnderDragonEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void kingdomcomecombat$trackArmorState(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(KINGDOMCOMECOMBAT_ARMOR_BROKEN, false);
    }

    @Override
    public boolean kingdomcomecombat$isArmorBroken() {
        return ((EnderDragonEntity) (Object) this).getDataTracker().get(KINGDOMCOMECOMBAT_ARMOR_BROKEN);
    }

    @Override
    public void kingdomcomecombat$setArmorBroken(boolean broken) {
        ((EnderDragonEntity) (Object) this).getDataTracker().set(KINGDOMCOMECOMBAT_ARMOR_BROKEN, broken);
    }

    @Inject(method = "clampScale", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$increaseDragonScale(float scale, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(com.kingdomcomecombat.config.CombatServerConfig.enderDragonOverhaulEnabled()
                ? 1.3F : 1.0F);
    }
    @Inject(method = "damagePart", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$blockProtectedDamage(
            ServerWorld world, EnderDragonPart part, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        EnderDragonEntity dragon = (EnderDragonEntity) (Object) this;
        if (EnderDragonBossHandler.blocksDamage(dragon, source.getSource())) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "damagePart", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float kingdomcomecombat$applyDragonBreathArrowDamage(
            float amount, ServerWorld world, EnderDragonPart part, DamageSource source
    ) {
        return EnderDragonBossHandler.modifyArrowDamage(
                world, (EnderDragonEntity) (Object) this, source.getSource(), amount);
    }
}
