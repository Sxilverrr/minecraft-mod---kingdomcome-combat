package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.config.CombatServerConfig;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractSkeletonEntity.class)
public abstract class SkeletonBodyAimMixin {
    private static final double BODY_AIM_HEIGHT = 1.0 / 3.0;
    private static final double HEAD_AIM_HEIGHT = 0.85;

    @ModifyArg(
            method = "shootAt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getBodyY(D)D"
            ),
            index = 0
    )
    private double kingdomcomecombat$chooseBodyOrHeadAim(double vanillaHeight) {
        if (!CombatServerConfig.skeletonBodyAimEnabled()) {
            return HEAD_AIM_HEIGHT;
        }
        AbstractSkeletonEntity skeleton = (AbstractSkeletonEntity) (Object) this;
        return skeleton.getRandom().nextFloat() < 0.70F ? BODY_AIM_HEIGHT : HEAD_AIM_HEIGHT;
    }
}
