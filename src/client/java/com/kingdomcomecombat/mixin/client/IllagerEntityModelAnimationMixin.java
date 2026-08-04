package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.animation.VanillaSkeletonGeckoAnimationApplier;
import com.kingdomcomecombat.client.animation.ClientDodgeAnimationState;
import com.kingdomcomecombat.client.animation.MobStanceHeadTargeting;
import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.client.mixin.IllagerModelPartsAccess;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.client.render.entity.state.IllagerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IllagerEntityModel.class)
public class IllagerEntityModelAnimationMixin implements IllagerModelPartsAccess {
    @Shadow private ModelPart head;
    @Shadow private ModelPart arms;
    @Shadow private ModelPart rightArm;
    @Shadow private ModelPart leftArm;
    @Shadow private ModelPart rightLeg;
    @Shadow private ModelPart leftLeg;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/IllagerEntityRenderState;)V", at = @At("TAIL"))
    private void kingdomcomecombat$applyCombatAnimation(
            IllagerEntityRenderState state,
            CallbackInfo ci
    ) {
        int entityId = ((EntityRenderStateKccAccess) state).kingdomcomecombat$getEntityId();
        boolean inCombatPose = ClientEntityGeckoAnimationState.shouldRenderCombatAnimation(entityId)
                && !ClientEntityGeckoAnimationState.getLayers(entityId).isEmpty();
        if (inCombatPose) {
            arms.visible = false;
            rightArm.visible = true;
            leftArm.visible = true;
            state.attacking = true;
        }

        VanillaSkeletonGeckoAnimationApplier.applyIllager(
                entityId,
                ((IllagerEntityModel<?>) (Object) this).getRootPart(),
                head,
                rightArm,
                leftArm,
                rightLeg,
                leftLeg
        );
        if (ClientEntityGeckoAnimationState.isStanceOnly(entityId)) {
            ModelPart root = ((IllagerEntityModel<?>) (Object) this).getRootPart();
            MobStanceHeadTargeting.apply(entityId, state, head, root, true);
        }
        ModelPart root = ((IllagerEntityModel<?>) (Object) this).getRootPart();
        ModelPart body = root.hasChild("body") ? root.getChild("body") : root;
        ClientDodgeAnimationState.applyBiped(entityId, body, head, rightArm, leftArm, rightLeg, leftLeg);
        ClientModelHurtboxCache.updateIllager(
                entityId,
                head,
                body,
                rightArm,
                leftArm,
                rightLeg,
                leftLeg
        );
    }

    @Override
    public ModelPart kingdomcomecombat$getRightArm() {
        return rightArm;
    }

    @Override
    public ModelPart kingdomcomecombat$getLeftArm() {
        return leftArm;
    }

    @Override
    public ModelPart kingdomcomecombat$getBody() {
        ModelPart root = ((IllagerEntityModel<?>) (Object) this).getRootPart();
        return root.hasChild("body") ? root.getChild("body") : null;
    }
}
