package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.ClientDodgeAnimationState;
import com.kingdomcomecombat.client.animation.MobStanceHeadTargeting;
import com.kingdomcomecombat.client.animation.VanillaSkeletonGeckoAnimationApplier;
import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import net.minecraft.client.render.entity.model.AbstractZombieModel;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractZombieModel.class)
public class ZombieEntityModelAnimationMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/ZombieEntityRenderState;)V", at = @At("TAIL"))
    private void kingdomcomecombat$applyGeckoLikeCombatAnimation(
            ZombieEntityRenderState state,
            CallbackInfo ci
    ) {
        AbstractZombieModel<ZombieEntityRenderState> model =
                (AbstractZombieModel<ZombieEntityRenderState>) (Object) this;
        VanillaSkeletonGeckoAnimationApplier.applyBiped(model, state);
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
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
