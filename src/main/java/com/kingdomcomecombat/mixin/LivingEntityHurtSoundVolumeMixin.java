package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.config.CombatServerConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtSoundVolumeMixin {
    @Shadow
    protected abstract SoundEvent getHurtSound(DamageSource source);

    @Shadow
    protected abstract float getSoundVolume();

    @Shadow
    public abstract float getSoundPitch();

    @Inject(method = "playHurtSound", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$scaleVanillaHurtSound(DamageSource source, CallbackInfo ci) {
        double multiplier = CombatServerConfig.vanillaHurtSoundVolumeMultiplier();
        if (Math.abs(multiplier - 1.0) < 0.001) {
            return;
        }

        SoundEvent soundEvent = getHurtSound(source);
        if (soundEvent != null && multiplier > 0.0) {
            LivingEntity self = (LivingEntity) (Object) this;
            self.getWorld().playSound(
                    null,
                    self.getX(),
                    self.getY(),
                    self.getZ(),
                    soundEvent,
                    self.getSoundCategory(),
                    getSoundVolume() * (float) multiplier,
                    getSoundPitch()
            );
        }
        ci.cancel();
    }
}
