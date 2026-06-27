package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.equipment.BloodiedEntityAccess;
import com.kingdomcomecombat.equipment.BloodiedEquipment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityBloodiedBodyMixin implements BloodiedEntityAccess {
    @Unique
    private static final TrackedData<Integer> KINGDOMCOMECOMBAT_BODY_BLOOD =
            DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique
    private static final double KINGDOMCOMECOMBAT_MIN_BODY_BLOOD = 0.20;
    @Unique
    private static final double KINGDOMCOMECOMBAT_FADE_PER_TICK = 0.80 / (7.0 * 60.0 * 20.0);
    @Unique
    private static final double KINGDOMCOMECOMBAT_LOW_BLOOD_FADE_PER_TICK =
            KINGDOMCOMECOMBAT_MIN_BODY_BLOOD / (15.0 * 60.0 * 20.0);
    @Unique
    private static final double KINGDOMCOMECOMBAT_TRACKED_SCALE = 10000.0;
    @Unique
    private static final String KINGDOMCOMECOMBAT_BODY_BLOOD_KEY =
            "kingdom_come_combat_body_blood_percent";

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void kingdomcomecombat$initBodyBlood(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(KINGDOMCOMECOMBAT_BODY_BLOOD, 0);
    }

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void kingdomcomecombat$tickBodyBlood(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.deathTime > 0 || self.isDead()) {
            return;
        }

        com.kingdomcomecombat.equipment.BloodiedEquipment.tickBloodDecay(self);
        double blood = kingdomcomecombat$getBodyBloodPercent();
        if (blood <= 0.0) {
            return;
        }

        if (self.isTouchingWater() && blood > KINGDOMCOMECOMBAT_MIN_BODY_BLOOD) {
            kingdomcomecombat$setBodyBloodPercent(Math.min(blood, KINGDOMCOMECOMBAT_MIN_BODY_BLOOD));
            return;
        }

        if (blood > KINGDOMCOMECOMBAT_MIN_BODY_BLOOD) {
            kingdomcomecombat$setBodyBloodPercent(Math.max(
                    KINGDOMCOMECOMBAT_MIN_BODY_BLOOD,
                    blood - KINGDOMCOMECOMBAT_FADE_PER_TICK
            ));
        } else {
            kingdomcomecombat$setBodyBloodPercent(Math.max(
                    0.0,
                    blood - KINGDOMCOMECOMBAT_LOW_BLOOD_FADE_PER_TICK
            ));
        }
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void kingdomcomecombat$writeBodyBlood(WriteView view, CallbackInfo ci) {
        double blood = kingdomcomecombat$getBodyBloodPercent();
        if (blood > 0.0) {
            view.putDouble(KINGDOMCOMECOMBAT_BODY_BLOOD_KEY, blood);
        }
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void kingdomcomecombat$readBodyBlood(ReadView view, CallbackInfo ci) {
        double blood = view.getDouble(KINGDOMCOMECOMBAT_BODY_BLOOD_KEY, 0.0);
        if (blood > 0.0) {
            kingdomcomecombat$setBodyBloodPercent(blood);
        }
    }

    @Override
    public double kingdomcomecombat$getBodyBloodPercent() {
        LivingEntity self = (LivingEntity) (Object) this;
        return Math.max(
                0.0,
                Math.min(
                        BloodiedEquipment.MAX_BLOOD_PERCENT,
                        self.getDataTracker().get(KINGDOMCOMECOMBAT_BODY_BLOOD) / KINGDOMCOMECOMBAT_TRACKED_SCALE
                )
        );
    }

    @Override
    public void kingdomcomecombat$addBodyBloodPercent(double percent) {
        if (percent <= 0.0) {
            return;
        }
        kingdomcomecombat$setBodyBloodPercent(Math.min(
                BloodiedEquipment.MAX_BLOOD_PERCENT,
                kingdomcomecombat$getBodyBloodPercent() + percent
        ));
    }

    @Unique
    private void kingdomcomecombat$setBodyBloodPercent(double percent) {
        LivingEntity self = (LivingEntity) (Object) this;
        int value = (int) Math.round(
                Math.max(0.0, Math.min(BloodiedEquipment.MAX_BLOOD_PERCENT, percent))
                        * KINGDOMCOMECOMBAT_TRACKED_SCALE
        );
        self.getDataTracker().set(KINGDOMCOMECOMBAT_BODY_BLOOD, value);
    }
}
