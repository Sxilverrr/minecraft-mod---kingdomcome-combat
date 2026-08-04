package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.ClientDodgeAnimationState;
import com.kingdomcomecombat.client.animation.MobStanceHeadTargeting;
import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.client.animation.VanillaSkeletonGeckoAnimationApplier;
import net.minecraft.client.render.entity.model.SkeletonEntityModel;
import net.minecraft.client.render.entity.state.SkeletonEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonEntityModel.class)
public class SkeletonEntityModelAnimationMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/SkeletonEntityRenderState;)V", at = @At("TAIL"))
    private void kingdomcomecombat$applyGeckoLikeCombatAnimation(
            SkeletonEntityRenderState state,
            CallbackInfo ci
    ) {
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        VanillaSkeletonGeckoAnimationApplier.apply(
                (SkeletonEntityModel<SkeletonEntityRenderState>) (Object) this,
                state
        );
        SkeletonEntityModel<SkeletonEntityRenderState> model =
                (SkeletonEntityModel<SkeletonEntityRenderState>) (Object) this;
        if (ClientEntityGeckoAnimationState.isStanceOnly(entityId)) {
            MobStanceHeadTargeting.apply(entityId, state, model.head, model.body);
        }
        ClientDodgeAnimationState.applyBiped(entityId, model.body, model.head, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg);
        ClientModelHurtboxCache.update(
                entityId,
                model.head,
                model.body,
                model.rightArm,
                model.leftArm,
                model.rightLeg,
                model.leftLeg
        );
    }
}
