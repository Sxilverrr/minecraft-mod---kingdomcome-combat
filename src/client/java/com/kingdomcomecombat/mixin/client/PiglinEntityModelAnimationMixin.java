package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.ClientDodgeAnimationState;
import com.kingdomcomecombat.client.animation.MobStanceHeadTargeting;
import com.kingdomcomecombat.client.animation.VanillaSkeletonGeckoAnimationApplier;
import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import net.minecraft.client.render.entity.model.PiglinEntityModel;
import net.minecraft.client.render.entity.state.PiglinEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinEntityModel.class)
public class PiglinEntityModelAnimationMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PiglinEntityRenderState;)V", at = @At("TAIL"))
    private void kingdomcomecombat$applyGeckoLikeCombatAnimation(
            PiglinEntityRenderState state,
            CallbackInfo ci
    ) {
        PiglinEntityModel model = (PiglinEntityModel) (Object) this;
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        VanillaSkeletonGeckoAnimationApplier.applyBiped(model, state);
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
