package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.LegacyBipedAnimationApplier;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.entity.mob.IllagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IllagerEntityModel.class)
public class LegacyIllagerEntityModelAnimationMixin {
    @Shadow private ModelPart head;
    @Shadow private ModelPart arms;
    @Shadow private ModelPart rightArm;
    @Shadow private ModelPart leftArm;
    @Shadow private ModelPart rightLeg;
    @Shadow private ModelPart leftLeg;

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void kingdomcomecombat$applyLegacyCombatPose(
            IllagerEntity entity,
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
        arms.visible = false;
        rightArm.visible = true;
        leftArm.visible = true;
        ModelPart root = ((IllagerEntityModel<?>) (Object) this).getPart();
        ModelPart body = root.hasChild("body") ? root.getChild("body") : root;
        LegacyBipedAnimationApplier.applyParts(
                entity, body, head, rightArm, leftArm, rightLeg, leftLeg);
    }
}
