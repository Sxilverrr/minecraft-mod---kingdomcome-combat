package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.LegacyBipedAnimationApplier;
import net.minecraft.client.render.entity.model.AbstractZombieModel;
import net.minecraft.entity.mob.HostileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractZombieModel.class)
public class LegacyZombieEntityModelAnimationMixin {
    @Inject(method = "setAngles", at = @At("TAIL"))
    private void kingdomcomecombat$applyLegacyCombatPose(
            HostileEntity entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!ClientEntityGeckoAnimationState.hasActiveCombatLayer(entity.getId())) {
            return;
        }
        LegacyBipedAnimationApplier.apply(
                (net.minecraft.client.render.entity.model.BipedEntityModel) (Object) this, entity);
    }
}
