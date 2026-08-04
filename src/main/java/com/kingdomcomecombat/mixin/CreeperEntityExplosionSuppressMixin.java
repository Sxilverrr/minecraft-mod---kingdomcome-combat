package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.combat.CreeperExplosionSuppressor;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityExplosionSuppressMixin {
    @Shadow
    @Final
    private static TrackedData<Boolean> IGNITED;

    @Shadow
    private int currentFuseTime;

    @Shadow
    public abstract void setFuseSpeed(int fuseSpeed);

    @Inject(method = "tick", at = @At("HEAD"))
    private void kingdomcomecombat$suppressExplosionAttempt(CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity) (Object) this;
        if (!CreeperExplosionSuppressor.consumeTick(creeper)) {
            return;
        }
        setFuseSpeed(-1);
        currentFuseTime = 0;
        creeper.getDataTracker().set(IGNITED, false);
    }
}
