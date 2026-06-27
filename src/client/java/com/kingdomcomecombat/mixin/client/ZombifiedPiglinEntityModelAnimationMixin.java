package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.MobStanceHeadTargeting;
import com.kingdomcomecombat.client.animation.VanillaSkeletonGeckoAnimationApplier;
import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import net.minecraft.client.render.entity.model.ZombifiedPiglinEntityModel;
import net.minecraft.client.render.entity.state.ZombifiedPiglinEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombifiedPiglinEntityModel.class)
public class ZombifiedPiglinEntityModelAnimationMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/ZombifiedPiglinEntityRenderState;)V", at = @At("TAIL"))
    private void kingdomcomecombat$applyGeckoLikeCombatAnimation(
            ZombifiedPiglinEntityRenderState state,
            CallbackInfo ci
    ) {
        ZombifiedPiglinEntityModel model = (ZombifiedPiglinEntityModel) (Object) this;
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        VanillaSkeletonGeckoAnimationApplier.applyBiped(model, state);
        if (ClientEntityGeckoAnimationState.isStanceOnly(entityId)) {
            MobStanceHeadTargeting.apply(entityId, state, model.head, model.body);
        }
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
