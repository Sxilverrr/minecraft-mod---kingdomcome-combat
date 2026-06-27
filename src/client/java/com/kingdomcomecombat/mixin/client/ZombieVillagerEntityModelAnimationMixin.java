package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.MobStanceHeadTargeting;
import com.kingdomcomecombat.client.animation.VanillaSkeletonGeckoAnimationApplier;
import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import net.minecraft.client.render.entity.model.ZombieVillagerEntityModel;
import net.minecraft.client.render.entity.state.ZombieVillagerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieVillagerEntityModel.class)
public class ZombieVillagerEntityModelAnimationMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/ZombieVillagerRenderState;)V", at = @At("TAIL"))
    private void kingdomcomecombat$applyZombieCombatAnimation(
            ZombieVillagerRenderState state,
            CallbackInfo ci
    ) {
        ZombieVillagerEntityModel<ZombieVillagerRenderState> model =
                (ZombieVillagerEntityModel<ZombieVillagerRenderState>) (Object) this;
        VanillaSkeletonGeckoAnimationApplier.applyBiped(model, state);
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
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
